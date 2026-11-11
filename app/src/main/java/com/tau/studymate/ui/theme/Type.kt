package com.tau.studymate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.tau.studymate.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BaseTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

/** Rubik (variable font, res/font/rubik.ttf) – matches the Figma redesign and covers Hebrew. */
@OptIn(ExperimentalTextApi::class)
val Rubik = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(R.font.rubik, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private fun TextStyle.rubik() = copy(fontFamily = Rubik)

val Typography = BaseTypography.run {
    Typography(
        displayLarge.rubik(), displayMedium.rubik(), displaySmall.rubik(),
        headlineLarge.rubik(), headlineMedium.rubik(), headlineSmall.rubik(),
        titleLarge.rubik(), titleMedium.rubik(), titleSmall.rubik(),
        bodyLarge.rubik(), bodyMedium.rubik(), bodySmall.rubik(),
        labelLarge.rubik(), labelMedium.rubik(), labelSmall.rubik()
    )
}
