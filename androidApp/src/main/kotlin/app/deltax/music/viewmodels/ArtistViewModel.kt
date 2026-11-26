package app.deltax.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deltax.music.shared.api.artwork.AnimatedArtworkService
import app.deltax.music.shared.db.MusicDatabase
import kotlinx.coroutines.CoroutineScope
import app.deltax.music.shared.viewmodels.ArtistViewModel as SharedArtistViewModel

class ArtistViewModel(
    artistId: String? = null,
    animatedArtworkService: AnimatedArtworkService,
    database: MusicDatabase,
    applicationScope: CoroutineScope,
) : ViewModel() {
    val shared = SharedArtistViewModel(artistId, viewModelScope, animatedArtworkService, database, applicationScope)
}
