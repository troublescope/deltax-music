package app.deltax.music.playback

internal suspend fun runWidgetPlaybackCommand(
    needsPromotion: Boolean,
    promote: suspend () -> Boolean,
    isCurrent: () -> Boolean,
    command: () -> Unit,
) {
    if (needsPromotion && !promote()) return
    if (isCurrent()) command()
}
