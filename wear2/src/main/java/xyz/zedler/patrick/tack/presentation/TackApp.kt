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
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.navigation.Screen
import xyz.zedler.patrick.tack.presentation.screen.MainScreen
import xyz.zedler.patrick.tack.presentation.screen.SettingsScreen
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.TempoTapUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun TackApp(
  metronomeUtil: MetronomeUtil,
  tempoTapUtil: TempoTapUtil,
  viewModel: MainViewModel,
  navController: NavHostController
) {
  SwipeDismissableNavHost(
    modifier = Modifier,
    navController = navController,
    startDestination = Screen.Main.route
  ) {
    composable(
      route = Screen.Main.route
    ) {
      Main(
        metronomeUtil = metronomeUtil,
        tempoTapUtil = tempoTapUtil,
        viewModel = viewModel,
        navController = navController
      )
    }
    composable(
      route = Screen.Settings.route
    ) {
      Settings(
        viewModel = viewModel
      )
    }
  }
}

@Composable
fun Main(
  metronomeUtil: MetronomeUtil,
  tempoTapUtil: TempoTapUtil,
  viewModel: MainViewModel,
  navController: NavHostController
) {
  MainScreen(
    viewModel = viewModel,
    onTempoCardSwipe = {
      metronomeUtil.tempo = it
    },
    onPlayButtonClick = {
      if (metronomeUtil.isPlaying) {
        metronomeUtil.stop()
      } else {
        metronomeUtil.start()
      }
    },
    onSettingsButtonClick = {
      navController.navigate(Screen.Settings.route)
    },
    onTempoTapButtonClick = {
      if (tempoTapUtil.tap()) {
        metronomeUtil.tempo = tempoTapUtil.tempo
        viewModel.onTempoChange(tempoTapUtil.tempo)
      }
    },
    onBeatModeButtonClick = {
      metronomeUtil.isBeatModeVibrate = !metronomeUtil.isBeatModeVibrate
      viewModel.onBeatModeVibrateChange(metronomeUtil.isBeatModeVibrate)
    }
  )
}

@Composable
fun Settings(
  viewModel: MainViewModel
) {
  SettingsScreen(
    //viewModel = viewModel
  )
}