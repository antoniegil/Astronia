package com.antoniegil.astronia.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback
import com.antoniegil.astronia.util.M3U8Channel
import kotlinx.coroutines.launch

@Composable
fun ChannelListSection(
    channels: List<M3U8Channel>,
    currentChannelUrl: String,
    isLoadingChannels: Boolean,
    listState: LazyListState,
    media3Player: com.antoniegil.astronia.player.Media3Player?,
    onChannelClick: (M3U8Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val showPlayerStatsButton = remember { com.antoniegil.astronia.util.SettingsManager.getShowPlayerStats(context) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("") }
    
    val groupSet = remember(channels) {
        channels.map { it.group }.filter { it.isNotEmpty() }.toSet().sorted()
    }
    
    val filteredChannels = remember(channels, searchQuery, selectedGroup) {
        var result = channels
        if (selectedGroup.isNotEmpty()) {
            result = result.filter { it.group == selectedGroup }
        }
        if (searchQuery.isNotEmpty()) {
            result = result.filter { channel ->
                channel.name.contains(searchQuery, ignoreCase = true)
            }
        }
        result
    }
    
    LaunchedEffect(isSearching) {
        if (!isSearching && currentChannelUrl.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.url == currentChannelUrl }
            if (currentIndex >= 0) {
                listState.animateScrollToItem(currentIndex)
            }
        }
    }
    
    Column(modifier = modifier) {
        var showStatsDialog by remember { mutableStateOf(false) }
        
        if (channels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.channel_count, filteredChannels.size),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            view.slightHapticFeedback()
                            isSearching = !isSearching
                            if (!isSearching) searchQuery = ""
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                    if (showPlayerStatsButton) {
                        IconButton(
                            onClick = { showStatsDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Info, "Stats")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isSearching) {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 2.dp, bottom = 0.dp),
                    text = searchQuery,
                    placeholderText = stringResource(R.string.search_channels),
                    onValueChange = { searchQuery = it },
                    onClear = {
                        scope.launch {
                            val currentIndex = channels.indexOfFirst { it.url == currentChannelUrl }
                            if (currentIndex >= 0) {
                                listState.animateScrollToItem(currentIndex)
                            }
                        }
                    }
                )
            }

            if (groupSet.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clipToBounds()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGroup.isEmpty(),
                                onClick = {
                                    view.slightHapticFeedback()
                                    selectedGroup = ""
                                    scope.launch {
                                        val currentIndex = channels.indexOfFirst { it.url == currentChannelUrl }
                                        if (currentIndex >= 0) {
                                            listState.animateScrollToItem(currentIndex)
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.all)) }
                            )
                        }
                        items(groupSet.toList()) { group ->
                            FilterChip(
                                selected = selectedGroup == group,
                                onClick = {
                                    view.slightHapticFeedback()
                                    selectedGroup = group
                                },
                                label = { Text(group) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredChannels,
                    key = { channel -> channel.id }
                ) { channel ->
                    ChannelItem(
                        channel = channel,
                        isPlaying = channel.url == currentChannelUrl,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        } else if (isLoadingChannels) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.loading_channels))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.single_stream),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (showStatsDialog) {
            PlayerStatsDialog(
                media3Player = media3Player,
                onDismiss = { showStatsDialog = false }
            )
        }
    }
}

@Composable
fun ChannelItem(
    channel: M3U8Channel,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = channel.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (channel.group.isNotEmpty()) {
                    Text(
                        text = channel.group,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.playing),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
