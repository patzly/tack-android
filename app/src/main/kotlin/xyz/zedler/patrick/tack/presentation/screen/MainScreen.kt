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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.navigation.Route
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun MainScreen(
  viewModel: MainViewModel = viewModel(),
  widthSizeClass: WindowWidthSizeClass
) {
  val isLandscape = widthSizeClass != WindowWidthSizeClass.Compact

  Box(modifier = Modifier.fillMaxSize()) {
    if (isLandscape) {
      Row(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.Center
        ) {
          Text("Metronome Options (Landscape Left)")
        }
        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.Center
        ) {
          Text("Metronome (Landscape Right)")
        }
      }
    } else {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text("Metronome (Portrait - Options as Dialog)")
      }
    }

    IconButton(
      onClick = {
        viewModel.navigateTo(Route.Settings)
      },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
        .padding(top = 32.dp) // Offset for edge-to-edge
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_rounded_more_vert),
        contentDescription = "Settings"
      )
    }
  }
}
