package app.deltax.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deltax.music.playback.SongDownloadManager
import app.deltax.music.shared.api.YouTubeRepository
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.model.AlbumItem
import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.settings.BlockedContent
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.AlbumLikeState
import app.deltax.music.shared.state.AlbumLikeToggleAlreadyInProgressException
import app.deltax.music.shared.state.GlobalYouTubeRepository
import app.deltax.music.shared.viewmodels.AlbumViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AlbumViewModel(
    val albumId: String? = null,
    private val database: MusicDatabase,
    private val downloadManager: SongDownloadManager,
    private val applicationScope: CoroutineScope,
    repository: YouTubeRepository = GlobalYouTubeRepository.instance,
) : ViewModel() {
    private val sharedViewModel =
        AlbumViewModel(albumId, viewModelScope, repository)

    val albumPage = sharedViewModel.albumPage
    val albumInfo = sharedViewModel.albumInfo
    val songs = sharedViewModel.songs
    val isLoading = sharedViewModel.isLoading
    val isLoadingMore = sharedViewModel.isLoadingMore
    val errorMessage = sharedViewModel.errorMessage
    val selectedAlbumId = sharedViewModel.selectedAlbumId
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()
    private var favoriteJob: Job? = null

    init {
        albumId?.let(::refreshFavorite)
    }

    fun loadAlbum() = sharedViewModel.loadAlbum()

    fun loadAlbum(albumId: String) {
        sharedViewModel.loadAlbum(albumId)
        refreshFavorite(albumId)
    }

    fun refresh() = sharedViewModel.refresh()

    fun loadMore(lastVisibleSongIndex: Int) = sharedViewModel.loadMore(lastVisibleSongIndex)

    suspend fun allSongs(): List<SongItem> = sharedViewModel.allSongs()

    fun toggleFavorite(
        album: AlbumItem,
        songs: List<SongItem>,
    ) {
        if (savingFavorite) return
        savingFavorite = true
        val cursor =
            albumPage.value
                ?.takeIf { it.album.id == album.id }
                ?.playlistPage
                ?.takeIf { it.hasMore }
        AlbumLikeState.toggleAlbumLike(album, songs) { result ->
            savingFavorite = false
            result
                .onSuccess { liked ->
                    if (selectedAlbumId.value == album.id) _isFavorite.value = liked
                    if (shouldAutoDownloadLikedAlbum(liked, Settings.shared.autoDownloadSavedCollections, songs.size, cursor != null)) {
                        applicationScope.launch(Dispatchers.IO) {
                            try {
                                val allSongs =
                                    songsForLikedAlbumDownload(songs, cursor != null) {
                                        GlobalYouTubeRepository.instance
                                            .completePlaylistPage(
                                                checkNotNull(cursor),
                                                applyContentFilters = false,
                                            ).songs
                                    }
                                if (allSongs.isNotEmpty()) downloadManager.downloadAll(allSongs)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.w("Unable to load album for automatic download type=%s", e::class.simpleName)
                            }
                        }
                    }
                }.onFailure {
                    if (it !is AlbumLikeToggleAlreadyInProgressException) _favoriteFailed.value = true
                }
        }
    }

    private var savingFavorite = false
    private val _favoriteFailed = MutableStateFlow(false)
    val favoriteFailed = _favoriteFailed.asStateFlow()

    fun consumeFavoriteFailure() {
        _favoriteFailed.value = false
    }

    private fun refreshFavorite(albumId: String) {
        favoriteJob?.cancel()
        favoriteJob =
            viewModelScope.launch(Dispatchers.IO) {
                database.dao().album(albumId).collect { album ->
                    _isFavorite.value = album?.album?.bookmarkedAt != null
                }
            }
    }
}

internal fun shouldAutoDownloadLikedAlbum(
    isLiked: Boolean,
    autoDownloadOnLike: Boolean,
    songCount: Int,
    hasMoreSongs: Boolean,
): Boolean = isLiked && autoDownloadOnLike && (songCount > 0 || hasMoreSongs)

internal suspend fun songsForLikedAlbumDownload(
    visibleSongs: List<SongItem>,
    hasMoreSongs: Boolean,
    loadComplete: suspend () -> List<SongItem>,
): List<SongItem> = BlockedContent.filterPlayableSongs(if (hasMoreSongs) loadComplete() else visibleSongs)
