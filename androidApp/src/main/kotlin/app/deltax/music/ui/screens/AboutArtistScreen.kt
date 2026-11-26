package app.deltax.music.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.deltax.music.LocalMiniPlayerBottomInset
import app.deltax.music.shared.model.ArtistItem
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.ui.screens.AboutArtistContent
import app.deltax.music.shared.ui.screens.ArtistPhotoActions
import app.deltax.music.shared.viewmodels.artistCatalogTitles
import app.deltax.music.ui.LocalImmersiveContent
import app.deltax.music.utils.ArtistPhotoSaver
import app.deltax.music.utils.startActivitySafely
import app.deltax.music.viewmodels.AboutArtistViewModel
import app.deltax.music.viewmodels.ArtistViewModel
import kotlinx.coroutines.CompletableDeferred
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AboutArtistScreen(
    artistId: String,
    artistName: String,
    onBackClick: () -> Unit,
    initialInfo: ArtistItem? = null,
    artistViewModel: ArtistViewModel =
        koinViewModel(
            key = "about_artist_yt_$artistId",
            parameters = { parametersOf(artistId) },
        ),
    aboutViewModel: AboutArtistViewModel =
        koinViewModel(
            key = "about_artist_lfm_$artistId",
            parameters = { parametersOf(artistId, artistName) },
        ),
) {
    val artistUiState by artistViewModel.shared.uiState.collectAsState()
    val info = artistUiState.artistInfo ?: initialInfo
    val uiState by aboutViewModel.shared.uiState.collectAsState()
    val catalogTitles = remember(artistUiState.sections) { artistCatalogTitles(artistUiState.sections) }
    LaunchedEffect(info, catalogTitles, artistUiState.errorMessage, Settings.shared.lastResolvedContentLanguage) {
        if (info != null || artistUiState.errorMessage != null) {
            aboutViewModel.shared.load(catalogTitles)
        }
    }
    AboutArtistContent(
        artistName = artistName,
        info = info,
        uiState = uiState,
        photoActions = rememberArtistPhotoActions(artistName),
        bottomContentPadding = LocalMiniPlayerBottomInset.current,
        onBackClick = onBackClick,
    )
}

@Composable
private fun rememberArtistPhotoActions(artistName: String): ArtistPhotoActions {
    val context = LocalContext.current
    val immersiveContent = LocalImmersiveContent.current
    var permissionResult by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionResult?.complete(it) }
    return remember(context, immersiveContent, artistName, permissionLauncher) {
        ArtistPhotoActions(
            share = { link ->
                context.startActivitySafely(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link)
                        },
                        null,
                    ),
                )
            },
            save = { photo ->
                val granted =
                    !ArtistPhotoSaver.needsStoragePermission ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED ||
                        CompletableDeferred<Boolean>()
                            .also {
                                permissionResult = it
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }.await()
                check(granted) { "Storage permission denied" }
                ArtistPhotoSaver.save(context, photo, artistName)
                true
            },
            showMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
            onViewerVisibilityChange = { immersiveContent.value = it },
        )
    }
}
