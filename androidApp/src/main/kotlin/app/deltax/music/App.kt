/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package app.deltax.music

import android.app.Application
import android.app.UiModeManager
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.quicksettings.TileService
import android.util.Log
import android.webkit.WebView
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import app.deltax.music.backup.BackupRestoreManager
import app.deltax.music.deviceconnect.AndroidDeviceConnect
import app.deltax.music.di.appModule
import app.deltax.music.locale.AppLocale
import app.deltax.music.onboarding.LegacyInstallMigration
import app.deltax.music.onboarding.OnboardingManager
import app.deltax.music.onboarding.shouldApplyTvContinuePlaybackDefault
import app.deltax.music.playback.AudioCacheManager
import app.deltax.music.playback.DownloadService
import app.deltax.music.playback.MusicService
import app.deltax.music.playback.SongDownloadManager
import app.deltax.music.playlistimport.AndroidPlaylistImportNotifier
import app.deltax.music.quicksettings.MusicRecognizerTileService
import app.deltax.music.recognition.MusicRecognitionService
import app.deltax.music.scrobble.LastFmSessionNotification
import app.deltax.music.shared.api.lastfm.LastFM
import app.deltax.music.shared.api.lastfm.LastFmGalleryBrowser
import app.deltax.music.shared.auth.AuthManager
import app.deltax.music.shared.db.MusicDatabase
import app.deltax.music.shared.db.checkAndroidDatabaseVersion
import app.deltax.music.shared.discord.DiscordRpcManager
import app.deltax.music.shared.discord.DiscordTokenStore
import app.deltax.music.shared.discord.observeDiscordInitialization
import app.deltax.music.shared.equalizer.EqualizerProfiles
import app.deltax.music.shared.listentogether.network.isAllowedListenTogetherServerUrl
import app.deltax.music.shared.settings.BlockedContent
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.state.GlobalYouTube
import app.deltax.music.shared.state.SongLikeState
import app.deltax.music.shared.state.SongMetadataState
import app.deltax.music.shared.state.coreModule
import app.deltax.music.shared.utils.isCrashReportingAvailable
import app.deltax.music.shared.youtube.di.sharedModule
import app.deltax.music.shared.youtube.playback.AndroidContextProvider
import app.deltax.music.shared.youtube.playback.PlaybackEventLevel
import app.deltax.music.shared.youtube.playback.PlaybackEventLog
import app.deltax.music.shared.youtube.playback.PlaybackNetworkStateStore
import app.deltax.music.ui.GlobalPlaybackNetworkMonitor
import app.deltax.music.update.AppUpdateManager
import app.deltax.music.utils.CrashHandler
import app.deltax.music.utils.IconUtils
import app.deltax.music.utils.SentryManager
import app.deltax.music.utils.createAppImageLoader
import app.deltax.music.utils.isAndroidTv
import app.deltax.music.widget.ExpressiveWidgetReceiver
import app.deltax.music.widget.MetrolistWidgetManager
import app.deltax.music.widget.MusicWidgetReceiver
import app.deltax.music.widget.PlaylistWidgetReceiver
import app.deltax.music.widget.RecognizerWidgetReceiver
import app.deltax.music.widget.RecognizerWidgets
import app.deltax.music.widget.TurntableWidgetReceiver
import app.deltax.music.widget.WidgetThemeConfig
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import app.deltax.music.shared.BuildConfig as SharedBuildConfig

