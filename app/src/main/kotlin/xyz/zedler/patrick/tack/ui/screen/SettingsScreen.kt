package xyz.zedler.patrick.tack.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.*
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
  viewModel: MainViewModel = viewModel(),
  onBack: () -> Unit
) {
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
    onBack = onBack,
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
  themeMode: String = "system",
  contrast: String = "standard",
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
  onBack: () -> Unit = {},
  onUpdateUseDynamicColors: (Boolean) -> Unit = {},
  onUpdateThemeHue: (Float) -> Unit = {},
  onUpdateThemeMode: (String) -> Unit = {},
  onUpdateContrast: (String) -> Unit = {},
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
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.title_settings)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back)
            )
          }
        },
        actions = {
          IconButton(onClick = { /* TODO: More menu */ }) {
            Icon(
              Icons.Rounded.MoreVert,
              contentDescription = stringResource(R.string.action_more)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
      item { TackCategoryHeader(stringResource(R.string.title_general)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = Icons.Rounded.Language,
            title = stringResource(R.string.settings_language),
            description = stringResource(R.string.settings_language_system),
            onClick = { /* TODO */ }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = Icons.Rounded.Palette,
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
            options = listOf("system", "light", "dark"),
            labels = listOf(
              stringResource(R.string.settings_theme_auto),
              stringResource(R.string.settings_theme_light),
              stringResource(R.string.settings_theme_dark)
            ),
            selected = themeMode,
            onSelect = onUpdateThemeMode
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = Icons.Rounded.Contrast,
            title = stringResource(R.string.settings_contrast),
            description = stringResource(R.string.settings_contrast_description),
            onClick = null
          )
          TackButtonGroup(
            options = listOf("standard", "medium", "high"),
            labels = listOf(
              stringResource(R.string.settings_contrast_standard),
              stringResource(R.string.settings_contrast_medium),
              stringResource(R.string.settings_contrast_high)
            ),
            selected = contrast,
            enabled = !useDynamicColors,
            onSelect = onUpdateContrast
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = Icons.Rounded.Vibration,
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
            icon = Icons.Rounded.Sensors,
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
            onSelect = onUpdateVibrationIntensity
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = Icons.Rounded.Animation,
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
            icon = Icons.Rounded.Download,
            title = stringResource(R.string.settings_backup),
            description = stringResource(R.string.settings_backup_description),
            onClick = { /* TODO */ }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = Icons.Rounded.SettingsBackupRestore,
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
            icon = Icons.Rounded.MusicNote,
            title = stringResource(R.string.settings_sound),
            description = sound,
            onClick = { /* TODO */ }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = Icons.Rounded.RecordVoiceOver,
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
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.settings_gain),
            description = stringResource(
              R.string.label_db_signed, if (gain > 0) "+$gain" else gain
            ),
            onClick = { /* TODO */ }
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = Icons.Rounded.Speaker,
            title = stringResource(R.string.settings_latency),
            description = stringResource(R.string.label_ms, latency),
            onClick = { /* TODO */ }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = Icons.Rounded.History,
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
            icon = Icons.Rounded.Bolt,
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
            onSelect = onUpdateFlashScreen
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = Icons.Rounded.FlashlightOn,
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
            onSelect = onUpdateFlashlight
          )
          HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
          TackSettingsListItem(
            icon = Icons.Rounded.Preview,
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
            onSelect = onUpdateKeepAwake
          )
        }
      }

      item { TackCategoryHeader(stringResource(R.string.title_controls)) }

      item {
        TackSettingsGroup {
          TackSettingsListItem(
            icon = Icons.Rounded.Brush,
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
            icon = Icons.Rounded.AdUnits,
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
            icon = Icons.Rounded.Schedule,
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
            icon = Icons.Rounded.Pin,
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
            icon = Icons.Rounded.Star,
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
  }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
  TackTheme(useDynamicColors = false, hue = 200f, theme = "system", contrast = "standard") {
    SettingsContent()
  }
}
