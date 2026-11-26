/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package app.deltax.music.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import app.deltax.music.BuildConfig
import app.deltax.music.LocalMiniPlayerBottomInset
import app.deltax.music.playback.LocalPlayerConnection
import app.deltax.music.playback.resolveIncomingPlaybackSong
import app.deltax.music.shared.discord.openDiscordUrl
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.app_name
import app.deltax.music.shared.generated.resources.logo
import app.deltax.music.shared.generated.resources.youtube_music
import app.deltax.music.shared.state.GlobalYouTubeRepository
import app.deltax.music.shared.ui.screens.AboutCommunityLinkKind
import app.deltax.music.shared.ui.screens.AboutScreen
import app.deltax.music.shared.ui.settings.LocalSettingsPageHasTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AndroidAboutScreen(
    onNavigateBack: () -> Unit,
    onDevtoolsUnlocked: () -> Unit = {},
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val miniPlayerInset = LocalMiniPlayerBottomInset.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val youtubeMusicLabel = stringResource(Res.string.youtube_music)

    AboutScreen(
        logo = painterResource(Res.drawable.logo),
        appName = stringResource(Res.string.app_name),
        version = BuildConfig.VERSION_NAME,
        isDebug = BuildConfig.DEBUG,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        bottomInset = miniPlayerInset,
        onNavigateBack = onNavigateBack.takeUnless { LocalSettingsPageHasTopBar.current },
        onOpenCommunityLink = { link ->
            if (link.kind == AboutCommunityLinkKind.DISCORD) {
                runCatching { context.openDiscordUrl(link.url) }
            } else {
                runCatching { uriHandler.openUri(link.url) }
            }
        },
        onDevtoolsUnlocked = onDevtoolsUnlocked,
        onPlayFavoriteSong =
            playerConnection?.let { connection ->
                { videoId ->
                    coroutineScope.launch {
                        val song =
                            resolveIncomingPlaybackSong(
                                videoId = videoId,
                                fallbackArtist = youtubeMusicLabel,
                                loadMetadata = GlobalYouTubeRepository.instance::getSongMetadata,
                                loadPlayerInformation = GlobalYouTubeRepository.instance::getSongPlayerInformation,
                            )
                        connection.playSong(song, "")
                    }
                }
            },
    )
}
