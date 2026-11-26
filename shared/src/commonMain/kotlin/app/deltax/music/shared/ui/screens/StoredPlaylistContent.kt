@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.deltax.music.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.cancel
import app.deltax.music.shared.generated.resources.clear
import app.deltax.music.shared.generated.resources.clear_collection_confirm
import app.deltax.music.shared.generated.resources.confirm_clear_collection
import app.deltax.music.shared.generated.resources.confirm_delete_stored_audio
import app.deltax.music.shared.generated.resources.menu
import app.deltax.music.shared.generated.resources.remove
import app.deltax.music.shared.generated.resources.remove_from_device_desc
import app.deltax.music.shared.generated.resources.remove_selected_count
import app.deltax.music.shared.generated.resources.remove_songs_from_device_confirm
import app.deltax.music.shared.generated.resources.search_no_results
import app.deltax.music.shared.model.CollectionMeta
import app.deltax.music.shared.model.CollectionSort
import app.deltax.music.shared.model.PlaylistItem
import app.deltax.music.shared.model.PlaylistSongSort
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.model.SortDirection
import app.deltax.music.shared.model.defaultDirection
import app.deltax.music.shared.model.filterForPlaylistSearch
import app.deltax.music.shared.model.indexOfPlaylistOccurrence
import app.deltax.music.shared.model.isReordering
import app.deltax.music.shared.model.presentCollection
import app.deltax.music.shared.model.sortedForLikedPlaylist
import app.deltax.music.shared.settings.ArtistNameAliases
import app.deltax.music.shared.settings.SavedPlaylistCollectionSorting
import app.deltax.music.shared.settings.SavedPlaylistSongSorting
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.SongLikeState
import app.deltax.music.shared.state.SongMetadataState
import app.deltax.music.shared.state.moveLikedSong
import app.deltax.music.shared.ui.components.AlertDialog
import app.deltax.music.shared.ui.components.CollectionFilterBar
import app.deltax.music.shared.ui.components.DownloadPlaylistFilterBar
import app.deltax.music.shared.ui.components.PlaylistOrderControls
import app.deltax.music.shared.ui.components.PopupContent
import app.deltax.music.shared.ui.components.PopupVisibility
import app.deltax.music.shared.ui.components.SongSelectionControls
import app.deltax.music.shared.ui.components.libraryCover
import app.deltax.music.shared.ui.components.rememberDownloadPlaylistFilter
import app.deltax.music.shared.ui.icons.MetrolistIcons
import app.deltax.music.shared.ui.navigation.navigationPopupSource
import app.deltax.music.shared.ui.playback.LocalPlaybackController
import app.deltax.music.shared.ui.playback.listState
import app.deltax.music.shared.ui.selection.rememberSelectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.concurrent.Volatile

class PendingCollectionPlayState {
    @Volatile
    private var job: Job? = null

    fun cancel() {
        val current = job
        job = null
        current?.cancel()
    }

    fun track(launched: Job?) {
        cancel()
        if (launched != null && !launched.isCompleted) {
            job = launched
            launched.invokeOnCompletion {
                if (job === launched) job = null
            }
        }
    }
}

@Composable
fun rememberPendingCollectionPlayState(key: Any?): PendingCollectionPlayState =
    remember(key) { PendingCollectionPlayState() }.also { state ->
        DisposableEffect(state) {
            onDispose { state.cancel() }
        }
    }

