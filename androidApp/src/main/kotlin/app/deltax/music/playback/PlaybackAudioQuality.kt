package app.deltax.music.playback

import app.deltax.music.shared.settings.Settings
import com.metrolist.innertubex.extraction.AudioQuality

internal fun playbackAudioQuality(): AudioQuality =
    try {
        AudioQuality.valueOf(Settings.shared.audioQuality)
    } catch (_: IllegalArgumentException) {
        AudioQuality.AUTO
    }
