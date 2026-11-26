/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package app.deltax.music.shared.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.settings_home_section_dynamic
import app.deltax.music.shared.generated.resources.settings_home_section_items
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.ui.screens.HomeShelfBuilder
import app.deltax.music.shared.ui.screens.homeContentText
import app.deltax.music.shared.viewmodels.HomeViewModel.HomeUiState
import org.jetbrains.compose.resources.stringResource

@Composable
fun settingsHomeSections(state: HomeUiState): List<SettingsHomeSection> =
    HomeShelfBuilder.build(state, homeContentText(), 1000.dp, includeHidden = true).map { section ->
        SettingsHomeSection(
            id = section.key,
            preferenceKey = section.visibilityKey,
            title = section.title,
            description =
                if (section.items.isNotEmpty()) {
                    stringResource(Res.string.settings_home_section_items, section.items.size)
                } else {
                    stringResource(Res.string.settings_home_section_dynamic)
                },
            visible = Settings.shared.shouldShowHomeSection(section.visibilityKey, section.title, section.legacyVisibilityKey),
        )
    }
