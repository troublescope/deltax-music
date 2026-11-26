package app.deltax.music.shared.db

fun interface OfflineSongAvailability {
    suspend fun songIds(): Set<String>
}
