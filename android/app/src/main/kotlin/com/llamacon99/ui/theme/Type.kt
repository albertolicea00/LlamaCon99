package com.llamacon99.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System default font family, but One UI's type scale leans heavier/bolder on titles than stock
// Material — bumping weight on the large title mimics that without shipping a custom font.
val LlamaTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
)

val ManzanaTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
)

val MaterialTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
)

val HorizonTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
)

val HyperTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
)

private fun technicalStyle(style: TextStyle) = style.copy(fontFamily = FontFamily.Monospace)

val MonoTypography = Typography().let { base ->
    base.copy(
        displayLarge = technicalStyle(base.displayLarge), displayMedium = technicalStyle(base.displayMedium), displaySmall = technicalStyle(base.displaySmall),
        headlineLarge = technicalStyle(base.headlineLarge), headlineMedium = technicalStyle(base.headlineMedium),
        headlineSmall = technicalStyle(base.headlineSmall).copy(fontSize = 28.sp, letterSpacing = 2.sp),
        titleLarge = technicalStyle(base.titleLarge), titleMedium = technicalStyle(base.titleMedium), titleSmall = technicalStyle(base.titleSmall),
        bodyLarge = technicalStyle(base.bodyLarge), bodyMedium = technicalStyle(base.bodyMedium), bodySmall = technicalStyle(base.bodySmall),
        labelLarge = technicalStyle(base.labelLarge), labelMedium = technicalStyle(base.labelMedium), labelSmall = technicalStyle(base.labelSmall),
    )
}

val FluentTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
)
