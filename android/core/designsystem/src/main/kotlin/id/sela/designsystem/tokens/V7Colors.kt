package id.sela.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palet SELA x Threads — hasil reverse engineering APK Threads v451.
 *
 * Design system Barcelona (bds) + Instagram (igds):
 * - Light: background #FFFFFF, teks #000000, divider #EFEFEF
 * - Dark: background #000000 (pure black), teks #F5F5F5, divider #262626
 * - Aksen: biru #0095F6
 * - Prinsip: monokrom total, NOL shadow, hairline divider, pill button
 *
 * Struktur init aman (pelajaran commit 6272805):
 * palet DI DALAM object, scheme pakai by lazy — tidak ada circular static init.
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
    val accentSoft: Color,
    val dangerSoft: Color,
    val successSoft: Color,
    val warningSoft: Color,
    val chatIncoming: Color,
)

object SelaV7 {
    // Palet ditaruh DI DALAM object (bukan top-level) agar tidak ada
    // circular static init. SelaV7.<clinit> self-contained.
    // Light = Threads light mode (bds/igds)
    private val lightPalette = SelaV7Palette(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF5F5F5),
        surfaceDim = Color(0xFFEFEFEF),
        ink = Color(0xFF000000),
        inkSoft = Color(0xFF737373),
        inkFaint = Color(0xFFA8A8A8),
        hairline = Color(0xFFEFEFEF),
        accentSoft = Color(0xFFE0F1FF),
        dangerSoft = Color(0xFFFFE5E5),
        successSoft = Color(0xFFE5F9EC),
        warningSoft = Color(0xFFFFF3E0),
        chatIncoming = Color(0xFFF5F5F5),
    )

    // Dark = Threads dark mode: PURE BLACK #000000
    private val darkPalette = SelaV7Palette(
        background = Color(0xFF000000),
        surface = Color(0xFF101010),
        surfaceDim = Color(0xFF1A1A1A),
        ink = Color(0xFFF5F5F5),
        inkSoft = Color(0xFFA8A8A8),
        inkFaint = Color(0xFF737373),
        hairline = Color(0xFF262626),
        accentSoft = Color(0xFF00376B),
        dangerSoft = Color(0xFF3A1A1A),
        successSoft = Color(0xFF1A3A24),
        warningSoft = Color(0xFF3A2E1A),
        chatIncoming = Color(0xFF1A1A1A),
    )

    /**
     * Palet aktif — diganti oleh [id.sela.designsystem.SelaV7Theme] sesuai mode.
     */
    @Volatile
    internal var palette: SelaV7Palette = darkPalette
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

    // Aksen biru Threads #0095F6 — pengganti lime sebagai warna aksi utama.
    // Nama 'lime' dipertahankan agar call site lama tidak rusak.
    val lime = Color(0xFF0095F6)
    val limeSoft: Color get() = palette.accentSoft
    val limeDeep = Color(0xFF0074CC)

    // Biru — aksen sekunder (tetap, kecuali soft)
    val peri = Color(0xFF0095F6)
    val periSoft: Color get() = palette.accentSoft
    val periDeep = Color(0xFF0074CC)

    // Maskot dihapus dari desain — properti dipertahankan agar tidak rusak,
    // tapi tidak dipakai di layar baru. Jangan pakai untuk UI baru.
    @Deprecated("Desain Threads tidak pakai maskot", ReplaceWith("SelaV7.surfaceDim"))
    val mascotBody = Color(0xFF262626)
    @Deprecated("Desain Threads tidak pakai maskot", ReplaceWith("SelaV7.surfaceDim"))
    val mascotBodyDark = Color(0xFF1A1A1A)
    val mascotEyeWhite = Color(0xFFFFFFFF)
    val mascotPupil = Color(0xFF000000)

    // Shortcut Home — monokrom Threads (tetap untuk kompatibilitas)
    val shortcutMap = Color(0xFF262626)
    val shortcutRoute = Color(0xFF262626)
    val shortcutMusic = Color(0xFF262626)
    val shortcutAsk = Color(0xFF262626)
    val shortcutFav = Color(0xFF262626)
    val shortcutMore = Color(0xFF262626)

    // Status (tetap, kecuali soft)
    val danger = Color(0xFFFF3B30)
    val dangerSoft: Color get() = palette.dangerSoft
    val success = Color(0xFF1CD14F)
    val successSoft: Color get() = palette.successSoft
    val warning = Color(0xFFFF9F0A)
    val warningSoft: Color get() = palette.warningSoft

    // Chat (incoming mengikuti mode)
    val chatIncoming: Color get() = palette.chatIncoming
    val chatOutgoing = Color(0xFF0095F6)

    // Token tambahan Threads (bds grey scale)
    val grey0 = Color(0xFFF5F5F5)
    val grey1 = Color(0xFFEFEFEF)
    val grey2 = Color(0xFFDBDBDB)
    val grey6 = Color(0xFF737373)
    val grey9 = Color(0xFF262626)
    val grey10 = Color(0xFF1A1A1A)
    val blue = Color(0xFF0095F6)
    val blueLight = Color(0xFFE0F1FF)
    val blueDark = Color(0xFF0074CC)
    val greenOnline = Color(0xFF1CD14F)
}

/** Skema gelap Threads — PURE BLACK #000000.
 * Lazy agar tidak di-init saat V7ColorsKt.<clinit> (hindari circular static init). */
val SelaV7DarkScheme by lazy {
    darkColorScheme(
    primary = SelaV7.lime,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00376B),
    onPrimaryContainer = Color(0xFFE0F1FF),
    secondary = SelaV7.peri,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color(0xFFF5F5F5),
    tertiary = SelaV7.limeDeep,
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceTint = Color(0xFF0095F6),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF1A1A1A),
    error = SelaV7.danger,
    onError = Color.White,
    errorContainer = Color(0xFF3A1A1A),
    onErrorContainer = Color(0xFFFFB4B4),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = SelaV7.limeDeep,
    scrim = Color.Black,
    )
}

/** Skema terang Threads — putih #FFFFFF.
 * Lazy agar tidak di-init saat V7ColorsKt.<clinit> (hindari circular static init). */
val SelaV7Scheme by lazy {
    lightColorScheme(
    primary = SelaV7.lime,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F1FF),
    onPrimaryContainer = Color(0xFF00376B),
    secondary = SelaV7.peri,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF000000),
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
    outlineVariant = Color(0xFFF5F5F5),
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
