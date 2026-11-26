package app.deltax.music.shared.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.deltax.music.shared.playback.PendingYouTubePlaybackReportStore
import app.deltax.music.shared.playlist.LibraryDeletionTracker
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.SongLikeState
import app.deltax.music.shared.youtube.logging.YtLogger
import com.metrolist.innertubex.InnerTube

object YouTubeSessionState {
    var showExpiredSessionNotice by mutableStateOf(false)
        private set

    internal fun notifyExpiredSession() {
        YouTubeSyncState.clear()
        showExpiredSessionNotice = true
    }

    fun dismissExpiredSessionNotice() {
        showExpiredSessionNotice = false
    }
}

internal fun isExpiredYouTubeSessionResponse(
    statusCode: Int,
    hasAuthenticatedSession: Boolean,
    forbiddenExpiresSession: Boolean = false,
): Boolean =
    hasAuthenticatedSession &&
        (statusCode == 401 || (forbiddenExpiresSession && statusCode == 403))

internal fun invalidateYouTubeSession(innerTube: InnerTube) {
    YouTubeSyncState.clear()
    clearStoredYouTubeSession()
    innerTube.replaceSession(
        cookie = null,
        visitorData = null,
        dataSyncId = null,
        authUser = "0",
        useLoginForBrowse = false,
    )
    LibraryDeletionTracker.clear()
}

internal fun invalidateYouTubeSessionIfCurrent(
    innerTube: InnerTube,
    expectedSession: InnerTube.SessionSnapshot,
): Boolean = innerTube.clearSessionIfMatches(expectedSession)

private fun clearStoredYouTubeSession() {
    YtLogger
        .w("YouTubeSession", "Clearing stored YouTube session data from Settings")
    runCatching { Settings.shared.lastHomePageJson = "" }
    Settings.shared.applyStringBatch(
        mapOf(
            "innerTubeCookie" to "",
            "visitorData" to "",
            "dataSyncId" to "",
            "innerTubeAuthUser" to "0",
            "accountName" to "",
            "accountEmail" to "",
            "accountChannelHandle" to "",
            "accountThumbnail" to "",
        ),
    )
    runCatching { Settings.shared.lastLibraryRefreshEpoch = 0L }
    runCatching { Settings.shared.lastLibraryRefreshAccount = "" }
    runCatching { Settings.shared.lastRemoteLibraryItemKeysJson = "[]" }
    runCatching { Settings.shared.pendingRemoteLibraryRemovalKeysJson = "[]" }
    runCatching { Settings.shared.deletedPlaylistIdsJson = "[]" }
    runCatching { PendingYouTubePlaybackReportStore.clear() }
    runCatching { SongLikeState.activateCurrentAccount() }
}
