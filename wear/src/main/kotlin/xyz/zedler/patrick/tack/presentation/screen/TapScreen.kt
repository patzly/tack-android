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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ripple
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.presentation.state.MainState
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun TapScreen(
  viewModel: MainViewModel = MainViewModel()
) {
  TackTheme {
    val state by viewModel.state.collectAsState()
    TapBox(
      state = state,
      onClick = {
        viewModel.tempoTap()
      }
    )
  }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun TapScreenSmall() {
  TapScreen()
}

@Composable
fun TapBox(
  state: MainState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(color = MaterialTheme.colorScheme.background)
      .padding(32.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
        .fillMaxSize()
        .border(
          width = 2.dp,
          color = MaterialTheme.colorScheme.tertiary,
          shape = CircleShape
        )
        .background(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = CircleShape
        )
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              if (event.type == PointerEventType.Press) {
                onClick()
              }
            }
          }
        }
        .clip(CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          onClick = {},
          indication = ripple(
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        )
    ) {
      Text(
        text = state.tempo.toString(),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.displayLarge.copy(
          fontSize = if (isSmallScreen()) 30.sp else 40.sp
        )
      )
    }
  }
}