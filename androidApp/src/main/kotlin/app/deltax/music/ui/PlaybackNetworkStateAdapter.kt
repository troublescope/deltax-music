package app.deltax.music.ui

import app.deltax.music.shared.youtube.playback.PlaybackNetworkState
import app.deltax.music.shared.youtube.playback.PlaybackNetworkStateStore

object PlaybackNetworkStateAdapter {
    @Volatile
    var latest: PlaybackNetworkState = PlaybackNetworkState(scope = "", connected = true)
        private set

    fun publish(
        scope: String,
        connected: Boolean,
        isCellular: Boolean = false,
    ) {
        latest = PlaybackNetworkState(scope = scope, connected = connected, isCellular = isCellular)
        PlaybackNetworkStateStore.publish(latest)
    }
}
