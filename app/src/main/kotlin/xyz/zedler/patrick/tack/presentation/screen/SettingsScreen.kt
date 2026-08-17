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

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.*
import xyz.zedler.patrick.tack.presentation.component.*
import xyz.zedler.patrick.tack.presentation.dialog.*
import xyz.zedler.patrick.tack.presentation.navigation.Route
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.LocaleUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel = viewModel()) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val isKeyInstalled by viewModel.isKeyInstalled.collectAsStateWithLifecycle()
  val isPlayStoreInstalled by viewModel.isPlayStoreInstalled.collectAsStateWithLifecycle()

  var showFeedbackDialog by rememberSaveable { mutableStateOf(false) }
  var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

  if (showFeedbackDialog) {
    FeedbackDialog(
      checkUnlockKey = settings.checkUnlockKey,
      isKeyInstalled = isKeyInstalled,
      isPlayStoreInstalled = isPlayStoreInstalled,
      onDismissRequest = { showFeedbackDialog = false },
      onSupportClick = { /* TODO: Show unlock dialog */ }
    )
  }

  if (showLanguageDialog) {
    LanguageDialog(
      currentLanguageCode = settings.language,
      onLanguageSelected = { viewModel.updateSettings(settings.copy(language = it)) },
      onDismissRequest = { showLanguageDialog = false }
    )
  }

  SettingsContent(
    settings = settings,
    onBack = { viewModel.popBackstack() },
    onAboutClick = { viewModel.navigateTo(Route.About) },
    onHelpClick = {},
    onFeedbackClick = { showFeedbackDialog = true },
    onLogcatClick = {},
    onLanguageClick = { showLanguageDialog = true },
    onUpdateSettings = viewModel::updateSettings
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  settings: AppSettings = AppSettings(),
  onBack: () -> Unit = {},
  onAboutClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  onFeedbackClick: () -> Unit = {},
  onLogcatClick: () -> Unit = {},
  onLanguageClick: () -> Unit = {},
  onUpdateSettings: (AppSettings) -> Unit = {}
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            stringResource(R.string.title_settings),
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
                Text(stringResource(R.string.action_back))
              }
            },
            state = rememberTooltipState(),
          ) {
            FilledIconButton(
              onClick = onBack,
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
              ),
              shapes = IconButtonDefaults.shapes()
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_arrow_back),
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
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
              onClick = { showMenu = true },
              modifier =
                Modifier
                  .minimumInteractiveComponentSize()
                  .size(
                    IconButtonDefaults.smallContainerSize(
                      IconButtonDefaults.IconButtonWidthOption.Narrow
                    )
                  ),
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
              ),
              shapes = IconButtonDefaults.shapes()
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_more_vert),
                contentDescription = stringResource(R.string.action_more),
                tint = MaterialTheme.colorScheme.onSurface
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
            val groupCount = 2

            DropdownMenuGroup(
              shapes = MenuDefaults.groupShape(0, groupCount),
            ) {
              val itemCount = 4

              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_about)) },
                onClick = {
                  showMenu = false
                  onAboutClick()
                },
                shape = MenuDefaults.itemShape(0, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_help)) },
                onClick = {
                  showMenu = false
                  onHelpClick()
                },
                shape = MenuDefaults.itemShape(1, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_feedback)) },
                onClick = {
                  showMenu = false
                  onFeedbackClick()
                },
                shape = MenuDefaults.itemShape(2, itemCount).shape
              )
            }

            Spacer(Modifier.height(MenuDefaults.GroupSpacing))

            DropdownMenuGroup(
              shapes = MenuDefaults.groupShape(1, groupCount),
            ) {
              val itemCount = 2

              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_logcat)) },
                onClick = {
                  showMenu = false
                  onLogcatClick()
                },
                shape = MenuDefaults.itemShape(1, itemCount).shape
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        scrollBehavior = scrollBehavior,
      )
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) { padding ->
    InsetLazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(padding),
      contentPadding = PaddingValues(
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          Text(
            text = stringResource(R.string.title_general),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          val itemCount = 1
          val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var languageIconTrigger by remember { mutableStateOf(false) }

          val localeName = if (settings.language == null) {
            stringResource(R.string.settings_language_system)
          } else {
            LocaleUtil.getLocaleName(settings.language)
          }

          SegmentedListItem(
            onClick = {
              onLanguageClick()
              languageIconTrigger = !languageIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(localeName)
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_language_anim,
                  trigger = languageIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.settings_language)) },
          )
        }
      }

      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 2
          val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var themeIconTrigger by remember { mutableStateOf(false) }
          var contrastIconTrigger by remember { mutableStateOf(false) }

          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_palette_anim,
                  trigger = themeIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            content = {
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = stringResource(R.string.settings_theme),
                  style = MaterialTheme.typography.bodyLarge
                )
                Text(
                  text = stringResource(R.string.settings_theme_description),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TackThemeSelection(
                  useDynamicColors = settings.useDynamicColors,
                  hue = settings.themeHue,
                  onHueChange = { onUpdateSettings(settings.copy(themeHue = it)) },
                  onUseDynamicColorsChange = { onUpdateSettings(settings.copy(useDynamicColors = it)) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                ConnectedButtonGroup(
                  options = AppTheme.entries.map { it.name },
                  labels = listOf(
                    stringResource(R.string.settings_theme_auto),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark)
                  ),
                  selected = settings.theme.name,
                  onSelect = { onUpdateSettings(settings.copy(theme = AppTheme.valueOf(it))) }
                )
              }
            },
          )

          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_contrast_anim,
                  trigger = contrastIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            content = {
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = stringResource(R.string.settings_contrast),
                  style = MaterialTheme.typography.bodyLarge
                )
                Text(
                  text = stringResource(R.string.settings_contrast_description),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                ConnectedButtonGroup(
                  options = AppContrast.entries.map { it.name },
                  labels = listOf(
                    stringResource(R.string.settings_contrast_standard),
                    stringResource(R.string.settings_contrast_medium),
                    stringResource(R.string.settings_contrast_high)
                  ),
                  selected = settings.contrast.name,
                  onSelect = { onUpdateSettings(settings.copy(contrast = AppContrast.valueOf(it))) },
                  enabled = !settings.useDynamicColors
                )

                if (settings.useDynamicColors) {
                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = stringResource(
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        R.string.settings_contrast_dynamic
                      } else {
                        R.string.settings_contrast_dynamic_unsupported
                      }
                    ),
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
  TackTheme {
    SettingsContent()
  }
}
