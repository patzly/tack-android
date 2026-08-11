package xyz.zedler.patrick.tack.presentation.screen

import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppTheme
import xyz.zedler.patrick.tack.presentation.component.AnimatedIcon
import xyz.zedler.patrick.tack.presentation.component.ConnectedButtonGroup
import xyz.zedler.patrick.tack.presentation.component.TackThemeSelection
import xyz.zedler.patrick.tack.presentation.dialog.FeedbackDialog
import xyz.zedler.patrick.tack.presentation.dialog.LanguageDialog
import xyz.zedler.patrick.tack.presentation.navigation.Route
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.LocaleUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel = viewModel()) {
  val useDynamicColors by viewModel.useDynamicColors.collectAsStateWithLifecycle()
  val themeHue by viewModel.themeHue.collectAsStateWithLifecycle()
  val themeMode by viewModel.theme.collectAsStateWithLifecycle()
  val contrast by viewModel.contrast.collectAsStateWithLifecycle()
  val haptic by viewModel.haptic.collectAsStateWithLifecycle()
  val vibrationIntensity by viewModel.vibrationIntensity.collectAsStateWithLifecycle()
  val reduceAnim by viewModel.reduceAnim.collectAsStateWithLifecycle()
  val sound by viewModel.sound.collectAsStateWithLifecycle()
  val ignoreFocus by viewModel.ignoreFocus.collectAsStateWithLifecycle()
  val gain by viewModel.gain.collectAsStateWithLifecycle()
  val latency by viewModel.latency.collectAsStateWithLifecycle()
  val resetTimer by viewModel.resetTimerOnStop.collectAsStateWithLifecycle()
  val flashScreen by viewModel.flashScreen.collectAsStateWithLifecycle()
  val flashlight by viewModel.flashlight.collectAsStateWithLifecycle()
  val keepAwake by viewModel.keepAwake.collectAsStateWithLifecycle()
  val activeBeat by viewModel.activeBeat.collectAsStateWithLifecycle()
  val permNotification by viewModel.permNotification.collectAsStateWithLifecycle()
  val showElapsed by viewModel.showElapsed.collectAsStateWithLifecycle()
  val bigTimeText by viewModel.bigTimeText.collectAsStateWithLifecycle()
  val bigLogo by viewModel.bigLogo.collectAsStateWithLifecycle()
  val checkUnlockKey by viewModel.checkUnlockKey.collectAsStateWithLifecycle()
  val languageCode by viewModel.language.collectAsStateWithLifecycle()

  var showFeedbackDialog by rememberSaveable { mutableStateOf(false) }
  var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

  if (showFeedbackDialog) {
    FeedbackDialog(
      checkUnlockKey = checkUnlockKey,
      onDismissRequest = { showFeedbackDialog = false },
      onSupportClick = { /* TODO: Show unlock dialog */ }
    )
  }

  if (showLanguageDialog) {
    LanguageDialog(
      currentLanguageCode = languageCode,
      onLanguageSelected = viewModel::updateLanguage,
      onDismissRequest = { showLanguageDialog = false }
    )
  }

  SettingsContent(
    useDynamicColors = useDynamicColors,
    themeHue = themeHue,
    themeMode = themeMode,
    contrast = contrast,
    haptic = haptic,
    vibrationIntensity = vibrationIntensity,
    reduceAnim = reduceAnim,
    sound = sound,
    ignoreFocus = ignoreFocus,
    gain = gain,
    latency = latency,
    resetTimer = resetTimer,
    flashScreen = flashScreen,
    flashlight = flashlight,
    keepAwake = keepAwake,
    activeBeat = activeBeat,
    permNotification = permNotification,
    showElapsed = showElapsed,
    bigTimeText = bigTimeText,
    bigLogo = bigLogo,
    localeName = if (languageCode == null) {
      stringResource(R.string.settings_language_system)
    } else {
      LocaleUtil.getLocaleName(languageCode)
    },
    onBack = {
      viewModel.popBackstack()
    },
    onAboutClick = {
      viewModel.navigateTo(Route.About)
    },
    onHelpClick = {},
    onFeedbackClick = { showFeedbackDialog = true },
    onLogcatClick = {},
    onLanguageClick = { showLanguageDialog = true },
    onUpdateUseDynamicColors = viewModel::updateUseDynamicColors,
    onUpdateThemeHue = viewModel::updateThemeHue,
    onUpdateThemeMode = viewModel::updateTheme,
    onUpdateContrast = viewModel::updateContrast,
    onUpdateHaptic = viewModel::updateHaptic,
    onUpdateVibrationIntensity = viewModel::updateVibrationIntensity,
    onUpdateReduceAnim = viewModel::updateReduceAnim,
    onUpdateIgnoreFocus = viewModel::updateIgnoreFocus,
    onUpdateResetTimer = viewModel::updateResetTimerOnStop,
    onUpdateFlashScreen = viewModel::updateFlashScreen,
    onUpdateFlashlight = viewModel::updateFlashlight,
    onUpdateKeepAwake = viewModel::updateKeepAwake,
    onUpdateActiveBeat = viewModel::updateActiveBeat,
    onUpdatePermNotification = viewModel::updatePermNotification,
    onUpdateShowElapsed = viewModel::updateShowElapsed,
    onUpdateBigTimeText = viewModel::updateBigTimeText,
    onUpdateBigLogo = viewModel::updateBigLogo,
    onClearAll = viewModel::clearAll
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  useDynamicColors: Boolean = true,
  themeHue: Float = 200f,
  themeMode: AppTheme = AppTheme.SYSTEM,
  contrast: AppContrast = AppContrast.STANDARD,
  haptic: Boolean = true,
  vibrationIntensity: String = "auto",
  reduceAnim: Boolean = false,
  sound: String = "Sine",
  ignoreFocus: Boolean = false,
  gain: Int = 0,
  latency: Long = 0L,
  resetTimer: Boolean = false,
  flashScreen: String = "off",
  flashlight: String = "off",
  keepAwake: String = "while_playing",
  activeBeat: Boolean = true,
  permNotification: Boolean = false,
  showElapsed: Boolean = false,
  bigTimeText: Boolean = false,
  bigLogo: Boolean = false,
  localeName: String = "Follow system",
  onBack: () -> Unit = {},
  onAboutClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  onFeedbackClick: () -> Unit = {},
  onLogcatClick: () -> Unit = {},
  onLanguageClick: () -> Unit = {},
  onUpdateUseDynamicColors: (Boolean) -> Unit = {},
  onUpdateThemeHue: (Float) -> Unit = {},
  onUpdateThemeMode: (AppTheme) -> Unit = {},
  onUpdateContrast: (AppContrast) -> Unit = {},
  onUpdateHaptic: (Boolean) -> Unit = {},
  onUpdateVibrationIntensity: (String) -> Unit = {},
  onUpdateReduceAnim: (Boolean) -> Unit = {},
  onUpdateIgnoreFocus: (Boolean) -> Unit = {},
  onUpdateResetTimer: (Boolean) -> Unit = {},
  onUpdateFlashScreen: (String) -> Unit = {},
  onUpdateFlashlight: (String) -> Unit = {},
  onUpdateKeepAwake: (String) -> Unit = {},
  onUpdateActiveBeat: (Boolean) -> Unit = {},
  onUpdatePermNotification: (Boolean) -> Unit = {},
  onUpdateShowElapsed: (Boolean) -> Unit = {},
  onUpdateBigTimeText: (Boolean) -> Unit = {},
  onUpdateBigLogo: (Boolean) -> Unit = {},
  onClearAll: () -> Unit = {}
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
    containerColor = MaterialTheme.colorScheme.surfaceContainer
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(padding),
      contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Text(
          text = stringResource(R.string.title_general),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 1
          val colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var languageIconTrigger by remember { mutableStateOf(false) }

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
                  animated = !reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.settings_language)) },
          )
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 2
          val colors = ListItemDefaults.colors(
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
                  animated = !reduceAnim
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
                  useDynamicColors = useDynamicColors,
                  hue = themeHue,
                  onHueChange = onUpdateThemeHue,
                  onUseDynamicColorsChange = onUpdateUseDynamicColors
                )

                Spacer(modifier = Modifier.height(4.dp))

                ConnectedButtonGroup(
                  options = AppTheme.entries.map { it.name },
                  labels = listOf(
                    stringResource(R.string.settings_theme_auto),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark)
                  ),
                  selected = themeMode.name,
                  onSelect = { onUpdateThemeMode(AppTheme.valueOf(it)) }
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
                  animated = !reduceAnim
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
                  selected = contrast.name,
                  onSelect = { onUpdateContrast(AppContrast.valueOf(it)) },
                  enabled = !useDynamicColors
                )

                if (useDynamicColors) {
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


  /*Scaffold(
      item { TackCategoryHeader(stringResource(R.string.title_general)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_language_anim,
            title = stringResource(R.string.settings_language),
            description = stringResource(R.string.settings_language_system),
            onClick = { /* TODO: Language dialog */ }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_palette_anim,
            title = stringResource(R.string.settings_theme),
            description = stringResource(R.string.settings_theme_description),
            onClick = null
          )
          TackThemeSelection(
            useDynamicColors = useDynamicColors,
            hue = themeHue,
            onHueChange = onUpdateThemeHue,
            onUseDynamicColorsChange = onUpdateUseDynamicColors
          )
          TackButtonGroup(
            options = AppTheme.entries.map { it.name },
            labels = listOf(
              stringResource(R.string.settings_theme_auto),
              stringResource(R.string.settings_theme_light),
              stringResource(R.string.settings_theme_dark)
            ),
            selected = themeMode.name,
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            onSelect = { onUpdateThemeMode(AppTheme.valueOf(it)) }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_contrast_anim,
            title = stringResource(R.string.settings_contrast),
            description = stringResource(R.string.settings_contrast_description),
            onClick = null
          )
          TackButtonGroup(
            options = AppContrast.entries.map { it.name },
            labels = listOf(
              stringResource(R.string.settings_contrast_standard),
              stringResource(R.string.settings_contrast_medium),
              stringResource(R.string.settings_contrast_high)
            ),
            selected = contrast.name,
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            enabled = !useDynamicColors,
            onSelect = { onUpdateContrast(AppContrast.valueOf(it)) }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_vibration_anim,
            title = stringResource(R.string.settings_haptic),
            description = stringResource(R.string.settings_haptic_description),
            trailing = {
              Switch(checked = haptic, onCheckedChange = onUpdateHaptic)
            },
            onClick = { onUpdateHaptic(!haptic) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_mobile_sensor_lo,
            title = stringResource(R.string.settings_vibration_intensity),
            description = stringResource(R.string.settings_vibration_intensity_description),
            onClick = null
          )
          TackButtonGroup(
            options = listOf("auto", "soft", "strong"),
            labels = listOf(
              stringResource(R.string.settings_vibration_intensity_auto),
              stringResource(R.string.settings_vibration_intensity_soft),
              stringResource(R.string.settings_vibration_intensity_strong)
            ),
            selected = vibrationIntensity,
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            onSelect = onUpdateVibrationIntensity
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_animation_anim,
            title = stringResource(R.string.settings_reduce_animations),
            description = stringResource(R.string.settings_reduce_animations_description),
            trailing = {
              Switch(checked = reduceAnim, onCheckedChange = onUpdateReduceAnim)
            },
            onClick = { onUpdateReduceAnim(!reduceAnim) }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_download,
            title = stringResource(R.string.settings_backup),
            description = stringResource(R.string.settings_backup_description),
            onClick = { /* TODO */ }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_reset_settings,
            title = stringResource(R.string.settings_reset),
            description = stringResource(R.string.settings_reset_description),
            onClick = onClearAll
          )
        }
      }

      item { TackCategoryHeader(stringResource(R.string.title_metronome)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_music_note_anim,
            title = stringResource(R.string.settings_sound),
            description = sound,
            onClick = { /* TODO: Sound dialog */ }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_select_to_speak_anim,
            title = stringResource(R.string.settings_ignore_focus),
            description = stringResource(R.string.settings_ignore_focus_description),
            trailing = {
              Switch(checked = ignoreFocus, onCheckedChange = onUpdateIgnoreFocus)
            },
            onClick = { onUpdateIgnoreFocus(!ignoreFocus) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_speaker_anim,
            title = stringResource(R.string.settings_gain),
            description = stringResource(
              R.string.label_db_signed,
              if (gain > 0) "+$gain" else gain.toString()
            ),
            onClick = { /* TODO: Gain dialog */ }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_media_output,
            title = stringResource(R.string.settings_latency),
            description = stringResource(R.string.label_ms, latency.toString()),
            onClick = { /* TODO: Latency dialog */ }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_autopause_anim,
            title = stringResource(R.string.settings_reset_timer),
            description = stringResource(R.string.settings_reset_timer_description),
            trailing = {
              Switch(checked = resetTimer, onCheckedChange = onUpdateResetTimer)
            },
            onClick = { onUpdateResetTimer(!resetTimer) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_bolt_anim,
            title = stringResource(R.string.settings_flash_screen),
            description = stringResource(R.string.settings_flash_screen_description),
            onClick = null
          )
          TackButtonGroup(
            options = listOf("off", "subtle", "strong"),
            labels = listOf(
              stringResource(R.string.settings_flash_screen_off),
              stringResource(R.string.settings_flash_screen_subtle),
              stringResource(R.string.settings_flash_screen_strong)
            ),
            selected = flashScreen,
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            onSelect = onUpdateFlashScreen
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_flashlight_on,
            title = stringResource(R.string.settings_flashlight),
            description = stringResource(R.string.settings_flash_screen_description),
            onClick = null
          )
          TackButtonGroup(
            options = listOf("off", "subtle", "strong"),
            labels = listOf(
              stringResource(R.string.settings_flash_screen_off),
              stringResource(R.string.settings_flash_screen_subtle),
              stringResource(R.string.settings_flash_screen_strong)
            ),
            selected = flashlight,
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            onSelect = onUpdateFlashlight
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_preview_anim,
            title = stringResource(R.string.settings_keep_awake),
            description = stringResource(R.string.settings_keep_awake_description),
            onClick = null
          )
          TackButtonGroup(
            options = listOf("always", "while_playing", "never"),
            labels = listOf(
              stringResource(R.string.settings_keep_awake_always),
              stringResource(R.string.settings_keep_awake_while_playing),
              stringResource(R.string.settings_keep_awake_never)
            ),
            selected = keepAwake,
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            onSelect = onUpdateKeepAwake
          )
        }
      }

      item { TackCategoryHeader(stringResource(R.string.title_controls)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_ink_highlighter,
            title = stringResource(R.string.settings_active_beat),
            description = stringResource(R.string.settings_active_beat_description),
            trailing = {
              Switch(checked = activeBeat, onCheckedChange = onUpdateActiveBeat)
            },
            onClick = { onUpdateActiveBeat(!activeBeat) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_ad_units,
            title = stringResource(R.string.settings_perm_notification),
            description = stringResource(R.string.settings_perm_notification_description),
            trailing = {
              Switch(checked = permNotification, onCheckedChange = onUpdatePermNotification)
            },
            onClick = { onUpdatePermNotification(!permNotification) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_schedule_anim,
            title = stringResource(R.string.settings_elapsed),
            description = stringResource(R.string.settings_elapsed_description),
            trailing = {
              Switch(checked = showElapsed, onCheckedChange = onUpdateShowElapsed)
            },
            onClick = { onUpdateShowElapsed(!showElapsed) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_123,
            title = stringResource(R.string.settings_big_time_text),
            description = stringResource(R.string.settings_big_time_text_description),
            trailing = {
              Switch(checked = bigTimeText, onCheckedChange = onUpdateBigTimeText)
            },
            onClick = { onUpdateBigTimeText(!bigTimeText) }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = R.drawable.ic_rounded_star,
            title = stringResource(R.string.settings_big_logo),
            description = stringResource(R.string.settings_big_logo_description),
            trailing = {
              Switch(checked = bigLogo, onCheckedChange = onUpdateBigLogo)
            },
            onClick = { onUpdateBigLogo(!bigLogo) }
          )
        }
      }
    }
  }*/
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
  TackTheme {
    SettingsContent()
  }
}
