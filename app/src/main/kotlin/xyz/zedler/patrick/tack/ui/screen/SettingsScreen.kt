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

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.AppColor
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.AppTheme
import xyz.zedler.patrick.tack.core.model.VibrationIntensity
import xyz.zedler.patrick.tack.ui.component.core.AnimatedIcon
import xyz.zedler.patrick.tack.ui.component.core.ConnectedButtonGroup
import xyz.zedler.patrick.tack.ui.component.core.InsetLazyColumn
import xyz.zedler.patrick.tack.ui.component.core.insetItem
import xyz.zedler.patrick.tack.ui.dialog.BackupDialog
import xyz.zedler.patrick.tack.ui.dialog.FeedbackDialog
import xyz.zedler.patrick.tack.ui.dialog.HelpDialog
import xyz.zedler.patrick.tack.ui.dialog.LanguageDialog
import xyz.zedler.patrick.tack.ui.dialog.ResetDialog
import xyz.zedler.patrick.tack.ui.dialog.SoundDialog
import xyz.zedler.patrick.tack.ui.dialog.UnlockDialog
import xyz.zedler.patrick.tack.ui.navigation.Route
import xyz.zedler.patrick.tack.ui.theme.LocalDimens
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.ui.util.titleRes
import xyz.zedler.patrick.tack.util.LocaleUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
  val context = LocalContext.current
  val haptic = LocalHaptic.current

  val appVendingKey = stringResource(R.string.app_vending_key)

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()

  val resolvedLanguage = remember(settings.language) {
    LocaleUtil.resolveLanguageCode(context, settings.language)
  }

  var showFeedbackDialog by rememberSaveable { mutableStateOf(false) }
  var showUnlockDialog by rememberSaveable { mutableStateOf(false) }
  var showHelpDialog by rememberSaveable { mutableStateOf(false) }
  var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
  var showBackupDialog by rememberSaveable { mutableStateOf(false) }
  var showResetDialog by rememberSaveable { mutableStateOf(false) }
  var showSoundDialog by rememberSaveable { mutableStateOf(false) }

  val launcherBackup = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    viewModel.exportLibrary(uri)
  }

  val launcherRestore = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    viewModel.importLibrary(context, uri)
  }

  LaunchedEffect(viewModel.uiEvent) {
    viewModel.uiEvent.collect { event ->
      when (event) {
        is MainViewModel.UiEvent.ShowToast -> {
          Toast.makeText(context, event.messageResId, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  if (showFeedbackDialog) {
    FeedbackDialog(
      checkUnlockKey = unlockState.checkUnlockKey,
      isKeyInstalled = unlockState.isKeyInstalled,
      isPlayStoreInstalled = unlockState.isPlayStoreInstalled,
      onSupport = { showUnlockDialog = true },
      onDismissRequest = { showFeedbackDialog = false }
    )
  }

  if (showUnlockDialog) {
    UnlockDialog(
      onOpen = {
        context.startActivity(
          Intent(Intent.ACTION_VIEW, appVendingKey.toUri())
        )
      },
      onDismissRequest = { showUnlockDialog = false }
    )
  }

  if (showHelpDialog) {
    HelpDialog(onDismissRequest = { showHelpDialog = false })
  }

  if (showLanguageDialog) {
    LanguageDialog(
      currentLanguageCode = resolvedLanguage,
      onLanguageSelected = { viewModel.updateSettings(settings.copy(language = it)) },
      onDismissRequest = { showLanguageDialog = false }
    )
  }

  if (showBackupDialog) {
    BackupDialog(
      onBackup = { launcherBackup.launch("song_library.json") },
      onRestore = { launcherRestore.launch(arrayOf("application/json")) },
      onDismissRequest = { showBackupDialog = false }
    )
  }

  if (showResetDialog) {
    ResetDialog(
      onReset = { viewModel.resetAll() },
      onDismissRequest = { showResetDialog = false }
    )
  }

  if (showSoundDialog) {
    SoundDialog(
      currentSound = settings.sound,
      onSoundSelected = {
        viewModel.updateSettings(settings.copy(sound = it))
      },
      onDismissRequest = { showSoundDialog = false }
    )
  }

  SettingsContent(
    settings = settings,
    resolvedLanguage = resolvedLanguage,
    hasVibrator = haptic.hasVibrator,
    supportsMainEffects = haptic.supportsMainEffects,
    onClick = { haptic.click() },
    onCheckedChange = { haptic.click() },
    onValueChange = { haptic.tick() },
    onBackClick = { viewModel.popBackstack() },
    onAboutClick = { viewModel.navigateTo(Route.About) },
    onHelpClick = { showHelpDialog = true },
    onFeedbackClick = { showFeedbackDialog = true },
    onLogcatClick = { viewModel.navigateTo(Route.Log) },
    onLanguageClick = { showLanguageDialog = true },
    onVibrationIntensityChanged = {
      haptic.intensity = it
      haptic.click()
    },
    onBackupClick = { showBackupDialog = true },
    onResetClick = { showResetDialog = true },
    onSoundClick = { showSoundDialog = true },
    onGainClick = { /* TODO */ },
    onLatencyClick = { /* TODO */ },
    onUpdateSettings = viewModel::updateSettings
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  settings: AppSettings = AppSettings(),
  resolvedLanguage: String? = null,
  hasVibrator: Boolean = true,
  supportsMainEffects: Boolean = true,
  onClick: () -> Unit = {},
  onCheckedChange: () -> Unit = {},
  onValueChange: () -> Unit = {},
  onBackClick: () -> Unit = {},
  onAboutClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  onFeedbackClick: () -> Unit = {},
  onLogcatClick: () -> Unit = {},
  onLanguageClick: () -> Unit = {},
  onVibrationIntensityChanged: (intensity: VibrationIntensity) -> Unit = {},
  onBackupClick: () -> Unit = {},
  onResetClick: () -> Unit = {},
  onSoundClick: () -> Unit = {},
  onGainClick: () -> Unit = {},
  onLatencyClick: () -> Unit = {},
  onUpdateSettings: (settings: AppSettings) -> Unit = {}
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
              onClick = {
                onClick()
                onBackClick()
              },
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
              onClick = {
                onClick()
                showMenu = true
              },
              modifier = Modifier
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
                  onClick()
                  showMenu = false
                  onAboutClick()
                },
                shape = MenuDefaults.itemShape(0, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_help)) },
                onClick = {
                  onClick()
                  showMenu = false
                  onHelpClick()
                },
                shape = MenuDefaults.itemShape(1, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_feedback)) },
                onClick = {
                  onClick()
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
                  onClick()
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
        scrollBehavior = scrollBehavior
      )
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) { padding ->
    val dimens = LocalDimens.current

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
            LocaleUtil.getLocaleName(resolvedLanguage)
          }

          SegmentedListItem(
            onClick = {
              onClick()
              onLanguageClick()
              languageIconTrigger = !languageIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(localeName)
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_language_anim,
                  trigger = languageIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.settings_language)) }
          )
        }
      }

      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 3
          val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var themeIconTrigger by remember { mutableStateOf(false) }
          var contrastIconTrigger by remember { mutableStateOf(false) }
          var reduceAnimIconTrigger by remember { mutableStateOf(false) }
          var isInitialComposition by remember { mutableStateOf(true) }

          LaunchedEffect(settings.reduceAnim) {
            if (isInitialComposition) {
              isInitialComposition = false
              return@LaunchedEffect
            }
            if (!settings.reduceAnim) {
              // only trigger animated icon if reduce anim is turned off
              reduceAnimIconTrigger = !reduceAnimIconTrigger
            }
          }

          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  Spacer(modifier = Modifier.height(4.dp))

                  ConnectedButtonGroup(
                    options = AppColor.entries.map { it.name },
                    labels = listOf(
                      stringResource(R.string.settings_theme_dynamic),
                      stringResource(R.string.settings_theme_static)
                    ),
                    checked = settings.color.name,
                    onCheckedChange = {
                      if (it != settings.color.name) {
                        onCheckedChange()
                        onUpdateSettings(settings.copy(color = AppColor.valueOf(it)))
                      }
                    }
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val interactionSource = remember { MutableInteractionSource() }
                val hueColors = remember {
                  (0..360 step 2).map {
                    Hct.from(it.toDouble(), 70.0, 60.0).toColor()
                  }
                }
                val hueBrush = remember(hueColors) {
                  Brush.linearGradient(hueColors)
                }

                Box {
                  if (settings.color == AppColor.STATIC) {
                    Slider(
                      value = settings.colorHue,
                      onValueChange = { },
                      valueRange = 0f..360f,
                      steps = 20,
                      interactionSource = interactionSource,
                      track = { sliderState ->
                        SliderDefaults.Track(
                          trackCornerSize = 8.dp,
                          sliderState = sliderState,
                          colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White
                          ),
                          modifier = Modifier
                            .height(24.dp)
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                              drawContent()
                              drawRect(brush = hueBrush, blendMode = BlendMode.SrcIn)
                            }
                        )
                      }
                    )
                  }

                  Slider(
                    enabled = settings.color == AppColor.STATIC,
                    value = settings.colorHue,
                    onValueChange = {
                      if (it != settings.colorHue) {
                        onValueChange()
                        onUpdateSettings(settings.copy(colorHue = it))
                      }
                    },
                    valueRange = 0f..360f,
                    steps = 20,
                    interactionSource = interactionSource,
                    track = { sliderState ->
                      SliderDefaults.Track(
                        enabled = settings.color == AppColor.STATIC,
                        trackCornerSize = 8.dp,
                        sliderState = sliderState,
                        drawStopIndicator = null,
                        colors = if (settings.color == AppColor.STATIC) {
                          SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            activeTickColor = Color.White,
                            inactiveTickColor = Color.White
                          )
                        } else {
                          SliderDefaults.colors(
                            disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                              alpha = 0.12f
                            ),
                            disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                              alpha = 0.12f
                            ),
                            disabledActiveTickColor = MaterialTheme.colorScheme.onSurface.copy(
                              alpha = 0.38f
                            ),
                            disabledInactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(
                              alpha = 0.38f
                            )
                          )
                        },
                        modifier = Modifier.height(24.dp)
                      )
                    }
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                ConnectedButtonGroup(
                  options = AppTheme.entries.map { it.name },
                  labels = listOf(
                    stringResource(R.string.settings_theme_auto),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark)
                  ),
                  checked = settings.theme.name,
                  onCheckedChange = {
                    if (it != settings.theme.name) {
                      onCheckedChange()
                      onUpdateSettings(settings.copy(theme = AppTheme.valueOf(it)))
                    }
                  }
                )
              }
            },
          )

          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
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

                if (settings.color == AppColor.DYNAMIC) {
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

                  Spacer(modifier = Modifier.height(4.dp))
                }

                ConnectedButtonGroup(
                  options = AppContrast.entries.map { it.name },
                  labels = listOf(
                    stringResource(R.string.settings_contrast_standard),
                    stringResource(R.string.settings_contrast_medium),
                    stringResource(R.string.settings_contrast_high)
                  ),
                  checked = settings.contrast.name,
                  onCheckedChange = {
                    if (it != settings.contrast.name) {
                      onCheckedChange()
                      contrastIconTrigger = !contrastIconTrigger
                      onUpdateSettings(settings.copy(contrast = AppContrast.valueOf(it)))
                    }
                  },
                  enabled = settings.color == AppColor.STATIC
                )
              }
            }
          )

          SegmentedListItem(
            onClick = {
              onClick()
              onUpdateSettings(settings.copy(reduceAnim = !settings.reduceAnim))
            },
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.settings_reduce_animations_description))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_animation_anim,
                  trigger = reduceAnimIconTrigger
                )
              }
            },
            trailingContent = {
              Switch(
                checked = settings.reduceAnim,
                onCheckedChange = null,
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemTrailingContentPaddingVertical
                )
              )
            },
            content = { Text(stringResource(R.string.settings_reduce_animations)) }
          )
        }
      }

      if (hasVibrator) {
        insetItem {
          Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            val itemCount = 2
            val colors = ListItemDefaults.segmentedColors(
              containerColor = MaterialTheme.colorScheme.surfaceBright
            )

            var hapticIconTrigger by remember { mutableStateOf(false) }

            SegmentedListItem(
              onClick = {
                onClick()
                hapticIconTrigger = !hapticIconTrigger
                onUpdateSettings(settings.copy(haptic = !settings.haptic))
              },
              shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
              colors = colors,
              supportingContent = {
                Column {
                  Text(stringResource(R.string.settings_haptic_description))

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = stringResource(R.string.settings_haptic_warning),
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.error
                  )
                }
              },
              leadingContent = {
                Box(
                  modifier = Modifier.padding(
                    vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                  )
                ) {
                  AnimatedIcon(
                    resId = R.drawable.ic_rounded_vibration_anim,
                    trigger = hapticIconTrigger,
                    animated = !settings.reduceAnim
                  )
                }
              },
              trailingContent = {
                Switch(
                  checked = settings.haptic,
                  onCheckedChange = null,
                  modifier = Modifier.padding(
                    vertical = dimens.segmentedListItemTrailingContentPaddingVertical
                  )
                )
              },
              content = { Text(stringResource(R.string.settings_haptic)) }
            )

            SegmentedListItem(
              shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
              colors = colors,
              leadingContent = {
                Box(
                  modifier = Modifier.padding(
                    vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                  )
                ) {
                  Icon(
                    painter = painterResource(R.drawable.ic_rounded_mobile_sensor_lo),
                    contentDescription = null
                  )
                }
              },
              content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Text(
                    text = stringResource(R.string.settings_vibration_intensity),
                    style = MaterialTheme.typography.bodyLarge
                  )
                  Text(
                    text = stringResource(R.string.settings_vibration_intensity_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  Spacer(modifier = Modifier.height(4.dp))

                  ConnectedButtonGroup(
                    options = remember(supportsMainEffects) {
                      val entriesWithoutUnset =
                        VibrationIntensity.entries - VibrationIntensity.UNSET
                      if (supportsMainEffects) {
                        entriesWithoutUnset.map { it.name }
                      } else {
                        (entriesWithoutUnset - VibrationIntensity.AUTO).map { it.name }
                      }
                    },
                    labels = listOfNotNull(
                      if (supportsMainEffects) {
                        stringResource(R.string.settings_vibration_intensity_auto)
                      } else null,
                      stringResource(R.string.settings_vibration_intensity_soft),
                      stringResource(R.string.settings_vibration_intensity_strong)
                    ),
                    checked = settings.vibrationIntensity.name,
                    onCheckedChange = {
                      val intensity = VibrationIntensity.valueOf(it)
                      if (intensity != settings.vibrationIntensity) {
                        onVibrationIntensityChanged(intensity)
                        onUpdateSettings(settings.copy(vibrationIntensity = intensity))
                      }
                    }
                  )
                }
              }
            )
          }
        }
      }

      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 2
          val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          SegmentedListItem(
            onClick = {
              onClick()
              onBackupClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.settings_backup_description))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_download),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.settings_backup)) },
          )

          SegmentedListItem(
            onClick = {
              onClick()
              onResetClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.settings_reset_description))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_reset_settings),
                  contentDescription = null
                )
              }
            },
            content = {
              Text(stringResource(R.string.settings_reset))
            }
          )
        }
      }

      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 4
          val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var soundIconTrigger by remember { mutableStateOf(false) }
          var ignoreFocusIconTrigger by remember { mutableStateOf(false) }
          var gainIconTrigger by remember { mutableStateOf(false) }

          SegmentedListItem(
            onClick = {
              onClick()
              onSoundClick()
              soundIconTrigger = !soundIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(settings.sound.titleRes))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_music_note_anim,
                  trigger = soundIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            content = {
              Text(stringResource(R.string.settings_sound))
            }
          )

          SegmentedListItem(
            onClick = {
              onClick()
              onUpdateSettings(settings.copy(ignoreFocus = !settings.ignoreFocus))
              ignoreFocusIconTrigger = !ignoreFocusIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.settings_ignore_focus_description))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_select_to_speak_anim,
                  trigger = ignoreFocusIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            trailingContent = {
              Switch(
                checked = settings.ignoreFocus,
                onCheckedChange = null,
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemTrailingContentPaddingVertical
                )
              )
            },
            content = { Text(stringResource(R.string.settings_ignore_focus)) }
          )

          SegmentedListItem(
            onClick = {
              onClick()
              onGainClick()
              gainIconTrigger = !gainIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            colors = colors,
            supportingContent = {
              val db = if (settings.gain > 0) "+$settings.gain" else settings.gain.toString()
              Text(stringResource(R.string.label_db_signed, db))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_speaker_anim,
                  trigger = gainIconTrigger,
                  animated = !settings.reduceAnim
                )
              }
            },
            content = {
              Text(stringResource(R.string.settings_gain))
            }
          )

          SegmentedListItem(
            onClick = {
              onClick()
              onLatencyClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index = 3, count = itemCount),
            colors = colors,
            supportingContent = {
              val latency = settings.latency.toString()
              Text(stringResource(R.string.label_ms, latency))
            },
            leadingContent = {
              Box(
                modifier = Modifier.padding(
                  vertical = dimens.segmentedListItemLeadingContentPaddingVertical
                )
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_media_output),
                  contentDescription = null
                )
              }
            },
            content = {
              Text(stringResource(R.string.settings_latency))
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
