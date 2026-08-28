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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.ui.theme

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class TackDimens(
  val dialSize: Dp,
  val paddingContent: Dp,
  val controlButtonSize: Dp,
  val spacingLarge: Dp
)

// 1. values/dimens.xml (Compact Portrait / Phone)
val CompactPortraitDimens = TackDimens(
  dialSize = 240.dp,
  paddingContent = 16.dp,
  controlButtonSize = 56.dp,
  spacingLarge = 20.dp
)

// 2. values-land/dimens.xml (Compact Height / Phone Landscape)
val CompactLandscapeDimens = TackDimens(
  dialSize = 180.dp,
  paddingContent = 12.dp,
  controlButtonSize = 48.dp,
  spacingLarge = 12.dp
)

// 3. values-sw600dp/dimens.xml (Medium Width / Tablet Portrait)
val MediumPortraitDimens = TackDimens(
  dialSize = 380.dp,
  paddingContent = 32.dp,
  controlButtonSize = 72.dp,
  spacingLarge = 40.dp
)

// 4. values-sw600dp-land/dimens.xml (Expanded Width / Tablet Landscape)
val ExpandedLandscapeDimens = TackDimens(
  dialSize = 320.dp,
  paddingContent = 24.dp,
  controlButtonSize = 64.dp,
  spacingLarge = 28.dp
)

val LocalTackDimens = compositionLocalOf { CompactPortraitDimens }

@Composable
fun rememberTackDimens(windowSizeClass: WindowSizeClass): TackDimens {
  return remember(windowSizeClass) {
    when {
      // 1. Phone Landscape: Vertikal wenig Platz (< 480dp)
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> {
        CompactLandscapeDimens
      }
      // 2. Tablet Landscape / Desktop: Breiter Screen (>= 840dp)
      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
        ExpandedLandscapeDimens
      }
      // 3. Tablet Portrait / Foldable aufgeklappt (600dp bis 839dp)
      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
        MediumPortraitDimens
      }
      // 4. Phone Portrait (Default: < 600dp Breite, >= 480dp Höhe)
      else -> {
        CompactPortraitDimens
      }
    }
  }
}