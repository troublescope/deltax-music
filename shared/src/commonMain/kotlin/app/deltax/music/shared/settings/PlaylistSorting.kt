package app.deltax.music.shared.settings

import app.deltax.music.shared.model.CollectionSort
import app.deltax.music.shared.model.PlaylistSongSort
import app.deltax.music.shared.model.SortDirection
import kotlinx.serialization.Serializable

@Serializable
data class SavedPlaylistSongSorting(
    val sort: PlaylistSongSort,
    val direction: SortDirection,
)

@Serializable
data class SavedPlaylistCollectionSorting(
    val sort: CollectionSort,
    val direction: SortDirection,
)
