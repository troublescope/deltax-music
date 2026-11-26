package app.deltax.music.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deltax.music.LocalMiniPlayerBottomInset
import app.deltax.music.playback.LocalPlayerConnection
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.error
import app.deltax.music.shared.generated.resources.share_album
import app.deltax.music.shared.generated.resources.share_song
import app.deltax.music.shared.model.AlbumItem
import app.deltax.music.shared.model.ArtistItem
import app.deltax.music.shared.model.PlaylistItem
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.GlobalYouTubeRepository
import app.deltax.music.shared.state.SongLikeState
import app.deltax.music.shared.ui.components.PopupContent
import app.deltax.music.shared.ui.screens.BrowseContent
import app.deltax.music.shared.utils.ShareUrls
import app.deltax.music.ui.component.SwipeGestureHandler
import app.deltax.music.ui.components.handleItemClick
import app.deltax.music.ui.menu.AlbumMenu
import app.deltax.music.ui.menu.ArtistMenu
import app.deltax.music.ui.menu.PlaylistMenu
import app.deltax.music.ui.menu.SongMenu
import app.deltax.music.utils.startActivitySafely
import app.deltax.music.viewmodels.BrowseViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BrowseScreen(
    browseId: String,
    params: String? = null,
    title: String? = null,
    startVideo: Boolean = false,
    onBackClick: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToPlaylist: (String, String?) -> Unit = { _, _ -> },
    onNavigateToPodcast: (String) -> Unit = {},
    onNavigateToBrowse: (String, String?, String) -> Unit = { _, _, _ -> },
) {
    val shareAlbumText = stringResource(Res.string.share_album)
    val shareSongText = stringResource(Res.string.share_song)
    val viewModel: BrowseViewModel =
        koinViewModel(
            key = listOf(browseId, params).joinToString("|"),
            parameters = { parametersOf(browseId, params, title) },
        )
    val page = viewModel.page

    var selectedMenuSong by remember { mutableStateOf<SongItem?>(null) }
    var selectedMenuAlbum by remember { mutableStateOf<AlbumItem?>(null) }
    var selectedMenuArtist by remember { mutableStateOf<ArtistItem?>(null) }
    var selectedMenuPlaylist by remember { mutableStateOf<PlaylistItem?>(null) }
    val playerConnection = LocalPlayerConnection.current
    val currentSong = playerConnection?.currentSong?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val isPlaying = playerConnection?.isPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pageTitle = page.title ?: title
    val songs = page.sections.flatMap { it.items }.filterIsInstance<SongItem>()
    // Song-only pages get the same Play and Shuffle row as expanded Home shelves.
    val showSongActions = songs.isNotEmpty() && page.sections.all { section -> section.items.all { it is SongItem } }

    BrowseContent(
        page = page,
        hasMore = viewModel.hasMore,
        browseId = browseId,
        title = pageTitle,
        isLoading = viewModel.isLoading,
        isLoadingMore = viewModel.isLoadingMore,
        error = stringResource(Res.string.error).takeIf { viewModel.loadFailed },
        onRetry = viewModel::load,
        onBackClick = onBackClick,
        onItemClick = { item, songs ->
            handleItemClick(
                item,
                onNavigateToAlbum,
                onNavigateToArtist,
                onNavigateToPlaylist,
                onNavigateToPodcast,
                playerConnection,
                scope,
                onNavigateToBrowse,
                songs,
                startVideo = startVideo,
            )
        },
        onItemLongClick = {
            if (it is SongItem) selectedMenuSong = it
            if (it is AlbumItem) selectedMenuAlbum = it
            if (it is ArtistItem) selectedMenuArtist = it
            if (it is PlaylistItem) selectedMenuPlaylist = it
        },
        loadMoreError = stringResource(Res.string.error).takeIf { viewModel.loadMoreFailed },
        onLoadMore = viewModel::loadMore,
        onLoadMoreRetry = viewModel::loadMore,
        contentPadding = PaddingValues(bottom = LocalMiniPlayerBottomInset.current),
        currentSongId = remember(currentSong) { derivedStateOf { currentSong.value?.id } },
        isPlaying = isPlaying,
        songContainer = { song, content ->
            SwipeGestureHandler(
                onSwipeLeft = { playerConnection?.addToQueue(song) },
                onSwipeRight = { playerConnection?.addToQueue(song, playNext = true) },
                enabled = playerConnection != null && Settings.shared.swipeToSong,
                content = content,
            )
        },
        headerContent =
            if (showSongActions) {
                {
                    HomeSectionActions(
                        onPlayAll = { playerConnection?.playQueue(songs, queueTitle = pageTitle, startVideo = false) },
                        onShuffle = { playerConnection?.shuffleQueue(songs, queueTitle = pageTitle) },
                    )
                }
            } else {
                null
            },
    )

    val context = LocalContext.current

    fun withAlbumSongs(
        album: AlbumItem,
        action: (List<SongItem>) -> Unit,
    ) {
        scope.launch {
            GlobalYouTubeRepository.instance
                .getAlbum(album.id)
                ?.songs
                ?.takeIf { it.isNotEmpty() }
                ?.let(action)
        }
    }

    PopupContent(selectedMenuArtist) { artist ->
        ArtistMenu(
            artist = artist,
            onDismiss = { selectedMenuArtist = null },
            onShuffle = {
                scope.launch {
                    playerConnection?.shuffleQueue(
                        GlobalYouTubeRepository.instance.getArtistSongs(artist.id),
                        artist.title,
                        songsOnly = true,
                    )
                }
            },
        )
    }
    PopupContent(selectedMenuPlaylist) { playlist ->
        PlaylistMenu(
            playlist = playlist,
            onDismiss = { selectedMenuPlaylist = null },
            onPlay = { playerConnection?.playPlaylist(playlist.id, playlist.title) },
            onShuffle = { playerConnection?.playPlaylist(playlist.id, playlist.title, shuffle = true) },
        )
    }
    PopupContent(selectedMenuAlbum) { album ->
        AlbumMenu(
            album = album,
            onDismiss = { selectedMenuAlbum = null },
            onPlay = { playerConnection?.playAlbum(album.id, album.title) },
            onShuffle = { playerConnection?.playAlbum(album.id, album.title, shuffle = true) },
            onShare = {
                val chooser =
                    Intent
                        .createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, ShareUrls.album(album.id))
                            },
                            shareAlbumText,
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivitySafely(chooser)
            },
            onPlayNext = {
                withAlbumSongs(album) { songs -> playerConnection?.addToQueue(songs, playNext = true) }
            },
            onAddToQueue = { withAlbumSongs(album) { songs -> playerConnection?.addToQueue(songs) } },
            onGoToArtist = onNavigateToArtist,
        )
    }
    PopupContent(selectedMenuSong) { song ->
        SongMenu(
            song = song,
            isLiked = SongLikeState.isLiked(song.id),
            onDismiss = { selectedMenuSong = null },
            onPlayNext = { playerConnection?.addToQueue(song, playNext = true) },
            onAddToQueue = { playerConnection?.addToQueue(song) },
            onLike = { SongLikeState.toggleLike(song) },
            onShare = {
                val chooser =
                    Intent
                        .createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, ShareUrls.song(song.id))
                            },
                            shareSongText,
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivitySafely(chooser)
            },
            onGoToAlbum = { song.album?.id?.let(onNavigateToAlbum) },
            onGoToArtist = onNavigateToArtist,
        )
    }
}
