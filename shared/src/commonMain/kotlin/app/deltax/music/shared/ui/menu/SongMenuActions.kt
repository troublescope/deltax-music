package app.deltax.music.shared.ui.menu

import androidx.compose.runtime.Composable
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.add_to_playlist
import app.deltax.music.shared.generated.resources.adjust_audio_equalizer
import app.deltax.music.shared.generated.resources.allow_song_to_be_liked_again
import app.deltax.music.shared.generated.resources.block_artist
import app.deltax.music.shared.generated.resources.dislike_song
import app.deltax.music.shared.generated.resources.dislike_this_song_on_youtube_music
import app.deltax.music.shared.generated.resources.equalizer
import app.deltax.music.shared.generated.resources.playback_speed
import app.deltax.music.shared.generated.resources.remove_dislike
import app.deltax.music.shared.generated.resources.share
import app.deltax.music.shared.generated.resources.start_radio
import app.deltax.music.shared.generated.resources.suggest_to_host
import app.deltax.music.shared.generated.resources.tempo_and_pitch
import app.deltax.music.shared.generated.resources.view_album
import app.deltax.music.shared.generated.resources.view_podcast
import app.deltax.music.shared.ui.LocalIsTv
import app.deltax.music.shared.ui.components.PlayerMenuAction
import app.deltax.music.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

@Composable
fun songPrimaryActions(
    onDismiss: () -> Unit,
    isGuest: Boolean = false,
    onStartRadio: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onAltShare: (() -> Unit)? = null,
    playbackEnabled: Boolean = true,
): List<PlayerMenuAction> =
    listOfNotNull(
        onStartRadio?.takeUnless { isGuest }?.let {
            action(MetrolistIcons.Outlined.Sensors, stringResource(Res.string.start_radio), onDismiss, it).copy(enabled = playbackEnabled)
        },
        onAddToPlaylist?.let {
            PlayerMenuAction(MetrolistIcons.AutoMirrored.Outlined.PlaylistAdd, stringResource(Res.string.add_to_playlist), onClick = it)
        },
        onShare?.takeUnless { LocalIsTv.current }?.let {
            action(MetrolistIcons.Outlined.Share, stringResource(Res.string.share), onDismiss, it, onLongClick = onAltShare)
        },
    )

@Composable
fun dislikeMenuAction(
    isDisliked: Boolean,
    onDismiss: () -> Unit,
    onDislike: (() -> Unit)?,
    showDescription: Boolean = false,
): PlayerMenuAction? =
    onDislike?.let {
        action(
            if (isDisliked) MetrolistIcons.Filled.ThumbDown else MetrolistIcons.Outlined.ThumbDown,
            stringResource(if (isDisliked) Res.string.remove_dislike else Res.string.dislike_song),
            onDismiss,
            it,
            description =
                if (showDescription) {
                    stringResource(
                        if (isDisliked) Res.string.allow_song_to_be_liked_again else Res.string.dislike_this_song_on_youtube_music,
                    )
                } else {
                    null
                },
        )
    }

@Composable
fun blockArtistMenuAction(
    onDismiss: () -> Unit,
    onBlockArtist: (() -> Unit)?,
    description: String? = null,
): PlayerMenuAction? =
    onBlockArtist?.let {
        action(MetrolistIcons.Outlined.Block, stringResource(Res.string.block_artist), onDismiss, it, description)
    }

@Composable
fun albumNavigationMenuAction(
    onDismiss: () -> Unit,
    onGoToAlbum: (() -> Unit)?,
    description: String? = null,
    isPodcast: Boolean = false,
): PlayerMenuAction? =
    onGoToAlbum?.let {
        action(
            MetrolistIcons.Outlined.Album,
            stringResource(if (isPodcast) Res.string.view_podcast else Res.string.view_album),
            onDismiss,
            it,
            description,
        )
    }

@Composable
fun suggestToHostMenuAction(
    onDismiss: () -> Unit,
    onSuggest: () -> Unit,
): PlayerMenuAction =
    action(MetrolistIcons.AutoMirrored.Outlined.QueueMusic, stringResource(Res.string.suggest_to_host), onDismiss, onSuggest)

@Composable
fun equalizerMenuAction(
    onOpen: () -> Unit,
    showDescription: Boolean = true,
): PlayerMenuAction =
    PlayerMenuAction(
        MetrolistIcons.Outlined.BarChart,
        stringResource(Res.string.equalizer),
        if (showDescription) stringResource(Res.string.adjust_audio_equalizer) else null,
        onClick = onOpen,
    )

@Composable
fun tempoPitchMenuAction(
    varispeed: Boolean,
    onOpen: () -> Unit,
): PlayerMenuAction =
    PlayerMenuAction(
        MetrolistIcons.Outlined.Tune,
        stringResource(if (varispeed) Res.string.playback_speed else Res.string.tempo_and_pitch),
        onClick = onOpen,
    )
