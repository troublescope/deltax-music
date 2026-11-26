package app.deltax.music.di

import android.app.Application
import app.deltax.music.R
import app.deltax.music.data.PlayHistoryRepository
import app.deltax.music.playback.AndroidPlayerPlatformServices
import app.deltax.music.playback.PlayerConnection
import app.deltax.music.shared.api.artwork.AnimatedArtworkService
import app.deltax.music.shared.api.artwork.DirectCanvasProvider
import app.deltax.music.shared.viewmodels.PlayerViewModel
import app.deltax.music.viewmodels.AboutArtistViewModel
import app.deltax.music.viewmodels.AccountViewModel
import app.deltax.music.viewmodels.AlbumViewModel
import app.deltax.music.viewmodels.ArtistViewModel
import app.deltax.music.viewmodels.BrowseViewModel
import app.deltax.music.viewmodels.HistoryViewModel
import app.deltax.music.viewmodels.HomeViewModel
import app.deltax.music.viewmodels.LibraryViewModel
import app.deltax.music.viewmodels.PlaybackViewModel
import app.deltax.music.viewmodels.PlaylistViewModel
import app.deltax.music.viewmodels.SearchViewModel
import app.deltax.music.viewmodels.StatsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelsModule =
    module {
        singleOf(::PlayHistoryRepository)

        viewModel { StatsViewModel(get<PlayHistoryRepository>()) }

        viewModel { params ->
            AlbumViewModel(
                params.getOrNull(),
                get(),
                get(),
                get<CoroutineScope>(),
            )
        }

        singleOf(::AnimatedArtworkService)

        viewModel { params ->
            BrowseViewModel(params[0], params[1], params[2])
        }

        viewModel { params ->
            ArtistViewModel(params.getOrNull(), get(), get(), get<CoroutineScope>())
        }

        viewModel { (id: String, name: String) -> AboutArtistViewModel(id, name) }

        viewModel {
            PlaybackViewModel(
                androidContext().applicationContext as Application,
                get(),
                get(),
            )
        }

        viewModelOf(::AccountViewModel)
        viewModel { HistoryViewModel(get<PlayHistoryRepository>(), get<CoroutineScope>()) }
        viewModelOf(::LibraryViewModel)
        viewModel {
            HomeViewModel(
                database = get(),
                likedSongsName = androidContext().getString(R.string.liked_songs),
            )
        }
        viewModelOf(::SearchViewModel)
        single { DirectCanvasProvider(get()) }

        viewModel { params ->
            PlaylistViewModel(
                params.getOrNull(),
                get(),
                get<CoroutineScope>(),
                thumbnailHint = params[1],
            )
        }

        single {
            PlayerViewModel(
                playerConnection = get<PlayerConnection>(),
                imageLoader = get(),
                platformContext = androidContext().applicationContext,
                platform = AndroidPlayerPlatformServices(androidContext().applicationContext, get(), get()),
                lyricsRepository = get(),
                animatedArtworkService = get(),
                musicVideoResolver = get(),
                viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            )
        }
    }
