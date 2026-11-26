package app.deltax.music.shared.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.album
import app.deltax.music.shared.generated.resources.albums
import app.deltax.music.shared.generated.resources.artist
import app.deltax.music.shared.generated.resources.artist_play_all
import app.deltax.music.shared.generated.resources.artists
import app.deltax.music.shared.generated.resources.back
import app.deltax.music.shared.generated.resources.browse
import app.deltax.music.shared.generated.resources.cached_playlist
import app.deltax.music.shared.generated.resources.cancel
import app.deltax.music.shared.generated.resources.clear
import app.deltax.music.shared.generated.resources.clear_recent_from_searches_question
import app.deltax.music.shared.generated.resources.clear_recent_searches_question
import app.deltax.music.shared.generated.resources.clear_search_history
import app.deltax.music.shared.generated.resources.clear_section
import app.deltax.music.shared.generated.resources.collection_sort_most_played
import app.deltax.music.shared.generated.resources.collection_sort_newest
import app.deltax.music.shared.generated.resources.collection_sort_oldest
import app.deltax.music.shared.generated.resources.delete
import app.deltax.music.shared.generated.resources.delete_all_recent_from_searches_question
import app.deltax.music.shared.generated.resources.delete_all_recent_searches_question
import app.deltax.music.shared.generated.resources.duration
import app.deltax.music.shared.generated.resources.episode
import app.deltax.music.shared.generated.resources.file_size
import app.deltax.music.shared.generated.resources.filter_all
import app.deltax.music.shared.generated.resources.filter_community_playlists
import app.deltax.music.shared.generated.resources.filter_downloaded
import app.deltax.music.shared.generated.resources.filter_episodes
import app.deltax.music.shared.generated.resources.filter_featured_playlists
import app.deltax.music.shared.generated.resources.filter_videos
import app.deltax.music.shared.generated.resources.forgotten_favorites
import app.deltax.music.shared.generated.resources.grid_view
import app.deltax.music.shared.generated.resources.history_sort_listening_time
import app.deltax.music.shared.generated.resources.home_unavailable
import app.deltax.music.shared.generated.resources.keep_listening
import app.deltax.music.shared.generated.resources.library_cached_songs
import app.deltax.music.shared.generated.resources.library_empty
import app.deltax.music.shared.generated.resources.library_favorite_songs
import app.deltax.music.shared.generated.resources.library_offline_songs
import app.deltax.music.shared.generated.resources.library_sort_updated
import app.deltax.music.shared.generated.resources.library_top_songs
import app.deltax.music.shared.generated.resources.library_uploaded_songs
import app.deltax.music.shared.generated.resources.liked
import app.deltax.music.shared.generated.resources.list_view
import app.deltax.music.shared.generated.resources.loading
import app.deltax.music.shared.generated.resources.manual
import app.deltax.music.shared.generated.resources.mix
import app.deltax.music.shared.generated.resources.monthly_audience_short
import app.deltax.music.shared.generated.resources.mood_and_genres
import app.deltax.music.shared.generated.resources.more_options
import app.deltax.music.shared.generated.resources.most_viewed_videos
import app.deltax.music.shared.generated.resources.my_top_100
import app.deltax.music.shared.generated.resources.n_track
import app.deltax.music.shared.generated.resources.new_label
import app.deltax.music.shared.generated.resources.new_releases
import app.deltax.music.shared.generated.resources.other_versions
import app.deltax.music.shared.generated.resources.play
import app.deltax.music.shared.generated.resources.playlist
import app.deltax.music.shared.generated.resources.playlist_direction_ascending
import app.deltax.music.shared.generated.resources.playlist_direction_descending
import app.deltax.music.shared.generated.resources.playlist_lock
import app.deltax.music.shared.generated.resources.playlist_order_original
import app.deltax.music.shared.generated.resources.playlist_reorder
import app.deltax.music.shared.generated.resources.playlists
import app.deltax.music.shared.generated.resources.podcast
import app.deltax.music.shared.generated.resources.podcasts
import app.deltax.music.shared.generated.resources.podcasts_for_you
import app.deltax.music.shared.generated.resources.profiles
import app.deltax.music.shared.generated.resources.quick_picks
import app.deltax.music.shared.generated.resources.recap
import app.deltax.music.shared.generated.resources.recent_from_searches
import app.deltax.music.shared.generated.resources.recent_searches
import app.deltax.music.shared.generated.resources.recognize_music
import app.deltax.music.shared.generated.resources.recommended
import app.deltax.music.shared.generated.resources.remove_from_recent_question
import app.deltax.music.shared.generated.resources.remove_recent_search_action
import app.deltax.music.shared.generated.resources.remove_recent_search_description
import app.deltax.music.shared.generated.resources.remove_recent_search_item_action
import app.deltax.music.shared.generated.resources.remove_recent_search_item_description
import app.deltax.music.shared.generated.resources.remove_recent_search_question
import app.deltax.music.shared.generated.resources.retry
import app.deltax.music.shared.generated.resources.save
import app.deltax.music.shared.generated.resources.search
import app.deltax.music.shared.generated.resources.search_library
import app.deltax.music.shared.generated.resources.search_no_results
import app.deltax.music.shared.generated.resources.search_songs_artists_podcasts
import app.deltax.music.shared.generated.resources.show_less
import app.deltax.music.shared.generated.resources.show_more
import app.deltax.music.shared.generated.resources.shuffle
import app.deltax.music.shared.generated.resources.similar_to
import app.deltax.music.shared.generated.resources.song_count
import app.deltax.music.shared.generated.resources.songs
import app.deltax.music.shared.generated.resources.sort_by_create_date
import app.deltax.music.shared.generated.resources.sort_by_name
import app.deltax.music.shared.generated.resources.sort_by_year
import app.deltax.music.shared.generated.resources.sort_options
import app.deltax.music.shared.generated.resources.speed_dial
import app.deltax.music.shared.generated.resources.start_typing_to_search_music
import app.deltax.music.shared.generated.resources.stored_search_songs
import app.deltax.music.shared.generated.resources.stored_sort_last_used
import app.deltax.music.shared.generated.resources.suggestions
import app.deltax.music.shared.generated.resources.title
import app.deltax.music.shared.generated.resources.top_results
import app.deltax.music.shared.generated.resources.track
import app.deltax.music.shared.generated.resources.tracks
import app.deltax.music.shared.generated.resources.uploaded_playlist_name
import app.deltax.music.shared.generated.resources.use_search_text
import app.deltax.music.shared.generated.resources.use_suggestion
import app.deltax.music.shared.generated.resources.verified_artists
import app.deltax.music.shared.generated.resources.voice_search
import app.deltax.music.shared.model.CollectionSort
import app.deltax.music.shared.model.PlaylistSongSort
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.ui.components.CollectionFilterBarText
import app.deltax.music.shared.ui.components.PlaylistOrderControlsText
import app.deltax.music.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun albumContentText() =
    AlbumContentText(
        play = stringResource(Res.string.artist_play_all),
        shuffle = stringResource(Res.string.shuffle),
        songs = stringResource(Res.string.songs),
        otherVersions = stringResource(Res.string.other_versions),
        showMore = stringResource(Res.string.show_more),
        showLess = stringResource(Res.string.show_less),
    )

