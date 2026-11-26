package app.deltax.music.shared.ui.menu

import app.deltax.music.shared.api.playlistSongs
import app.deltax.music.shared.model.AlbumItem
import app.deltax.music.shared.model.ArtistItem
import app.deltax.music.shared.model.EpisodeItem
import app.deltax.music.shared.model.PlaylistItem
import app.deltax.music.shared.model.PodcastItem
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.model.YTItem
import app.deltax.music.shared.model.toSongItem
import app.deltax.music.shared.state.GlobalYouTubeRepository

suspend fun loadMediaItemSongs(item: YTItem): List<SongItem> {
    val repository = GlobalYouTubeRepository.instance
    return when (item) {
        is SongItem -> {
            listOf(item)
        }

        is EpisodeItem -> {
            listOf(item.toSongItem())
        }

        is AlbumItem -> {
            repository.getAlbum(item.id)?.songs.orEmpty()
        }

        is ArtistItem -> {
            repository.getArtistSongs(item.id)
        }

        is PlaylistItem -> {
            repository.getPlaylist(item.id).playlistSongs()
        }

        is PodcastItem -> {
            // Every page, so playback covers the whole podcast and can start from its first episode.
            buildList {
                repository.getPodcastPages(item.id).collect { page -> page.episodes.mapTo(this) { it.toSongItem(page.podcast.title) } }
            }
        }

        else -> {
            emptyList()
        }
    }
}
