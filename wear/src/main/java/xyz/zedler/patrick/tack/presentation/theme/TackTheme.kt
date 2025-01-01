/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import xyz.zedler.patrick.tack.R

@Composable
fun TackTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = tackColorScheme(),
    typography = tackTypography(),
    content = content
  )
}

@Composable
private fun tackColorScheme(): ColorScheme {
  return ColorScheme(
    primary = Color(0xFFDAC66F),
    primaryDim = Color(0xFFBEAB57),
    primaryContainer = Color(0xFF534600),
    onPrimary = Color(0xFF393000),
    onPrimaryContainer = Color(0xFFF8E288),
    secondary = Color(0xFFD1C6A2),
    secondaryDim = Color(0xFFD1C6A2),
    secondaryContainer = Color(0xFF4D472A),
    onSecondary = Color(0xFF363016),
    onSecondaryContainer = Color(0xFFEEE2BC),
    tertiary = Color(0xFFA9D0B3),
    tertiaryDim = Color(0xFFA9D0B3),
    tertiaryContainer = Color(0xFF2B4E38),
    onTertiary = Color(0xFF143723),
    onTertiaryContainer = Color(0xFFC4ECCF),
    surfaceContainerLow = Color(0xFF1E1C13),
    surfaceContainer = Color(0xFF222017),
    surfaceContainerHigh = Color(0xFF2C2A21),
    onSurface = Color(0xFFE8E2D4),
    onSurfaceVariant = Color(0xFFCDC6B4),
    outline = Color(0xFF969080),
    outlineVariant = Color(0xFF4B4739),
    error = Color(0xFFffb595),
    onError = Color(0xFF542105),
    errorContainer = Color(0xFF713619),
    onErrorContainer = Color(0xFFffdbcd)
  )
}

@Composable
private fun tackTypography(): Typography {
  val jostBook = remember { FontFamily(Font(R.font.jost_book)) }
  val jostMedium = remember { FontFamily(Font(R.font.jost_medium)) }
  return Typography(
    defaultFontFamily = jostBook,
    displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = jostBook),
    displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = jostBook),
    displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = jostMedium),
    titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = jostMedium),
    titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = jostMedium),
    titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = jostMedium),
    labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = jostMedium),
    labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = jostMedium),
    labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = jostMedium),
    bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = jostBook),
    bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = jostBook),
    bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = jostMedium),
    bodyExtraSmall = MaterialTheme.typography.bodyExtraSmall.copy(fontFamily = jostBook)
  )
}