package id.sela.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palet SELA V7 — "Plopp".
 *
 * Mengikuti REFERENCE_CROPS/ dari paket desain V7 Bayu:
 * - Krem hangat #FFFDF1 sebagai latar utama
 * - Lime #A9F500 sebagai identitas dan aksi utama
 * - Navy #06133E untuk teks dan tombol primer
 * - Periwinkle #91A6FF untuk aksen peta/informasi
 *
 * Menggantikan palet pastel V5 untuk redesign total V7.
 */
object SelaV7 {
    // Latar
    val background = Color(0xFFFFFDF1) // krem hangat
    val surface = Color(0xFFFFFEF8)
    val surfaceDim = Color(0xFFF5F0DC)

    // Tinta (teks)
    val ink = Color(0xFF06133E) // navy
    val inkSoft = Color(0xFF3D4A6B)
    val inkFaint = Color(0xFF8A93B2)
    val hairline = Color(0xFFE8E2CC)

    // Lime — identitas utama
    val lime = Color(0xFFA9F500)
    val limeSoft = Color(0xFFE4F9B8)
    val limeDeep = Color(0xFF6FA800)

    // Periwinkle — aksen peta/info
    val peri = Color(0xFF91A6FF)
    val periSoft = Color(0xFFDFE6FF)
    val periDeep = Color(0xFF4A5FC9)

    // Maskot hijau
    val mascotBody = Color(0xFFA9F500)
    val mascotBodyDark = Color(0xFF8FD600)
    val mascotEyeWhite = Color(0xFFFFFFFF)
    val mascotPupil = Color(0xFF06133E)

    // Kartu shortcut Home (6 warna pastel cerah)
    val shortcutMap = Color(0xFFB8F04A)   // hijau lime
    val shortcutRoute = Color(0xFF7FB8F0) // biru
    val shortcutMusic = Color(0xFFF5A8B8) // pink
    val shortcutAsk = Color(0xFFB8A8F0)   // ungu
    val shortcutFav = Color(0xFFFFD080)   // kuning
    val shortcutMore = Color(0xFFD8D8D8)  // abu

    // Status
    val danger = Color(0xFFE8696B)
    val dangerSoft = Color(0xFFFBDCDC)
    val success = Color(0xFF43A86F)
    val successSoft = Color(0xFFD9F2E2)
    val warning = Color(0xFFE8A13D)
    val warningSoft = Color(0xFFFBEBD2)

    // Chat
    val chatIncoming = Color(0xFFF0EDDF) // bubble krem
    val chatOutgoing = Color(0xFF91A6FF) // bubble periwinkle
}

/** Skema terang V7 — satu-satunya tema redesign. */
val SelaV7Scheme = lightColorScheme(
    primary = SelaV7.lime,
    onPrimary = SelaV7.ink,
    primaryContainer = SelaV7.limeSoft,
    onPrimaryContainer = SelaV7.ink,
    secondary = SelaV7.peri,
    onSecondary = Color.White,
    secondaryContainer = SelaV7.periSoft,
    onSecondaryContainer = SelaV7.ink,
    tertiary = SelaV7.limeDeep,
    onTertiary = Color.White,
    background = SelaV7.background,
    onBackground = SelaV7.ink,
    surface = SelaV7.surface,
    onSurface = SelaV7.ink,
    surfaceVariant = SelaV7.surfaceDim,
    onSurfaceVariant = SelaV7.inkSoft,
    surfaceTint = SelaV7.lime,
    outline = SelaV7.hairline,
    outlineVariant = Color(0xFFF0EAD2),
    error = SelaV7.danger,
    onError = Color.White,
    errorContainer = SelaV7.dangerSoft,
    onErrorContainer = Color(0xFF6E1F22),
    inverseSurface = SelaV7.ink,
    inverseOnSurface = SelaV7.background,
    inversePrimary = SelaV7.lime,
    scrim = SelaV7.ink,
)
