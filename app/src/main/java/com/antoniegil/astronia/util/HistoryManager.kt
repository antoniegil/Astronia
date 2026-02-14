package com.antoniegil.astronia.util

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

object HistoryManager {
    
    val historyFlow: StateFlow<List<HistoryItem>>
        get() = SettingsManager.historyFlow
    
    fun getHistory(context: Context): List<HistoryItem> {
        return SettingsManager.getInstance(context).getHistory()
    }
    
    fun addOrUpdateHistory(context: Context, url: String, name: String, lastChannelUrl: String? = null, lastChannelId: String? = null, logoUrl: String = "") {
        SettingsManager.getInstance(context).addOrUpdateHistory(url, name, lastChannelUrl, lastChannelId, logoUrl)
    }
    
    fun deleteHistoryItem(context: Context, item: HistoryItem) {
        SettingsManager.getInstance(context).deleteHistoryItem(item)
    }
    
    suspend fun deleteHistoryItemWithUndo(
        context: Context,
        item: HistoryItem,
        snackbarHostState: androidx.compose.material3.SnackbarHostState,
        resources: android.content.res.Resources
    ) {
        deleteHistoryItem(context, item)
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(com.antoniegil.astronia.R.string.item_deleted),
            actionLabel = resources.getString(com.antoniegil.astronia.R.string.undo),
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            addOrUpdateHistory(
                context,
                item.url,
                item.name,
                item.lastChannelUrl,
                item.lastChannelId
            )
        }
    }
}
