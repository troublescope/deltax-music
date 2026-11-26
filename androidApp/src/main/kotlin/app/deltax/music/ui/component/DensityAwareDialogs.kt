package app.deltax.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.DialogProperties
import app.deltax.music.shared.ui.LocalIsTv
import app.deltax.music.shared.ui.components.Dialog

@Composable
fun DensityAwareDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val isTv = LocalIsTv.current
    Dialog(onDismissRequest, properties) {
        BackHandler(enabled = isTv && properties.dismissOnBackPress, onBack = onDismissRequest)
        CompositionLocalProvider(LocalDensity provides density, content = content)
    }
}
