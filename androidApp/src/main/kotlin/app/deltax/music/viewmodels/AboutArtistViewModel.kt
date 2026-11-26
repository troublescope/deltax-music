package app.deltax.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deltax.music.shared.viewmodels.AboutArtistViewModel as SharedAboutArtistViewModel

class AboutArtistViewModel(
    artistId: String,
    artistName: String,
) : ViewModel() {
    val shared = SharedAboutArtistViewModel(artistId, artistName, viewModelScope)
}
