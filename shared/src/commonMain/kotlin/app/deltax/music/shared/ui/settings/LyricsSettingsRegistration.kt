/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@file:Suppress("FunctionName", "ktlint:standard:no-wildcard-imports")

package app.deltax.music.shared.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deltax.music.shared.generated.resources.*
import app.deltax.music.shared.repository.LyricsRepository
import app.deltax.music.shared.settings.LyricsTextSizeRange
import app.deltax.music.shared.settings.PlayerBackgroundStyle
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.ui.components.lyrics.LyricsOffsetEditor
import app.deltax.music.shared.ui.icons.MetrolistIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import kotlin.math.roundToInt

@Composable
internal fun SettingsUiRegistryBuilder.registerLyricsSettings(host: SharedSettingsHost) {
    val settings = Settings.shared
    val lyricsPositionChoices =
        listOf(
            SettingsChoice("LEFT", stringResource(Res.string.lyrics_text_position_left)),
            SettingsChoice("CENTER", stringResource(Res.string.lyrics_text_position_center)),
            SettingsChoice("RIGHT", stringResource(Res.string.lyrics_text_position_right)),
        )
    val lyricsAnimationChoices =
        listOf(
            SettingsChoice("Apple", stringResource(Res.string.apple_music_style)),
            SettingsChoice("Braccato", stringResource(Res.string.braccato_style)),
            SettingsChoice("Expressive", stringResource(Res.string.expressive_style)),
            SettingsChoice("Simple", stringResource(Res.string.simple_style)),
            SettingsChoice("None", stringResource(Res.string.none)),
        )
    page(
        id = SettingsPageId.LYRICS,
        title = stringResource(Res.string.lyrics_settings_title),
        subtitle = stringResource(Res.string.settings_lyrics_description),
        icon = MetrolistIcons.Outlined.Lyrics,
        tone = SettingsTone.ROSE,
    ) {
        group("alignment", stringResource(Res.string.lyrics_alignment)) {
            choice(
                id = "lyricsPosition",
                title = stringResource(Res.string.lyrics_alignment),
                valueLabel = lyricsPositionChoices.firstOrNull { it.id == settings.lyricsTextPosition.uppercase() }?.label,
                choices = lyricsPositionChoices,
                selectedChoiceId = settings.lyricsTextPosition.uppercase(),
            ) { settings.lyricsTextPosition = it }
            choice(
                id = "activeLyricsPosition",
                title = stringResource(Res.string.active_lyrics_alignment),
                valueLabel = lyricsPositionChoices.firstOrNull { it.id == settings.activeLyricsTextPosition.uppercase() }?.label,
                choices = lyricsPositionChoices,
                selectedChoiceId = settings.activeLyricsTextPosition.uppercase(),
            ) { settings.activeLyricsTextPosition = it }
            switch(
                id = "respectAgentPositioning",
                title = stringResource(Res.string.lyrics_respect_agent_positioning),
                description = stringResource(Res.string.lyrics_respect_agent_positioning_desc),
                checked = settings.respectAgentPositioning,
            ) { settings.respectAgentPositioning = it }
        }
        group("behavior", stringResource(Res.string.lyrics_behavior)) {
            switch(
                id = "preferWordSyncedLyrics",
                title = stringResource(Res.string.prefer_word_synced_lyrics),
                description = stringResource(Res.string.prefer_word_synced_lyrics_description),
                checked = settings.preferWordSyncedLyrics,
            ) { settings.preferWordSyncedLyrics = it }
            custom(
                id = "globalLyricsOffset",
                title = stringResource(Res.string.global_lyrics_offset),
                description = stringResource(Res.string.global_lyrics_offset_description),
            ) { onSettingValueChanged ->
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
                    LyricsOffsetEditor(
                        offsetMs = settings.globalLyricsOffsetMs,
                        onOffsetChange = {
                            settings.globalLyricsOffsetMs = it
                            onSettingValueChanged()
                        },
                        onDone = null,
                        title = stringResource(Res.string.global_lyrics_offset),
                    )
                    Text(
                        text = stringResource(Res.string.global_lyrics_offset_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            switch(
                id = "lyricsClickToSeek",
                title = stringResource(Res.string.lyrics_change_on_tap),
                description = stringResource(Res.string.lyrics_change_on_tap_desc),
                checked = settings.lyricsClickToSeek,
            ) { settings.lyricsClickToSeek = it }
            switch(
                id = "lyricsAutoScroll",
                title = stringResource(Res.string.lyrics_auto_scroll),
                description = stringResource(Res.string.lyrics_auto_scroll_desc),
                checked = settings.lyricsAutoScroll,
            ) { settings.lyricsAutoScroll = it }
            switch(
                id = "showLyricsOnPlayer",
                title = stringResource(Res.string.lyrics_show_on_player),
                description = stringResource(Res.string.lyrics_show_on_player_desc),
                checked = settings.showLyricsOnPlayer,
            ) { settings.showLyricsOnPlayer = it }
            switch(
                id = "hideStatusBar",
                title = stringResource(Res.string.lyrics_hide_status_bar_fullscreen),
                description = stringResource(Res.string.lyrics_hide_status_bar_fullscreen_desc),
                checked = settings.lyricsHideStatusBarInFullscreen,
            ) { settings.lyricsHideStatusBarInFullscreen = it }
        }
        group("display", stringResource(Res.string.lyrics_display)) {
            val colorChoices =
                listOf(
                    SettingsChoice("Theme", stringResource(Res.string.lyrics_color_follow_theme)),
                    SettingsChoice("Neutral", stringResource(Res.string.theme_palette_neutral)),
                )
            val colorChoice = if (settings.lyricsNeutralColor) "Neutral" else "Theme"
            choice(
                id = "lyricsColor",
                title = stringResource(Res.string.lyrics_text_color),
                valueLabel = colorChoices.first { it.id == colorChoice }.label,
                choices = colorChoices,
                selectedChoiceId = colorChoice,
            ) { settings.lyricsNeutralColor = it == "Neutral" }
            choice(
                id = "lyricsAnimation",
                title = stringResource(Res.string.lyrics_animation_style),
                valueLabel = lyricsAnimationChoices.firstOrNull { it.id == settings.lyricsAnimationStyle }?.label,
                choices = lyricsAnimationChoices,
                selectedChoiceId = settings.lyricsAnimationStyle,
            ) { settings.lyricsAnimationStyle = it }
            inlineSlider(
                id = "lyricsTextSize",
                title = stringResource(Res.string.lyrics_text_size),
                valueLabel = settings.lyricsTextSize.roundToInt().toString(),
                value = settings.lyricsTextSize,
                defaultValue = settings.defaultValue(settings::lyricsTextSize),
                range = LyricsTextSizeRange,
                steps = 29,
            ) { settings.lyricsTextSize = it.roundToInt().toFloat() }
            inlineSlider(
                id = "lyricsLineSpacing",
                title = stringResource(Res.string.lyrics_line_spacing),
                valueLabel = "${(settings.lyricsLineSpacing * 10).roundToInt() / 10f}x",
                value = settings.lyricsLineSpacing,
                defaultValue = settings.defaultValue(settings::lyricsLineSpacing),
                range = 1f..2.5f,
                steps = 14,
            ) { settings.lyricsLineSpacing = (it * 10).roundToInt() / 10f }
            inlineSlider(
                id = "subtitleTextSize",
                title = stringResource(Res.string.subtitle_text_size),
                valueLabel = settings.subtitleTextSize.roundToInt().toString(),
                value = settings.subtitleTextSize,
                defaultValue = settings.defaultValue(settings::subtitleTextSize),
                range = 10f..40f,
                steps = 29,
            ) { settings.subtitleTextSize = it.roundToInt().toFloat() }
        }
        group("accessibility", stringResource(Res.string.lyrics_accessibility)) {
            switch(
                id = "lyricsActiveByRealTiming",
                title = stringResource(Res.string.lyrics_active_by_real_timing),
                description = stringResource(Res.string.lyrics_active_by_real_timing_description),
                checked = settings.lyricsActiveByRealTiming,
            ) { settings.lyricsActiveByRealTiming = it }
            switch(
                id = "lyricsProgressiveBlur",
                title = stringResource(Res.string.lyrics_progressive_blur),
                description = stringResource(Res.string.lyrics_progressive_blur_description),
                checked = settings.lyricsProgressiveBlur,
            ) { settings.lyricsProgressiveBlur = it }
            if (settings.playerBackgroundStyle == PlayerBackgroundStyle.LIQUID) {
                switch(
                    id = "lyricsPlusLighter",
                    title = stringResource(Res.string.lyrics_plus_lighter),
                    description = stringResource(Res.string.lyrics_plus_lighter_desc),
                    checked = settings.lyricsPlusLighter,
                ) { settings.lyricsPlusLighter = it }
            }
        }
        group("lyricsAdvanced", stringResource(Res.string.lyrics_settings_title)) {
            if (SettingsPageId.LYRICS_SOURCES in host.availablePages) {
                navigation(
                    "sources",
                    stringResource(Res.string.lyrics_source_preference),
                    SettingsPageId.LYRICS_SOURCES,
                    description = stringResource(Res.string.settings_lyrics_sources_overview),
                )
            }
            if (SettingsPageId.LYRICS_ROMANIZATION in host.availablePages) {
                navigation(
                    "romanization",
                    stringResource(Res.string.lyrics_romanize_title),
                    SettingsPageId.LYRICS_ROMANIZATION,
                    description = stringResource(Res.string.settings_lyrics_romanization_overview),
                )
            }
        }
    }

    if (SettingsPageId.LYRICS_SOURCES in host.availablePages) {
        page(
            id = SettingsPageId.LYRICS_SOURCES,
            title = stringResource(Res.string.lyrics_source_preference),
            subtitle = stringResource(Res.string.settings_lyrics_sources_overview),
            icon = MetrolistIcons.Outlined.Lyrics,
            tone = SettingsTone.ROSE,
        ) {
            group(
                id = "providers",
                title = stringResource(Res.string.lyrics_source_preference),
                description = stringResource(Res.string.drag_to_reorder),
            ) {
                custom(
                    id = "providerOrder",
                    title = stringResource(Res.string.lyrics_source_preference),
                ) {
                    LyricsProviderOrderSetting()
                }
            }
            val scope = rememberCoroutineScope()
            val lyricsRepository = remember { KoinPlatform.getKoin().get<LyricsRepository>() }
            group("fetchedLyrics", stringResource(Res.string.fetched_lyrics)) {
                action(
                    id = "clearFetchedLyrics",
                    title = stringResource(Res.string.clear_fetched_lyrics),
                    description = stringResource(Res.string.clear_fetched_lyrics_description),
                    confirmationMessage = stringResource(Res.string.clear_fetched_lyrics_confirm),
                ) { scope.launch { lyricsRepository.clearFetchedLyrics() } }
            }
        }
    }

    if (SettingsPageId.LYRICS_ROMANIZATION in host.availablePages) {
        val romanizationLanguages = parseRomanizationLanguages(settings.lyricsRomanizeLanguages)
        page(
            id = SettingsPageId.LYRICS_ROMANIZATION,
            title = stringResource(Res.string.lyrics_romanize_title),
            subtitle = stringResource(Res.string.settings_lyrics_romanization_overview),
            icon = MetrolistIcons.Outlined.Translate,
            tone = SettingsTone.ROSE,
        ) {
            group("display", stringResource(Res.string.lyrics_romanization_display)) {
                val largerTextChoices =
                    listOf(
                        SettingsChoice("lyrics", stringResource(Res.string.lyrics)),
                        SettingsChoice("romanization", stringResource(Res.string.lyrics_romanize_title)),
                    )
                val romanizeAsMain = settings.isLyricsRomanizationActive && settings.lyricsRomanizeAsMain
                val largerTextChoiceId = if (romanizeAsMain) "romanization" else "lyrics"
                choice(
                    id = "romanizationLargerText",
                    title = stringResource(Res.string.lyrics_romanization_larger_text),
                    description = stringResource(Res.string.lyrics_romanization_larger_text_description),
                    valueLabel = largerTextChoices.first { it.id == largerTextChoiceId }.label,
                    choices = largerTextChoices,
                    selectedChoiceId = largerTextChoiceId,
                ) { selected ->
                    val useRomanization = selected == "romanization"
                    settings.lyricsRomanizeAsMain = useRomanization
                    if (useRomanization) {
                        settings.lyricsRomanizationEnabled = true
                    }
                }
                if (romanizeAsMain) {
                    switch(
                        id = "showSecondaryLyrics",
                        title = stringResource(Res.string.lyrics_romanization_show_original),
                        description = stringResource(Res.string.lyrics_romanization_show_original_description),
                        checked = settings.lyricsShowOriginalLyrics,
                    ) { settings.lyricsShowOriginalLyrics = it }
                } else {
                    switch(
                        id = "showSecondaryLyrics",
                        title = stringResource(Res.string.lyrics_romanization_show_romanization),
                        description = stringResource(Res.string.lyrics_romanization_show_romanization_description),
                        checked = settings.lyricsRomanizationEnabled,
                    ) { settings.lyricsRomanizationEnabled = it }
                }
                switch(
                    id = "syllableRomanization",
                    title = stringResource(Res.string.lyrics_romanization_syllable_title),
                    description = stringResource(Res.string.lyrics_romanization_syllable_description),
                    checked = settings.lyricsSyllableRomanizationEnabled,
                ) { settings.lyricsSyllableRomanizationEnabled = it }
                switch(
                    id = "cyrillicPerLine",
                    title = stringResource(Res.string.lyrics_romanization_detect_cyrillic_per_line),
                    description = stringResource(Res.string.lyrics_romanization_detect_cyrillic_per_line_description),
                    checked = settings.lyricsRomanizeCyrillicByLine,
                ) { settings.lyricsRomanizeCyrillicByLine = it }
            }
            group(
                id = "languages",
                title = stringResource(Res.string.languages),
                description = stringResource(Res.string.lyrics_romanization_languages_description),
            ) {
                romanizationLanguages.forEach { (language, checked) ->
                    switch(
                        id = "romanize:$language",
                        title = romanizationLanguageLabel(language),
                        checked = checked,
                    ) { enabled ->
                        settings.lyricsRomanizeLanguages =
                            romanizationLanguages
                                .toMutableMap()
                                .apply { this[language] = enabled }
                                .entries
                                .joinToString(",") { (name, selected) -> "$name:$selected" }
                    }
                }
            }
        }
    }
}
