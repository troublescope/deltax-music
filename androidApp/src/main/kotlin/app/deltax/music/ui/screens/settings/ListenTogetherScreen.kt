package app.deltax.music.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.deltax.music.LocalListenTogetherManager
import app.deltax.music.LocalMiniPlayerBottomInset
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.copied
import app.deltax.music.shared.generated.resources.listen_together_debug_clip_label
import app.deltax.music.shared.generated.resources.listen_together_debug_link_copied
import app.deltax.music.shared.generated.resources.listen_together_debug_upload_failed
import app.deltax.music.shared.generated.resources.listen_together_debug_uploading
import app.deltax.music.shared.generated.resources.listen_together_share_logs
import app.deltax.music.utils.CrashLogUploader
import app.deltax.music.utils.buildListenTogetherDebugReport
import app.deltax.music.utils.startActivitySafely
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import timber.log.Timber
import app.deltax.music.shared.ui.listentogether.ListenTogetherBlockedUsersScreen as SharedListenTogetherBlockedUsersScreen
import app.deltax.music.shared.ui.listentogether.ListenTogetherLogsScreen as SharedListenTogetherLogsScreen
import app.deltax.music.shared.ui.listentogether.ListenTogetherScreen as SharedListenTogetherScreen
import app.deltax.music.shared.ui.listentogether.ListenTogetherServerScreen as SharedListenTogetherServerScreen
import app.deltax.music.shared.ui.listentogether.ListenTogetherSettingsScreen as SharedListenTogetherSettingsScreen

@Composable
fun ListenTogetherScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    initialRoomCode: String? = null,
) {
    val context = LocalContext.current
    val manager = LocalListenTogetherManager.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    val copiedMessage = stringResource(Res.string.copied)
    val uploadingMessage = stringResource(Res.string.listen_together_debug_uploading)
    val linkCopiedMessage = stringResource(Res.string.listen_together_debug_link_copied)
    val uploadFailedMessage = stringResource(Res.string.listen_together_debug_upload_failed)
    val clipLabel = stringResource(Res.string.listen_together_debug_clip_label)
    SharedListenTogetherScreen(
        initialRoomCode = initialRoomCode,
        manager = manager,
        onNavigateBack = onNavigateBack,
        onNavigateToSettings = onNavigateToSettings,
        onCopyText = { label, text -> context.copyToClipboard(label, text, copiedMessage) },
        onEventMessage = { message, isFailure ->
            Toast.makeText(context, message, if (isFailure) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        },
        bottomInset = LocalMiniPlayerBottomInset.current,
        onDebugUpload =
            manager?.let { listenTogether ->
                {
                    if (!uploading) {
                        uploading = true
                        Toast.makeText(context, uploadingMessage, Toast.LENGTH_SHORT).show()
                        scope.launch {
                            try {
                                val report = buildListenTogetherDebugReport(context, listenTogether)
                                val paste = CrashLogUploader().upload(report)
                                context.copyToClipboard(clipLabel, paste.url, linkCopiedMessage)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.w(e, "Listen Together log upload failed")
                                Toast.makeText(context, uploadFailedMessage, Toast.LENGTH_LONG).show()
                            } finally {
                                uploading = false
                            }
                        }
                    }
                }
            },
    )
}

@Composable
fun ListenTogetherSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToServer: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToLogs: () -> Unit,
) {
    SharedListenTogetherSettingsScreen(
        manager = LocalListenTogetherManager.current,
        onNavigateBack = onNavigateBack,
        onNavigateToServer = onNavigateToServer,
        onNavigateToBlockedUsers = onNavigateToBlockedUsers,
        onNavigateToLogs = onNavigateToLogs,
        bottomInset = LocalMiniPlayerBottomInset.current,
    )
}

@Composable
fun ListenTogetherServerScreen(onNavigateBack: () -> Unit) {
    SharedListenTogetherServerScreen(
        manager = LocalListenTogetherManager.current,
        onNavigateBack = onNavigateBack,
        bottomInset = LocalMiniPlayerBottomInset.current,
    )
}

@Composable
fun ListenTogetherBlockedUsersScreen(onNavigateBack: () -> Unit) {
    SharedListenTogetherBlockedUsersScreen(
        manager = LocalListenTogetherManager.current,
        onNavigateBack = onNavigateBack,
        bottomInset = LocalMiniPlayerBottomInset.current,
    )
}

@Composable
fun ListenTogetherLogsScreen(onNavigateBack: () -> Unit) {
    val listenTogetherShareLogsText = stringResource(Res.string.listen_together_share_logs)

    val context = LocalContext.current
    val copiedMessage = stringResource(Res.string.copied)
    SharedListenTogetherLogsScreen(
        manager = LocalListenTogetherManager.current,
        onNavigateBack = onNavigateBack,
        onCopyText = { label, text -> context.copyToClipboard(label, text, copiedMessage) },
        onShareText = { logText ->
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, logText)
                }
            context.startActivitySafely(
                Intent.createChooser(shareIntent, listenTogetherShareLogsText),
            )
        },
        bottomInset = LocalMiniPlayerBottomInset.current,
    )
}

private fun Context.copyToClipboard(
    label: String,
    text: String,
    copiedMessage: String,
) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, copiedMessage, Toast.LENGTH_SHORT).show()
}
