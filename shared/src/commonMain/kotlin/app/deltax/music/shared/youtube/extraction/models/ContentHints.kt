package app.deltax.music.shared.youtube.extraction.models

import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.model.isLive
import app.deltax.music.shared.model.isUploadedType
import app.deltax.music.shared.model.isVideoSong
import app.deltax.music.shared.settings.MusicVideoQuality
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.settings.targetHeight
import app.deltax.music.shared.utils.getDeviceDisplayMaxVideoHeight
import app.deltax.music.shared.youtube.playback.PlaybackBandwidthMeter
import app.deltax.music.shared.youtube.playback.PlaybackNetworkState
import app.deltax.music.shared.youtube.playback.PlaybackNetworkStateStore
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.strategy.PlaybackClientCatalog

fun SongItem.toContentHints(wantVideo: Boolean? = null): ContentHints {
    val uploaded = isUploaded || isUploadedType(musicVideoType)
    val normalizedType = musicVideoType?.uppercase()
    val devtoolsPlaybackSelection = Settings.shared.devtoolsPlaybackClient

    val kidsHint =
        when {
            normalizedType?.contains("KIDS") == true -> true
            normalizedType?.contains("YTKIDS") == true -> true
            else -> null
        }

    val liveHint = true.takeIf { isLive }

    val quality = Settings.shared.musicVideoQuality
    return ContentHints(
        isExplicit = isExplicit,
        isKidsContent = kidsHint,
        isLive = liveHint,
        isUploaded = uploaded,
        isLocal = isLocal,
        endpointParams = endpointParams,
        playbackClientOverrideId =
            devtoolsPlaybackSelection
                .takeUnless { it == PlaybackClientCatalog.AUTOMATIC_ID || it == PlaybackClientCatalog.SABR_FIRST_ID }
                ?.takeIf { PlaybackClientCatalog.findDevtoolsOverride(it) != null }
                ?: if (liveHint == true) "ANDROID" else null,
        sabrFirst = devtoolsPlaybackSelection == PlaybackClientCatalog.SABR_FIRST_ID,
        wantVideo = !uploaded && (wantVideo ?: (isVideoSong || liveHint == true)),
        maxVideoHeight =
            when (quality) {
                MusicVideoQuality.AUTO -> automaticVideoMaxHeight()
                else -> quality.targetHeight
            },
    ).withStreamCapabilities(allowProgressive = playbackPlaysProgressiveAudio)
}

/** Whether this platform's player can play audio from a muxed progressive file. */
internal expect val playbackPlaysProgressiveAudio: Boolean

fun automaticVideoMaxHeight(
    networkState: PlaybackNetworkState = PlaybackNetworkStateStore.current,
    bandwidthBps: Long = PlaybackBandwidthMeter.estimatedBandwidthBps,
    deviceMaxHeight: Int = getDeviceDisplayMaxVideoHeight(),
): Int {
    val isCellular = networkState.scope.contains("cell", ignoreCase = true) || !networkState.connected
    val bandwidthCap =
        when {
            bandwidthBps >= 15_000_000L -> 2160
            bandwidthBps >= 8_000_000L -> 1440
            bandwidthBps >= 4_000_000L -> 1080
            bandwidthBps >= 2_000_000L -> 720
            else -> 480
        }
    val effectiveCap = if (isCellular) minOf(720, bandwidthCap) else bandwidthCap
    return minOf(effectiveCap, deviceMaxHeight)
}