@Composable
fun StoredPlaylistContent(
    id: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    songs: List<SongItem>,
    emptyContent: @Composable () -> Unit,
    listState: LazyListState,
    onBackClick: (() -> Unit)? = null,
    onCollectionMenu: (List<SongItem>) -> Unit,
    onTrackMenu: (SongItem) -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    extraInfo: (@Composable (Int, SongItem) -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    collectionMetadata: Map<String, CollectionMeta>? = null,
    downloadFilterDatabase: MusicDatabase? = null,
    playCounts: Map<String, Int> = emptyMap(),
    playCountsLoaded: Boolean = true,
    onRetryPlayCounts: (() -> Unit)? = null,
    pendingPlayState: PendingCollectionPlayState = rememberPendingCollectionPlayState(id),
    onCollectionSortChange: (CollectionSort) -> Unit = {},
    onRemoveSongs: ((Set<String>) -> Unit)? = null,
    onClearCollection: (() -> Unit)? = null,
    onReorderSongs: ((List<SongItem>) -> Unit)? = null,
    hasMoreSongs: Boolean = false,
    loadCompleteSongs: (suspend () -> List<SongItem>?)? = null,
    onDownload: (List<SongItem>) -> Unit,
    onAddToPlaylist: (List<SongItem>) -> Unit,
    trackContainer: @Composable (SongItem, Boolean, @Composable () -> Unit) -> Unit = { _, _, content -> content() },
) {
    val isDownloadedCollection = id == "local:downloaded"
    val isCachedCollection = id == "local:cached"
    val playlistSortingKey = "special:${id.removePrefix("local:")}"
    val defaultCollectionSort =
        when {
            isDownloadedCollection -> Settings.shared.downloadedCollectionSort
            isCachedCollection -> Settings.shared.cachedCollectionSort
            else -> CollectionSort.NEWEST
        }
    val defaultCollectionDirection =
        when {
            isDownloadedCollection -> Settings.shared.downloadedCollectionSortDirection
            isCachedCollection -> Settings.shared.cachedCollectionSortDirection
            else -> SortDirection.DESCENDING
        }
    val defaultPlaylistSorting =
        Settings.shared.playlistSongSorting(
            playlistSortingKey,
            Settings.shared.likedSongSort,
            Settings.shared.likedSongSortDirection,
        )
    var collectionDirection by remember(id) {
        mutableStateOf(
            Settings.shared
                .playlistCollectionSorting(
                    playlistSortingKey,
                    defaultCollectionSort,
                    defaultCollectionDirection,
                ).direction,
        )
    }
    val playback = LocalPlaybackController.current
    val playbackState by remember(playback) { playback.listState }.collectAsState(playback.state.value.listState())
    var query by remember(id) { mutableStateOf("") }
    var collectionSort by remember(id) {
        mutableStateOf(
            Settings.shared
                .playlistCollectionSorting(
                    playlistSortingKey,
                    defaultCollectionSort,
                    defaultCollectionDirection,
                ).sort
                .let { saved ->
                    if (isCachedCollection || saved != CollectionSort.MANUAL) saved else CollectionSort.NEWEST
                },
        )
    }
    LaunchedEffect(id, collectionSort) { onCollectionSortChange(collectionSort) }
    var playlistSort by remember(id) { mutableStateOf(defaultPlaylistSorting.sort) }
    var playlistSortDirection by remember(id) { mutableStateOf(defaultPlaylistSorting.direction) }
    val reorderMode = playlistSort.isReordering(Settings.shared.playlistEditLock, onReorderSongs != null, hasMoreSongs)
    val selection = rememberSelectionState(id)
    val downloadFilter = downloadFilterDatabase?.let { rememberDownloadPlaylistFilter(it) }
    val downloadFilterState = downloadFilter?.state?.collectAsState()?.value
    LaunchedEffect(downloadFilterState) { selection.exit() }
    var pendingRemoval by remember(id) { mutableStateOf<Set<String>?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val metadataRevision by SongMetadataState.revision.collectAsState()
    val listeningTimes = if (playlistSort == PlaylistSongSort.LISTENING_TIME) SongMetadataState.listeningTimes else emptyMap()
    val likedSongRecency = SongLikeState.likedSongRecency
    val cachedCustomOrder =
        remember(isCachedCollection, Settings.shared.cachedSongsCustomOrder) {
            if (isCachedCollection) Settings.shared.cachedSongsOrder() else emptyList()
        }
    val artistAliases = if (collectionMetadata != null && collectionSort == CollectionSort.ARTIST) ArtistNameAliases.aliases else emptyMap()
    val orderedSongs =
        remember(
            songs,
            likedSongRecency,
            downloadFilterState,
            metadataRevision,
            listeningTimes,
            collectionMetadata,
            playCounts,
            collectionDirection,
            collectionSort,
            cachedCustomOrder,
            artistAliases,
            isCachedCollection,
            playlistSort,
            playlistSortDirection,
            reorderMode,
            onReorderSongs != null,
        ) {
            when {
                collectionMetadata != null -> {
                    (downloadFilterState?.apply(songs) ?: songs).presentCollection(
                        "",
                        collectionSort,
                        collectionDirection,
                        customOrder = if (isCachedCollection) cachedCustomOrder else emptyList(),
                        artistAliases = artistAliases,
                    ) { song ->
                        collectionMetadata[song.id]?.copy(playCount = playCounts[song.id] ?: 0)
                    }
                }

                onReorderSongs != null && !reorderMode -> {
                    songs
                        .sortedForLikedPlaylist(
                            playlistSort,
                            playlistSortDirection,
                            likedSongRecency,
                            listeningTimes,
                        )
                }

                else -> {
                    songs
                }
            }
        }
    val displayedSongs = remember(orderedSongs, query) { orderedSongs.filterForPlaylistSearch(query) }
    val displayedIds = remember(displayedSongs) { displayedSongs.mapTo(mutableSetOf()) { it.id } }
    val selectedSongs =
        remember(displayedSongs, selection.active, selection.selectedIds) {
            if (selection.active) displayedSongs.filter { selection.isSelected(it.id) } else emptyList()
        }
    val selectionEnabled = collectionMetadata != null && onRemoveSongs != null

    LaunchedEffect(songs) {
        selection.retain(songs.map { it.id })
    }

    val customCover = Settings.shared.collectionCovers[id.removePrefix("local:")]
    val scope = rememberCoroutineScope()
    val currentPlayCountsReady =
        rememberUpdatedState(collectionMetadata == null || collectionSort != CollectionSort.MOST_PLAYED || playCountsLoaded)
    val currentOrderedSongs = rememberUpdatedState(orderedSongs)
    val currentQuery = rememberUpdatedState(query)

    fun playCollection(
        song: SongItem? = null,
        shuffle: Boolean = false,
    ) {
        if (collectionSort == CollectionSort.MOST_PLAYED && !currentPlayCountsReady.value) {
            onRetryPlayCounts?.invoke()
        }
        val expectedGeneration = playback.reserveExplicitPlaybackGeneration()

        pendingPlayState.track(
            scope.launch {
                val complete =
                    if (hasMoreSongs && loadCompleteSongs != null) {
                        loadCompleteSongs()
                            ?.sortedForLikedPlaylist(
                                playlistSort,
                                playlistSortDirection,
                                SongLikeState.likedSongRecency,
                                listeningTimes,
                            ) ?: return@launch
                    } else {
                        snapshotFlow { currentPlayCountsReady.value to currentOrderedSongs.value }
                            .first { (ready, _) -> ready }
                            .second
                    }
                if (playback.explicitPlaybackGeneration != expectedGeneration) {
                    return@launch
                }
                val playable =
                    (if (song == null) complete.filterForPlaylistSearch(currentQuery.value) else complete)
                        .filterNot { it.isUnavailable }
                if (shuffle) {
                    playback.shuffleQueueAtGeneration(expectedGeneration, playable)
                } else {
                    val target = song?.let { playable.indexOfPlaylistOccurrence(it) } ?: 0
                    if (target in playable.indices) {
                        playback.playQueueAtGeneration(expectedGeneration, playable, target)
                    }
                }
            },
        )
    }

    PlaylistContent(
        isTrackSelected = { selection.isSelected(it.id) },
        trackContainer = { song, _, content ->
            trackContainer(song, reorderMode || selection.active, content)
        },
        playlist =
            PlaylistItem(
                id = id,
                title = title,
                thumbnail = customCover ?: songs.firstOrNull { !it.thumbnail.isNullOrBlank() }?.thumbnail,
                customThumbnail = customCover,
                author = subtitle,
                isLocal = true,
            ),
        songs = displayedSongs,
        artworkSongs = songs,
        text = playlistContentText(useTrackCount = id.removePrefix("local:") in setOf("podcasts", "SE")),
        hasMoreSongs = hasMoreSongs,
        isLoading = hasMoreSongs,
        onMoreOptions = { onCollectionMenu(displayedSongs) },
        onPlayAll = { playCollection() },
        onShuffle = { playCollection(shuffle = true) },
        onTrackClick = { song, _ ->
            if (selection.active) {
                selection.toggle(song.id)
            } else {
                playCollection(song)
            }
        },
        onTrackMenuClick = { song, _ -> onTrackMenu(song) },
        searchQuery = query,
        onSearchQueryChange = { query = it },
        onTrackLongClick = { song ->
            if (!reorderMode) {
                selection.begin(song.id)
            }
        },
        selectionActive = selection.active,
        selectionControls = {
            SongSelectionControls(
                selectedCount = selectedSongs.size,
                selectedSongs = selectedSongs,
                onPlay = {
                    pendingPlayState.cancel()
                    selectedSongs.firstOrNull { !it.isUnavailable }?.let { playback.play(it, selectedSongs) }
                },
                onShuffle = {
                    pendingPlayState.cancel()
                    playback.shuffleQueue(selectedSongs)
                },
                allSelected = selection.allSelected(displayedIds),
                onDone = selection::exit,
                onSelectAll = { selection.toggleAll(displayedIds) },
                onPlayNext = { playback.playNext(selectedSongs) },
                onAddToQueue = { playback.addToQueue(selectedSongs) },
                onDownload = { onDownload(selectedSongs) },
                onAddToPlaylist = { onAddToPlaylist(selectedSongs) },
                onRemove =
                    when {
                        id == "local:liked" -> {
                            { SongLikeState.removeLikes(selectedSongs.map { it.id }) }
                        }

                        selectionEnabled -> {
                            { pendingRemoval = selectedSongs.mapTo(mutableSetOf()) { it.id } }
                        }

                        else -> {
                            null
                        }
                    },
                removeLabel =
                    if (id != "local:liked" && selectionEnabled) {
                        stringResource(Res.string.remove_selected_count, selectedSongs.size)
                    } else {
                        null
                    },
            )
        },
        onArtistClick = onArtistClick,
        onBackClick = onBackClick,
        fallbackIcon = icon,
        cover = libraryCover(id.removePrefix("local:")).takeIf { customCover == null },
        listState = listState,
        currentSongId = playbackState.currentSongId,
        currentQueueItemId = playbackState.originalQueueItemId,
        currentSongOccurrenceKey = playbackState.currentSongOccurrenceKey,
        isPlaying = playbackState.isPlaying,
        header =
            headerAction?.let { action ->
                {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        action()
                    }
                }
            },
        aboveOrderControls =
            if (selectionEnabled && downloadFilter != null) {
                { DownloadPlaylistFilterBar(downloadFilter, downloadFilterDatabase) }
            } else {
                null
            },
        orderControls =
            when {
                selectionEnabled -> {
                    {
                        Column {
                            CollectionFilterBar(
                                showSearch = false,
                                text =
                                    collectionFilterBarText(
                                        includeClear = true,
                                        includeManual = isCachedCollection,
                                    ),
                                query = query,
                                onQueryChange = { query = it },
                                sort = collectionSort,
                                onSortChange = {
                                    collectionSort = it
                                    collectionDirection = it.defaultDirection
                                    onCollectionSortChange(it)
                                    Settings.shared.setPlaylistCollectionSorting(
                                        playlistSortingKey,
                                        SavedPlaylistCollectionSorting(it, it.defaultDirection),
                                    )
                                },
                                sortDirection = collectionDirection,
                                onSortDirectionChange = {
                                    collectionDirection = it
                                    Settings.shared.setPlaylistCollectionSorting(
                                        playlistSortingKey,
                                        SavedPlaylistCollectionSorting(collectionSort, it),
                                    )
                                },
                                onClear = { confirmClear = true },
                            )
                        }
                    }
                }

                onReorderSongs != null -> {
                    {
                        PlaylistOrderControls(
                            text = playlistOrderControlsText(),
                            sort = playlistSort,
                            sortDirection = playlistSortDirection,
                            reorderMode = reorderMode,
                            canReorder = true,
                            onSortChange = {
                                playlistSort = it
                                Settings.shared.setPlaylistSongSorting(
                                    playlistSortingKey,
                                    SavedPlaylistSongSorting(it, playlistSortDirection),
                                )
                            },
                            onSortDirectionChange = {
                                playlistSortDirection = it
                                Settings.shared.setPlaylistSongSorting(
                                    playlistSortingKey,
                                    SavedPlaylistSongSorting(playlistSort, it),
                                )
                            },
                            onReorderModeChange = {
                                Settings.shared.playlistEditLock = !it
                                selection.exit()
                            },
                        )
                    }
                }

                else -> {
                    null
                }
            },
        reorderMode = reorderMode,
        onReorder = { fromIndex, toIndex ->
            val from = displayedSongs.getOrNull(fromIndex)
            val to = displayedSongs.getOrNull(toIndex)
            if (from != null && to != null) {
                onReorderSongs?.invoke(moveLikedSong(songs, from.id, to.id))
            }
        },
        emptyContent = {
            if (downloadFilterState?.playlist != null) {
                if (!downloadFilterState.loading && !downloadFilterState.failed) {
                    Text(stringResource(Res.string.search_no_results), modifier = Modifier.padding(16.dp))
                }
            } else {
                emptyContent()
            }
        },
        trackTrailingContent =
            if (selectionEnabled || selection.active || extraInfo != null) {
                { song, index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        extraInfo?.invoke(index, song)
                        if (selection.active) {
                            Checkbox(
                                checked = selection.isSelected(song.id),
                                onCheckedChange = { selection.toggle(song.id) },
                            )
                        } else {
                            IconButton(
                                modifier = Modifier.navigationPopupSource(),
                                onClick = { onTrackMenu(song) },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(MetrolistIcons.Default.MoreVert, stringResource(Res.string.menu))
                            }
                            if (selectionEnabled) {
                                IconButton(onClick = { onRemoveSongs(setOf(song.id)) }, shapes = IconButtonDefaults.shapes()) {
                                    Icon(
                                        MetrolistIcons.Outlined.Delete,
                                        contentDescription = stringResource(Res.string.remove_from_device_desc),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                null
            },
    )

    val removalIds = pendingRemoval
    PopupContent(removalIds) { removalIds ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = {
                Text(pluralStringResource(Res.plurals.remove_songs_from_device_confirm, removalIds.size, removalIds.size))
            },
            text = { Text(stringResource(Res.string.confirm_delete_stored_audio)) },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSongs?.invoke(removalIds)
                        selection.exit()
                        pendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(Res.string.remove)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoval = null },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
    PopupVisibility(visible = confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(Res.string.clear_collection_confirm, title)) },
            text = { Text(stringResource(Res.string.confirm_clear_collection)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCollection?.invoke()
                        selection.exit()
                        confirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(Res.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
