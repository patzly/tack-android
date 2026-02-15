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

package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.TempoTap
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
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(color = MaterialTheme.colorScheme.background)
      .padding(32.dp)
  ) {
    TempoTap(
      isTouched = isPressed,
      modifier = modifier.fillMaxSize()
    )
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            onClick()
            waitForUpOrCancellation()
          }
        }
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = {}
        )
    ) {
      val typefaceMedium = remember {
        FontFamily(Font(R.font.google_sans_flex_medium))
      }
      Text(
        text = buildAnnotatedString {
          withStyle(style = SpanStyle(fontFeatureSettings = "tnum")) {
            append(state.tempo.toString())
          }
        },
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = if (isSmallScreen()) 30.sp else 38.sp,
        ),
        fontFamily = typefaceMedium
      )
    }
  }
}