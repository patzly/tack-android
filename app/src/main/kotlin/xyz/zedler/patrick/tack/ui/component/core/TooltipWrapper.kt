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

package xyz.zedler.patrick.tack.ui.component.core

import androidx.compose.foundation.MutatePriority
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.ui.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipWrapper(
  text: String,
  modifier: Modifier = Modifier,
  positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
  spacingBetweenTooltipAndAnchor: Dp = 4.dp,
  content: @Composable () -> Unit
) {
  val haptic = LocalHaptic.current
  val baseState = rememberTooltipState()

  val state = remember(baseState, haptic) {
    object : TooltipState by baseState {
      override suspend fun show(mutatePriority: MutatePriority) {
        if (mutatePriority == MutatePriority.PreventUserInput
          || mutatePriority == MutatePriority.Default
        ) {
          haptic.longClick()
        }
        baseState.show(mutatePriority)
      }
    }
  }

  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
      positioning = positioning,
      spacingBetweenTooltipAndAnchor = spacingBetweenTooltipAndAnchor
    ),
    tooltip = {
      PlainTooltip {
        Text(text)
      }
    },
    state = state,
    modifier = modifier,
    content = content
  )
}