@Composable
fun homeContentText() =
    HomeContentText(
        moodsAndGenres = stringResource(Res.string.mood_and_genres),
        speedDial = stringResource(Res.string.speed_dial),
        newReleases = stringResource(Res.string.new_releases),
        similarTo = stringResource(Res.string.similar_to),
        podcastsForYou = stringResource(Res.string.podcasts_for_you),
        charts = stringResource(Res.string.most_viewed_videos),
        albumCharts = stringResource(Res.string.albums),
        editorialPlaylists = stringResource(Res.string.filter_featured_playlists),
        recapPlaylists = stringResource(Res.string.recap),
        shuffle = stringResource(Res.string.shuffle),
        playAll = stringResource(Res.string.play),
        keepListening = stringResource(Res.string.keep_listening),
        quickPicks = stringResource(Res.string.quick_picks),
        forgottenFavorites = stringResource(Res.string.forgotten_favorites),
        loading = stringResource(Res.string.loading),
        tryAgain = stringResource(Res.string.retry),
        homeUnavailable = stringResource(Res.string.home_unavailable),
    )

@Composable
fun libraryContentText() =
    LibraryContentText(
        filterLabels =
            mapOf(
                LibraryFilter.TRACKS to stringResource(Res.string.tracks),
                LibraryFilter.PLAYLISTS to stringResource(Res.string.playlists),
                LibraryFilter.ARTISTS to stringResource(Res.string.artists),
                LibraryFilter.ALBUMS to stringResource(Res.string.albums),
                LibraryFilter.PODCASTS to stringResource(Res.string.podcasts),
            ),
        sortLabels =
            mapOf(
                LibrarySortType.DOWNLOADED to stringResource(Res.string.filter_downloaded),
                LibrarySortType.RECENTLY_ADDED to stringResource(Res.string.sort_by_create_date),
                LibrarySortType.NAME to stringResource(Res.string.sort_by_name),
                LibrarySortType.LAST_UPDATED to stringResource(Res.string.library_sort_updated),
                LibrarySortType.ARTIST to stringResource(Res.string.artist),
                LibrarySortType.YEAR to stringResource(Res.string.sort_by_year),
                LibrarySortType.SONG_COUNT to stringResource(Res.string.songs),
                LibrarySortType.LENGTH to stringResource(Res.string.duration),
                LibrarySortType.PLAYTIME to stringResource(Res.string.history_sort_listening_time),
                LibrarySortType.MANUAL to stringResource(Res.string.manual),
            ),
        empty = stringResource(Res.string.library_empty),
        sortOptions = stringResource(Res.string.sort_options),
        grid = stringResource(Res.string.grid_view),
        list = stringResource(Res.string.list_view),
        artist = stringResource(Res.string.artist),
        loading = stringResource(Res.string.loading),
        search = stringResource(Res.string.search),
        searchPlaceholder = stringResource(Res.string.search_library),
        searchNoResults = stringResource(Res.string.search_no_results),
    )

