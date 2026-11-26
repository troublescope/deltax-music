/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package app.deltax.music.ui.menu

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.deltax.music.playback.PlayerConnection
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.ui.components.rememberModalSheetState
import app.deltax.music.shared.ui.menu.SongDetailsContent
import app.deltax.music.shared.ui.theme.ComponentShapes
import app.deltax.music.shared.youtube.playback.PlaybackResource
import app.deltax.music.ui.component.AdaptiveModalBottomSheet
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsSheet(
    song: SongItem?,
    playbackResource: PlaybackResource? = null,
    volume: Float? = null,
    onDismiss: () -> Unit,
) {
    val database: MusicDatabase = koinInject()
    val playerConnection: PlayerConnection = koinInject()
    val currentResource by playerConnection.currentPlaybackResource.collectAsState()
    val currentVolume by playerConnection.volume.collectAsState()
    val sheetState = rememberModalSheetState(SheetValue.Hidden)
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = ComponentShapes.BottomSheetShape) {
        SongDetailsContent(song, database, playbackResource ?: currentResource, volume ?: currentVolume)
    }
}
