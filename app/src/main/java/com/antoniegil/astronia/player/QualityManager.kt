package com.antoniegil.astronia.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import com.antoniegil.astronia.R

data class VideoQuality(
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val pixelWidthHeightRatio: Float = 1.0f,
    val id: String,
    val labelSuffix: String = ""
) {
    val label: String
        get() {
            return when {
                height > 0 -> "${height}p"
                width > 0 -> "${width}w"
                bitrate > 0 -> "${bitrate / 1000}k"
                else -> R.string.unknown
            }.let { base ->
                (if (labelSuffix.isNotEmpty()) "$base ($labelSuffix)" else base) as String
            }
        }
    
    val isBitrateOnly: Boolean
        get() = height <= 0 && width <= 0 && bitrate > 0
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object QualityManager {
    
    fun parseQualities(tracks: Tracks, trackSelectionParameters: TrackSelectionParameters): Pair<List<VideoQuality>, VideoQuality?> {
        val qualities = mutableListOf<VideoQuality>()
        var currentSelectedQuality: VideoQuality? = null
        val videoGroupType = C.TRACK_TYPE_VIDEO
        
        for (group in tracks.groups) {
            if (group.type == videoGroupType) {
                for (i in 0 until group.length) {
                    if (group.isTrackSupported(i)) {
                         val format = group.getTrackFormat(i)
                         val id = format.id ?: "${format.width}x${format.height}@${format.bitrate}"
                         val quality = VideoQuality(
                             width = format.width,
                             height = format.height,
                             bitrate = format.bitrate,
                             pixelWidthHeightRatio = format.pixelWidthHeightRatio,
                             id = id
                         )
                         
                         if (quality.height > 0 || quality.width > 0 || quality.bitrate > 0) {
                            qualities.add(quality)
                         }
                         
                         if (group.isTrackSelected(i)) {
                            currentSelectedQuality = quality
                         }
                    }
                }
            }
        }
        
        val qualitiesWithLabels = qualities.groupBy { it.label }.flatMap { (_, sameLabelQualities) ->
            val qualitiesWithSuffixes = sameLabelQualities.map { quality ->
                val ratio = if (quality.height > 0) {
                     (quality.width * quality.pixelWidthHeightRatio) / quality.height
                } else 0f
                
                val suffix = when {
                    kotlin.math.abs(ratio - 1.77f) < 0.1f -> "16:9"
                    kotlin.math.abs(ratio - 1.33f) < 0.15f -> "4:3"
                    kotlin.math.abs(ratio - 2.33f) < 0.1f -> "21:9"
                    ratio > 0 -> String.format("%.2f:1", ratio)
                    else -> ""
                }
                Pair(quality, suffix)
            }
            
            val distinctSuffixes = qualitiesWithSuffixes.map { it.second }.filter { it.isNotEmpty() }.distinct()
            
            if (distinctSuffixes.size > 1) {
                qualitiesWithSuffixes.map { (quality, suffix) ->
                    if (suffix.isNotEmpty()) quality.copy(labelSuffix = suffix) else quality
                }
            } else {
                sameLabelQualities
            }
        }
        
        val uniqueQualities = qualitiesWithLabels
            .groupBy { 
                if (it.isBitrateOnly) it.id else "${it.width}x${it.height}"
            }
            .map { (_, sameResQualities) -> 
                sameResQualities.maxByOrNull { it.bitrate } ?: sameResQualities.first()
            }
            .sortedWith(compareByDescending<VideoQuality> { it.height }
                .thenByDescending { it.bitrate }
                .thenByDescending { it.width })
        
        val finalCurrentQuality = if (currentSelectedQuality != null) {
            uniqueQualities.find { 
                it.width == currentSelectedQuality.width && 
                it.height == currentSelectedQuality.height && 
                it.bitrate == currentSelectedQuality.bitrate 
            } ?: currentSelectedQuality
        } else null
            
        val hasOverride = tracks.groups.any { group ->
            group.type == C.TRACK_TYPE_VIDEO && trackSelectionParameters.overrides.containsKey(group.mediaTrackGroup)
        }
        
        return Pair(uniqueQualities, if (hasOverride) finalCurrentQuality else null)
    }
    
    fun setQuality(player: ExoPlayer?, quality: VideoQuality?) {
        val exoPlayer = player ?: return
        val currentParams = exoPlayer.trackSelectionParameters

        if (quality == null) {
            val hasOverride = exoPlayer.currentTracks.groups.any { group ->
                group.type == C.TRACK_TYPE_VIDEO && currentParams.overrides.containsKey(group.mediaTrackGroup)
            }
            if (!hasOverride) {
                return
            }
            
            exoPlayer.trackSelectionParameters = currentParams
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
            return
        }

        val tracks = exoPlayer.currentTracks
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    
                    val isMatch = if (quality.isBitrateOnly) {
                        format.bitrate == quality.bitrate
                    } else {
                        format.width == quality.width &&
                        format.height == quality.height &&
                        format.bitrate == quality.bitrate &&
                        kotlin.math.abs(format.pixelWidthHeightRatio - quality.pixelWidthHeightRatio) < 0.01f
                    }
                    
                    if (isMatch) {
                        val existingOverride = currentParams.overrides[group.mediaTrackGroup]
                        if (existingOverride != null && 
                            existingOverride.trackIndices.size == 1 && 
                            existingOverride.trackIndices[0] == i) {
                            return
                        }
                        
                        val newParams = currentParams
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                            .setOverrideForType(
                                TrackSelectionOverride(
                                    group.mediaTrackGroup,
                                    listOf(i)
                                )
                            )
                            .build()
                        exoPlayer.trackSelectionParameters = newParams
                        return
                    }
                }
            }
        }
    }
    
    fun selectQualityByPreference(qualities: List<VideoQuality>, preference: Int): VideoQuality? {
        if (qualities.isEmpty()) return null
        if (qualities.size == 1) return null
        
        val result = when (preference) {
            0 -> qualities.firstOrNull()
            1 -> {
                val isBitrateMode = qualities.firstOrNull()?.isBitrateOnly == true
                if (isBitrateMode) {
                    qualities.firstOrNull { it.bitrate in 1290000..2540000 }
                        ?: qualities.lastOrNull()
                } else {
                    qualities.firstOrNull { it.height in 481..719 }
                        ?: qualities.lastOrNull()
                }
            }
            else -> null
        }
        return result
    }
}
