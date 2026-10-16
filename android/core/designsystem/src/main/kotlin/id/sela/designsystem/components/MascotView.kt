package id.sela.designsystem

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.sela.designsystem.tokens.SelaPastel

/**
 * Maskot 2D original SELA — "Sela".
 *
 * 10 animasi sebagai animated WebP (16-20 frame, render Blender).
 * Dirender via [AnimatedImageDrawable] native (ImageDecoder, API 28+) —
 * animasi dijamin berjalan, tanpa tergantung decoder pihak ketiga.
 * API 26-27: fallback frame pertama statis.
 */
enum class SelaMascotExpression {
    Happy,
    Thinking,
    Sleeping,
    Idle,
    Welcome,
    Searching,
    RouteFound,
    WaitingBus,
    BusApproaching,
    Offline,
    Error,
    Success,
    Empty,
}

/** File WebP per ekspresi (nama resource drawable). */
private fun SelaMascotExpression.assetName(): String = when (this) {
    SelaMascotExpression.Idle -> "sela_mascot_idle"
    SelaMascotExpression.Welcome -> "sela_mascot_welcome"
    SelaMascotExpression.Searching -> "sela_mascot_searching"
    SelaMascotExpression.RouteFound -> "sela_mascot_route_found"
    SelaMascotExpression.WaitingBus -> "sela_mascot_waiting_bus"
    SelaMascotExpression.BusApproaching -> "sela_mascot_bus_approaching"
    SelaMascotExpression.Offline -> "sela_mascot_offline"
    SelaMascotExpression.Error -> "sela_mascot_error"
    SelaMascotExpression.Success -> "sela_mascot_success"
    SelaMascotExpression.Empty -> "sela_mascot_empty"
    // Alias lama -> peta ke animasi terdekat
    SelaMascotExpression.Happy -> "sela_mascot_welcome"
    SelaMascotExpression.Thinking -> "sela_mascot_searching"
    SelaMascotExpression.Sleeping -> "sela_mascot_offline"
}

@Composable
fun SelaMascot(
    expression: SelaMascotExpression = SelaMascotExpression.Idle,
    modifier: Modifier = Modifier,
    diameter: Dp = 120.dp,
    // bodyColor dipertahankan untuk kompatibilitas API.
    bodyColor: Color = SelaPastel.mint,
    // animate dipertahankan untuk kompatibilitas API; WebP selalu beranimasi
    // (gerakan karakter itu sendiri, bukan dekorasi).
    animate: Boolean = true,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val resId = remember(expression) {
        context.resources.getIdentifier(
            expression.assetName(), "drawable", context.packageName,
        )
    }
    if (resId == 0) return

    // Decode WebP animasi -> AnimatedImageDrawable (API 28+).
    val drawable: Drawable? = remember(resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(context.resources, resId),
                )
            }.getOrNull()
        } else {
            runCatching { context.getDrawable(resId) }.getOrNull()
        }
    }
    if (drawable == null) return

    // Pastikan animasi berjalan selama composable aktif.
    if (drawable is AnimatedImageDrawable) {
        DisposableEffect(drawable) {
            drawable.start()
            onDispose { drawable.stop() }
        }
    }

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                setImageDrawable(drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { view ->
            view.setImageDrawable(drawable)
            (drawable as? AnimatedImageDrawable)?.let { if (!it.isRunning) it.start() }
        },
        modifier = modifier
            .size(diameter)
            .semantics { this.contentDescription = contentDescription ?: "Maskot Sela" },
    )
}
