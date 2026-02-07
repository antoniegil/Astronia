package com.antoniegil.astronia.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.antoniegil.astronia.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataManager {
    
    fun getBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "astronia_backup_$timestamp.json"
    }
    
    fun prepareBackupContent(context: Context): String? {
        return try {
            val prefManager = SettingsManager.getInstance(context)
            val historyList = prefManager.getHistory()
            
            if (historyList.isEmpty()) {
                return null
            }
            
            val backupDir = File(context.filesDir, "backup_files")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val historyWithFiles = historyList.map { item ->
                if (item.url.startsWith("content://")) {
                    try {
                        val uri = item.url.toUri()
                        val fileName = "${System.currentTimeMillis()}_${item.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}"
                        val destFile = File(backupDir, fileName)
                        
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        item.copy(url = "file://${destFile.absolutePath}")
                    } catch (e: Exception) {
                        ErrorHandler.logError("DataManager", "Failed to copy local file: ${item.url}", e)
                        item
                    }
                } else {
                    item
                }
            }
            
            JSONArray(SettingsManager.serializeHistoryToJson(historyWithFiles)).toString(2)
        } catch (e: Exception) {
            ErrorHandler.logError("DataManager", "Failed to prepare backup", e)
            null
        }
    }
    
    fun writeBackupToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            ErrorHandler.logError("DataManager", "Failed to write backup", e)
            false
        }
    }
    
    fun restoreHistory(context: Context, uri: Uri): Pair<Boolean, Int> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            inputStream?.close()
            
            if (jsonString.isNullOrEmpty()) {
                return Pair(false, 0)
            }
            
            val prefManager = SettingsManager.getInstance(context)
            val historyItems = SettingsManager.parseHistoryJson(jsonString)
            
            prefManager.clearHistory()
            
            var successCount = 0
            historyItems.forEach { item ->
                prefManager.addOrUpdateHistory(
                    item.url,
                    item.name,
                    item.lastChannelUrl,
                    item.lastChannelId
                )
                successCount++
            }
            
            Pair(true, successCount)
        } catch (e: Exception) {
            ErrorHandler.logError("DataManager", "Failed to restore history", e)
            Pair(false, 0)
        }
    }
}

@Composable
fun rememberBackupExportLauncher(
    backupContent: String,
    onComplete: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val success = DataManager.writeBackupToUri(context, it, backupContent)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(if (success) R.string.export_success else R.string.export_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete()
                }
            }
        }
    }
    
    return { launcher.launch(DataManager.getBackupFilename()) }
}

@Composable
fun rememberHistoryRestoreLauncher(
    onComplete: (Boolean, Int) -> Unit = { _, _ -> }
): Pair<androidx.activity.result.ActivityResultLauncher<String>, String> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val mimeType = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        "*/*"
    } else {
        "application/json"
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val (success, count) = withContext(Dispatchers.IO) {
                    DataManager.restoreHistory(context, it)
                }
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(R.plurals.restore_success, count, count),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.restore_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    onComplete(success, count)
                }
            }
        }
    }
    
    return Pair(launcher, mimeType)
}
