package com.antoniegil.astronia.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataManager {
    
    fun backupHistory(context: Context): Pair<Boolean, String?> {
        return try {
            val prefManager = SettingsManager.getInstance(context)
            val historyList = prefManager.getHistory()
            
            if (historyList.isEmpty()) {
                return Pair(false, null)
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
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "astronia_backup_$timestamp.json"
            val jsonContent = JSONArray(SettingsManager.serializeHistoryToJson(historyWithFiles)).toString(2)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray())
                    }
                    val downloadPath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
                    Pair(true, downloadPath)
                } ?: Pair(false, null)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val backupFile = File(downloadsDir, fileName)
                FileOutputStream(backupFile).use { outputStream ->
                    outputStream.write(jsonContent.toByteArray())
                }
                
                Pair(true, backupFile.absolutePath)
            }
        } catch (e: Exception) {
            ErrorHandler.logError("DataManager", "Failed to backup history", e)
            Pair(false, null)
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
