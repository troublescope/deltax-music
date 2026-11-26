package app.deltax.music.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deltax.music.shared.ui.components.ModalSheetState
import app.deltax.music.shared.ui.menu.LocalContextMenuAnchor
import app.deltax.music.shared.ui.menu.MediaContextMenu
import app.deltax.music.shared.ui.menu.MediaMenuModel
import app.deltax.music.shared.ui.theme.ComponentShapes
import app.deltax.music.ui.component.AdaptiveModalBottomSheet
import app.deltax.music.shared.ui.components.rememberModalSheetState as rememberBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaMenuBottomSheet(
    onDismissRequest: () -> Unit,
    expanded: Boolean = true,
    scrollContent: Boolean = true,
    model: MediaMenuModel? = null,
    canDismiss: () -> Boolean = { true },
    sheetState: ModalSheetState =
        rememberBottomSheetState(SheetValue.Hidden, confirmValueChange = { it != SheetValue.Hidden || canDismiss() }),
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismiss = { if (canDismiss()) onDismissRequest() }
    val anchor = LocalContextMenuAnchor.current
    if (anchor.isMouseInput && model != null) {
        MediaContextMenu(model = model, expanded = expanded, offset = anchor.offset, onDismiss = dismiss)
        return
    }
    if (!expanded) return
    val bottomPadding = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    AdaptiveModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        shape = ComponentShapes.BottomSheetShape,
    ) {
        if (!scrollContent) {
            content()
            return@AdaptiveModalBottomSheet
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = bottomPadding),
            content = content,
        )
    }
}
