package app.deltax.music.shared.ui.crash

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.crash_close
import app.deltax.music.shared.generated.resources.crash_copy_logs
import app.deltax.music.shared.generated.resources.crash_screenshot_description
import app.deltax.music.shared.generated.resources.crash_screenshot_hint
import app.deltax.music.shared.generated.resources.crash_screenshot_title
import app.deltax.music.shared.generated.resources.crash_share_logs
import app.deltax.music.shared.ui.components.AlertDialog
import app.deltax.music.shared.ui.components.PopupVisibility
import org.jetbrains.compose.resources.stringResource

@Composable
fun CrashScreenshotHintText() {
    Text(
        stringResource(Res.string.crash_screenshot_hint),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun CrashScreenshotHint(
    onCopy: (() -> Unit)?,
    onShare: (() -> Unit)?,
) {
    var visible by remember { mutableStateOf(false) }
    ObserveCrashScreenshots { visible = true }
    CrashScreenshotDialog(visible, onDismiss = { visible = false }, onCopy, onShare)
}

@Composable
fun CrashScreenshotDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCopy: (() -> Unit)?,
    onShare: (() -> Unit)?,
) {
    PopupVisibility(visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.crash_screenshot_title)) },
            text = { Text(stringResource(Res.string.crash_screenshot_description)) },
            confirmButton = {
                Column {
                    if (onCopy != null) {
                        TextButton(onClick = {
                            onDismiss()
                            onCopy()
                        }) { Text(stringResource(Res.string.crash_copy_logs)) }
                    }
                    if (onShare != null) {
                        TextButton(onClick = {
                            onDismiss()
                            onShare()
                        }) { Text(stringResource(Res.string.crash_share_logs)) }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.crash_close)) }
            },
        )
    }
}

@Composable
internal expect fun ObserveCrashScreenshots(onScreenshot: () -> Unit)
