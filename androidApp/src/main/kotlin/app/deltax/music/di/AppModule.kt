/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package app.deltax.music.di

import app.deltax.music.playback.AudioCacheManager
import app.deltax.music.playback.ExoPlaybackBenchmarkPlayer
import app.deltax.music.playback.PlayerConnection
import app.deltax.music.playback.SongDownloadManager
import app.deltax.music.shared.api.YouTubeRepository
import app.deltax.music.shared.api.musicvideo.MusicVideoResolver
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.db.daos.DatabaseDao
import app.deltax.music.shared.db.entities.toSongItem
import app.deltax.music.shared.db.getDatabaseBuilder
import app.deltax.music.shared.youtube.benchmark.PlaybackBenchmarkEnvironment
import app.deltax.music.shared.youtube.benchmark.PlaybackBenchmarkRunner
import app.deltax.music.shared.youtube.logging.MetrolistInnerTubeLogger
import app.deltax.music.shared.youtube.playback.DefaultStreamResolver
import app.deltax.music.shared.youtube.playback.LocalAudioSource
import app.deltax.music.shared.youtube.strategy.ClientHealthTracker
import app.deltax.music.widget.MetrolistWidgetManager
import app.deltax.music.widget.PlaylistWidgetManager
import coil3.ImageLoader
import coil3.imageLoader
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.strategy.ContentAwareFallbackStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule =
    module {
        single<CoroutineScope> {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        single<MusicDatabase> {
            getDatabaseBuilder(androidContext()).build()
        }

        single<DatabaseDao> {
            get<MusicDatabase>().dao()
        }

        single<AudioCacheManager> {
            AudioCacheManager(
                context = androidContext(),
                database = get(),
            )
        }

        single<LocalAudioSource> {
            LocalAudioSource { id, song ->
                val storedSong = song ?: get<MusicDatabase>().dao().getSongById(id)?.toSongItem()
                storedSong?.let { get<AudioCacheManager>().getCachedPlaybackResource(it) }
            }
        }

        single<SongDownloadManager> {
            SongDownloadManager(
                context = androidContext(),
                audioCacheManager = get(),
                database = get(),
            )
        }

        single<ImageLoader> {
            androidContext().imageLoader
        }

        single<PlayerConnection> {
            PlayerConnection(
                context = androidContext(),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            )
        }

        single<PlaylistWidgetManager> {
            PlaylistWidgetManager(
                context = androidContext(),
                database = get(),
                applicationScope = get(),
                imageLoader = get(),
            )
        }

        single<MetrolistWidgetManager> {
            MetrolistWidgetManager(
                context = androidContext(),
                playlistWidgetManager = get(),
                imageLoader = get(),
            )
        }

        single {
            val innerTube = get<InnerTube>()
            val tokenProvider = get<TokenProvider>()
            val cipherService = get<YouTubeCipherService>()
            val musicVideoResolver = get<MusicVideoResolver>()
            PlaybackBenchmarkRunner(
                environmentFor = { anonymousSession ->
                    val benchmarkHealthTracker = ClientHealthTracker(persistenceEnabled = false)
                    val benchmarkInnerTube = innerTube.createIsolatedSession(includeAccount = !anonymousSession)
                    val benchmarkExtractor =
                        InnerTubeExtractor(
                            configParser =
                                YtConfigParserImpl(
                                    innerTube.httpClient,
                                    benchmarkInnerTube,
                                    get<RemotePlayerConfigStore>(),
                                    MetrolistInnerTubeLogger,
                                    cipherService = cipherService,
                                ),
                            cipherService = cipherService,
                            innerTube = benchmarkInnerTube,
                            fallbackStrategy = ContentAwareFallbackStrategy(benchmarkHealthTracker),
                            tokenProvider = tokenProvider,
                            clientHealthMonitor = benchmarkHealthTracker,
                            logger = MetrolistInnerTubeLogger,
                        )
                    PlaybackBenchmarkEnvironment(
                        httpClient = benchmarkInnerTube.httpClient,
                        youtubeRepository = YouTubeRepository(benchmarkInnerTube),
                        // The runner calls this off the main thread; ExoPlayer must be created and configured on main.
                        playerFactory = { runBlocking(Dispatchers.Main) { ExoPlaybackBenchmarkPlayer(androidContext()) } },
                        resolverFor = {
                            DefaultStreamResolver(
                                extractor = benchmarkExtractor,
                                innerTube = benchmarkInnerTube,
                                tokenProvider = tokenProvider,
                                clientHealthTracker = benchmarkHealthTracker,
                                playerConfigRefresh = { cipherService.refreshAfterStreamRejection() },
                                musicVideoResolver = musicVideoResolver,
                            )
                        },
                    )
                },
            )
        }

        includes(viewModelsModule)
    }