@Composable
fun libraryAutoPlaylists(hasNewUploads: Boolean = false): List<LibraryAutoPlaylist> {
    val newLabel = stringResource(Res.string.new_label)
    val playlists =
        buildList {
            if (Settings.shared.showLikedPlaylist) {
                add(
                    LibraryAutoPlaylist(
                        "liked",
                        stringResource(Res.string.liked),
                        stringResource(Res.string.library_favorite_songs),
                        MetrolistIcons.Rounded.FavoriteBorder,
                    ),
                )
            }
            if (Settings.shared.showDownloadedPlaylist) {
                add(
                    LibraryAutoPlaylist(
                        "downloaded",
                        stringResource(Res.string.filter_downloaded),
                        stringResource(Res.string.library_offline_songs),
                        MetrolistIcons.Outlined.Download,
                    ),
                )
            }
            if (Settings.shared.showTopPlaylist) {
                add(
                    LibraryAutoPlaylist(
                        "top100",
                        stringResource(Res.string.my_top_100),
                        stringResource(Res.string.library_top_songs),
                        MetrolistIcons.AutoMirrored.Outlined.TrendingUp,
                    ),
                )
            }
            if (Settings.shared.showCachedPlaylist) {
                add(
                    LibraryAutoPlaylist(
                        "cached",
                        stringResource(Res.string.cached_playlist),
                        stringResource(Res.string.library_cached_songs),
                        MetrolistIcons.Outlined.Cached,
                    ),
                )
            }
            if (Settings.shared.showUploadedPlaylist) {
                add(
                    LibraryAutoPlaylist(
                        "uploaded",
                        stringResource(Res.string.uploaded_playlist_name),
                        stringResource(Res.string.library_uploaded_songs),
                        MetrolistIcons.Outlined.CloudUpload,
                        badge = newLabel.takeIf { hasNewUploads },
                    ),
                )
            }
        }
    return remember(playlists) { playlists }
}

@Composable
fun collectionFilterBarText(
    includeClear: Boolean = false,
    includeManual: Boolean = false,
) = CollectionFilterBarText(
    searchLabel = stringResource(Res.string.stored_search_songs),
    sortLabels =
        buildMap {
            put(CollectionSort.NEWEST, stringResource(Res.string.sort_by_create_date))
            put(CollectionSort.TITLE, stringResource(Res.string.title))
            put(CollectionSort.ARTIST, stringResource(Res.string.artist))
            put(CollectionSort.SIZE, stringResource(Res.string.file_size))
            put(CollectionSort.LAST_USED, stringResource(Res.string.stored_sort_last_used))
            put(CollectionSort.MOST_PLAYED, stringResource(Res.string.collection_sort_most_played))
            if (includeManual) {
                put(CollectionSort.MANUAL, stringResource(Res.string.manual))
            }
        },
    sortOptions = stringResource(Res.string.sort_options),
    clear = stringResource(Res.string.clear).takeIf { includeClear },
    reorder = if (includeManual) stringResource(Res.string.playlist_reorder) else "",
    lock = if (includeManual) stringResource(Res.string.playlist_lock) else "",
)

@Composable
fun playlistContentText(useTrackCount: Boolean = false) =
    PlaylistContentText(
        play = stringResource(Res.string.artist_play_all),
        shuffle = stringResource(Res.string.shuffle),
        songCount = { count ->
            pluralStringResource(if (useTrackCount) Res.plurals.n_track else Res.plurals.song_count, count, count)
        },
        suggestions = stringResource(Res.string.suggestions),
    )