class App :
    Application(),
    SingletonImageLoader.Factory {
    internal val startup = AppStartup()
    internal lateinit var recovery: StartupRecovery
        private set
    internal var infrastructureReady = false
        private set
    internal var restoringBackup = false
        private set

    val playbackNetworkMonitor by lazy { GlobalPlaybackNetworkMonitor(this) }

    lateinit var onboardingManager: OnboardingManager
        private set
    lateinit var legacyInstallMigration: LegacyInstallMigration
        private set

    var hadLegacyInstallationAtStartup: Boolean = false
        private set

    companion object {
        private const val TAG = "App"
        lateinit var instance: App
            private set

        private var startupUpdateCheckClaimed = false

        @Synchronized
        fun claimStartupUpdateCheck(): Boolean {
            if (startupUpdateCheckClaimed) return false
            startupUpdateCheckClaimed = true
            return true
        }
    }

    override fun attachBaseContext(base: Context) {
        val process = currentProcessName()
        if (process != "${base.packageName}:crash") CrashHandler.install(base)
        if (process == "${base.packageName}:crash") {
            super.attachBaseContext(base)
            return
        }
        if (currentProcessName() == "${base.packageName}:potoken") {
            super.attachBaseContext(base)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) WebView.setDataDirectorySuffix("potoken")
        } else if (process == base.packageName) {
            recovery =
                StartupRecovery(CrashHandler.store(base).pendingReportId()) {
                    infrastructureReady = false
                    CrashHandler.record(base, it)
                }
            var context = base
            recovery.run { context = AppLocale.wrap(base) }
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(AppLocale.wrap(base))
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    override fun onCreate() {
        super.onCreate()
        Timber.plant(AppFailureTree())
        instance = this
        AndroidContextProvider.applicationContext = applicationContext
        val processName = currentProcessName()
        if (processName == packageName) {
            Settings.shared.hasCanvasToggle = true
            recovery.run { initializeMainProcess() }
            return
        }
        setResourceReaderAndroidContext(applicationContext)
        if (processName == "$packageName:potoken" || processName == "$packageName:crash") {
            OkHttp.initialize(this)
        }
        if (processName == "$packageName:potoken" && BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun initializeMainProcess() {
        setResourceReaderAndroidContext(applicationContext)
        val appStart = SystemClock.elapsedRealtime()
        val hasPendingRestore = BackupRestoreManager.hasPendingRestore(this)
        restoringBackup = hasPendingRestore
        CoroutineScope(Dispatchers.Main.immediate).launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                startup.initialize(
                    restore = {
                        if (hasPendingRestore) {
                            withContext(Dispatchers.IO) { BackupRestoreManager.applyPendingRestore(this@App) }
                            Settings.shared.androidLastStorageAudit = 0L
                            Settings.shared.androidLastTrackedPlaylistSync = 0L
                        }
                    },
                    start = {
                        initializeApp(appStart)
                        infrastructureReady = true
                        withContext(Dispatchers.IO) {
                            Snapshot.withMutableSnapshot { SongLikeState.initialize() }
                            AuthManager().restorePersistedSession()
                        }
                        if (onboardingManager.isComplete) startDatabaseDependentServices()
                    },
                )
                restoringBackup = false
                if (hasPendingRestore) refreshWidgetsAfterRestore()
            } catch (error: Throwable) {
                recovery.capture(error)
            }
        }
    }

    private var wallpaperColorsListener: WallpaperManager.OnColorsChangedListener? = null
    private var widgetThemeObservationStarted = false

    private fun refreshWidgetsAfterRestore() {
        val manager = AppWidgetManager.getInstance(this)
        listOf(
            MusicWidgetReceiver::class.java,
            ExpressiveWidgetReceiver::class.java,
            PlaylistWidgetReceiver::class.java,
            TurntableWidgetReceiver::class.java,
            RecognizerWidgetReceiver::class.java,
        ).forEach { receiver ->
            val component = ComponentName(this, receiver)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                sendBroadcast(
                    Intent(
                        AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                    ).setComponent(component).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }

    private fun observeWidgetThemeChanges() {
        if (widgetThemeObservationStarted) return
        widgetThemeObservationStarted = true
        get<CoroutineScope>().launch {
            snapshotFlow { WidgetThemeConfig.fromSettings() }
                .distinctUntilChanged()
                .collectLatest {
                    refreshInstalledWidgetsTheme()
                }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getSystemService(UiModeManager::class.java)?.addContrastChangeListener(mainExecutor) {
                get<CoroutineScope>().launch { refreshInstalledWidgetsTheme() }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && wallpaperColorsListener == null) {
            runCatching {
                val listener =
                    WallpaperManager.OnColorsChangedListener { _, _ ->
                        if (Settings.shared.materialYouEnabled) {
                            get<CoroutineScope>().launch { refreshInstalledWidgetsTheme() }
                        }
                    }
                WallpaperManager.getInstance(this).addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
                wallpaperColorsListener = listener
            }.onFailure { error ->
                Timber.tag(TAG).w(error, "Could not observe wallpaper color changes for widgets")
            }
        }
    }

    private suspend fun refreshInstalledWidgetsTheme() {
        runCatching {
            get<MetrolistWidgetManager>().refreshWidgetsTheme()
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Timber.tag(TAG).w(error, "Failed to refresh widgets after theme change")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!infrastructureReady) return
        get<CoroutineScope>().launch { refreshInstalledWidgetsTheme() }
    }

    private fun initializeApp(appStart: Long) {
        checkAndroidDatabaseVersion(this)
        LastFM.initialize(SharedBuildConfig.LASTFM_API_KEY, SharedBuildConfig.LASTFM_SECRET)
        LastFM.renderedPageLoader = LastFmGalleryBrowser::load
        playbackNetworkMonitor.start()
        AppLocale.sync(this)
        onboardingManager = OnboardingManager(this)
        legacyInstallMigration = LegacyInstallMigration(this)
        hadLegacyInstallationAtStartup = legacyInstallMigration.hasLegacyInstallation()
        applyTvContinuePlaybackDefault()
        if (hadLegacyInstallationAtStartup) Settings.shared.migrateLegacyV13AccountSettings()
        setPlaybackServicesEnabled(onboardingManager.isComplete)
        migrateTelemetryConsent()
        migrateCleartextIntegrations()
        migrateLegacyEqualizerProfiles()
        EqualizerProfiles.ensureDefaultProfile()

        if (onboardingManager.isComplete) SentryManager.initialize(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val koinStart = SystemClock.elapsedRealtime()
        startKoin {
            androidContext(this@App)
            modules(coreModule, sharedModule, appModule)
        }
        Timber.tag(TAG).d("Koin init complete elapsed=%dms", SystemClock.elapsedRealtime() - koinStart)

        get<CoroutineScope>().launch(Dispatchers.Main.immediate) {
            observeDiscordInitialization {
                DiscordTokenStore.init(this@App)
                DiscordRpcManager.init(GlobalYouTube.client)
            }
        }

        get<CoroutineScope>().launch {
            IconUtils.syncWithPreference(this@App)
        }

        get<CoroutineScope>().launch {
            BlockedContent.refreshAiSlopBlocklist()
        }

        val nm = NotificationManagerCompat.from(this)
        nm.createNotificationChannel(
            NotificationChannelCompat
                .Builder(AppUpdateManager.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(R.string.updates_channel_name))
                .setDescription(getString(R.string.updates_channel_description))
                .build(),
        )
        nm.createNotificationChannel(
            NotificationChannelCompat
                .Builder(SongDownloadManager.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.downloads))
                .build(),
        )
        nm.createNotificationChannel(
            NotificationChannelCompat
                .Builder(SongDownloadManager.COMPLETION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(R.string.completed_downloads))
                .build(),
        )
        AndroidPlaylistImportNotifier.ensureChannel(this)
        LastFmSessionNotification.register(this)

        Timber.tag(TAG).d("App onCreate complete elapsed=%dms", SystemClock.elapsedRealtime() - appStart)
    }

    private fun migrateLegacyEqualizerProfiles() {
        if (Settings.shared.equalizerProfilesJson.isNotEmpty()) return
        val legacy = getSharedPreferences("nanosonic_eq_profiles", MODE_PRIVATE)
        EqualizerProfiles.migrateLegacy(legacy.getString("eq_profiles", null), legacy.getString("active_profile_id", null))
    }

    private fun migrateCleartextIntegrations() {
        if (!isAllowedListenTogetherServerUrl(Settings.shared.listenTogetherServerUrl)) {
            Settings.shared.listenTogetherServerUrl = Settings.DEFAULT_LISTEN_TOGETHER_SERVER_URL
        }
        Settings.shared.listenTogetherCustomServers =
            Settings.shared.listenTogetherCustomServers
                .lineSequence()
                .filter(::isAllowedListenTogetherServerUrl)
                .joinToString("\n")
    }

    private fun migrateTelemetryConsent() {
        if (!isCrashReportingAvailable()) return
        val preferences = getSharedPreferences("metrolist_security_migrations", MODE_PRIVATE)
        if (!preferences.getBoolean("telemetry_consent_v1", false)) {
            Settings.shared.sentryEnabled = false
            preferences.edit { putBoolean("telemetry_consent_v1", true) }
        }
    }

    private fun applyTvContinuePlaybackDefault() {
        val preferences = getSharedPreferences("metrolist_onboarding", MODE_PRIVATE)
        val applied = preferences.getBoolean("tv_continue_playback_default_v1", false)
        if (
            shouldApplyTvContinuePlaybackDefault(
                isTv = isAndroidTv(),
                hasLegacyInstallation = hadLegacyInstallationAtStartup,
                onboardingComplete = onboardingManager.isComplete,
                migrationApplied = applied,
            )
        ) {
            Settings.shared.promptContinuePlayback = true
            preferences.edit { putBoolean("tv_continue_playback_default_v1", true) }
        }
    }

    fun completeOnboarding(sendCrashReports: Boolean) {
        if (isCrashReportingAvailable()) Settings.shared.sentryEnabled = sendCrashReports
        onboardingManager.complete()
        setPlaybackServicesEnabled(true)
        SentryManager.initialize(this)
        startDatabaseDependentServices()
    }

    private fun setPlaybackServicesEnabled(enabled: Boolean) {
        val state =
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        listOf(MusicService::class.java, DownloadService::class.java).forEach { service ->
            packageManager.setComponentEnabledSetting(
                ComponentName(this, service),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    internal fun restoreDeviceConnectIfEnabled() {
        val canUseLocalNetwork = AndroidDeviceConnect.canUseLocalNetwork(this)
        get<CoroutineScope>().launch {
            AndroidDeviceConnect.manager.restoreIfEnabled(applicationContext, canUseLocalNetwork)
        }
    }

    private fun startDatabaseDependentServices() {
        get<CoroutineScope>().launch(Dispatchers.IO) {
            SongLikeState.recoverDatabaseLikes(get<MusicDatabase>())
        }
        get<CoroutineScope>().launch(Dispatchers.IO) { SentryManager.reportPlaybackClientStats() }
        get<CoroutineScope>().launch {
            MusicRecognitionService.status.collect {
                try {
                    TileService.requestListeningState(this@App, ComponentName(this@App, MusicRecognizerTileService::class.java))
                } catch (_: IllegalArgumentException) {
                    // Secondary profiles such as MIUI's Dual Apps (user 999) cannot host Quick Settings tiles.
                }
            }
        }
        get<CoroutineScope>().launch {
            MusicRecognitionService.status.collectLatest {
                RecognizerWidgets.update(this@App)
            }
        }
        observeWidgetThemeChanges()
        SongMetadataState.bind(get<MusicDatabase>(), get<CoroutineScope>())
        get<SongDownloadManager>().observeTrackedPlaylists(get<CoroutineScope>())
        get<CoroutineScope>().launch {
            PlaybackNetworkStateStore.state
                .map { it.connected }
                .distinctUntilChanged()
                .collectLatest { connected ->
                    if (connected) get<AudioCacheManager>().repairDownloadedArtwork()
                }
        }
        get<CoroutineScope>().launch {
            val settings = Settings.shared
            val location = Json.encodeToString(listOf(settings.audioCacheLocation, settings.downloadLocation, settings.customDownloadPath))
            val now = System.currentTimeMillis()
            if (get<AudioCacheManager>().needsLegacyAudioImport() ||
                startupMaintenanceDue(settings.androidLastStorageAudit, now, STORAGE_AUDIT_INTERVAL_MS) ||
                settings.androidStorageAuditLocation != location
            ) {
                runStartupStorageAudit(
                    audit = { get<AudioCacheManager>().reconcileStorage() },
                    onSuccess = {
                        settings.androidStorageAuditLocation = location
                        settings.androidLastStorageAudit = now
                    },
                    onFailure = { Timber.tag(TAG).w(it, "Startup storage audit failed; retrying on next startup") },
                )
            }
        }
        get<CoroutineScope>().launch {
            val now = System.currentTimeMillis()
            if (PlaybackNetworkStateStore.current.connected &&
                startupMaintenanceDue(Settings.shared.androidLastTrackedPlaylistSync, now, TRACKED_PLAYLIST_SYNC_INTERVAL_MS)
            ) {
                if (get<SongDownloadManager>().syncTrackedPlaylists()) Settings.shared.androidLastTrackedPlaylistSync = now
            }
        }
        get<CoroutineScope>().launch {
            SongLikeState.newLikes.collect { song ->
                if (Settings.shared.autoDownloadOnLike) get<SongDownloadManager>().download(song)
            }
        }
        get<CoroutineScope>().launch(Dispatchers.IO) {
            if (get<SongDownloadManager>().hasRunnableDownloads()) DownloadService.start(this@App)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader = createAppImageLoader(context)
}

private class AppFailureTree : Timber.Tree() {
    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority == Log.WARN || priority == Log.ERROR

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val level =
            when (priority) {
                Log.ERROR -> PlaybackEventLevel.ERROR
                Log.WARN -> PlaybackEventLevel.WARN
                else -> return
            }
        if (message.startsWith("[YtLogger] ")) return
        val messageWithoutThrowable =
            t?.let { throwable ->
                val stackTrace =
                    StringWriter(256)
                        .also { writer ->
                            throwable.printStackTrace(PrintWriter(writer, false))
                        }.toString()
                message.removeSuffix("\n$stackTrace").removeSuffix(stackTrace).removeSuffix("\n")
            } ?: message
        PlaybackEventLog.captureApplicationFailure(level, tag ?: "Android", messageWithoutThrowable, t)
    }
}

private fun currentProcessName(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Application.getProcessName()
    } else {
        File("/proc/self/cmdline").readText().substringBefore('\u0000')
    }
