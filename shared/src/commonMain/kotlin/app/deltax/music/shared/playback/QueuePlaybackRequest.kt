package app.deltax.music.shared.playback

import app.deltax.music.shared.model.SongItem

data class QueuePlaybackRequest(
    val requestId: Long,
    val song: SongItem,
    val positionMs: Long = 0L,
    val resumePlayback: Boolean = true,
    // Set by Previous so a track that cannot play offline is skipped further back instead of forward again.
    val backward: Boolean = false,
)
