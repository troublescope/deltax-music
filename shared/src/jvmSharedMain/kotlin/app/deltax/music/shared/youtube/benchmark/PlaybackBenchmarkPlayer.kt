package app.deltax.music.shared.youtube.benchmark

import app.deltax.music.shared.youtube.playback.PlaybackResource
import io.ktor.client.HttpClient

interface PlaybackBenchmarkPlayer {
    val failure: Throwable?

    suspend fun prepare(
        resource: PlaybackResource,
        httpClient: HttpClient,
        onResponseStatus: (Int) -> Unit,
    ): Boolean

    suspend fun play()

    suspend fun seekTo(positionMs: Long)

    suspend fun currentPosition(): Long

    suspend fun duration(): Long

    suspend fun isPlaying(): Boolean

    suspend fun wantsPlayback(): Boolean

    suspend fun close()
}
