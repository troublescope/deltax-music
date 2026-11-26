package app.deltax.music.playback

import app.deltax.music.shared.equalizer.EqualizerConfiguration

internal data class AudioOffloadEligibility(
    val equalizer: EqualizerConfiguration,
    val normalizationGain: Float,
    val skipSilence: Boolean,
    val speed: Float,
    val pitch: Float,
    val crossfade: Boolean,
    val meterActive: Boolean,
    val audioResourceAvailable: Boolean,
    val outputFailed: Boolean = false,
) {
    val canOffload: Boolean
        get() =
            audioResourceAvailable && !outputFailed &&
                equalizer.isIdentity &&
                normalizationGain == 1f &&
                !skipSilence &&
                speed == 1f && pitch == 1f &&
                !crossfade && !meterActive
}
