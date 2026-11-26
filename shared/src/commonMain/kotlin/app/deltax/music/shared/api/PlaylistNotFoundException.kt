package app.deltax.music.shared.api

class PlaylistNotFoundException(
    val playlistId: String,
) : Exception()
