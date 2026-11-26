package app.deltax.music.shared.cast

interface MdnsCastDiscovery {
    fun start(
        onDeviceFound: (CastDevice) -> Unit,
        onDeviceLost: (String) -> Unit,
        onError: (CastError) -> Unit = {},
    )

    fun stop()

    fun close() = stop()
}
