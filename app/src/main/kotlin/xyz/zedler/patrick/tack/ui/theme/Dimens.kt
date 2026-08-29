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

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

data class TackDimens(
  val mainControlsPaddingBottom: Dp = 16.dp,
  val mainControlsCenterButtonSize: DpSize = IconButtonDefaults.largeContainerSize(
    IconButtonDefaults.IconButtonWidthOption.Wide
  ),
  val mainControlsSideButtonSize: DpSize = IconButtonDefaults.largeContainerSize(
    IconButtonDefaults.IconButtonWidthOption.Narrow
  ),
  val mainControlsIconSize: Dp = IconButtonDefaults.largeIconSize,
  val mainControlsButtonSpacing: Dp = 8.dp,
  val mainControlsButtonTooltipSpacing: Dp = 8.dp,
)

// phone
val CompactPortraitDimens = TackDimens()

// phone landscape
val CompactLandscapeDimens = TackDimens()

// tablet portrait
val MediumPortraitDimens = TackDimens(
  mainControlsPaddingBottom = 56.dp
)

// tablet landscape
val ExpandedLandscapeDimens = TackDimens()

val LocalDimens = compositionLocalOf { CompactPortraitDimens }

@Composable
fun rememberTackDimens(windowSizeClass: WindowSizeClass): TackDimens {
  return remember(windowSizeClass) {
    when {
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> {
        CompactLandscapeDimens
      }
      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
        ExpandedLandscapeDimens
      }
      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
        MediumPortraitDimens
      }
      else -> {
        CompactPortraitDimens
      }
    }
  }
}
