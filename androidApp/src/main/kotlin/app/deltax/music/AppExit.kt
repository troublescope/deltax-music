package app.deltax.music

import android.app.Activity
import app.deltax.music.shared.settings.Settings
import app.deltax.music.utils.isAndroidTv

internal fun Activity.exitApp() {
    if (isAndroidTv() && Settings.shared.stopMusicOnTaskClear) {
        // TV has no recents gesture; a confirmed root Back exit removes the task instead.
        finishAndRemoveTask()
    } else {
        finish()
    }
}
