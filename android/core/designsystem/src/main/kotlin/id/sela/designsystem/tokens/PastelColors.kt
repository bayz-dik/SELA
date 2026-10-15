package id.sela.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import id.sela.model.ServiceType

/**
 * Palet pastel SELA — "Pagi Cerah".
 *
 * Dunia visual baru: playful 3D, claymorphism, aurora gradients di atas
 * warm off-white. Setiap rona punya tiga tingkat — [rona] (warna utama
 * pastel), [rona]Soft (latar/tint), [rona]Deep (teks/aksen di atas pastel,
 * lolos kontras).
 *
 * Menggantikan [SelaPalette] ("Malam Kota") untuk redesign total.
 */
object SelaPastel {
    // Latar
    val background = Color(0xFFFDFBF7) // warm off-white
    val surface = Color(0xFFFFFFFF)
    val cream = Color(0xFFFFF8F0)
    val creamDeep = Color(0xFFF6ECDC)

    // Tinta (teks)
    val ink = Color(0xFF2A2A3A)
    val inkSoft = Color(0xFF55556A)
    val inkFaint = Color(0xFF8E8EA3)
    val hairline = Color(0xFFEDE5D5)

    // Biru lembut
    val blue = Color(0xFFA8C8F0)
    val blueSoft = Color(0xFFDCE9FB)
    val blueDeep = Color(0xFF4E7CC4)

    // Lavender
    val lavender = Color(0xFFC8B8F0)
    val lavenderSoft = Color(0xFFE7DFFB)
    val lavenderDeep = Color(0xFF8266CC)

    // Mint
    val mint = Color(0xFFA8E8C8)
    val mintSoft = Color(0xFFD9F5E5)
    val mintDeep = Color(0xFF3D9E6B)

    // Ungu pastel
    val purple = Color(0xFFD8B8F0)
    val purpleSoft = Color(0xFFF0E2FC)
    val purpleDeep = Color(0xFF9358C9)

    // Pink
    val pink = Color(0xFFF4B8D0)
    val pinkSoft = Color(0xFFFBE3EC)
    val pinkDeep = Color(0xFFC95F8B)

    // Peach & kuning cerah
    val peach = Color(0xFFFFD9B3)
    val peachSoft = Color(0xFFFFEEDD)
    val peachDeep = Color(0xFFD98A3D)
    val sunny = Color(0xFFFFE08A)
    val sunnyDeep = Color(0xFFB98A12)

    // Layanan — pastel, tapi tetap saling berbeda satu sama lain
    val brt = Color(0xFFFF9D9D)
    val brtDeep = Color(0xFFD95F5F)
    val jakLingko = Color(0xFF8FCBF5)
    val jakLingkoDeep = Color(0xFF3E8FC7)
    val bus = Color(0xFF8FDFB2)
    val busDeep = Color(0xFF3D9E6B)

    // Status
    val danger = Color(0xFFE8696B)
    val dangerSoft = Color(0xFFFBDCDC)
    val dangerDeep = Color(0xFFB3261E)
    val success = Color(0xFF43A86F)
    val successSoft = Color(0xFFD9F2E2)
    val warning = Color(0xFFE8A13D)
    val warningSoft = Color(0xFFFBEBD2)
}

/** Skema terang "Pagi Cerah" — satu-satunya tema redesign pastel. */
val SelaPastelScheme = lightColorScheme(
    primary = SelaPastel.blueDeep,
    onPrimary = Color.White,
    primaryContainer = SelaPastel.blueSoft,
    onPrimaryContainer = Color(0xFF1E3A63),
    secondary = SelaPastel.lavenderDeep,
    onSecondary = Color.White,
    secondaryContainer = SelaPastel.lavenderSoft,
    onSecondaryContainer = Color(0xFF3D2E63),
    tertiary = SelaPastel.mintDeep,
    onTertiary = Color.White,
    tertiaryContainer = SelaPastel.mintSoft,
    onTertiaryContainer = Color(0xFF1E4A33),
    background = SelaPastel.background,
    onBackground = SelaPastel.ink,
    surface = SelaPastel.surface,
    onSurface = SelaPastel.ink,
    surfaceVariant = SelaPastel.cream,
    onSurfaceVariant = SelaPastel.inkSoft,
    surfaceTint = SelaPastel.blue,
    surfaceDim = SelaPastel.creamDeep,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SelaPastel.cream,
    surfaceContainer = Color(0xFFF9F3E8),
    surfaceContainerHigh = Color(0xFFF3EADA),
    surfaceContainerHighest = Color(0xFFEBDDC4),
    outline = SelaPastel.hairline,
    outlineVariant = Color(0xFFF5EFE2),
    error = SelaPastel.danger,
    onError = Color.White,
    errorContainer = SelaPastel.dangerSoft,
    onErrorContainer = Color(0xFF6E1F22),
    inverseSurface = SelaPastel.ink,
    inverseOnSurface = SelaPastel.cream,
    inversePrimary = SelaPastel.blue,
    scrim = SelaPastel.ink,
)

/** Warna pastel tiap layanan — untuk titik, lencana, dan chip filter. */
fun ServiceType.pastelColor(): Color = when (this) {
    ServiceType.BRT -> SelaPastel.brt
    ServiceType.JAKLINGKO -> SelaPastel.jakLingko
    ServiceType.BUS -> SelaPastel.bus
}

/** Varian deep (lolos kontras) tiap layanan — untuk teks di atas pastel. */
fun ServiceType.pastelDeepColor(): Color = when (this) {
    ServiceType.BRT -> SelaPastel.brtDeep
    ServiceType.JAKLINGKO -> SelaPastel.jakLingkoDeep
    ServiceType.BUS -> SelaPastel.busDeep
}
