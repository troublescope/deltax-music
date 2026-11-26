package app.deltax.music.shared.ui.wrapped

import app.deltax.music.shared.model.SongItem
import app.deltax.music.shared.playback.downloadSabrAudio
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.GlobalYouTube
import app.deltax.music.shared.state.GlobalYouTubeRepository
import app.deltax.music.shared.storage.downloadAudioWithRanges
import app.deltax.music.shared.youtube.extraction.models.toContentHints
import com.metrolist.innertubex.extraction.AudioQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URI

internal open class JvmWrappedAudioLibrary(
    protected val directory: File,
) : WrappedAudioLibrary {
    @Volatile
    private var closed = false

    override suspend fun prepare(song: SongItem): String =
        withContext(Dispatchers.IO) {
            check(!closed)
            val resolver = checkNotNull(GlobalYouTubeRepository.streamResolver)
            val quality = runCatching { AudioQuality.valueOf(Settings.shared.audioQuality) }.getOrDefault(AudioQuality.AUTO)
            val hints = song.toContentHints(wantVideo = false).withStreamCapabilities(allowHls = false, allowSabr = true)
            val resource = resolver.resolve(song.id, hints, quality, song)
            currentCoroutineContext().ensureActive()
            check(!closed && !resource.isLive && !resource.isMuxedProgressive)
            check(resource.clientName == "local" || resource.isSabr || resource.isDirectFile) {
                "Wrapped soundtrack requires local, SABR or direct audio"
            }
            check(directory.isDirectory || directory.mkdirs())
            val file = File.createTempFile("track-", ".audio", directory)
            var complete = false
            try {
                val budget = minOf(WRAPPED_AUDIO_FILE_LIMIT, directory.usableSpace - WRAPPED_AUDIO_RESERVE)
                if (resource.clientName == "local") {
                    openLocalAudio(resource.url).use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var written = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                check(!closed)
                                val length = input.read(buffer)
                                if (length < 0) break
                                check(written + length <= budget && directory.usableSpace >= length + WRAPPED_AUDIO_RESERVE)
                                output.write(buffer, 0, length)
                                written += length
                            }
                            check(written >= 1024 && (resource.contentLengthBytes == null || written == resource.contentLengthBytes))
                        }
                    }
                } else if (resource.isSabr) {
                    downloadSabrAudio(resource, file, maxBytes = budget)
                } else {
                    RandomAccessFile(file, "rw").use { output ->
                        downloadAudioWithRanges(GlobalYouTube.client, resource, budget) { offset, bytes, length ->
                            check(!closed && directory.usableSpace >= length + WRAPPED_AUDIO_RESERVE)
                            output.seek(offset)
                            output.write(bytes, 0, length)
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                check(!closed)
                complete = true
                file.absolutePath
            } finally {
                if (!complete) file.delete()
                if (closed) directory.deleteRecursively()
            }
        }

    protected open fun openLocalAudio(url: String): InputStream {
        val file = if (url.startsWith("file:")) File(URI(url)) else File(url)
        check(file.isAbsolute && file.isFile)
        return file.inputStream()
    }

    override fun close() {
        closed = true
        directory.deleteRecursively()
    }
}
