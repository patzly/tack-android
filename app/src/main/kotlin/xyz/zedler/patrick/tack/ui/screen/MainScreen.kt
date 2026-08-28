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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.dialog.OptionsContent
import xyz.zedler.patrick.tack.ui.dialog.OptionsDialog
import xyz.zedler.patrick.tack.ui.navigation.Route
import xyz.zedler.patrick.tack.ui.theme.LocalTackDimens
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.theme.rememberTackDimens
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
  viewModel: MainViewModel,
  windowSizeClass: WindowSizeClass
) {
  var showOptionsDialog by remember { mutableStateOf(false) }

  if (showOptionsDialog) {
    OptionsDialog(
      onDismissRequest = { showOptionsDialog = false }
    )
  }

  MainContent(
    windowSizeClass = windowSizeClass,
    onSettingsClick = {
      viewModel.navigateTo(Route.Settings)
    },
    onOptionsClick = {
      showOptionsDialog = true
      viewModel.navigateTo(Route.Settings)
    }
  )
}

private enum class MainLayoutStrategy {
  CompactPortrait,
  CompactLandscape,
  MediumPortrait,
  DualPaneLandscape
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainContent(
  windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(
    DpSize(412.dp, 924.dp)
  ),
  onSettingsClick: () -> Unit = {},
  onOptionsClick: () -> Unit = {}
) {
  val dimens = rememberTackDimens(windowSizeClass)

  val layoutStrategy = remember(windowSizeClass) {
    when {
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> {
        MainLayoutStrategy.CompactLandscape
      }

      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
        MainLayoutStrategy.DualPaneLandscape
      }

      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
        MainLayoutStrategy.MediumPortrait
      }

      else -> {
        MainLayoutStrategy.CompactPortrait
      }
    }
  }

  CompositionLocalProvider(LocalTackDimens provides dimens) {
    when (layoutStrategy) {
      MainLayoutStrategy.CompactPortrait -> {
        CompactPortraitLayout(
          onSettingsClick = onSettingsClick,
          onOptionsClick = onOptionsClick
        )
      }

      MainLayoutStrategy.CompactLandscape -> {
        CompactLandscapeLayout(onOptionsClick = onOptionsClick)
      }

      MainLayoutStrategy.MediumPortrait -> {
        MediumPortraitLayout(onOptionsClick = onOptionsClick)
      }

      MainLayoutStrategy.DualPaneLandscape -> {
        DualPaneLandscapeLayout()
      }
    }
  }
}

@Composable
private fun CompactPortraitLayout(
  onSettingsClick: () -> Unit,
  onOptionsClick: () -> Unit
) {
  val dimens = LocalTackDimens.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(dimens.paddingContent),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text("Compact Portrait", style = MaterialTheme.typography.titleMedium)
    MetronomeCoreUI(size = dimens.dialSize)
    Button(
      onClick = onOptionsClick,
      modifier = Modifier.height(dimens.controlButtonSize)
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_rounded_upload),
        contentDescription = "Options"
      )
      Spacer(Modifier.width(8.dp))
      Text("Options")
    }
  }
}

@Composable
private fun CompactLandscapeLayout(onOptionsClick: () -> Unit) {
  val dimens = LocalTackDimens.current
  Row(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(dimens.paddingContent),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text("Compact Landscape", style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(dimens.spacingLarge))
      Button(
        onClick = onOptionsClick,
        modifier = Modifier.height(dimens.controlButtonSize)
      ) {
        Text("Options")
      }
    }
    MetronomeCoreUI(size = dimens.dialSize)
  }
}

@Composable
private fun MediumPortraitLayout(onOptionsClick: () -> Unit) {
  val dimens = LocalTackDimens.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(dimens.paddingContent),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceEvenly
  ) {
    Text("Medium Portrait (Scaled)", style = MaterialTheme.typography.headlineMedium)
    MetronomeCoreUI(size = dimens.dialSize)
    Button(
      onClick = onOptionsClick,
      modifier = Modifier.height(dimens.controlButtonSize)
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_rounded_upload),
        contentDescription = "Options"
      )
      Spacer(Modifier.width(8.dp))
      Text("Options", style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun DualPaneLandscapeLayout() {
  val dimens = LocalTackDimens.current
  Row(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Surface(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
      color = MaterialTheme.colorScheme.surfaceContainer
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(dimens.paddingContent)
      ) {
        OptionsContent()
      }
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(dimens.paddingContent),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("Metronome", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(dimens.spacingLarge))
        MetronomeCoreUI(size = dimens.dialSize)
      }
    }
  }
}

@Composable
fun MetronomeCoreUI(size: androidx.compose.ui.unit.Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .background(
        MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge
      ),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "Dial\n${size.value.toInt()}dp",
      color = MaterialTheme.colorScheme.onPrimaryContainer
    )
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  TackTheme {
    MainContent()
  }
}
