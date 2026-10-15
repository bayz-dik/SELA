package id.sela.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografi pastel — besar, ekspresif, dan ramah. Display memakai
 * ExtraBold dengan tracking rapat untuk kesan playful-premium;
 * body tetap lega agar mudah dibaca di bawah sinar matahari (di halte!).
 *
 * Catatan: memakai typeface sistem. Bila nanti ada lisensi font rounded
 * (mis. Nunito/Quicksand), cukup timpa [SelaPastelTypography] di satu tempat.
 */
val SelaPastelTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, lineHeight = 58.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 50.sp,
        letterSpacing = (-1.2).sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 42.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Angka hitung mundur ETA — besar, tabular, tidak bergeser saat berubah. */
val PastelNumerals = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 40.sp,
    lineHeight = 46.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum",
)

/** Varian ringkas untuk ETA di daftar. */
val PastelNumeralsSmall = PastelNumerals.copy(fontSize = 24.sp, lineHeight = 30.sp)
