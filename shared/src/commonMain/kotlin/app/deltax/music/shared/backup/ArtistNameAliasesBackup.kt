package app.deltax.music.shared.backup

import app.deltax.music.shared.constants.ArtistNameAliasesJsonKey
import app.deltax.music.shared.settings.ArtistNameAliases
import app.deltax.music.shared.settings.PortableSetting
import app.deltax.music.shared.settings.PortableSettings
import app.deltax.music.shared.settings.SettingType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun PortableSettings?.withLegacyArtistNameAliases(serialized: String): PortableSettings {
    val aliases = ArtistNameAliases.deserialize(serialized)
    val settings = this ?: PortableSettings(entries = emptyList())
    return settings.copy(
        entries =
            settings.entries.filterNot { it.key == ArtistNameAliasesJsonKey } +
                PortableSetting(ArtistNameAliasesJsonKey, SettingType.STRING, Json.encodeToString(aliases)),
    )
}
