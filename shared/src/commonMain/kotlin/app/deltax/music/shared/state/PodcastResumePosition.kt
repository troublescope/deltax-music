package app.deltax.music.shared.state

import app.deltax.music.shared.db.entities.isEpisodeComplete
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.model.isPodcast

fun podcastResumePosition(
    song: SongItem,
    savedPositionMs: Long?,
    durationMs: Long = song.duration?.times(1000L) ?: 0L,
    requestedPositionMs: Long? = null,
): Long {
    requestedPositionMs?.let { return it.coerceAtLeast(0L) }
    if (!song.isPodcast) return 0L
    val position =
        savedPositionMs?.takeIf { it >= 0L }
            ?: song.playbackPositionMs?.takeIf { it > 0L }
            ?: song.playbackProgressPercent?.takeIf { it in 0..100 && durationMs > 0L }?.let { durationMs * it / 100L }
            ?: 0L
    return if (isEpisodeComplete(position, durationMs)) 0L else position
}
