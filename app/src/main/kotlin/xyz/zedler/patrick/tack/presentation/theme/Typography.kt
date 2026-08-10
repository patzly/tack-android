package xyz.zedler.patrick.tack.presentation.theme

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
  labelSmall = baseline.labelSmall.copy(fontFamily = GoogleSansFlex),
  displayLargeEmphasized = baseline.displayLargeEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  displayMediumEmphasized = baseline.displayMediumEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  displaySmallEmphasized = baseline.displaySmallEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  headlineLargeEmphasized = baseline.headlineLargeEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  headlineMediumEmphasized = baseline.headlineMediumEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  headlineSmallEmphasized = baseline.headlineSmallEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  titleLargeEmphasized = baseline.titleLargeEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  titleMediumEmphasized = baseline.titleMediumEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  titleSmallEmphasized = baseline.titleSmallEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  bodyLargeEmphasized = baseline.bodyLargeEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  bodyMediumEmphasized = baseline.bodyMediumEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  bodySmallEmphasized = baseline.bodySmallEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  labelLargeEmphasized = baseline.labelLargeEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  labelMediumEmphasized = baseline.labelMediumEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  ),
  labelSmallEmphasized = baseline.labelSmallEmphasized.copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight.Medium
  )
)