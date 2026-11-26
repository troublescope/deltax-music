package app.deltax.music.shared.youtube.di

import app.deltax.music.shared.BuildConfig
import app.deltax.music.shared.api.lyrics.LyprioLyricsProvider
import app.deltax.music.shared.api.lyrics.LyricsClient
import app.deltax.music.shared.api.lyrics.LyricsFetcher
import app.deltax.music.shared.api.lyrics.YouTubeLyricsApi
import app.deltax.music.shared.api.musicvideo.MusicVideoResolver
import app.deltax.music.shared.repository.LyricsRepository
import app.deltax.music.shared.state.GlobalYouTube
import app.deltax.music.shared.youtube.logging.MetrolistInnerTubeLogger
import app.deltax.music.shared.youtube.lyrics.InnerTubeLyricsApi
import app.deltax.music.shared.youtube.playback.DefaultStreamResolver
import app.deltax.music.shared.youtube.playback.LocalAudioSource
import app.deltax.music.shared.youtube.playback.MetrolistPlayerConfigRepository
import app.deltax.music.shared.youtube.playback.PlatformTokenProvider
import app.deltax.music.shared.youtube.playback.StreamResolver
import app.deltax.music.shared.youtube.strategy.ClientHealthTracker
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.StreamExtractor
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.YtConfigParser
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.strategy.ClientFallbackStrategy
import com.metrolist.innertubex.extraction.strategy.ClientHealthMonitor
import com.metrolist.innertubex.extraction.strategy.ContentAwareFallbackStrategy
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val sharedModule =
    module {
        single {
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
        single { RemotePlayerConfigStore(get(), MetrolistPlayerConfigRepository, MetrolistInnerTubeLogger) }
        single<YtConfigParser> {
            YtConfigParserImpl(
                get(),
                get(),
                get(),
                MetrolistInnerTubeLogger,
                cipherService = get<YouTubeCipherService>(),
            )
        }

        single { ClientHealthTracker() }
        single<ClientHealthMonitor> { get<ClientHealthTracker>() }
        single<ClientFallbackStrategy> { ContentAwareFallbackStrategy(get()) }

        single { YouTubeCipherService(get(), get(), MetrolistInnerTubeLogger) }

        single<TokenProvider> { PlatformTokenProvider() }

        single<StreamExtractor> {
            InnerTubeExtractor(
                configParser = get(),
                cipherService = get<YouTubeCipherService>(),
                innerTube = get<InnerTube>(),
                fallbackStrategy = get(),
                tokenProvider = get<TokenProvider>(),
                clientHealthMonitor = get(),
                logger = MetrolistInnerTubeLogger,
            )
        }

        single<StreamResolver> {
            DefaultStreamResolver(
                extractor = get(),
                innerTube = get(),
                tokenProvider = getOrNull(),
                clientHealthTracker = get(),
                playerConfigRefresh = { get<YouTubeCipherService>().refreshAfterStreamRejection() },
                musicVideoResolver = get(),
                localAudioSource = getOrNull<LocalAudioSource>(),
            )
        }

        single<MusicVideoResolver> { MusicVideoResolver() }

        single { GlobalYouTube.repository }
        single<YouTubeLyricsApi> { InnerTubeLyricsApi(get()) }
        single {
            LyricsClient(
                httpClient = get(),
                youtubeLyricsApi = get(),
                lyprioLyricsProvider =
                    LyprioLyricsProvider.createOrNull(
                        httpClient = get(),
                        baseUrl = BuildConfig.LYPRIO_BASE_URL,
                        signingKeyBase64 = BuildConfig.LYPRIO_HMAC_KEY,
                        allowInsecureHttp = BuildConfig.DEBUG,
                    ),
            )
        }
        single<LyricsFetcher> { get<LyricsClient>() }
        single {
            LyricsRepository(
                lyricsClient = get(),
                databaseDao = getOrNull(),
                youtubeLyricsApi = get(),
            )
        }
    }
