/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.deltax.music.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deltax.music.LocalMiniPlayerBottomInset
import app.deltax.music.LocalStartupPresentation
import app.deltax.music.playback.LocalPlayerConnection
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.error_occurred
import app.deltax.music.shared.generated.resources.home_load_failed
import app.deltax.music.shared.generated.resources.home_offline_empty
import app.deltax.music.shared.generated.resources.no_playable_items
import app.deltax.music.shared.generated.resources.pin_to_speed_dial
import app.deltax.music.shared.generated.resources.player_not_ready
import app.deltax.music.shared.generated.resources.recognize_music
import app.deltax.music.shared.generated.resources.refresh
import app.deltax.music.shared.generated.resources.refreshing
import app.deltax.music.shared.generated.resources.shuffle
import app.deltax.music.shared.generated.resources.try_again
import app.deltax.music.shared.generated.resources.unpin_from_speed_dial
import app.deltax.music.shared.model.BrowseItem
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.model.YTItem
import app.deltax.music.shared.model.isAvailableOffline
import app.deltax.music.shared.settings.LibraryFilterType
import app.deltax.music.shared.ui.LocalIsTv
import app.deltax.music.shared.ui.components.ChipShelf
import app.deltax.music.shared.ui.components.HomeLoadingPlaceholder
import app.deltax.music.shared.ui.components.PopupVisibility
import app.deltax.music.shared.ui.components.RefreshIndicator
import app.deltax.music.shared.ui.icons.MetrolistIcons
import app.deltax.music.shared.ui.navigation.LocalRootContentActive
import app.deltax.music.shared.ui.screens.HomeContent
import app.deltax.music.shared.ui.screens.HomeShelf
import app.deltax.music.shared.ui.screens.homeContentText
import app.deltax.music.shared.ui.tv.RequestTvFocus
import app.deltax.music.shared.ui.tv.tvFocusHighlight
import app.deltax.music.shared.ui.wrapped.WrappedHomeCard
import app.deltax.music.shared.youtube.playback.PlaybackNetworkStateStore
import app.deltax.music.ui.component.AdaptiveModalBottomSheet
import app.deltax.music.ui.component.tabContentTopPadding
import app.deltax.music.ui.components.ErrorBox
import app.deltax.music.ui.components.handleItemClick
import app.deltax.music.ui.utils.playSong
import app.deltax.music.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun HomeScreen(
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToPlaylist: (String, String?) -> Unit = { _, _ -> },
    onNavigateToPodcast: (String) -> Unit = {},
    onNavigateToRecognition: () -> Unit = {},
    onNavigateToWrapped: () -> Unit = {},
    onNavigateToBrowse: (String, String?, String) -> Unit = { _, _, _ -> },
    onNavigateToVideoBrowse: (String, String?, String) -> Unit = onNavigateToBrowse,
    onNavigateToHomeSection: (HomeShelf) -> Unit = {},
    onNavigateToLibrary: (LibraryFilterType?) -> Unit = {},
    floatingActionsBottomInset: State<Dp>,
    scrollToTopEvents: Flow<Long> = emptyFlow(),
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
) {
    val playerNotReadyText = stringResource(Res.string.player_not_ready)
    val noPlayableItemsText = stringResource(Res.string.no_playable_items)
    val shuffleText = stringResource(Res.string.shuffle)

    val rootActive = LocalRootContentActive.current
    val context = LocalContext.current
    val uiState by viewModel.shared.uiState.collectAsStateWithLifecycle()
    val startupPresentation = LocalStartupPresentation.current
    SideEffect { startupPresentation?.homeCacheReady = !uiState.isRestoringCache }
    val playerConnection = LocalPlayerConnection.current
    val currentSong = playerConnection?.currentSong?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val isPlaying = playerConnection?.isPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val database: MusicDatabase = koinInject()
    val scope = rememberCoroutineScope()
    val networkState by PlaybackNetworkStateStore.state.collectAsStateWithLifecycle()
    val cachedSongIds by remember(database) {
        database
            .dao()
            .cachedAudioIds()
            .map { it.toSet() }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = emptySet())
    val isOffline = !networkState.connected
    val isHomeItemEnabled: (YTItem) -> Boolean =
        remember(isOffline, cachedSongIds) {
            { item -> !isOffline || item.isAvailableOffline(cachedSongIds) }
        }

    val listState = rememberLazyListState()

    var lastScrollToTopEvent by rememberSaveable { mutableLongStateOf(-1L) }
    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { event ->
            if (event != lastScrollToTopEvent) {
                lastScrollToTopEvent = event
                listState.scrollToItem(0)
            }
        }
    }

    var selectedMenuItem by remember { mutableStateOf<YTItem?>(null) }
    var showSpeedDialSurpriseMenu by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val showTopRefreshIndicator = isRefreshing || uiState.isRefreshing
    val isTv = LocalIsTv.current
    // Keep scroll-driven visibility out of HomeScreen's composition scope so scrolling never recomposes the feed.
    val showFloatingActions = remember { mutableStateOf(true) }
    val fabNestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        when {
                            available.y < -1f -> showFloatingActions.value = false
                            available.y > 1f -> showFloatingActions.value = true
                        }
                    }
                    return Offset.Zero
                }
            }
        }

    val selectedChip = uiState.selectedChip

    val miniPlayerInset = LocalMiniPlayerBottomInset.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val enablePullToRefresh = !isTv
    val topPadding = tabContentTopPadding()

    val initialTvFocusRequester = remember { FocusRequester() }
    RequestTvFocus(initialTvFocusRequester, enabled = rootActive && !showTopRefreshIndicator)

    val shuffleHomeContent =
        remember(playerConnection, context, playerNotReadyText, noPlayableItemsText, shuffleText, uiState, isHomeItemEnabled) {
            {
                if (playerConnection == null) {
                    Toast.makeText(context, playerNotReadyText, Toast.LENGTH_SHORT).show()
                } else {
                    val songs = uiState.loadedHomeSongs().filter(isHomeItemEnabled)
                    if (songs.isEmpty()) {
                        Toast.makeText(context, noPlayableItemsText, Toast.LENGTH_SHORT).show()
                    } else {
                        playerConnection.shuffleQueue(
                            songs,
                            queueTitle = shuffleText,
                        )
                    }
                }
            }
        }

    val sharedText = homeContentText()
    val onHomeItemClick: (YTItem) -> Unit =
        remember(
            onNavigateToAlbum,
            onNavigateToArtist,
            onNavigateToPlaylist,
            onNavigateToPodcast,
            playerConnection,
            scope,
            onNavigateToBrowse,
        ) {
            { item ->
                handleItemClick(
                    item,
                    onNavigateToAlbum,
                    onNavigateToArtist,
                    onNavigateToPlaylist,
                    onNavigateToPodcast,
                    playerConnection,
                    scope,
                    onNavigateToBrowse,
                    startVideo = false,
                )
            }
        }
    val onVideoItemClick: (YTItem) -> Unit =
        remember(playerConnection, scope, onNavigateToVideoBrowse) {
            { item ->
                when (item) {
                    is SongItem -> playSong(item, playerConnection, scope)
                    is BrowseItem -> onNavigateToVideoBrowse(item.id, item.params, item.title)
                    else -> Unit
                }
            }
        }
    val onSpeedDialItemClick: (YTItem) -> Unit =
        remember(playerConnection, onHomeItemClick) {
            { item ->
                if (item is SongItem) {
                    playerConnection?.startRadio(item)
                } else {
                    onHomeItemClick(item)
                }
            }
        }
    val onItemLongClick: (YTItem) -> Unit = remember { { selectedMenuItem = it } }
    val onShuffleSongs: (List<SongItem>) -> Unit =
        remember(playerConnection, sharedText.shuffle) {
            { songs -> playerConnection?.shuffleQueue(songs, queueTitle = sharedText.shuffle) }
        }
    val onPlaySongs: (List<SongItem>) -> Unit =
        remember(playerConnection, sharedText.quickPicks) {
            { songs -> playerConnection?.playQueue(songs, queueTitle = sharedText.quickPicks) }
        }
    val onPlayAlbum: (app.deltax.music.shared.model.AlbumItem) -> Unit =
        remember(playerConnection) {
            { album -> playerConnection?.playAlbum(album.id, album.title) }
        }
    val onShowSurpriseMenu: () -> Unit = remember { { showSpeedDialSurpriseMenu = true } }
    val currentSongId = remember(currentSong) { derivedStateOf { currentSong.value?.id } }
    val contentPadding =
        remember(topPadding, miniPlayerInset) {
            PaddingValues(
                top = topPadding,
                bottom = miniPlayerInset + 16.dp,
            )
        }
    val homeModifier =
        remember(startupPresentation) {
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    startupPresentation?.homeLaidOut = true
                }.focusGroup()
                .focusRestorer()
        }

    val homeContent: @Composable () -> Unit = {
        HomeContent(
            uiState = uiState,
            shelfCache = viewModel.shared.shelfCache,
            onLoadMore = viewModel.shared::loadMoreSections,
            text = sharedText,
            onVideoItemClick = onVideoItemClick,
            onItemClick = onHomeItemClick,
            onSpeedDialItemClick = onSpeedDialItemClick,
            onItemLongClick = onItemLongClick,
            onShuffleSongs = onShuffleSongs,
            onPlaySongs = onPlaySongs,
            onLibraryClick = onNavigateToLibrary,
            onLocalShelfClick = onNavigateToHomeSection,
            onArtistClick = onNavigateToArtist,
            onPlayAlbum = onPlayAlbum,
            isItemEnabled = isHomeItemEnabled,
            onShowSurpriseMenu = onShowSurpriseMenu,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
            isLandscape = isLandscape,
            speedDialSurpriseAtEndOfFirstPage = true,
            listState = listState,
            contentPadding = contentPadding,
            modifier = homeModifier,
            header = {
                if (isTv) {
                    item(key = "tv_refresh", contentType = "tv_refresh") {
                        Button(
                            onClick = {
                                isRefreshing = true
                                viewModel.shared.refresh { isRefreshing = false }
                            },
                            enabled = rootActive && !showTopRefreshIndicator,
                            modifier =
                                Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .focusRequester(initialTvFocusRequester)
                                    .tvFocusHighlight(),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(if (showTopRefreshIndicator) Res.string.refreshing else Res.string.refresh))
                        }
                    }
                }
                when {
                    uiState.showLoadingPlaceholder -> {
                        item(key = "home_loading", contentType = "home_loading") {
                            HomeLoadingPlaceholder(isLandscape = isLandscape)
                        }
                    }

                    isOffline && uiState.hasNoHomeContent() -> {
                        item(key = "home_offline", contentType = "home_offline") {
                            Text(stringResource(Res.string.home_offline_empty), modifier = Modifier.padding(24.dp))
                        }
                    }

                    !isOffline && !uiState.error.isNullOrBlank() -> {
                        item(key = "home_error", contentType = "home_error") {
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Text(stringResource(Res.string.error_occurred))
                                ErrorBox(stringResource(Res.string.home_load_failed))
                                TextButton(onClick = { viewModel.shared.refresh {} }, shapes = ButtonDefaults.shapes()) {
                                    Text(stringResource(Res.string.try_again))
                                }
                            }
                        }
                    }

                    else -> {
                        item(key = "home_chips", contentType = "home_chips") {
                            ChipShelf(
                                options = uiState.chips,
                                selectedOption = selectedChip,
                                onSelectionChange = viewModel.shared::toggleChip,
                                label = { it.title },
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        item(key = "wrapped_card", contentType = "wrapped_card") {
                            WrappedHomeCard(
                                onClick = onNavigateToWrapped,
                                onDismissed = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                            )
                        }
                    }
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().nestedScroll(fabNestedScrollConnection),
    ) {
        if (enablePullToRefresh) {
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.shared.refresh { isRefreshing = false }
                },
                state = pullToRefreshState,
                indicator = {
                    RefreshIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = topPadding),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                homeContent()
            }
        } else {
            homeContent()
        }

        if (!isTv) {
            HomeFloatingActions(
                visibleState = showFloatingActions,
                bottomInsetState = floatingActionsBottomInset,
                onNavigateToRecognition = onNavigateToRecognition,
                onShuffle = shuffleHomeContent,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }

    HomeItemMenu(
        selectedItem = selectedMenuItem,
        onDismiss = { selectedMenuItem = null },
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToPodcast = onNavigateToPodcast,
        onSongMetadataUpdated = viewModel.shared::updateSongMetadata,
        active = rootActive,
    )

    PopupVisibility(visible = rootActive && showSpeedDialSurpriseMenu) {
        HomeSpeedDialSurpriseMenu(
            isPinned = uiState.speedDialSurprisePinned,
            onDismiss = { showSpeedDialSurpriseMenu = false },
            onSetPinned = viewModel.shared::setSpeedDialSurprisePinned,
        )
    }
}

@Composable
private fun HomeFloatingActions(
    visibleState: State<Boolean>,
    bottomInsetState: State<Dp>,
    onNavigateToRecognition: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = LocalIsTv.current || visibleState.value,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier.padding(bottom = bottomInsetState.value),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SmallFloatingActionButton(
                onClick = onNavigateToRecognition,
                shape = RoundedCornerShape(8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = MetrolistIcons.Outlined.AudioCapture,
                    contentDescription = stringResource(Res.string.recognize_music),
                )
            }
            FloatingActionButton(
                onClick = onShuffle,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = MetrolistIcons.Rounded.Shuffle,
                    contentDescription = stringResource(Res.string.shuffle),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeSpeedDialSurpriseMenu(
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onSetPinned: (Boolean) -> Unit,
) {
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(Res.string.shuffle),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            ListItem(
                leadingContent = {
                    Icon(
                        if (isPinned) MetrolistIcons.Outlined.KeepOff else MetrolistIcons.Filled.PushPin,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier.clickable {
                        onSetPinned(!isPinned)
                        onDismiss()
                    },
            ) {
                Text(stringResource(if (isPinned) Res.string.unpin_from_speed_dial else Res.string.pin_to_speed_dial))
            }
        }
    }
}
