@file:Suppress("FunctionName")

package app.deltax.music.shared.ui.mobile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.db.StoredHistoryRow
import app.deltax.music.shared.db.deleteHistoryEntries
import app.deltax.music.shared.db.historyChanges
import app.deltax.music.shared.db.historyHasRemoteTokens
import app.deltax.music.shared.db.historySong
import app.deltax.music.shared.db.syncYouTubeHistory
import app.deltax.music.shared.db.updateHistorySong
import app.deltax.music.shared.db.visibleHistoryIds
import app.deltax.music.shared.db.visibleHistoryPage
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.delete_failed
import app.deltax.music.shared.generated.resources.error
import app.deltax.music.shared.model.BuiltinPlaylist
import app.deltax.music.shared.model.HistoryEntryUi
import app.deltax.music.shared.model.HistorySortMode
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.model.SortDirection
import app.deltax.music.shared.model.defaultDirection
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.GlobalYouTubeRepository
import app.deltax.music.shared.ui.menu.BuiltinPlaylistMenuButton
import app.deltax.music.shared.ui.playback.PlaybackController
import app.deltax.music.shared.ui.screens.HistoryContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MobileHistoryScreen(
    database: MusicDatabase,
    playbackController: PlaybackController,
    onBack: () -> Unit,
    formatMonth: (Long) -> String,
    onSongMenu: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    val scope = rememberCoroutineScope()
    val loader =
        remember(database) {
            MobileHistoryLoader { limit, offset, query, mode, direction ->
                database.visibleHistoryPage(limit, offset, query, mode, direction, catalogArtistIds = true, preserveUnicodeOrder = true)
            }
        }
    val rows = loader.rows
    var entries by remember(database) { mutableStateOf(emptyList<HistoryEntryUi>()) }
    var displayedEntries by remember(database) { mutableStateOf(emptyList<HistoryEntryUi>()) }
    val hasMore = loader.hasMore
    var query by remember { mutableStateOf("") }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }
    var sortMode by remember { mutableStateOf(HistorySortMode.NEWEST) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var selectionJob by remember { mutableStateOf<Job?>(null) }
    var selectionRevision by remember { mutableLongStateOf(0L) }
    var selectedHasRemoteEntries by remember(database) { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var failedDeletion by remember { mutableStateOf<Pair<Set<String>, Boolean>?>(null) }
    var failedUpdate by remember { mutableStateOf<(() -> Unit)?>(null) }
    var refreshKey by remember(database) { mutableIntStateOf(0) }
    LaunchedEffect(
        query,
        sortMode,
        sortDirection,
        database,
        refreshKey,
        Settings.shared.innerTubeCookie,
        Settings.shared.dataSyncId,
        Settings.shared.innerTubeAuthUser,
        Settings.shared.ytmSync,
        Settings.shared.pauseListenHistory,
    ) {
        database.historyChanges().collectLatest { loader.refresh(query, sortMode, sortDirection) }
    }
    LaunchedEffect(
        database,
        refreshKey,
        Settings.shared.innerTubeCookie,
        Settings.shared.dataSyncId,
        Settings.shared.innerTubeAuthUser,
        Settings.shared.ytmSync,
        Settings.shared.pauseListenHistory,
    ) {
        try {
            database.syncYouTubeHistory()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
        }
    }
    LaunchedEffect(rows, query, sortMode, sortDirection) {
        val result =
            withContext(Dispatchers.Default) {
                val context = currentCoroutineContext()
                val ui =
                    rows.map {
                        context.ensureActive()
                        it.toHistoryEntry()
                    }
                ui
            }
        entries = result
        displayedEntries = result
        val selectedQuery = query
        while (selectedIds.isNotEmpty()) {
            val selectedAtRead = selectedIds
            val revisionAtRead = selectionRevision
            val visibleIds =
                database
                    .visibleHistoryIds(
                        selectedQuery,
                        catalogArtistIds = true,
                        preserveUnicodeOrder = true,
                    ).mapTo(mutableSetOf(), Long::toString)
            currentCoroutineContext().ensureActive()
            if (query != selectedQuery) break
            if (selectionRevision == revisionAtRead && selectedIds === selectedAtRead) {
                selectedIds = selectedAtRead.intersect(visibleIds)
                break
            }
            // A newer choice superseded this read. Recheck only if it still needs pruning.
            if (selectedIds.all(visibleIds::contains)) break
        }
    }

    LaunchedEffect(selectedIds, rows) {
        selectedHasRemoteEntries = false
        try {
            selectedHasRemoteEntries = database.historyHasRemoteTokens(selectedIds.mapTo(mutableSetOf(), String::toLong))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            selectedHasRemoteEntries = false
        }
    }

    fun deleteEntries(
        ids: Set<String>,
        alsoRemote: Boolean,
    ) {
        if (ids.isEmpty() || isDeleting) return
        selectionJob?.cancel()
        selectionRevision++
        isDeleting = true
        failedDeletion = null
        failedUpdate = null
        scope.launch {
            try {
                database.deleteHistoryEntries(
                    ids.mapTo(mutableSetOf(), String::toLong),
                    removeRemote =
                        if (alsoRemote) {
                            { tokens -> GlobalYouTubeRepository.instance.removeHistoryItems(tokens) }
                        } else {
                            null
                        },
                )
                selectedIds = emptySet()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                failedDeletion = ids to alsoRemote
            } finally {
                isDeleting = false
            }
        }
    }

    fun updateEntry(
        songId: String,
        title: String,
        artists: String,
        album: String?,
    ) {
        failedUpdate = null
        failedDeletion = null
        scope.launch {
            try {
                database.updateHistorySong(songId, title, artists, album)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                failedUpdate = { updateEntry(songId, title, artists, album) }
            }
        }
    }

    HistoryContent(
        headerActions = { BuiltinPlaylistMenuButton(BuiltinPlaylist.HISTORY, database) },
        entries = entries,
        displayedEntries = displayedEntries,
        query = query,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortDirectionChange = { sortDirection = it },
        selectedIds = selectedIds,
        isLoading = loader.isLoading,
        onLoadMore = if (hasMore) ({ scope.launch { loader.loadMore() } }) else null,
        isDeleting = isDeleting,
        error =
            when {
                failedDeletion != null -> stringResource(Res.string.delete_failed)
                failedUpdate != null || loader.failed -> stringResource(Res.string.error)
                else -> null
            },
        canDeleteRemote = GlobalYouTubeRepository.instance.isHistorySyncEnabled(),
        selectedHasRemoteEntries = selectedHasRemoteEntries,
        onQueryChange = { value ->
            if (query != value) {
                selectionJob?.cancel()
                selectionRevision++
            }
            query = value
        },
        onSortChange = {
            sortMode = it
            sortDirection = it.defaultDirection
        },
        onRefresh = {
            when {
                failedDeletion != null -> failedDeletion?.let { deleteEntries(it.first, it.second) }
                failedUpdate != null -> failedUpdate?.invoke()
                else -> refreshKey++
            }
        },
        onPlay = { entry -> scope.launch { database.historySong(entry.id.toLong())?.let(playbackController::play) } },
        onSongMenu = { entry -> scope.launch { database.historySong(entry.id.toLong())?.let(onSongMenu) } },
        onToggleSelection = { id ->
            selectionJob?.cancel()
            selectionRevision++
            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        },
        onSelectAll = { ids ->
            selectionJob?.cancel()
            selectionRevision++
            if (ids.isEmpty() || ids.size < selectedIds.size) {
                selectedIds = emptySet()
            } else {
                val selectedQuery = query
                selectionJob =
                    scope.launch {
                        val allIds =
                            database
                                .visibleHistoryIds(
                                    selectedQuery,
                                    catalogArtistIds = true,
                                    preserveUnicodeOrder = true,
                                ).mapTo(mutableSetOf(), Long::toString)
                        currentCoroutineContext().ensureActive()
                        if (query == selectedQuery) {
                            selectedIds = allIds
                            selectionRevision++
                        }
                    }
            }
        },
        onUpdate = { entry, title, artists, album -> updateEntry(entry.songId, title, artists, album) },
        onDelete = { entry, alsoRemote -> deleteEntries(setOf(entry.id), alsoRemote) },
        onDeleteSelected = ::deleteEntries,
        onNavigateBack = onBack,
        formatMonth = formatMonth,
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        modifier = modifier,
        canRefresh = true,
    )
}

private fun StoredHistoryRow.toHistoryEntry() =
    HistoryEntryUi(
        id.toString(),
        songId,
        title,
        artists,
        album,
        thumbnailUrl,
        playedAt,
        remoteFeedbackToken,
        playTimeMs,
        remoteDateLabel,
        remoteOrder,
        hasEstimatedDate = source == "YOUTUBE" && playTimeMs == null,
    )
