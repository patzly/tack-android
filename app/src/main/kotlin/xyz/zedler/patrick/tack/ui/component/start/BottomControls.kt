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

package xyz.zedler.patrick.tack.ui.component.start

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.BeatMode
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.ui.component.core.AnimatedIcon
import xyz.zedler.patrick.tack.ui.theme.LocalDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControls(
  settings: AppSettings,
  metronomeState: MetronomeState,
  onOptionsClick: () -> Unit,
  onPlayStopChange: (Boolean) -> Unit,
  onBeatModeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dimens = LocalDimens.current

  val optionsInteractionSource = remember { MutableInteractionSource() }
  val playStopInteractionSource = remember { MutableInteractionSource() }
  val beatModeInteractionSource = remember { MutableInteractionSource() }

  ButtonGroup(
    overflowIndicator = {},
    horizontalArrangement = Arrangement.spacedBy(dimens.bottomControlsButtonSpacing),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.padding(bottom = dimens.bottomControlsPaddingBottom),
  ) {
    customItem(
      buttonGroupContent = {
        var optionsIconTrigger by remember { mutableStateOf(false) }
        val optionsText = stringResource(R.string.title_options)

        FilledTonalIconButton(
          onClick = {
            onOptionsClick()
            optionsIconTrigger = !optionsIconTrigger
          },
          shapes = IconButtonDefaults.shapes(),
          interactionSource = optionsInteractionSource,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(dimens.bottomControlsSideButtonSize)
            .animateWidth(optionsInteractionSource),
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              positioning = TooltipAnchorPosition.Above
            ),
            tooltip = {
              PlainTooltip {
                Text(optionsText)
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize(),
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              AnimatedIcon(
                resId = R.drawable.ic_rounded_tune_anim,
                trigger = optionsIconTrigger,
                animated = !settings.reduceAnim,
                contentDescription = optionsText,
                modifier = Modifier.size(dimens.bottomControlsIconSize),
              )
            }
          }
        }
      },
      menuContent = {}
    )

    customItem(
      buttonGroupContent = {
        val playStopText = stringResource(R.string.action_play_stop)

        IconToggleButton(
          checked = metronomeState.isPlaying,
          onCheckedChange = onPlayStopChange,
          shapes = IconButtonDefaults.toggleableShapes(),
          colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            checkedContainerColor = MaterialTheme.colorScheme.tertiary,
            checkedContentColor = MaterialTheme.colorScheme.onTertiary,
          ),
          interactionSource = playStopInteractionSource,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(dimens.bottomControlsCenterButtonSize)
            .animateWidth(playStopInteractionSource),
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              positioning = TooltipAnchorPosition.Above
            ),
            tooltip = {
              PlainTooltip {
                Text(playStopText)
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize(),
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              AnimatedIcon(
                resId1 = R.drawable.ic_rounded_play_to_stop_fill_anim,
                resId2 = R.drawable.ic_rounded_stop_to_play_fill_anim,
                trigger = metronomeState.isPlaying,
                animated = !settings.reduceAnim,
                contentDescription = playStopText,
                modifier = Modifier.size(dimens.bottomControlsIconSize),
              )
            }
          }
        }
      },
      menuContent = {}
    )

    customItem(
      buttonGroupContent = {
        val beatModeText = stringResource(R.string.action_beat_mode)

        FilledTonalIconButton(
          onClick = onBeatModeClick,
          shapes = IconButtonDefaults.shapes(),
          interactionSource = beatModeInteractionSource,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(dimens.bottomControlsSideButtonSize)
            .animateWidth(beatModeInteractionSource),
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              positioning = TooltipAnchorPosition.Above
            ),
            tooltip = {
              PlainTooltip {
                Text(beatModeText)
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize(),
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              AnimatedIcon(
                resId1 = R.drawable.ic_rounded_volume_up_to_vibration_anim,
                resId2 = R.drawable.ic_rounded_vibration_to_volume_up_anim,
                trigger = settings.beatMode == BeatMode.VIBRATION,
                animated = !settings.reduceAnim,
                contentDescription = beatModeText,
                modifier = Modifier.size(dimens.bottomControlsIconSize),
              )
            }
          }
        }
      },
      menuContent = {},
    )
  }
}
