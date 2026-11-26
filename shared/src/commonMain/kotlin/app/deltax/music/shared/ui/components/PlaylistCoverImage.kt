package app.deltax.music.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.logo
import app.deltax.music.shared.model.PlaylistItem
import app.deltax.music.shared.ui.components.images.AsyncImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun PlaylistCoverImage(
    playlist: PlaylistItem,
    modifier: Modifier = Modifier,
) {
    PlaylistCoverImage(
        url = libraryArtwork(playlist.id, playlist.thumbnail, playlist.customThumbnail),
        modifier = modifier,
    )
}

@Composable
fun PlaylistCoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    previewUrl: String? = null,
) {
    val placeholder = painterResource(Res.drawable.logo)
    val coverModifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer)
    if (url.isAutoPlaylistIcon()) {
        AutoPlaylistThumbnail(url, coverModifier)
    } else if (!url.isNullOrBlank()) {
        AsyncImage(
            url = url,
            modifier = coverModifier,
            contentScale = contentScale,
            error = placeholder,
            previewUrl = previewUrl,
        )
    } else {
        Image(
            painter = placeholder,
            contentDescription = null,
            modifier = coverModifier,
            contentScale = contentScale,
        )
    }
}