@Composable
fun playlistOrderControlsText() =
    PlaylistOrderControlsText(
        sortLabels =
            mapOf(
                PlaylistSongSort.ORIGINAL to stringResource(Res.string.playlist_order_original),
                PlaylistSongSort.DATE_ADDED to stringResource(Res.string.sort_by_create_date),
                PlaylistSongSort.TITLE to stringResource(Res.string.title),
                PlaylistSongSort.ARTIST to stringResource(Res.string.artist),
                PlaylistSongSort.ALBUM to stringResource(Res.string.album),
                PlaylistSongSort.DURATION to stringResource(Res.string.duration),
                PlaylistSongSort.YEAR to stringResource(Res.string.sort_by_year),
                PlaylistSongSort.LISTENING_TIME to stringResource(Res.string.history_sort_listening_time),
            ),
        sortOptions = stringResource(Res.string.sort_options),
        ascending = stringResource(Res.string.playlist_direction_ascending),
        descending = stringResource(Res.string.playlist_direction_descending),
        reorder = stringResource(Res.string.playlist_reorder),
        lock = stringResource(Res.string.playlist_lock),
    )

@Composable
fun searchContentText() =
    SearchContentText(
        search = stringResource(Res.string.search),
        suggestions = stringResource(Res.string.suggestions),
        recommended = stringResource(Res.string.recommended),
        recentSearches = stringResource(Res.string.recent_searches),
        recentFromSearches = stringResource(Res.string.recent_from_searches),
        clear = stringResource(Res.string.clear),
        clearHistory = stringResource(Res.string.clear_search_history),
        useSearchText = stringResource(Res.string.use_search_text),
        startTypingToSearch = stringResource(Res.string.start_typing_to_search_music),
        noResultsFound = stringResource(Res.string.search_no_results),
        filterAll = stringResource(Res.string.filter_all),
        filterSongs = stringResource(Res.string.tracks),
        filterAlbums = stringResource(Res.string.albums),
        filterArtists = stringResource(Res.string.artists),
        filterPlaylists = stringResource(Res.string.playlists),
        filterVideos = stringResource(Res.string.filter_videos),
        filterPodcasts = stringResource(Res.string.podcasts),
        browse = stringResource(Res.string.browse),
        verifiedArtists = stringResource(Res.string.verified_artists),
        filterEpisodes = stringResource(Res.string.filter_episodes),
        filterFeaturedPlaylists = stringResource(Res.string.filter_featured_playlists),
        filterCommunityPlaylists = stringResource(Res.string.filter_community_playlists),
        filterProfiles = stringResource(Res.string.profiles),
        topResults = stringResource(Res.string.top_results),
        moreOptions = stringResource(Res.string.more_options),
        clearSection = { title -> stringResource(Res.string.clear_section, title) },
        removeRecentSearchQuestion = stringResource(Res.string.remove_recent_search_question),
        removeRecentSearchDescription = { item -> stringResource(Res.string.remove_recent_search_description, item) },
        removeFromRecentQuestion = stringResource(Res.string.remove_from_recent_question),
        removeRecentItemDescription = { title -> stringResource(Res.string.remove_recent_search_item_description, title) },
        clearRecentSearchesQuestion = stringResource(Res.string.clear_recent_searches_question),
        deleteAllRecentSearchesQuestion = stringResource(Res.string.delete_all_recent_searches_question),
        clearRecentFromSearchesQuestion = stringResource(Res.string.clear_recent_from_searches_question),
        deleteAllRecentFromSearchesQuestion = stringResource(Res.string.delete_all_recent_from_searches_question),
        removeRecentSearchAction = stringResource(Res.string.remove_recent_search_action),
        removeRecentSearchItemAction = stringResource(Res.string.remove_recent_search_item_action),
        delete = stringResource(Res.string.delete),
        cancel = stringResource(Res.string.cancel),
        useSuggestion = stringResource(Res.string.use_suggestion),
        shuffle = stringResource(Res.string.shuffle),
        mix = stringResource(Res.string.mix),
        track = stringResource(Res.string.track),
        album = stringResource(Res.string.album),
        artist = stringResource(Res.string.artist),
        playlist = stringResource(Res.string.playlist),
        podcast = stringResource(Res.string.podcast),
        episode = stringResource(Res.string.episode),
        monthlyAudienceShort = stringResource(Res.string.monthly_audience_short),
        play = stringResource(Res.string.play),
        save = stringResource(Res.string.save),
    )

@Composable
fun searchScreenText() =
    SearchScreenText(
        contentText = searchContentText(),
        searchPlaceholder = stringResource(Res.string.search_songs_artists_podcasts),
        back = stringResource(Res.string.back),
        clear = stringResource(Res.string.clear),
        microphoneContentDescription =
            stringResource(
                if (Settings.shared.voiceSearchEnabled) Res.string.voice_search else Res.string.recognize_music,
            ),
    )
