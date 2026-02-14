package com.antoniegil.astronia.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.antoniegil.astronia.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
        val prefManager = SettingsManager.getInstance(context)
        val historyList = prefManager.getHistory()
        
        if (historyList.isEmpty()) {
            return null
        }
        
        val jsonArray = JSONArray()
        historyList.forEach { item ->
            val jsonObj = JSONObject().apply {
                put("name", item.name)
                put("timestamp", item.timestamp)
                item.lastChannelUrl?.let { put("lastChannelUrl", it) }
                item.lastChannelId?.let { put("lastChannelId", it) }
                
                val m3uContent = if (item.url.startsWith("http://") || item.url.startsWith("https://")) {
                    item.url
                } else {
                    val uri = item.url.toUri()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().use { it.readText() }
                    } ?: item.url
                }
                put("content", m3uContent)
            }
            jsonArray.put(jsonObj)
        }
        
        return jsonArray.toString(2)
    }
    
    fun writeBackupToUri(context: Context, uri: Uri, content: String): Boolean {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        }
        return true
    }
    
    fun restoreHistory(context: Context, uri: Uri): Pair<Boolean, Int> {
        val inputStream = context.contentResolver.openInputStream(uri)
        val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
        inputStream?.close()
        
        if (jsonString.isNullOrEmpty()) {
            return Pair(false, 0)
        }
        
        val jsonArray = JSONArray(jsonString)
        val historyItems = mutableListOf<HistoryItem>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonObj = jsonArray.getJSONObject(i)
            
            val content = jsonObj.getString("content")
            val itemUrl = if (content.startsWith("http://") || content.startsWith("https://")) {
                content
            } else {
                val restoreDir = File(context.filesDir, "restored_files")
                if (!restoreDir.exists()) {
                    restoreDir.mkdirs()
                }
                
                val fileName = "${System.currentTimeMillis()}_${jsonObj.getString("name").replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.m3u"
                val destFile = File(restoreDir, fileName)
                destFile.writeText(content)
                
                "file://${destFile.absolutePath}"
            }
            
            historyItems.add(
                HistoryItem(
                    url = itemUrl,
                    name = jsonObj.getString("name"),
                    timestamp = jsonObj.getLong("timestamp"),
                    lastChannelUrl = if (jsonObj.has("lastChannelUrl")) jsonObj.getString("lastChannelUrl") else null,
                    lastChannelId = if (jsonObj.has("lastChannelId")) jsonObj.getString("lastChannelId") else null
                )
            )
        }
        
        if (historyItems.isEmpty()) {
            return Pair(false, 0)
        }
        
        val prefManager = SettingsManager.getInstance(context)
        val existingHistory = prefManager.getHistory()
        val existingUrls = existingHistory.map { it.url }.toSet()
        
        prefManager.clearHistory()
        
        val mergedItems = mutableListOf<HistoryItem>()
        mergedItems.addAll(existingHistory)
        
        historyItems.forEach { item ->
            if (!existingUrls.contains(item.url)) {
                mergedItems.add(item)
            }
        }
        
        mergedItems.sortByDescending { it.timestamp }
        
        var successCount = 0
        mergedItems.forEach { item ->
            prefManager.addOrUpdateHistory(
                item.url,
                item.name,
                item.lastChannelUrl,
                item.lastChannelId
            )
            successCount++
        }
        
        return Pair(true, successCount)
    }
}

@Composable
fun rememberBackupExportLauncher(
    backupContent: String,
    onComplete: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val successMsg = stringResource(R.string.export_success)
    val failedMsg = stringResource(R.string.export_failed)
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val success = DataManager.writeBackupToUri(context, it, backupContent)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (success) successMsg else failedMsg,
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
    val failedMsg = stringResource(R.string.restore_failed)
    
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
                    @Suppress("LocalContextResourcesRead")
                    val message = if (success) {
                        context.resources.getQuantityString(R.plurals.restore_success, count, count)
                    } else {
                        failedMsg
                    }
                    Toast.makeText(
                        context,
                        message,
                        if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    ).show()
                    onComplete(success, count)
                }
            }
        }
    }
    
    return Pair(launcher, mimeType)
}
