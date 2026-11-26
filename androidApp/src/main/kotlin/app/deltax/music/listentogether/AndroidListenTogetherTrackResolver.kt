package app.deltax.music.listentogether

import app.deltax.music.shared.listentogether.ListenTogetherTrackResolver
import app.deltax.music.shared.listentogether.toSongItem
import app.deltax.music.shared.model.SongItem

internal fun androidListenTogetherTrackResolver(
    metadata: suspend (String) -> SongItem?,
    fallbackArtist: suspend () -> String,
): ListenTogetherTrackResolver =
    ListenTogetherTrackResolver { track ->
        metadata(track.id) ?: track.toSongItem(
            fallbackArtist = fallbackArtist(),
            omitNonPositiveDuration = true,
        )
    }
