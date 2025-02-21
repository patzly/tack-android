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

package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.AnimatedIcon
import xyz.zedler.patrick.tack.presentation.components.BeatIconButton
import xyz.zedler.patrick.tack.presentation.components.BeatsRow
import xyz.zedler.patrick.tack.presentation.components.TextIconButton
import xyz.zedler.patrick.tack.presentation.state.MainState
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun BeatsScreen(
  viewModel: MainViewModel = MainViewModel()
) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    ScreenScaffold (
      scrollState = scrollableState,
      modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
    ) {
      val state by viewModel.state.collectAsState()
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .rotaryScrollable(
            RotaryScrollableDefaults.behavior(scrollableState = scrollableState),
            focusRequester = rememberActiveFocusRequester()
          )
      ) {
        item {
          ListHeader(
            // Necessary padding to prevent cut-off by time text
            contentPadding = PaddingValues(top = 24.dp)
          ) {
            Text(
              text = stringResource(id = R.string.wear_title_beats_count, state.beats.size),
              style = MaterialTheme.typography.titleMedium,
              textAlign = TextAlign.Center
            )
          }
        }
        item {
          ControlCard(
            state = state,
            labelAdd = stringResource(id = R.string.wear_action_add_beat),
            labelRemove = stringResource(id = R.string.wear_action_remove_beat),
            onClickAdd = {
              viewModel.addBeat()
            },
            onClickRemove = {
              viewModel.removeBeat()
            },
            addEnabled = state.beats.size < Constants.BEATS_MAX,
            removeEnabled = state.beats.size > 1,
            animated = !state.reduceAnim
          ) {
            state.beats.forEachIndexed { index, beat ->
              val triggerIndex = if (index < state.beatTriggers.size) {
                index
              } else {
                state.beatTriggers.size - 1
              }
              val trigger = state.beatTriggers[triggerIndex]
              BeatIconButton(
                index = index,
                tickType = beat,
                animTrigger = trigger,
                onClick = {
                  val next = when (beat) {
                    Constants.TickType.NORMAL -> Constants.TickType.STRONG
                    Constants.TickType.STRONG -> Constants.TickType.MUTED
                    else -> Constants.TickType.NORMAL
                  }
                  viewModel.changeBeat(index, next)
                },
                reduceAnim = state.reduceAnim
              )
            }
          }
        }
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_subdivisions_count, state.subdivisions.size),
              style = MaterialTheme.typography.titleMedium,
              textAlign = TextAlign.Center
            )
          }
        }
        item {
          ControlCard(
            state = state,
            labelAdd = stringResource(id = R.string.wear_action_add_sub),
            labelRemove = stringResource(id = R.string.wear_action_remove_sub),
            onClickAdd = {
              viewModel.addSubdivision()
            },
            onClickRemove = {
              viewModel.removeSubdivision()
            },
            addEnabled = state.subdivisions.size < Constants.SUBS_MAX,
            removeEnabled = state.subdivisions.size > 1,
            animated = !state.reduceAnim
          ) {
            state.subdivisions.forEachIndexed { index, subdivision ->
              val triggerIndex = if (index < state.subdivisionTriggers.size) {
                index
              } else {
                state.subdivisionTriggers.size - 1
              }
              val trigger = state.subdivisionTriggers[triggerIndex]
              BeatIconButton(
                index = index,
                tickType = subdivision,
                animTrigger = trigger,
                onClick = {
                  val next = when (subdivision) {
                    Constants.TickType.NORMAL -> Constants.TickType.MUTED
                    Constants.TickType.SUB -> Constants.TickType.NORMAL
                    else -> Constants.TickType.SUB
                  }
                  viewModel.changeSubdivision(index, next)
                },
                reduceAnim = state.reduceAnim,
                enabled = index != 0
              )
            }
          }
        }
        item {
          val interactionSource1 = remember { MutableInteractionSource() }
          val interactionSource2 = remember { MutableInteractionSource() }
          val interactionSource3 = remember { MutableInteractionSource() }
          ButtonGroup(
            spacing = 8.dp,
            modifier = Modifier.padding(top = 8.dp)
          ) {
            TextIconButton(
              interactionSource = interactionSource1,
              modifier = Modifier.animateWidth(interactionSource1),
              label = "3",
              reduceAnim = state.reduceAnim,
              onClick = {
                viewModel.updateSwing(3)
              },
            )
            TextIconButton(
              interactionSource = interactionSource2,
              modifier = Modifier.animateWidth(interactionSource2),
              label = "5",
              reduceAnim = state.reduceAnim,
              onClick = {
                viewModel.updateSwing(5)
              },
            )
            TextIconButton(
              interactionSource = interactionSource3,
              modifier = Modifier.animateWidth(interactionSource3),
              label = "7",
              reduceAnim = state.reduceAnim,
              onClick = {
                viewModel.updateSwing(7)
              }
            )
          }
        }
      }
    }
  }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun BeatsScreenSmall() {
  BeatsScreen()
}

@Composable
fun ControlCard(
  state: MainState,
  labelAdd: String,
  labelRemove: String,
  onClickAdd: () -> Unit,
  onClickRemove: () -> Unit,
  addEnabled: Boolean,
  removeEnabled: Boolean,
  animated: Boolean = true,
  content: @Composable RowScope.() -> Unit
) {
  val size = if (isSmallScreen()) 42.dp else IconButtonDefaults.SmallButtonSize
  Card(
    onClick = {},
    enabled = false,
    contentPadding = PaddingValues(0.dp),
    modifier = Modifier
      .wrapContentHeight()
      .fillMaxWidth()
      .defaultMinSize(minHeight = 40.dp)
  ) {
    Row {
      val animTriggerAdd = remember { mutableStateOf(false) }
      val animTriggerRemove = remember { mutableStateOf(false) }
      IconButton(
        enabled = removeEnabled,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = {
          animTriggerRemove.value = !animTriggerRemove.value
          onClickRemove()
        },
        modifier = Modifier.size(size)
      ) {
        AnimatedIcon(
          resId = R.drawable.ic_rounded_remove_anim,
          description = labelRemove,
          trigger = animTriggerRemove.value,
          animated = !state.reduceAnim
        )
      }
      BeatsRow(
        animated = animated,
        modifier = Modifier.wrapContentHeight().weight(1f)
      ) {
        content()
      }
      IconButton(
        enabled = addEnabled,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = {
          animTriggerAdd.value = !animTriggerAdd.value
          onClickAdd()
        },
        modifier = Modifier.size(size)
      ) {
        AnimatedIcon(
          resId = R.drawable.ic_rounded_add_anim,
          description = labelAdd,
          trigger = animTriggerAdd.value,
          animated = !state.reduceAnim
        )
      }
    }
  }
}