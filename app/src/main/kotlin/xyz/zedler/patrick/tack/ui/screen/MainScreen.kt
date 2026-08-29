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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.ui.component.main.MainControls
import xyz.zedler.patrick.tack.ui.dialog.FeedbackDialog
import xyz.zedler.patrick.tack.ui.dialog.HelpDialog
import xyz.zedler.patrick.tack.ui.dialog.OptionsDialog
import xyz.zedler.patrick.tack.ui.dialog.UnlockDialog
import xyz.zedler.patrick.tack.ui.navigation.Route
import xyz.zedler.patrick.tack.ui.theme.LocalDimens
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.theme.rememberTackDimens
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
  viewModel: MainViewModel,
  windowSizeClass: WindowSizeClass
) {
  val haptic = LocalHaptic.current

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()

  var showUnlockDialog by remember { mutableStateOf(false) }
  var showHelpDialog by remember { mutableStateOf(false) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var showOptionsDialog by remember { mutableStateOf(false) }

  if (showUnlockDialog) {
    UnlockDialog(onDismissRequest = { showUnlockDialog = false })
  }

  if (showHelpDialog) {
    HelpDialog(onDismissRequest = { showHelpDialog = false })
  }

  if (showFeedbackDialog) {
    FeedbackDialog(
      checkUnlockKey = settings.checkUnlockKey,
      isKeyInstalled = unlockState.isKeyInstalled,
      isPlayStoreInstalled = unlockState.isPlayStoreInstalled,
      onDismissRequest = { showFeedbackDialog = false },
      onSupport = { showUnlockDialog = true }
    )
  }

  if (showOptionsDialog) {
    OptionsDialog(onDismissRequest = { showOptionsDialog = false })
  }

  MainContent(
    windowSizeClass = windowSizeClass,
    // app bar menu
    onSupportClick = {
      showUnlockDialog = true
    },
    onMoreClick = {
      haptic.click()
    },
    onSettingsClick = {
      haptic.click()
      viewModel.navigateTo(Route.Settings)
    },
    onAboutClick = {
      haptic.click()
      viewModel.navigateTo(Route.About)
    },
    onHelpClick = {
      haptic.click()
      showHelpDialog = true
    },
    onFeedbackClick = {
      haptic.click()
      showFeedbackDialog = true
    },
    // main controls
    onOptionsClick = {
      haptic.click()
      showOptionsDialog = true
    },
    onPlayStopChange = {
      haptic.click()
      viewModel.togglePlay()
    },
    onBeatModeClick = {
      haptic.click()
      // TODO
    }
  )
}

@OptIn(
  ExperimentalMaterial3WindowSizeClassApi::class,
  ExperimentalMaterial3Api::class
)
@Composable
fun MainContent(
  settings: AppSettings = AppSettings(),
  metronomeState: MetronomeState = MetronomeState(),
  windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(
    DpSize(412.dp, 924.dp)
  ),
  // app bar menu
  onSupportClick: () -> Unit = {},
  onMoreClick: () -> Unit = {},
  onSettingsClick: () -> Unit = {},
  onAboutClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  onFeedbackClick: () -> Unit = {},
  // main controls
  onOptionsClick: () -> Unit = {},
  onPlayStopChange: (Boolean) -> Unit = {},
  onBeatModeClick: () -> Unit = {}
) {
  val dimens = rememberTackDimens(windowSizeClass)

  val layoutStrategy = remember(windowSizeClass) {
    when {
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> {
        MainLayoutStrategy.CompactLandscape
      }

      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
        MainLayoutStrategy.ExpandedLandscape
      }

      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
        MainLayoutStrategy.MediumPortrait
      }

      else -> {
        MainLayoutStrategy.CompactPortrait
      }
    }
  }

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            stringResource(R.string.app_name),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              TooltipAnchorPosition.Below
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(R.string.app_name))
              }
            },
            state = rememberTooltipState(),
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_rounded_star),
              contentDescription = stringResource(R.string.app_name),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          var showMenu by remember { mutableStateOf(false) }

          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              TooltipAnchorPosition.Below
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(R.string.action_more))
              }
            },
            state = rememberTooltipState(),
          ) {
            FilledIconButton(
              onClick = {
                onMoreClick()
                showMenu = true
              },
              modifier =
                Modifier
                  .minimumInteractiveComponentSize()
                  .size(
                    IconButtonDefaults.smallContainerSize(
                      IconButtonDefaults.IconButtonWidthOption.Narrow
                    )
                  ),
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
              ),
              shapes = IconButtonDefaults.shapes()
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_more_vert),
                contentDescription = stringResource(R.string.action_more)
              )
            }
          }

          DropdownMenuPopup(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
              MenuAnchorPosition.Below,
              offset = DpOffset(x = (-8).dp, 0.dp)
            )
          ) {
            DropdownMenuGroup(
              shapes = MenuDefaults.groupShape(0, 1),
            ) {
              val itemCount = 4

              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_settings)) },
                onClick = {
                  onSettingsClick()
                  showMenu = false
                },
                shape = MenuDefaults.itemShape(0, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_about)) },
                onClick = {
                  onAboutClick()
                  showMenu = false
                },
                shape = MenuDefaults.itemShape(1, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_help)) },
                onClick = {
                  onHelpClick()
                  showMenu = false
                },
                shape = MenuDefaults.itemShape(2, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_feedback)) },
                onClick = {
                  onFeedbackClick()
                  showMenu = false
                },
                shape = MenuDefaults.itemShape(3, itemCount).shape
              )
            }
          }
        },
        scrollBehavior = scrollBehavior
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(padding)
        .padding(padding),
    ) {
      CompositionLocalProvider(LocalDimens provides dimens) {
        when (layoutStrategy) {
          MainLayoutStrategy.CompactPortrait -> {
            CompactPortraitContent(
              settings = settings,
              metronomeState = metronomeState,
              onOptionsClick = onOptionsClick,
              onPlayStopChange = onPlayStopChange,
              onBeatModeClick = onBeatModeClick
            )
          }

          MainLayoutStrategy.CompactLandscape -> {
            CompactLandscapeContent()
          }

          MainLayoutStrategy.MediumPortrait -> {
            MediumPortraitContent()
          }

          MainLayoutStrategy.ExpandedLandscape -> {
            ExpandedLandscapeContent()
          }
        }
      }
    }
  }
}

@Composable
private fun CompactPortraitContent(
  settings: AppSettings,
  metronomeState: MetronomeState,
  // main controls
  onOptionsClick: () -> Unit,
  onPlayStopChange: (Boolean) -> Unit,
  onBeatModeClick: () -> Unit,
) {
  ConstraintLayout(modifier = Modifier.fillMaxSize()) {
    val (mainControls) = createRefs()

    MainControls(
      settings = settings,
      metronomeState = metronomeState,
      onOptionsClick = onOptionsClick,
      onPlayStopChange = onPlayStopChange,
      onBeatModeClick = onBeatModeClick,
      modifier = Modifier.constrainAs(mainControls) {
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
      }
    )
  }
}

@Composable
private fun CompactLandscapeContent() {
  // TODO
}

@Composable
private fun MediumPortraitContent() {
  // TODO
}

@Composable
private fun ExpandedLandscapeContent() {
  // TODO
}

private enum class MainLayoutStrategy {
  CompactPortrait,
  CompactLandscape,
  MediumPortrait,
  ExpandedLandscape
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  TackTheme {
    MainContent()
  }
}