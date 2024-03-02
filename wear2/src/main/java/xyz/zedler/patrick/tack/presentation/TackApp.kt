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

package xyz.zedler.patrick.tack.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import xyz.zedler.patrick.tack.presentation.navigation.Screen
import xyz.zedler.patrick.tack.presentation.screen.GainScreen
import xyz.zedler.patrick.tack.presentation.screen.LatencyScreen
import xyz.zedler.patrick.tack.presentation.screen.MainScreen
import xyz.zedler.patrick.tack.presentation.screen.SettingsScreen
import xyz.zedler.patrick.tack.presentation.screen.SoundScreen
import xyz.zedler.patrick.tack.presentation.screen.TempoScreen
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun TackApp(viewModel: MainViewModel) {
  val navController = rememberSwipeDismissableNavController()
  SwipeDismissableNavHost(
    modifier = Modifier,
    navController = navController,
    startDestination = Screen.Main.route
  ) {
    composable(route = Screen.Main.route) {
      MainScreen(
        viewModel = viewModel,
        onSettingsButtonClick = {
          navController.navigate(Screen.Settings.route)
        },
        onTempoCardClick = {
          navController.navigate(Screen.Tempo.route)
        }
      )
    }
    composable(route = Screen.Settings.route) {
      SettingsScreen(
        viewModel = viewModel,
        onSoundClick = {
          navController.navigate(Screen.Sound.route)
        },
        onGainClick = {
          navController.navigate(Screen.Gain.route)
        },
        onLatencyClick = {
          navController.navigate(Screen.Latency.route)
        }
      )
    }
    composable(route = Screen.Tempo.route) {
      TempoScreen(viewModel = viewModel)
    }
    composable(route = Screen.Gain.route) {
      GainScreen(viewModel = viewModel)
    }
    composable(route = Screen.Sound.route) {
      SoundScreen(viewModel = viewModel)
    }
    composable(route = Screen.Latency.route) {
      LatencyScreen(viewModel = viewModel)
    }
  }
}