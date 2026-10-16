package id.sela.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * FontFamily Threads — IGUIBetav9 hasil reverse APK Threads v451.
 * Dibundel di res/font/igui_*.ttf (pemakaian pribadi).
 */
val ThreadsFontFamily = FontFamily(
    Font(id.sela.designsystem.R.font.igui_regular, FontWeight.Normal),
    Font(id.sela.designsystem.R.font.igui_semibold, FontWeight.SemiBold),
    Font(id.sela.designsystem.R.font.igui_bold, FontWeight.Bold),
)

/**
 * Tipografi Threads — ketat, hierarki jelas.
 * Dari reverse: caption 12sp → body 14-15sp → title 18-22sp.
 */
val ThreadsTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = ThreadsFontFamily,
        fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp,
        color = androidx.compose.ui.graphics.Color(0xFF737373),
    ),
)
