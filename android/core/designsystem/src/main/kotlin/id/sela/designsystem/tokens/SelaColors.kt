package id.sela.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import id.sela.model.ServiceType

/**
 * Palet merek SELA — "Malam Kota".
 *
 * Identitas: peta gelap malam hari dengan denyut volt. Aksen utama [volt]
 * (chartreuse elektrik) dipakai untuk elemen live, aksi primer, dan logo.
 * Warna layanan tetap mudah dibedakan satu sama lain maupun dari volt.
 */
@Deprecated(
    "Redesign pastel 2026-10: gunakan SelaPastel + SelaPastelScheme. " +
        "Palet volt/malam dipertahankan sementara untuk layar lama.",
)
object SelaPalette {
    // Merek
    val volt = Color(0xFFCDF138)
    val onVolt = Color(0xFF141A08)
    val voltDim = Color(0xFF2A3312)
    val amber = Color(0xFFFFB224)
    val amberDeep = Color(0xFFB45309)

    // Malam (tema gelap)
    val malam = Color(0xFF0A0D14)
    val malamSurface = Color(0xFF10141D)
    val malamSurfaceHigh = Color(0xFF1A2130)
    val malamSurfaceHighest = Color(0xFF232C40)
    val paperWhite = Color(0xFFF5F2E9)
    val mutedSlate = Color(0xFF9AA3B5)
    val hairlineDark = Color.White.copy(alpha = 0.08f)
    val outlineDark = Color(0xFF2A3346)

    // Siang (tema terang)
    val paper = Color(0xFFF5F3EA)
    val paperSurface = Color(0xFFFDFCF8)
    val pine = Color(0xFF0B5B3E)
    val pineDeep = Color(0xFF083F2C)
    val ink = Color(0xFF141A15)
    val mutedInk = Color(0xFF5A6472)
    val hairlineLight = Color(0xFFE3E1D5)

    // Layanan — sama di kedua tema, saling berbeda dan berbeda dari volt
    val brt = Color(0xFFFF5D5D)
    val jakLingko = Color(0xFF38BDF8)
    val bus = Color(0xFF34D399)

    // Status
    val danger = Color(0xFFFF7A7A)
    val dangerDeep = Color(0xFFB3261E)
    val success = Color(0xFF34D399)
}

/** Label Indonesia untuk tipe layanan. */
fun ServiceType.label(): String = when (this) {
    ServiceType.BRT -> "BRT"
    ServiceType.JAKLINGKO -> "JakLingko"
    ServiceType.BUS -> "Bus Biasa"
}

/** Warna khas tiap layanan. */
fun ServiceType.color(): Color = when (this) {
    ServiceType.BRT -> SelaPalette.brt
    ServiceType.JAKLINGKO -> SelaPalette.jakLingko
    ServiceType.BUS -> SelaPalette.bus
}

/** Skema gelap "Malam Kota" — tema utama SELA. */
val SelaDarkScheme = darkColorScheme(
    primary = SelaPalette.volt,
    onPrimary = SelaPalette.onVolt,
    primaryContainer = SelaPalette.voltDim,
    onPrimaryContainer = Color(0xFFEAF7B0),
    secondary = SelaPalette.mutedSlate,
    onSecondary = SelaPalette.malam,
    secondaryContainer = SelaPalette.malamSurfaceHigh,
    onSecondaryContainer = SelaPalette.paperWhite,
    tertiary = SelaPalette.amber,
    onTertiary = SelaPalette.onVolt,
    tertiaryContainer = Color(0xFF3A2A0C),
    onTertiaryContainer = Color(0xFFFFD98A),
    background = SelaPalette.malam,
    onBackground = SelaPalette.paperWhite,
    surface = SelaPalette.malamSurface,
    onSurface = SelaPalette.paperWhite,
    surfaceVariant = SelaPalette.malamSurfaceHigh,
    onSurfaceVariant = SelaPalette.mutedSlate,
    surfaceTint = SelaPalette.volt,
    surfaceDim = SelaPalette.malam,
    surfaceBright = SelaPalette.malamSurfaceHighest,
    surfaceContainerLowest = SelaPalette.malam,
    surfaceContainerLow = Color(0xFF0E1219),
    surfaceContainer = SelaPalette.malamSurface,
    surfaceContainerHigh = SelaPalette.malamSurfaceHigh,
    surfaceContainerHighest = SelaPalette.malamSurfaceHighest,
    outline = SelaPalette.outlineDark,
    outlineVariant = SelaPalette.hairlineDark,
    error = SelaPalette.danger,
    onError = SelaPalette.malam,
    errorContainer = Color(0xFF3A1414),
    onErrorContainer = Color(0xFFFFB4AB),
    inverseSurface = SelaPalette.paperWhite,
    inverseOnSurface = SelaPalette.malamSurface,
    inversePrimary = SelaPalette.pine,
    scrim = Color.Black,
)

/**
 * Skema AMOLED (fitur #60): varian Malam Kota dengan hitam murni (#000000)
 * di semua permukaan — menghemat baterai layar OLED karena piksel hitam mati.
 * Hanya dipakai saat tema gelap; perilaku default tidak berubah.
 */
val SelaAmoledScheme = SelaDarkScheme.copy(
    background = Color(0xFF000000),
    onBackground = SelaPalette.paperWhite,
    surface = Color(0xFF000000),
    onSurface = SelaPalette.paperWhite,
    surfaceVariant = Color(0xFF0B0B0B),
    onSurfaceVariant = SelaPalette.mutedSlate,
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF101010),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1A1A1A),
)

/** Skema terang "Siang" — mengikuti tema sistem. */
val SelaLightScheme = lightColorScheme(
    primary = SelaPalette.pine,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBDD),
    onPrimaryContainer = SelaPalette.pineDeep,
    secondary = SelaPalette.mutedInk,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAE4),
    onSecondaryContainer = Color(0xFF3A423C),
    tertiary = SelaPalette.amberDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = SelaPalette.paper,
    onBackground = SelaPalette.ink,
    surface = SelaPalette.paperSurface,
    onSurface = SelaPalette.ink,
    surfaceVariant = Color(0xFFECEAE0),
    onSurfaceVariant = SelaPalette.mutedInk,
    surfaceTint = SelaPalette.pine,
    surfaceDim = Color(0xFFE7E4D6),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF8F1),
    surfaceContainer = Color(0xFFF3F1E7),
    surfaceContainerHigh = Color(0xFFECEADF),
    surfaceContainerHighest = Color(0xFFE5E3D5),
    outline = Color(0xFFC9CFC4),
    outlineVariant = SelaPalette.hairlineLight,
    error = SelaPalette.dangerDeep,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = SelaPalette.ink,
    inverseOnSurface = SelaPalette.paper,
    inversePrimary = SelaPalette.volt,
    scrim = Color.Black,
)
