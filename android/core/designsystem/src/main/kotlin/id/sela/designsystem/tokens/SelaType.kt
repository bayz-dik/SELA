package id.sela.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografi SELA — tegas, rapat, tanpa dekorasi.
 *
 * Angka ETA memakai [EtaNumerals]: extra-bold + angka tabular (tnum) supaya
 * hitung mundur tidak "bergoyang" saat digit berubah.
 */
val SelaTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/** Angka hitung mundur ETA — besar, tabular, tidak bergeser saat berubah. */
val EtaNumerals = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
)

/** Varian ringkas untuk ETA di daftar. */
val EtaNumeralsSmall = EtaNumerals.copy(fontSize = 22.sp, lineHeight = 28.sp)

/** Wordmark "SELA" — huruf kapital rapat, dipakai di bilah atas. */
val SelaWordmark = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 2.sp,
)
