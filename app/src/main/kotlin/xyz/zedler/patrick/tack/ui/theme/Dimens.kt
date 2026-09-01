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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

open class TackDimens(
  typography: Typography = Typography(),

  val tempoPickerSize: Dp = 184.dp,
  val tempoPickerBpmTextStyle: TextStyle = typography.displayLarge,
  val tempoPickerLabelTextStyle: TextStyle = typography.bodyLargeEmphasized,

  val bottomControlsPaddingBottom: Dp = 16.dp,
  val bottomControlsCenterButtonSize: DpSize = IconButtonDefaults.largeContainerSize(
    IconButtonDefaults.IconButtonWidthOption.Wide
  ),
  val bottomControlsSideButtonSize: DpSize = IconButtonDefaults.largeContainerSize(
    IconButtonDefaults.IconButtonWidthOption.Narrow
  ),
  val bottomControlsIconSize: Dp = IconButtonDefaults.largeIconSize,
  val bottomControlsButtonSpacing: Dp = 8.dp,
  val bottomControlsButtonTooltipSpacing: Dp = 8.dp,
)

// phone
class CompactPortraitDimens(
  typography: Typography
) : TackDimens(typography = typography)

// phone landscape
class CompactLandscapeDimens(
  typography: Typography
) : TackDimens(
  typography = typography,

  tempoPickerSize = 136.dp
)

// tablet portrait
class MediumPortraitDimens(
  typography: Typography
) : TackDimens(
  typography = typography,

  tempoPickerSize = 304.dp,

  bottomControlsPaddingBottom = 56.dp
)

// tablet landscape
class ExpandedLandscapeDimens(
  typography: Typography
) : TackDimens(
  typography = typography,

  tempoPickerSize = 184.dp
)

val LocalDimens = compositionLocalOf { TackDimens() }

@Composable
fun TextUnit.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

@Composable
fun rememberTackDimens(
  windowSizeClass: WindowSizeClass,
  typography: Typography = MaterialTheme.typography
): TackDimens {
  return remember(windowSizeClass, typography) {
    when {
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> {
        CompactLandscapeDimens(typography)
      }
      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
        ExpandedLandscapeDimens(typography)
      }
      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
        MediumPortraitDimens(typography)
      }
      else -> {
        CompactPortraitDimens(typography)
      }
    }
  }
}
