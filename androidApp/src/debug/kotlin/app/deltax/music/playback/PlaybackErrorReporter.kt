package app.deltax.music.playback

import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.playback.PlaybackErrorContext
import app.deltax.music.shared.youtube.playback.PlaybackResource

internal object PlaybackErrorReporter {
    fun setActivePlayback(
        song: SongItem,
        resource: PlaybackResource?,
    ) = Unit

    fun clearActivePlayback() = Unit

    fun report(
        error: Throwable,
        context: PlaybackErrorContext,
    ) = Unit
}
