package app.deltax.music.shared.ui

import app.deltax.music.shared.generated.resources.Res
import app.deltax.music.shared.generated.resources.error_unknown
import app.deltax.music.shared.generated.resources.page_load_failed_with_error
import com.metrolist.innertubex.InnerTubeHttpException
import org.jetbrains.compose.resources.getString

suspend fun Throwable.localizedPageLoadError(): String =
    getString(
        Res.string.page_load_failed_with_error,
        (this as? InnerTubeHttpException)?.status?.toString() ?: this::class.simpleName ?: getString(Res.string.error_unknown),
    )
