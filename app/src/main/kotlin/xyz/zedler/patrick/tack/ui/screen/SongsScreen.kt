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

package xyz.zedler.patrick.tack.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SongsScreen(
  widthSizeClass: WindowWidthSizeClass
) {
  val isLandscape = widthSizeClass != WindowWidthSizeClass.Compact

  if (isLandscape) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Songs List (Landscape Left)")
      }
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Current Song Details (Landscape Right)")
      }
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text("Songs List (Portrait Full)")
      }
      Box(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Current Song Bar (Portrait Bottom)")
      }
    }
  }
}
