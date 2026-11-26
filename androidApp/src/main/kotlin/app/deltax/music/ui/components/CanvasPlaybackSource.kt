package app.deltax.music.ui.components

import app.deltax.music.shared.api.artwork.YouTubeCanvasClip
import app.deltax.music.shared.youtube.playback.PlaybackResource
import app.deltax.music.ui.player.CanvasCache

internal data class CanvasPlaybackSource(
    val url: String,
    val cacheKey: String?,
    val clips: List<YouTubeCanvasClip>,
)

internal fun isPreparedCanvasUrl(url: String) = url.startsWith("file:")

internal fun canvasPlaybackSources(
    url: String,
    resource: PlaybackResource?,
    cacheKey: String?,
    clips: List<YouTubeCanvasClip>,
    cachedAvailable: Boolean,
    preparingClips: Boolean = false,
): Pair<CanvasPlaybackSource, CanvasPlaybackSource?> {
    // Keep SABR canvas at t=0; mid-video seeks under pending attestation return no media.
    val liveClips = if (resource?.hasSabrVideo == true) emptyList() else clips
    if (!CanvasCache.isCachedUrl(url) && !isPreparedCanvasUrl(url)) {
        return CanvasPlaybackSource(url, cacheKey.takeUnless { preparingClips }, liveClips) to null
    }
    val originalUrl =
        resource
            ?.let {
                if (it.hasSabrVideo) "deltaxmusic://sabr-video/${it.videoId}" else it.videoUrl
            }?.takeUnless { CanvasCache.isCachedUrl(it) || it.isBlank() }
    // Prepared clips use a separate timeline and cache key from full-video streams.
    val recovery = originalUrl?.let { CanvasPlaybackSource(it, null, liveClips) }
    if (CanvasCache.isCachedUrl(url) && !cachedAvailable && recovery != null) return recovery to null
    return CanvasPlaybackSource(url, cacheKey.takeUnless { isPreparedCanvasUrl(url) }, emptyList()) to recovery
}
