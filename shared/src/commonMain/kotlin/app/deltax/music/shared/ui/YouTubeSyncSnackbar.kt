package app.deltax.music.shared.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import app.deltax.music.shared.auth.YouTubeSessionState
import app.deltax.music.shared.auth.YouTubeSyncState
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.try_again
import app.deltax.music.shared.generated.resources.youtube_sync_failed
import app.deltax.music.shared.youtube.playback.PlaybackNetworkStateStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@Composable
fun YouTubeSyncSnackbar(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(snackbarHostState) {
        var dismissedFailure: YouTubeSyncState.Failure? = null
        YouTubeSyncState.failures
            .combine(PlaybackNetworkStateStore.state) { failures, network ->
                if (network.connected) failures.values.firstOrNull() else null
            }.distinctUntilChanged()
            .collectLatest { current ->
                val failure = current ?: return@collectLatest
                if (failure === dismissedFailure || YouTubeSessionState.showExpiredSessionNotice) return@collectLatest
                val result =
                    snackbarHostState.showSnackbar(
                        message = getString(Res.string.youtube_sync_failed),
                        actionLabel = getString(Res.string.try_again),
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                when (result) {
                    SnackbarResult.Dismissed -> {
                        dismissedFailure = failure
                    }

                    SnackbarResult.ActionPerformed -> {
                        scope.launch {
                            YouTubeSyncState.succeeded(failure.operation)
                            failure.retry()
                        }
                    }
                }
            }
    }
}
