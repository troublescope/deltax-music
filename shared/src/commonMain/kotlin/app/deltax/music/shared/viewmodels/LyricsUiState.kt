package app.deltax.music.shared.viewmodels

import app.deltax.music.shared.model.AgentInfo
import app.deltax.music.shared.model.LyricsEntry
import app.deltax.music.shared.repository.LyricsProviderSearch

data class LyricsUiState(
    val songId: String? = null,
    val lyrics: List<LyricsEntry> = emptyList(),
    val rawLyrics: String? = null,
    val songwriters: List<String> = emptyList(),
    val agents: Map<String, AgentInfo> = emptyMap(),
    val provider: String? = null,
    val queryTitle: String? = null,
    val queryArtist: String? = null,
    val timingOffsetMs: Int = 0,
    val isLoading: Boolean = false,
    val providerSearch: LyricsProviderSearch? = null,
)
