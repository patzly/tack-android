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

package xyz.zedler.patrick.tack.presentation

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import xyz.zedler.patrick.tack.presentation.navigation.Screen
import xyz.zedler.patrick.tack.presentation.screen.BeatsScreen
import xyz.zedler.patrick.tack.presentation.screen.BookmarksScreen
import xyz.zedler.patrick.tack.presentation.screen.GainScreen
import xyz.zedler.patrick.tack.presentation.screen.LatencyScreen
import xyz.zedler.patrick.tack.presentation.screen.MainScreen
import xyz.zedler.patrick.tack.presentation.screen.SettingsScreen
import xyz.zedler.patrick.tack.presentation.screen.SoundScreen
import xyz.zedler.patrick.tack.presentation.screen.TapScreen
import xyz.zedler.patrick.tack.presentation.screen.TempoScreen
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun TackApp(
  viewModel: MainViewModel,
  onPermissionRequestClick: () -> Unit,
  onRateClick: () -> Unit
) {
  val navController = rememberSwipeDismissableNavController()
  val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
    viewModel.updateCurrentRoute(destination.route.toString())
  }
  DisposableEffect(navController) {
    navController.addOnDestinationChangedListener(listener)
    onDispose {
      navController.removeOnDestinationChangedListener(listener)
    }
  }

  TackTheme {
    val state by viewModel.state.collectAsState()
    val mainScreen = state.currentRoute == Screen.Main.route
    val timeAlpha by animateFloatAsState(
      targetValue = if (state.isPlaying && state.keepAwake && mainScreen) .5f else 1f,
      label = "timeAlpha",
      animationSpec = TweenSpec(durationMillis = if (state.reduceAnim) 0 else 300)
    )

    AppScaffold(
      timeText = {
        val style = TimeTextDefaults.timeTextStyle().copy(
          fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = timeAlpha)
        )
        TimeText { time ->
          timeTextCurvedText(time, style)
        }
      }
    ) {
      SwipeDismissableNavHost(
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
            },
            onBeatsButtonClick = {
              navController.navigate(Screen.Beats.route)
            },
            onTempoTapButtonClick = {
              navController.navigate(Screen.Tap.route)
            },
            onBookmarksButtonClick = {
              navController.navigate(Screen.Bookmarks.route)
            },
            onPermissionRequestClick = onPermissionRequestClick
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
            },
            onRateClick = onRateClick
          )
        }
        composable(route = Screen.Tempo.route) {
          TempoScreen(viewModel = viewModel)
        }
        composable(route = Screen.Beats.route) {
          BeatsScreen(viewModel = viewModel)
        }
        composable(route = Screen.Tap.route) {
          TapScreen(viewModel = viewModel)
        }
        composable(route = Screen.Bookmarks.route) {
          BookmarksScreen(viewModel = viewModel)
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
  }
}