/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package app.deltax.music.utils

import android.app.Application
import app.deltax.music.shared.BuildConfig
import app.deltax.music.shared.playback.PlaybackClientStats
import app.deltax.music.shared.settings.Settings
import app.deltax.music.shared.utils.isCrashReportingAvailable
import app.deltax.music.shared.utils.redactDiagnosticText
import app.deltax.music.shared.utils.redactDiagnosticUrl
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryAttribute
import io.sentry.SentryAttributes
import io.sentry.SentryEvent
import io.sentry.SentryLogLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.logger.SentryLogParameters
import io.sentry.protocol.Request

internal fun androidSentryRelease(version: String): String = "app.deltax.music@$version"

object SentryManager {
    private lateinit var application: Application

    @Synchronized
    fun initialize(application: Application) {
        this.application = application
        applyEnabled(Settings.shared.sentryEnabled)
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        Settings.shared.sentryEnabled = enabled
        applyEnabled(enabled)
    }

    suspend fun reportPlaybackClientStats() {
        val report = PlaybackClientStats.takeReportOnLaunch() ?: return
        if (Sentry.isEnabled()) sendPlaybackClientStats(report)
    }

    private fun applyEnabled(enabled: Boolean) {
        if (!enabled || !isCrashReportingAvailable()) {
            Sentry.close()
            return
        }
        if (Sentry.isEnabled()) return

        SentryAndroid.init(application) { options ->
            options.dsn = "https://fe4474514a54a507252b26c60e229016@o4511744603193344.ingest.de.sentry.io/4511744608436304"
            options.release = androidSentryRelease(BuildConfig.APP_VERSION)
            BuildConfig.SOURCE_REVISION.takeIf(String::isNotBlank)?.let { options.setTag("source_revision", it) }
            options.sampleRate = 1.0
            options.tracesSampleRate = 0.0
            options.isDebug = false
            options.environment = "production"
            options.isAttachServerName = false
            options.isEnableUncaughtExceptionHandler = true
            options.isEnableAutoSessionTracking = true
            options.isAnrEnabled = true
            options.isAttachAnrThreadDump = false
            options.isTombstoneEnabled = true
            options.isAttachRawTombstone = true
            options.addInAppInclude("app.deltax.music")
            options.logs.isEnabled = true
            options.logs.beforeSend =
                SentryOptions.Logs.BeforeSendLogCallback { log ->
                    log.takeIf { it.body == PLAYBACK_CLIENT_STATS_MESSAGE || it.body.startsWith(PLAYBACK_FAILURE_LOG_PREFIX) }
                }
            options.beforeBreadcrumb =
                SentryOptions.BeforeBreadcrumbCallback { breadcrumb, _ ->
                    sanitizeBreadcrumb(breadcrumb)
                }
            options.beforeSend =
                SentryOptions.BeforeSendCallback { event, _ ->
                    event.takeUnless { it.getTag("playback.recovered") == "true" }?.let(::sanitizeSentryEvent)
                }
        }
    }
}

internal const val PLAYBACK_CLIENT_STATS_MESSAGE = "Playback client stats"
internal const val PLAYBACK_FAILURE_LOG_PREFIX = "Playback failure log\n"

internal fun sendPlaybackClientStats(report: Map<String, Map<String, Int>>) {
    report.forEach { (profile, counts) ->
        val attributes =
            listOf(SentryAttribute.stringAttribute("playback.profile", profile)) +
                counts.map { (counter, value) -> SentryAttribute.integerAttribute("playback.$counter", value) }
        Sentry.logger().log(
            SentryLogLevel.INFO,
            SentryLogParameters.create(SentryAttributes.of(*attributes.toTypedArray())),
            PLAYBACK_CLIENT_STATS_MESSAGE,
        )
    }
}

internal fun sanitizeSentryEvent(event: SentryEvent): SentryEvent {
    event.serverName = null
    event.request =
        event.request?.let { request ->
            Request().apply {
                url = request.url?.let(::redactDiagnosticUrl)
                method = request.method
            }
        }
    event.message?.let { message ->
        message.formatted = message.formatted?.let(::redactDiagnosticText)
        message.message = message.message?.let(::redactDiagnosticText)
        message.params = message.params?.map(::redactDiagnosticText)
    }
    event.throwable = null
    event.exceptions?.forEach { exception ->
        exception.value = exception.value?.let(::redactDiagnosticText)
        exception.stacktrace?.frames?.forEach { it.vars = null }
    }
    event.threads?.forEach { thread ->
        thread.name = thread.name?.let(::redactDiagnosticText)
        thread.stacktrace?.frames?.forEach { it.vars = null }
    }
    event.breadcrumbs?.forEach(::sanitizeBreadcrumb)
    event.extras = event.extras?.mapValues { (_, value) -> (value as? String)?.let(::redactDiagnosticText) ?: value }
    return event
}

internal fun sanitizeBreadcrumb(breadcrumb: Breadcrumb): Breadcrumb {
    breadcrumb.message = breadcrumb.message?.let(::redactDiagnosticText)
    breadcrumb.data.keys.removeAll { it == "http.query" || it == "http.fragment" }
    breadcrumb.data.replaceAll { _, value -> (value as? String)?.let(::redactDiagnosticText) ?: value }
    return breadcrumb
}
