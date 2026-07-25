package com.omnidapt.pd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrandBlue = Color(0xFF1378F2)
val DeepBlue = Color(0xFF10264F)
val MedicalGreen = Color(0xFF18C875)
val SoftRed = Color(0xFFE84747)
val Ink = Color(0xFF18233E)
val MutedText = Color(0xFF7C8497)
val Border = Color(0xFFE6EBF3)
val PageBg = Color(0xFFF8FAFE)
val PanelBg = Color.White

// Premium medical-tech visual tokens. The restrained opacity keeps long-form
// clinical content readable while allowing the animated backdrop to show through.
val AuraBlue = Color(0xFF3B82F6)
val AuraCyan = Color(0xFF67E8F9)
val AuraIndigo = Color(0xFF818CF8)
val PremiumSurface = Color(0xF2FFFFFF)
val PremiumSurfaceStrong = Color(0xFAFFFFFF)
val PremiumBorder = Color(0xFFDFE9F7)
val PremiumDivider = Color(0xFFE9EFF8)
val PremiumBlueSoft = Color(0xFFEAF3FF)

private val OminidaptColors = lightColorScheme(
    primary = BrandBlue,
    primaryContainer = Color(0xFFE6F0FF),
    onPrimaryContainer = DeepBlue,
    secondary = DeepBlue,
    secondaryContainer = Color(0xFFEAF0FA),
    onSecondaryContainer = DeepBlue,
    tertiary = MedicalGreen,
    tertiaryContainer = Color(0xFFE4F8EF),
    onTertiaryContainer = Color(0xFF086A3C),
    background = PageBg,
    surface = PanelBg,
    surfaceVariant = Color(0xFFF4F7FC),
    onSurfaceVariant = MutedText,
    error = SoftRed,
    errorContainer = Color(0xFFFFECEC),
    onErrorContainer = Color(0xFF8C1D18),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Ink,
    onSurface = Ink
)

private val OminidaptShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

private val OminidaptTypography = Typography(
    headlineLarge = TextStyle(
        color = Ink,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        color = Ink,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = TextStyle(
        color = Ink,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        color = Ink,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        color = Ink,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        color = Ink,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
)

@Composable
fun OminidaptTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OminidaptColors,
        shapes = OminidaptShapes,
        typography = OminidaptTypography,
        content = content
    )
}
