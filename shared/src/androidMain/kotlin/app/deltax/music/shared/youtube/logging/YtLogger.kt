package app.deltax.music.shared.youtube.logging

import app.deltax.music.shared.youtube.playback.PlaybackEventLevel
import app.deltax.music.shared.youtube.playback.PlaybackEventLog
import timber.log.Timber

actual object YtLogger {
    actual fun d(
        tag: String,
        message: String,
    ) {
        PlaybackEventLog.captureYtLogger(PlaybackEventLevel.DEBUG, tag, message)
        Timber.tag(tag).d(message)
    }

    actual fun w(
        tag: String,
        message: String,
    ) {
        PlaybackEventLog.captureYtLogger(PlaybackEventLevel.WARN, tag, message)
        Timber.tag(tag).w("[YtLogger] $message")
    }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        PlaybackEventLog.captureYtLogger(PlaybackEventLevel.ERROR, tag, message, throwable)
        if (throwable != null) {
            Timber.tag(tag).e(throwable, "[YtLogger] $message")
        } else {
            Timber.tag(tag).e("[YtLogger] $message")
        }
    }
}
