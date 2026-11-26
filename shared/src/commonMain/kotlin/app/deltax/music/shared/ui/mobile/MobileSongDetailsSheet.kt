package app.deltax.music.shared.ui.mobile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.ui.components.ModalBottomSheet
import app.deltax.music.shared.ui.menu.SongDetailsContent
import app.deltax.music.shared.ui.playback.PlaybackUiState
import app.deltax.music.shared.ui.theme.ComponentShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileSongDetailsSheet(
    state: PlaybackUiState,
    database: MusicDatabase,
    onDismiss: () -> Unit,
    song: SongItem? = state.currentSong,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = ComponentShapes.BottomSheetShape) {
        SongDetailsContent(
            song = song,
            database = database,
            playbackResource = state.currentPlaybackResource,
            durationMs = state.durationMs.takeIf { it > 0 && state.currentSong?.id == song?.id },
        )
    }
}
