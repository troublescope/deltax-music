package app.deltax.music.ui.menu

import androidx.compose.runtime.Composable
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.model.YTItem
import app.deltax.music.shared.ui.menu.SpeedDialToggle
import org.koin.compose.koinInject
import app.deltax.music.shared.ui.menu.rememberSpeedDialToggle as rememberSharedSpeedDialToggle

@Composable
internal fun rememberSpeedDialToggle(item: YTItem): SpeedDialToggle = rememberSharedSpeedDialToggle(item, koinInject<MusicDatabase>())
