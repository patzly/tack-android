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

package xyz.zedler.patrick.tack.ui.component.main

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.ui.component.core.AnimatedIcon
import xyz.zedler.patrick.tack.ui.component.core.VerticalButtonGroup
import xyz.zedler.patrick.tack.ui.theme.LocalDimens

sealed interface TempoSkipperPosition {
  object Start : TempoSkipperPosition
  object End : TempoSkipperPosition
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoSkipper(
  settings: AppSettings,
  position: TempoSkipperPosition,
  modifier: Modifier,
  onTempoChangeDelta: (Int) -> Unit,
) {
  val dimens = LocalDimens.current
  val layoutDirection = LocalLayoutDirection.current

  val isRtl = layoutDirection == LayoutDirection.Rtl
  val isIncrease = if (isRtl) {
    position == TempoSkipperPosition.Start
  } else {
    position == TempoSkipperPosition.End
  }

  val tooltipPositioning = if (position == TempoSkipperPosition.Start) {
    TooltipAnchorPosition.End
  } else {
    TooltipAnchorPosition.Start
  }
  val stringRes = if (isIncrease) {
    R.string.options_incremental_amount_increase
  } else {
    R.string.options_incremental_amount_decrease
  }

  val interactionSources = remember { List(3) { MutableInteractionSource() } }

  VerticalButtonGroup(
    overflowIndicator = {},
    verticalArrangement = Arrangement.spacedBy(dimens.tempoSkipperButtonSpacing),
    modifier = modifier
  ) {
    customItem(
      buttonGroupContent = {
        var topIconTrigger by remember { mutableStateOf(false) }

        FilledTonalIconButton(
          onClick = {
            onTempoChangeDelta(if (isIncrease) 1 else -1)
            topIconTrigger = !topIconTrigger
          },
          shapes = IconButtonDefaults.shapes(),
          interactionSource = interactionSources[0],
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(dimens.tempoSkipperButtonSize)
            .animateHeight(interactionSources[0])
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              positioning = tooltipPositioning,
              spacingBetweenTooltipAndAnchor = dimens.tempoSkipperButtonTooltipSpacing
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(stringRes, 1))
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize()
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              AnimatedIcon(
                resId = if (position == TempoSkipperPosition.Start) {
                  R.drawable.ic_rounded_navigate_before_anim
                } else {
                  R.drawable.ic_rounded_navigate_after_anim
                },
                trigger = topIconTrigger,
                animated = !settings.reduceAnim,
                description = stringResource(stringRes, 1),
                modifier = Modifier.size(dimens.tempoSkipperIconSize)
              )
            }
          }
        }
      },
      menuContent = {}
    )

    customItem(
      buttonGroupContent = {
        var centerIconTrigger by remember { mutableStateOf(false) }

        FilledTonalIconButton(
          onClick = {
            onTempoChangeDelta(if (isIncrease) 5 else -5)
            centerIconTrigger = !centerIconTrigger
          },
          shapes = IconButtonDefaults.shapes(),
          interactionSource = interactionSources[1],
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(dimens.tempoSkipperButtonSize)
            .animateHeight(interactionSources[1])
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              positioning = tooltipPositioning,
              spacingBetweenTooltipAndAnchor = dimens.tempoSkipperButtonTooltipSpacing
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(stringRes, 5))
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize()
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              AnimatedIcon(
                resId = if (position == TempoSkipperPosition.Start) {
                  R.drawable.ic_rounded_keyboard_double_arrow_left_anim
                } else {
                  R.drawable.ic_rounded_keyboard_double_arrow_right_anim
                },
                trigger = centerIconTrigger,
                animated = !settings.reduceAnim,
                description = stringResource(stringRes, 5),
                modifier = Modifier.size(dimens.tempoSkipperIconSize)
              )
            }
          }
        }
      },
      menuContent = {}
    )

    customItem(
      buttonGroupContent = {
        var bottomIconTrigger by remember { mutableStateOf(false) }

        FilledTonalIconButton(
          onClick = {
            onTempoChangeDelta(if (isIncrease) 10 else -10)
            bottomIconTrigger = !bottomIconTrigger
          },
          shapes = IconButtonDefaults.shapes(),
          interactionSource = interactionSources[2],
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(dimens.tempoSkipperButtonSize)
            .animateHeight(interactionSources[2])
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              positioning = tooltipPositioning,
              spacingBetweenTooltipAndAnchor = dimens.tempoSkipperButtonTooltipSpacing
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(stringRes, 10))
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize()
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              AnimatedIcon(
                resId = if (position == TempoSkipperPosition.Start) {
                  R.drawable.ic_rounded_triple_arrow_left_anim
                } else {
                  R.drawable.ic_rounded_triple_arrow_right_anim
                },
                trigger = bottomIconTrigger,
                animated = !settings.reduceAnim,
                description = stringResource(stringRes, 10),
                modifier = Modifier.size(dimens.tempoSkipperIconSize)
              )
            }
          }
        }
      },
      menuContent = {}
    )
  }
}