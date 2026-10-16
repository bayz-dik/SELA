package id.sela.designsystem

import androidx.compose.material3.darkColorScheme
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
/**
 * Palet V7 yang bisa ganti terang/gelap.
 * Warna identitas (lime, maskot, shortcut) sama di kedua mode;
 * warna permukaan & tinta menyesuaikan mode.
 */
@androidx.compose.runtime.Immutable
data class SelaV7Palette(
    val background: Color,
    val surface: Color,
    val surfaceDim: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val hairline: Color,
    val limeSoft: Color,
    val periSoft: Color,
    val dangerSoft: Color,
    val successSoft: Color,
    val warningSoft: Color,
    val chatIncoming: Color,
)

object SelaV7 {
    // Palet ditaruh DI DALAM object (bukan top-level) agar tidak ada
    // circular static init: SelaV7 -> top-level palette -> V7ColorsKt.<clinit>
    // -> SelaV7Scheme -> SelaV7.lime (belum init) -> NPE.
    // Dengan ini, SelaV7.<clinit> self-contained tanpa dependensi eksternal.
    private val lightPalette = SelaV7Palette(
        background = Color(0xFFFFFDF1),
        surface = Color(0xFFFFFEF8),
        surfaceDim = Color(0xFFF5F0DC),
        ink = Color(0xFF06133E),
        inkSoft = Color(0xFF3D4A6B),
        inkFaint = Color(0xFF8A93B2),
        hairline = Color(0xFFE8E2CC),
        limeSoft = Color(0xFFE4F9B8),
        periSoft = Color(0xFFDFE6FF),
        dangerSoft = Color(0xFFFBDCDC),
        successSoft = Color(0xFFD9F2E2),
        warningSoft = Color(0xFFFBEBD2),
        chatIncoming = Color(0xFFF0EDDF),
    )

    private val darkPalette = SelaV7Palette(
        background = Color(0xFF0A1128),
        surface = Color(0xFF111A36),
        surfaceDim = Color(0xFF1A2342),
        ink = Color(0xFFF2F4FA),
        inkSoft = Color(0xFFB9C2D8),
        inkFaint = Color(0xFF7A86A3),
        hairline = Color(0xFF232E52),
        limeSoft = Color(0xFF2E3A10),
        periSoft = Color(0xFF232B52),
        dangerSoft = Color(0xFF4A1D1F),
        successSoft = Color(0xFF1A3A28),
        warningSoft = Color(0xFF4A3418),
        chatIncoming = Color(0xFF1A2342),
    )

    /**
     * Palet aktif — diganti oleh [id.sela.designsystem.SelaV7Theme] sesuai mode.
     * Semua properti di bawah membaca palet aktif, jadi toggle dark mode
     * langsung tercermin di seluruh layar tanpa ubah call site.
     */
    @Volatile
    internal var palette: SelaV7Palette = lightPalette
        private set

    internal fun setDarkMode(dark: Boolean) {
        palette = if (dark) darkPalette else lightPalette
    }

    // Latar (mengikuti mode)
    val background: Color get() = palette.background
    val surface: Color get() = palette.surface
    val surfaceDim: Color get() = palette.surfaceDim

    // Tinta (mengikuti mode)
    val ink: Color get() = palette.ink
    val inkSoft: Color get() = palette.inkSoft
    val inkFaint: Color get() = palette.inkFaint
    val hairline: Color get() = palette.hairline

    // Lime — identitas utama (tetap)
    val lime = Color(0xFFA9F500)
    val limeSoft: Color get() = palette.limeSoft
    val limeDeep = Color(0xFF6FA800)

    // Periwinkle — aksen peta/info (tetap, kecuali soft)
    val peri = Color(0xFF91A6FF)
    val periSoft: Color get() = palette.periSoft
    val periDeep = Color(0xFF4A5FC9)

    // Maskot hijau (tetap — maskot selalu lime)
    val mascotBody = Color(0xFFA9F500)
    val mascotBodyDark = Color(0xFF8FD600)
    val mascotEyeWhite = Color(0xFFFFFFFF)
    val mascotPupil = Color(0xFF06133E)

    // Kartu shortcut Home (6 warna pastel cerah — tetap)
    val shortcutMap = Color(0xFFB8F04A)
    val shortcutRoute = Color(0xFF7FB8F0)
    val shortcutMusic = Color(0xFFF5A8B8)
    val shortcutAsk = Color(0xFFB8A8F0)
    val shortcutFav = Color(0xFFFFD080)
    val shortcutMore = Color(0xFFD8D8D8)

    // Status (tetap, kecuali soft)
    val danger = Color(0xFFE8696B)
    val dangerSoft: Color get() = palette.dangerSoft
    val success = Color(0xFF43A86F)
    val successSoft: Color get() = palette.successSoft
    val warning = Color(0xFFE8A13D)
    val warningSoft: Color get() = palette.warningSoft

    // Chat (incoming mengikuti mode)
    val chatIncoming: Color get() = palette.chatIncoming
    val chatOutgoing = Color(0xFF91A6FF)
}

/** Skema gelap V7 — navy pekat, lime tetap sebagai identitas.
 * Lazy agar tidak di-init saat V7ColorsKt.<clinit> (hindari circular static init). */
val SelaV7DarkScheme by lazy {
    darkColorScheme(
    primary = SelaV7.lime,
    onPrimary = Color(0xFF0A1128),
    primaryContainer = Color(0xFF3A4A12),
    onPrimaryContainer = SelaV7.lime,
    secondary = SelaV7.peri,
    onSecondary = Color(0xFF0A1128),
    secondaryContainer = Color(0xFF2A3358),
    onSecondaryContainer = SelaV7.peri,
    tertiary = SelaV7.limeDeep,
    onTertiary = Color.White,
    background = Color(0xFF0A1128),
    onBackground = Color(0xFFF2F4FA),
    surface = Color(0xFF111A36),
    onSurface = Color(0xFFF2F4FA),
    surfaceVariant = Color(0xFF1A2342),
    onSurfaceVariant = Color(0xFFB9C2D8),
    surfaceTint = SelaV7.lime,
    outline = Color(0xFF232E52),
    outlineVariant = Color(0xFF1A2342),
    error = SelaV7.danger,
    onError = Color.White,
    errorContainer = Color(0xFF4A1D1F),
    onErrorContainer = Color(0xFFFFB4B4),
    inverseSurface = Color(0xFFFFFDF1),
    inverseOnSurface = Color(0xFF06133E),
    inversePrimary = SelaV7.limeDeep,
    scrim = Color.Black,
    )
}

/** Skema terang V7 — satu-satunya tema redesign.
 * Lazy agar tidak di-init saat V7ColorsKt.<clinit> (hindari circular static init). */
val SelaV7Scheme by lazy {
    lightColorScheme(
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
}
