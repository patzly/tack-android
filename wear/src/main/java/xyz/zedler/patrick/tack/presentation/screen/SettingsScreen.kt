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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.Sound
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun SettingsScreen(
  viewModel: MainViewModel = MainViewModel(),
  onSoundClick: () -> Unit = {},
  onGainClick: () -> Unit = {},
  onLatencyClick: () -> Unit = {},
  onRateClick: () -> Unit = {}
) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    ScreenScaffold(
      timeText = {
        TimeText(
          timeTextStyle = MaterialTheme.typography.labelMedium,
          modifier = Modifier.scrollAway(scrollableState)
        )
      },
      scrollIndicator = {
        ScrollIndicator(
          state = scrollableState
        )
      }
    ) {
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
          .rotaryScrollable(
            RotaryScrollableDefaults.behavior(scrollableState = scrollableState),
            focusRequester = rememberActiveFocusRequester()
          )
      ) {
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_settings),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          val sound by viewModel.sound.observeAsState(Constants.Def.SOUND)
          var name = stringResource(id = R.string.wear_settings_sound_sine)
          when (sound) {
            Sound.WOOD -> {
              name = stringResource(id = R.string.wear_settings_sound_wood)
            }
            Sound.MECHANICAL -> {
              name = stringResource(id = R.string.wear_settings_sound_mechanical)
            }
            Sound.BEATBOXING_1 -> {
              name = stringResource(id = R.string.wear_settings_sound_beatboxing_1)
            }
            Sound.BEATBOXING_2 -> {
              name = stringResource(id = R.string.wear_settings_sound_beatboxing_2)
            }
            Sound.HANDS -> {
              name = stringResource(id = R.string.wear_settings_sound_hands)
            }
            Sound.FOLDING -> {
              name = stringResource(id = R.string.wear_settings_sound_folding)
            }
          }
          ClickChip(
            label = stringResource(R.string.wear_settings_sound),
            secondaryLabel = name,
            onClick = onSoundClick
          )
        }
        item {
          val gain by viewModel.gain.observeAsState(Constants.Def.GAIN)
          ClickChip(
            label = stringResource(R.string.wear_settings_gain),
            secondaryLabel = stringResource(R.string.wear_label_db, gain),
            onClick = onGainClick
          )
        }
        item {
          val latency by viewModel.latency.observeAsState(Constants.Def.LATENCY)
          ClickChip(
            label = stringResource(R.string.wear_settings_latency),
            secondaryLabel = stringResource(R.string.wear_label_ms, latency),
            onClick = onLatencyClick
          )
        }
        item {
          val ignoreFocus by viewModel.ignoreFocus.observeAsState(Constants.Def.IGNORE_FOCUS)
          ToggleChip(
            checked = ignoreFocus,
            onCheckedChange = {
              viewModel.changeIgnoreFocus(it)
            },
            label = stringResource(id = R.string.wear_settings_ignore_focus),
            secondaryLabel = stringResource(id = R.string.wear_settings_ignore_focus_description)
          )
        }
        item {
          val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.Def.ALWAYS_VIBRATE)
          ToggleChip(
            checked = alwaysVibrate,
            onCheckedChange = {
              viewModel.changeAlwaysVibrate(it)
            },
            label = stringResource(id = R.string.wear_settings_always_vibrate),
            secondaryLabel = stringResource(id = R.string.wear_settings_always_vibrate_description)
          )
        }
        item {
          val strongVibration by viewModel.strongVibration.observeAsState(
            Constants.Def.STRONG_VIBRATION
          )
          ToggleChip(
            checked = strongVibration,
            onCheckedChange = {
              viewModel.changeStrongVibration(it)
            },
            label = stringResource(id = R.string.wear_settings_strong_vibration),
            secondaryLabel = stringResource(id = R.string.wear_settings_strong_vibration_description)
          )
        }
        item {
          val flashScreen by viewModel.flashScreen.observeAsState(Constants.Def.FLASH_SCREEN)
          ToggleChip(
            checked = flashScreen,
            onCheckedChange = {
              viewModel.changeFlashScreen(it)
            },
            label = stringResource(id = R.string.wear_settings_flash_screen),
            secondaryLabel = stringResource(id = R.string.wear_settings_flash_screen_description)
          )
        }
        item {
          val keepAwake by viewModel.keepAwake.observeAsState(Constants.Def.KEEP_AWAKE)
          ToggleChip(
            checked = keepAwake,
            onCheckedChange = {
              viewModel.changeKeepAwake(it)
            },
            label = stringResource(id = R.string.wear_settings_keep_awake),
            secondaryLabel = stringResource(id = R.string.wear_settings_keep_awake_description)
          )
        }
        item {
          val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
          ToggleChip(
            checked = reduceAnim,
            onCheckedChange = {
              viewModel.changeReduceAnim(it)
            },
            label = stringResource(id = R.string.wear_settings_reduce_animations),
            secondaryLabel = stringResource(
              id = R.string.wear_settings_reduce_animations_description
            )
          )
        }
        item {
          ClickChip(
            label = stringResource(R.string.wear_settings_rate),
            secondaryLabel = stringResource(R.string.wear_settings_rate_description),
            onClick = onRateClick
          )
        }
        item {
          Column(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
          ) {
            Text(
              text = stringResource(id = R.string.wear_label_developer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
              text = stringResource(id = R.string.wear_app_developer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterHorizontally)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ClickChip(
  label: String,
  secondaryLabel: String,
  onClick: () -> Unit
) {
  Chip(
    onClick = onClick,
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    secondaryLabel = {
      Text(
        text = secondaryLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    },
    colors = ChipDefaults.secondaryChipColors(
      backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    modifier = Modifier.fillMaxWidth()
  )
}

@Composable
fun ToggleChip(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  label: String,
  secondaryLabel: String
) {
  SwitchButton (
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = Modifier.fillMaxWidth(),
    enabled = true,
    colors = SwitchButtonDefaults.switchButtonColors(
      uncheckedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    interactionSource = null,
    icon = null,
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    secondaryLabel = {
      Text(
        text = secondaryLabel,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis
      )
    }
  )
}