package app.deltax.music.shared.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.add_to_playlist
import app.deltax.music.shared.generated.resources.cancel
import app.deltax.music.shared.generated.resources.deselect_all
import app.deltax.music.shared.generated.resources.dislike_all
import app.deltax.music.shared.generated.resources.like_all
import app.deltax.music.shared.generated.resources.n_selected
import app.deltax.music.shared.generated.resources.play
import app.deltax.music.shared.generated.resources.remove_from_playlist
import app.deltax.music.shared.generated.resources.select_all
import app.deltax.music.shared.generated.resources.shuffle
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.state.SongLikeState
import app.deltax.music.shared.ui.components.ModalBottomSheet
import app.deltax.music.shared.ui.components.PlayerMenuAction
import app.deltax.music.shared.ui.components.rememberModalSheetState
import app.deltax.music.shared.ui.icons.MetrolistIcons
import app.deltax.music.shared.ui.theme.ComponentShapes
import app.deltax.music.shared.ui.utils.verticalScrollWithDrag
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Menu for a multi-selection of songs. Mirrors the Android `SelectionSongMenu` layout:
 * header toggle + play, primary grid (shuffle / add to playlist), then grouped actions.
 * Every action is optional; a null callback hides the entry.
 */
@Composable
fun songSelectionMenuModel(
    songs: List<SongItem>,
    onDismiss: () -> Unit,
    isGuest: Boolean = false,
    downloadState: MediaDownloadState = MediaDownloadState.NOT_DOWNLOADED,
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeLabel: String? = null,
    onLikeAll: (() -> Unit)? = null,
    additionalActions: List<PlayerMenuAction> = emptyList(),
): MediaMenuModel {
    val playLabel = stringResource(Res.string.play)
    val shuffleLabel = stringResource(Res.string.shuffle)
    val addToPlaylistLabel = stringResource(Res.string.add_to_playlist)
    val likeAllLabel = stringResource(Res.string.like_all)
    val dislikeAllLabel = stringResource(Res.string.dislike_all)
    val removeFromPlaylistLabel = stringResource(Res.string.remove_from_playlist)
    val title = pluralStringResource(Res.plurals.n_selected, songs.size, songs.size)
    val allLiked = songs.isNotEmpty() && songs.all { SongLikeState.isLiked(it.id) }

    val headerActions =
        listOfNotNull(
            onLikeAll?.let {
                PlayerMenuAction(
                    icon = if (allLiked) MetrolistIcons.Filled.Favorite else MetrolistIcons.Outlined.FavoriteBorder,
                    title = if (allLiked) dislikeAllLabel else likeAllLabel,
                    selected = allLiked,
                    onClick = it,
                )
            },
            onPlay?.takeUnless { isGuest }?.let {
                action(MetrolistIcons.Rounded.PlayArrow, playLabel, onDismiss, it, prominent = true)
            },
        )

    val primaryActions =
        listOfNotNull(
            onShuffle?.takeUnless { isGuest }?.let {
                action(MetrolistIcons.Outlined.Shuffle, shuffleLabel, onDismiss, it)
            },
            onAddToPlaylist?.let {
                PlayerMenuAction(MetrolistIcons.AutoMirrored.Outlined.PlaylistAdd, addToPlaylistLabel, onClick = it)
            },
        )

    val actionGroups =
        listOf(
            if (isGuest) emptyList() else queueActions(onDismiss, onPlayNext, onAddToQueue),
            listOfNotNull(
                downloadAction(downloadState, 0, false, onDismiss, onDownload),
                onRemove?.let {
                    action(MetrolistIcons.Outlined.Delete, removeLabel ?: removeFromPlaylistLabel, onDismiss, it)
                },
            ),
            additionalActions,
        )

    return MediaMenuModel(
        title = title,
        subtitle = null,
        thumbnail = songs.firstOrNull()?.previewThumbnailUrlOrNull(),
        fallbackIcon = MetrolistIcons.AutoMirrored.Outlined.QueueMusic,
        headerActions = headerActions,
        primaryActions = primaryActions,
        actionGroups = actionGroups,
    )
}

private fun SongItem.previewThumbnailUrlOrNull(): String? = thumbnail?.takeIf { it.isNotBlank() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSelectionMenuSheet(
    model: MediaMenuModel,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        sheetState = rememberModalSheetState(SheetValue.Hidden),
        onDismissRequest = onDismiss,
        shape = ComponentShapes.BottomSheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScrollWithDrag(rememberScrollState()).padding(bottom = 16.dp),
        ) {
            MediaMenuSheetContent(model)
        }
    }
}

/**
 * Separate menu for the history screen: history entries are not [SongItem]s and only support
 * deletion, so they get their own short menu instead of the song selection menu.
 */
@Composable
fun historySelectionMenuModel(
    count: Int,
    deleteLabel: String,
    allDisplayedSelected: Boolean,
    onDismiss: () -> Unit,
    onToggleDisplayedSelection: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
): MediaMenuModel {
    val toggleSelectionLabel = stringResource(if (allDisplayedSelected) Res.string.deselect_all else Res.string.select_all)
    val cancelLabel = stringResource(Res.string.cancel)
    return MediaMenuModel(
        title = pluralStringResource(Res.plurals.n_selected, count, count),
        subtitle = null,
        thumbnail = null,
        fallbackIcon = MetrolistIcons.Outlined.Delete,
        actionGroups =
            listOf(
                listOfNotNull(
                    action(
                        if (allDisplayedSelected) MetrolistIcons.Outlined.Close else MetrolistIcons.Filled.Check,
                        toggleSelectionLabel,
                        onDismiss,
                        onToggleDisplayedSelection,
                    ),
                    action(MetrolistIcons.Outlined.Close, cancelLabel, onDismiss, onCancel),
                    if (count > 0) action(MetrolistIcons.Outlined.Delete, deleteLabel, onDismiss, onDelete) else null,
                ),
            ),
    )
}
