package app.deltax.music.shared.settings

import app.deltax.music.shared.constants.DiscordPendingAuthSessionKey
import app.deltax.music.shared.constants.LyricsOpenRouterOAuthKey
import app.deltax.music.shared.constants.LyricsOpenRouterOAuthSessionKey

internal val secureSettingsKeys =
    setOf(
        DiscordPendingAuthSessionKey,
        "innerTubeCookie",
        "visitorData",
        "dataSyncId",
        "innerTubeAuthUser",
        "lastFmSession",
        "listenBrainzToken",
        "lyricsAiApiKey",
        "lyricsAiApiKeys",
        LyricsOpenRouterOAuthKey,
        LyricsOpenRouterOAuthSessionKey,
        "lyricsDeepLApiKey",
        "accountName",
        "accountEmail",
        "accountChannelHandle",
        "accountThumbnail",
        "discordAccessToken",
        "discordRefreshToken",
        "listenTogetherDesktopSession",
        "listenTogetherServerUrl",
        "listenTogetherCustomServers",
        "listenTogetherServerMetadata",
        "deviceConnectPairedDevices",
        "listenTogetherPairedRoomCode",
        "spotifySession",
    )
