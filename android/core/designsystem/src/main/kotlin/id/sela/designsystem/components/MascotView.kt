package id.sela.designsystem

import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Maskot 2.5D original SELA — "Sela".
 *
 * Render 3D asli dari Blender pipeline: 10 animasi sebagai animated WebP
 * (16-20 frame per animasi, ~1.8MB total). Karakter konsisten di semua animasi.
 *
 * 10 animasi (SelaMascotExpression -> file WebP):
 * - Idle -> sela_mascot_idle.webp
 * - Welcome -> sela_mascot_welcome.webp
 * - Searching -> sela_mascot_searching.webp
 * - RouteFound -> sela_mascot_route_found.webp
 * - WaitingBus -> sela_mascot_waiting_bus.webp
 * - BusApproaching -> sela_mascot_bus_approaching.webp
 * - Offline -> sela_mascot_offline.webp
 * - Error -> sela_mascot_error.webp
 * - Success -> sela_mascot_success.webp
 * - Empty -> sela_mascot_empty.webp
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
    // bodyColor & animate dipertahankan untuk kompatibilitas API.
    // Animasi WebP adalah gerakan karakter itu sendiri (bukan dekorasi),
    // jadi selalu diputar.
    bodyColor: Color = SelaPastel.mint,
    animate: Boolean = true,
    contentDescription: String? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resId = androidx.compose.runtime.remember(expression) {
        context.resources.getIdentifier(
            expression.assetName(), "drawable", context.packageName,
        )
    }

    if (resId == 0) return

    // Animated WebP via Coil
    AsyncImage(
        model = resId,
        contentDescription = contentDescription,
        modifier = modifier
            .size(diameter)
            .semantics { this.contentDescription = contentDescription ?: "Maskot Sela" },
    )
}
