/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.deltax.music.ui.menu

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deltax.music.LocalListenTogetherManager
import app.deltax.music.LocalPlaylistAdder
import app.deltax.music.listentogether.AndroidListenTogetherManager
import app.deltax.music.playback.AudioCacheManager
import app.deltax.music.playback.LocalPlayerConnection
import app.deltax.music.playback.SongDownloadManager
import app.deltax.music.playback.launchSystemEqualizer
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.action_download
import app.deltax.music.shared.generated.resources.back
import app.deltax.music.shared.generated.resources.blocked_artists_count
import app.deltax.music.shared.generated.resources.could_not_queue_download
import app.deltax.music.shared.generated.resources.disliked_song
import app.deltax.music.shared.generated.resources.download_not_found
import app.deltax.music.shared.generated.resources.equalizer
import app.deltax.music.shared.generated.resources.listen_together
import app.deltax.music.shared.generated.resources.music_video_quality_auto
import app.deltax.music.shared.generated.resources.music_video_quality_resolution
import app.deltax.music.shared.generated.resources.music_video_resolution
import app.deltax.music.shared.generated.resources.music_video_resolution_current
import app.deltax.music.shared.generated.resources.music_video_resolution_desc
import app.deltax.music.shared.generated.resources.percent_symbol
import app.deltax.music.shared.generated.resources.playback_speed
import app.deltax.music.shared.generated.resources.queued_song
import app.deltax.music.shared.generated.resources.remove_download
import app.deltax.music.shared.generated.resources.removed_dislike
import app.deltax.music.shared.generated.resources.removed_download
import app.deltax.music.shared.generated.resources.song_details
import app.deltax.music.shared.generated.resources.suggested_to_host
import app.deltax.music.shared.generated.resources.system_equalizer_unavailable
import app.deltax.music.shared.generated.resources.tempo_and_pitch
import app.deltax.music.shared.generated.resources.unknown
import app.deltax.music.shared.generated.resources.unknown_artist
import app.deltax.music.shared.generated.resources.volume
import app.deltax.music.shared.listentogether.ListenTogetherRoomRole
import app.deltax.music.shared.model.previewAspectRatio
import app.deltax.music.shared.settings.MusicVideoQuality
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.settings.displayName
import app.deltax.music.shared.settings.targetHeight
import app.deltax.music.shared.state.SongLikeState
import app.deltax.music.shared.ui.LocalIsTv
import app.deltax.music.shared.ui.components.PlayerMenuAction
import app.deltax.music.shared.ui.components.PlayerMenuActionGroup
import app.deltax.music.shared.ui.components.PlayerMenuPrimaryActions
import app.deltax.music.shared.ui.components.PopupVisibility
import app.deltax.music.shared.ui.components.TempoPitchControls
import app.deltax.music.shared.ui.components.rememberModalSheetState
import app.deltax.music.shared.ui.equalizer.EqualizerDialog
import app.deltax.music.shared.ui.equalizer.MetroEqSheet
import app.deltax.music.shared.ui.icons.MetrolistIcons
import app.deltax.music.shared.ui.menu.ExpressiveMediaMenuHeader
import app.deltax.music.shared.ui.menu.albumNavigationMenuAction
import app.deltax.music.shared.ui.menu.artistLinksText
import app.deltax.music.shared.ui.menu.artistNavigationMenuAction
import app.deltax.music.shared.ui.menu.blockArtistMenuAction
import app.deltax.music.shared.ui.menu.dislikeMenuAction
import app.deltax.music.shared.ui.menu.equalizerMenuAction
import app.deltax.music.shared.ui.menu.rememberDeleteUploadAction
import app.deltax.music.shared.ui.menu.songPrimaryActions
import app.deltax.music.shared.ui.menu.suggestToHostMenuAction
import app.deltax.music.shared.ui.menu.tempoPitchMenuAction
import app.deltax.music.shared.ui.navigation.NavigationBackHandler
import app.deltax.music.shared.ui.navigation.acceptsInput
import app.deltax.music.shared.ui.navigation.blockInput
import app.deltax.music.shared.ui.navigation.rememberNavigationMotion
import app.deltax.music.shared.ui.theme.ComponentShapes
import app.deltax.music.shared.utils.formatPlaybackSpeed
import app.deltax.music.shared.viewmodels.PlayerViewModel
import app.deltax.music.ui.component.rememberMetroEqProfilePicker
import app.deltax.music.ui.download.rememberDownloadPermissionGate
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerMenu(
    playerViewModel: PlayerViewModel,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    playbackPitch: Float,
    onPlaybackPitchChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToListenTogether: () -> Unit = {},
    onShowSongDetails: () -> Unit = onDismiss,
    extraActions: List<PlayerMenuAction> = emptyList(),
) {
    val suggestedToHostText = stringResource(Res.string.suggested_to_host)

    val uiState by playerViewModel.uiState.collectAsState()
    val currentSong = uiState.currentSong
    val songTitle = currentSong?.title ?: stringResource(Res.string.unknown)
    val artistName = currentSong?.artists?.joinToString(", ") { it.displayName } ?: stringResource(Res.string.unknown_artist)
    val thumbnail = currentSong?.displayThumbnail
    val firstArtistId = currentSong?.artists?.firstOrNull()?.id
    val blockedArtistText = pluralStringResource(Res.plurals.blocked_artists_count, 1, 1)
    val dislikedText = stringResource(Res.string.disliked_song)
    val removedDislikeText = stringResource(Res.string.removed_dislike)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database: MusicDatabase = koinInject()
    val deleteUploadAction = currentSong?.toSongItem()?.let { rememberDeleteUploadAction(it, database, onDismiss) }
    val downloadManager: SongDownloadManager = koinInject()
    val audioCacheManager: AudioCacheManager = koinInject()
    val requireDownloadNotifications = rememberDownloadPermissionGate()
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRole by listenTogetherManager?.role?.collectAsState()
        ?: remember { mutableStateOf(ListenTogetherRoomRole.NONE) }
    val isInListenTogetherRoom by listenTogetherManager?.isInRoom?.collectAsState()
        ?: remember { mutableStateOf(false) }

    val isGuest = isInListenTogetherRoom && listenTogetherRole == ListenTogetherRoomRole.GUEST

    val systemEqualizerUnavailable = stringResource(Res.string.system_equalizer_unavailable)
    val systemEqualizerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val playerConnection = LocalPlayerConnection.current
    val currentPlaybackResource by playerConnection?.currentPlaybackResource?.collectAsState() ?: remember { mutableStateOf(null) }

    val addToPlaylist = LocalPlaylistAdder.current
    val isTv = LocalIsTv.current
    val menuSheetState = rememberModalSheetState()
    var showPlaybackSpeed by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    if (showEqualizer) {
        EqualizerDialog(
            onDismiss = { if (isTv) showEqualizer = false else onDismiss() },
            rememberProfilePicker = { onPicked -> rememberMetroEqProfilePicker(onPicked) },
            onOpenSystemEqualizer = {
                if (!launchSystemEqualizer(context, playerConnection?.audioSessionId ?: 0) {
                        systemEqualizerLauncher.launch(it)
                    }
                ) {
                    Toast.makeText(context, systemEqualizerUnavailable, Toast.LENGTH_SHORT).show()
                }
            },
            metroEqContent =
                if (isTv) {
                    null
                } else {
                    { close, searchAutoEq ->
                        MetroEqSheet(
                            onDismiss = close,
                            varispeed = Settings.shared.varispeed,
                            rememberProfilePicker = { onPicked -> rememberMetroEqProfilePicker(onPicked) },
                            initialAutoEqSearch = searchAutoEq,
                        ) { onBack ->
                            TempoPitchControls(
                                speed = playbackSpeed,
                                pitch = playbackPitch,
                                onSpeedChange = onPlaybackSpeedChange,
                                onPitchChange = onPlaybackPitchChange,
                                speedEnabled = !isInListenTogetherRoom,
                                varispeed = Settings.shared.varispeed,
                                onDone = onBack,
                            )
                        }
                    }
                },
        )
        if (!isTv) return
    }
    var showVideoResolution by remember { mutableStateOf(false) }
    var downloadRefreshKey by remember { mutableIntStateOf(0) }
    var isDownloaded by remember(currentSong?.id) { mutableStateOf(false) }
    val isDisliked = currentSong?.id?.let { SongLikeState.isDisliked(it) } ?: false

    LaunchedEffect(currentSong?.id, downloadRefreshKey) {
        val songId = currentSong?.id
        isDownloaded =
            if (songId == null) {
                false
            } else {
                audioCacheManager.isDownloaded(songId)
            }
    }

    val artistAction = artistNavigationMenuAction(currentSong?.artists.orEmpty(), onNavigateToArtist, onDismiss)
    MediaMenuBottomSheet(
        onDismissRequest = onDismiss,
        scrollContent = false,
        sheetState = menuSheetState,
    ) {
        if (showVideoResolution) {
            VideoResolutionPage(
                currentHeight = currentPlaybackResource?.videoHeight,
                availableHeights = currentPlaybackResource?.availableVideoHeights.orEmpty(),
                selectedQuality = Settings.shared.musicVideoQuality,
                onSelect = { quality ->
                    playerViewModel.changeMusicVideoResolution(quality)
                    showVideoResolution = false
                    onDismiss()
                },
                onBack = { showVideoResolution = false },
            )
        } else {
            val motion = rememberNavigationMotion()
            NavigationBackHandler(enabled = showPlaybackSpeed && !isTv) { showPlaybackSpeed = false }
            AnimatedContent(
                targetState = showPlaybackSpeed,
                transitionSpec = { motion.transition(isPop = !targetState) },
                label = "playerMenuTempoPitch",
            ) { playbackSpeedPage ->
                Box(Modifier.blockInput(!transition.acceptsInput(EnterExitState.Visible))) {
                    if (playbackSpeedPage) {
                        PlaybackSpeedPage(
                            speed = playbackSpeed,
                            onSpeedChange = onPlaybackSpeedChange,
                            pitch = playbackPitch,
                            onPitchChange = onPlaybackPitchChange,
                            speedEnabled = !isInListenTogetherRoom,
                            varispeed = Settings.shared.varispeed,
                            onBack = { showPlaybackSpeed = false },
                        )
                    } else {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding =
                                PaddingValues(
                                    bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                                ),
                        ) {
                            item {
                                ExpressiveMediaMenuHeader(
                                    title = songTitle,
                                    subtitle = artistName,
                                    thumbnail = thumbnail,
                                    thumbnailAspectRatio = currentSong?.toSongItem()?.previewAspectRatio ?: 1f,
                                    subtitleLinks =
                                        artistLinksText(currentSong?.artists.orEmpty()) {
                                            onNavigateToArtist(it)
                                            onDismiss()
                                        },
                                )
                            }

                            if (extraActions.isNotEmpty()) {
                                item { PlayerMenuActionGroup(actions = extraActions) }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(28.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.volume),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            val percentSymbol = stringResource(Res.string.percent_symbol)
                                            val volumeLabelStyle =
                                                MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFeatureSettings = "tnum",
                                                )
                                            val textMeasurer = rememberTextMeasurer()
                                            val volumeLabelWidth =
                                                with(LocalDensity.current) {
                                                    textMeasurer
                                                        .measure(100.toString() + percentSymbol, volumeLabelStyle)
                                                        .size.width
                                                        .toDp()
                                                }
                                            val sliderState = rememberSliderState(value = volume)
                                            sliderState.value = volume
                                            Slider(
                                                state = sliderState,
                                                onValueChange = onVolumeChange,
                                                modifier = Modifier.weight(1f),
                                                colors =
                                                    SliderDefaults.colors(
                                                        thumbColor = MaterialTheme.colorScheme.primary,
                                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                    ),
                                            )

                                            Text(
                                                text = (volume.coerceIn(0f, 1f) * 100).roundToInt().toString() + percentSymbol,
                                                modifier = Modifier.width(volumeLabelWidth),
                                                style = volumeLabelStyle,
                                                textAlign = TextAlign.End,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                PlayerMenuPrimaryActions(
                                    actions =
                                        songPrimaryActions(
                                            onDismiss = onDismiss,
                                            isGuest = isGuest,
                                            onStartRadio = currentSong?.let { song -> { playerViewModel.startRadio(song.toSongItem()) } },
                                            onAddToPlaylist = {
                                                currentSong?.toSongItem()?.let { song ->
                                                    addToPlaylist(listOf(song))
                                                    onDismiss()
                                                }
                                            },
                                            onShare = playerViewModel::shareSong,
                                            onAltShare = { playerViewModel.copySongLink() },
                                        ),
                                )
                            }

                            if (isGuest) {
                                item {
                                    PlayerMenuActionGroup(
                                        actions =
                                            listOf(
                                                suggestToHostMenuAction(onDismiss) {
                                                    currentSong?.toSongItem()?.let { AndroidListenTogetherManager.suggestTrack(it) }
                                                    Toast.makeText(context, suggestedToHostText, Toast.LENGTH_SHORT).show()
                                                },
                                            ),
                                    )
                                }
                            }

                            deleteUploadAction?.let { action ->
                                item { PlayerMenuActionGroup(actions = listOf(action)) }
                            }

                            item {
                                PlayerMenuActionGroup(
                                    actions =
                                        buildList {
                                            artistAction?.let(::add)
                                            albumNavigationMenuAction(
                                                onDismiss,
                                                currentSong?.albumId?.let { albumId -> { onNavigateToAlbum(albumId) } },
                                                currentSong?.album?.takeIf { it.isNotBlank() },
                                            )?.let(::add)
                                            add(
                                                PlayerMenuAction(
                                                    icon = MetrolistIcons.Outlined.Info,
                                                    title = stringResource(Res.string.song_details),
                                                    onClick = onShowSongDetails,
                                                ),
                                            )
                                        },
                                )
                            }

                            if (uiState.isVideoModeActive && !isInListenTogetherRoom) {
                                item {
                                    PlayerMenuActionGroup(
                                        actions =
                                            listOf(
                                                PlayerMenuAction(
                                                    icon = MetrolistIcons.Outlined.Tune,
                                                    title = stringResource(Res.string.music_video_resolution),
                                                    description = stringResource(Res.string.music_video_resolution_desc),
                                                    onClick = { showVideoResolution = true },
                                                ),
                                            ),
                                    )
                                }
                            }

                            item {
                                PlayerMenuActionGroup(
                                    actions =
                                        buildList {
                                            add(
                                                PlayerMenuAction(
                                                    icon =
                                                        if (isDownloaded) {
                                                            MetrolistIcons.Outlined.OfflinePin
                                                        } else {
                                                            MetrolistIcons.Outlined.Download
                                                        },
                                                    title =
                                                        stringResource(
                                                            if (isDownloaded) Res.string.remove_download else Res.string.action_download,
                                                        ),
                                                    onClick = {
                                                        val song = currentSong?.toSongItem()
                                                        if (song != null) {
                                                            if (isDownloaded) {
                                                                coroutineScope.launch {
                                                                    val removed = downloadManager.removeDownload(song)
                                                                    Toast
                                                                        .makeText(
                                                                            context,
                                                                            getString(
                                                                                if (removed) {
                                                                                    Res.string.removed_download
                                                                                } else {
                                                                                    Res.string.download_not_found
                                                                                },
                                                                            ),
                                                                            Toast.LENGTH_SHORT,
                                                                        ).show()
                                                                    downloadRefreshKey++
                                                                    onDismiss()
                                                                }
                                                            } else {
                                                                requireDownloadNotifications {
                                                                    coroutineScope.launch {
                                                                        val ok = downloadManager.download(song)
                                                                        Toast
                                                                            .makeText(
                                                                                context,
                                                                                if (ok) {
                                                                                    getString(
                                                                                        Res.string.queued_song,
                                                                                        song.title,
                                                                                    )
                                                                                } else {
                                                                                    getString(Res.string.could_not_queue_download)
                                                                                },
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                        if (ok) downloadRefreshKey++
                                                                    }
                                                                }
                                                                onDismiss()
                                                            }
                                                        }
                                                    },
                                                ),
                                            )
                                            dislikeMenuAction(
                                                isDisliked,
                                                onDismiss,
                                                onDislike = {
                                                    playerViewModel.toggleDislikeCurrentSong()
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            if (isDisliked) removedDislikeText else dislikedText,
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                },
                                            )?.let(::add)
                                            blockArtistMenuAction(
                                                onDismiss,
                                                onBlockArtist =
                                                    firstArtistId?.let {
                                                        {
                                                            playerViewModel.blockCurrentArtist()
                                                            Toast.makeText(context, blockedArtistText, Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                description = artistName,
                                            )?.let(::add)
                                        },
                                )
                            }

                            if (Settings.shared.listenTogetherEnabled) {
                                item {
                                    PlayerMenuActionGroup(
                                        actions =
                                            listOf(
                                                PlayerMenuAction(
                                                    icon = MetrolistIcons.Default.Groups,
                                                    title = stringResource(Res.string.listen_together),
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToListenTogether()
                                                    },
                                                ),
                                            ),
                                    )
                                }
                            }

                            item {
                                PlayerMenuActionGroup(
                                    actions =
                                        listOfNotNull(
                                            equalizerMenuAction(onOpen = {
                                                if (isTv) {
                                                    showEqualizer = true
                                                } else {
                                                    coroutineScope.launch {
                                                        if (menuSheetState.hide()) showEqualizer = true
                                                    }
                                                }
                                            }),
                                            tempoPitchMenuAction(Settings.shared.varispeed) { showPlaybackSpeed = true },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSpeedPage(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    speedEnabled: Boolean,
    varispeed: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(if (varispeed) Res.string.playback_speed else Res.string.tempo_and_pitch),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        TempoPitchControls(speed, pitch, onSpeedChange, onPitchChange, speedEnabled, varispeed, onDone = onBack)
    }
}

@Composable
private fun VideoResolutionPage(
    currentHeight: Int?,
    availableHeights: List<Int>,
    selectedQuality: MusicVideoQuality,
    onSelect: (MusicVideoQuality) -> Unit,
    onBack: () -> Unit,
) {
    val selectableQualities =
        MusicVideoQuality.entries.filter { quality ->
            quality.targetHeight == null ||
                quality.targetHeight in availableHeights ||
                quality.targetHeight == currentHeight
        }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.back)) }
                Text(
                    text = stringResource(Res.string.music_video_resolution),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item {
            PlayerMenuActionGroup(
                actions =
                    selectableQualities.map { quality ->
                        PlayerMenuAction(
                            icon = MetrolistIcons.Outlined.Tune,
                            title = quality.displayName(),
                            description =
                                if (
                                    quality == selectedQuality ||
                                    (quality.targetHeight != null && quality.targetHeight == currentHeight)
                                ) {
                                    stringResource(Res.string.music_video_resolution_current)
                                } else {
                                    null
                                },
                            onClick = { onSelect(quality) },
                        )
                    },
            )
        }
    }
}

@Composable
private fun MusicVideoQuality.displayName(): String =
    when (this) {
        MusicVideoQuality.AUTO -> stringResource(Res.string.music_video_quality_auto)
        MusicVideoQuality.P2160 -> "4K (2160p)"
        else -> stringResource(Res.string.music_video_quality_resolution, name.drop(1))
    }
