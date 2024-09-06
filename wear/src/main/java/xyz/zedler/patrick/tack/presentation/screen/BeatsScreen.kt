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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.BeatIconButton
import xyz.zedler.patrick.tack.presentation.components.FadingEdgeRow
import xyz.zedler.patrick.tack.presentation.components.TextIconButton
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun BeatsScreen(viewModel: MainViewModel = MainViewModel()) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    ScreenScaffold (
      scrollState = scrollableState,
      modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
    ) {
      val beats by viewModel.beats.observeAsState(Constants.Def.BEATS.split(","))
      val subdivisions by viewModel.subdivisions.observeAsState(
        Constants.Def.SUBDIVISIONS.split(",")
      )
      val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
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
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_beats_count, beats.size),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          ControlCard(
            viewModel = viewModel,
            labelAdd = stringResource(id = R.string.wear_action_add_beat),
            labelRemove = stringResource(id = R.string.wear_action_remove_beat),
            onClickAdd = {
              viewModel.addBeat()
            },
            onClickRemove = {
              viewModel.removeBeat()
            },
            addEnabled = beats.size < Constants.BEATS_MAX,
            removeEnabled = beats.size > 1
          ) {
            beats.forEachIndexed { index, beat ->
              val triggerIndex = if (index < viewModel.beatTriggers.size) {
                index
              } else {
                viewModel.beatTriggers.size - 1
              }
              val trigger by viewModel.beatTriggers[triggerIndex].observeAsState(false)
              BeatIconButton(
                tickType = beat,
                index = index,
                animTrigger = trigger,
                reduceAnim = reduceAnim,
                onClick = {
                  val next = when (beat) {
                    Constants.TickType.NORMAL -> Constants.TickType.STRONG
                    Constants.TickType.STRONG -> Constants.TickType.MUTED
                    else -> Constants.TickType.NORMAL
                  }
                  viewModel.changeBeat(index, next)
                }
              )
            }
          }
        }
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_subdivisions_count, subdivisions.size),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          ControlCard(
            viewModel = viewModel,
            labelAdd = stringResource(id = R.string.wear_action_add_sub),
            labelRemove = stringResource(id = R.string.wear_action_remove_sub),
            onClickAdd = {
              viewModel.addSubdivision()
            },
            onClickRemove = {
              viewModel.removeSubdivision()
            },
            addEnabled = subdivisions.size < Constants.SUBS_MAX,
            removeEnabled = subdivisions.size > 1
          ) {
            subdivisions.forEachIndexed { index, subdivision ->
              val triggerIndex = if (index < viewModel.subdivisionTriggers.size) {
                index
              } else {
                viewModel.subdivisionTriggers.size - 1
              }
              val trigger by viewModel.subdivisionTriggers[triggerIndex].observeAsState(false)
              BeatIconButton(
                tickType = subdivision,
                index = index,
                enabled = index != 0,
                animTrigger = trigger,
                reduceAnim = reduceAnim,
                onClick = {
                  val next = when (subdivision) {
                    Constants.TickType.NORMAL -> Constants.TickType.MUTED
                    Constants.TickType.SUB -> Constants.TickType.NORMAL
                    else -> Constants.TickType.SUB
                  }
                  viewModel.changeSubdivision(index, next)
                }
              )
            }
          }
        }
        item {
          Row (
            modifier = Modifier.padding(top = 8.dp)
          ) {
            TextIconButton(
              label = "3",
              reduceAnim = reduceAnim,
              onClick = {
                viewModel.setSwing(3)
              }
            )
            TextIconButton(
              label = "5",
              reduceAnim = reduceAnim,
              onClick = {
                viewModel.setSwing(5)
              },
              modifier = Modifier.padding(horizontal = 8.dp)
            )
            TextIconButton(
              label = "7",
              reduceAnim = reduceAnim,
              onClick = {
                viewModel.setSwing(7)
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
  viewModel: MainViewModel,
  labelAdd: String,
  labelRemove: String,
  onClickAdd: () -> Unit,
  onClickRemove: () -> Unit,
  addEnabled: Boolean,
  removeEnabled: Boolean,
  content: @Composable RowScope.() -> Unit
) {
  val size = if (isSmallScreen()) 42.dp else IconButtonDefaults.SmallButtonSize
  Card(
    onClick = {},
    enabled = false,
    contentPadding = PaddingValues(0.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    modifier = Modifier
      .height(size)
      .fillMaxWidth()
  ) {
    Row {
      val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
      val animTriggerAdd = remember { mutableStateOf(false) }
      val animTriggerRemove = remember { mutableStateOf(false) }
      IconButton(
        enabled = removeEnabled,
        onClick = {
          animTriggerRemove.value = !animTriggerRemove.value
          onClickRemove()
        },
        modifier = Modifier.size(size)
      ) {
        val targetTint = if (removeEnabled) {
          IconButtonDefaults.filledTonalIconButtonColors().contentColor
        } else {
          IconButtonDefaults.filledTonalIconButtonColors().disabledContentColor
        }
        val tint by animateColorAsState(
          targetValue = targetTint,
          label = "remove",
          animationSpec = TweenSpec(durationMillis = 200)
        )
        AnimatedVectorDrawable(
          resId = R.drawable.ic_rounded_remove_anim,
          description = labelRemove,
          color = tint,
          trigger = animTriggerRemove.value,
          animated = !reduceAnim
        )
      }
      FadingEdgeRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f)
      ) {
        content()
      }
      IconButton(
        enabled = addEnabled,
        onClick = {
          animTriggerAdd.value = !animTriggerAdd.value
          onClickAdd()
        },
        modifier = Modifier.size(size)
      ) {
        val targetTint = if (addEnabled) {
          IconButtonDefaults.filledTonalIconButtonColors().contentColor
        } else {
          IconButtonDefaults.filledTonalIconButtonColors().disabledContentColor
        }
        val tint by animateColorAsState(
          targetValue = targetTint,
          label = "add",
          animationSpec = TweenSpec(durationMillis = 200)
        )
        AnimatedVectorDrawable(
          resId = R.drawable.ic_rounded_add_anim,
          description = labelAdd,
          color = tint,
          trigger = animTriggerAdd.value,
          animated = !reduceAnim
        )
      }
    }
  }
}