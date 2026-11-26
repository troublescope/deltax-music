package app.deltax.music.shared.api.lyrics

import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.utils.getSystemLanguage

internal fun lyricsTranslationTargetLanguage(configuredLanguage: String = Settings.shared.lyricsAiTargetLanguage): String =
    if (configuredLanguage == "device" || configuredLanguage.isBlank()) {
        runCatching { getSystemLanguage() }.getOrNull().orEmpty()
    } else {
        configuredLanguage
    }

internal fun lyricsTranslationLanguageMatches(
    language: String,
    target: String,
): Boolean =
    language.isNotBlank() && target.isNotBlank() &&
        language.replace('_', '-').substringBefore('-').equals(target.replace('_', '-').substringBefore('-'), ignoreCase = true)
