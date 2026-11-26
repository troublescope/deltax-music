package app.deltax.music.shared.utils

import app.deltax.music.shared.BuildConfig

fun isCrashReportingAvailable(appVersion: String = BuildConfig.APP_VERSION): Boolean = '+' !in appVersion
