package app.deltax.music.shared.settings

import app.deltax.music.shared.model.SortDirection
import kotlinx.serialization.Serializable

@Serializable
data class LibrarySorting(
    val sort: LibraryContentSort,
    val direction: SortDirection,
)
