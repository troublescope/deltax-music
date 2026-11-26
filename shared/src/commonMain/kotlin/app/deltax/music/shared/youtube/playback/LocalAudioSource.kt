package app.deltax.music.shared.youtube.playback

import app.deltax.music.shared.model.SongItem

fun interface LocalAudioSource {
    suspend fun resolve(
        videoId: String,
        song: SongItem?,
    ): PlaybackResource?
}
