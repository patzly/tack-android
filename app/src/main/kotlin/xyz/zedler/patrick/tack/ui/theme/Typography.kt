package xyz.zedler.patrick.tack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import xyz.zedler.patrick.tack.R

private val GoogleSansFlex = FontFamily(
  Font(R.font.google_sans_flex_regular, FontWeight.Normal),
  Font(R.font.google_sans_flex_medium, FontWeight.Medium)
)

private val baseline = androidx.compose.material3.Typography()

val TackTypography = Typography(
  displayLarge = baseline.displayLarge.copy(fontFamily = GoogleSansFlex),
  displayMedium = baseline.displayMedium.copy(fontFamily = GoogleSansFlex),
  displaySmall = baseline.displaySmall.copy(fontFamily = GoogleSansFlex),
  headlineLarge = baseline.headlineLarge.copy(fontFamily = GoogleSansFlex),
  headlineMedium = baseline.headlineMedium.copy(fontFamily = GoogleSansFlex),
  headlineSmall = baseline.headlineSmall.copy(fontFamily = GoogleSansFlex),
  titleLarge = baseline.titleLarge.copy(fontFamily = GoogleSansFlex),
  titleMedium = baseline.titleMedium.copy(fontFamily = GoogleSansFlex),
  titleSmall = baseline.titleSmall.copy(fontFamily = GoogleSansFlex),
  bodyLarge = baseline.bodyLarge.copy(fontFamily = GoogleSansFlex),
  bodyMedium = baseline.bodyMedium.copy(fontFamily = GoogleSansFlex),
  bodySmall = baseline.bodySmall.copy(fontFamily = GoogleSansFlex),
  labelLarge = baseline.labelLarge.copy(fontFamily = GoogleSansFlex),
  labelMedium = baseline.labelMedium.copy(fontFamily = GoogleSansFlex),
  labelSmall = baseline.labelSmall.copy(fontFamily = GoogleSansFlex)
)