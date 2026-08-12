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

package xyz.zedler.patrick.tack.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.zedler.patrick.tack.core.model.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
  name = "settings",
  produceMigrations = { context ->
    listOf(SharedPreferencesMigration(
      context,
      context.packageName + "_preferences")
    )
  }
)

class AppDataStore(private val dataStore: DataStore<Preferences>) {

  constructor(context: Context) : this(context.dataStore)

  val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
    val default = AppSettings()
    AppSettings(
      // Appearance
      language = prefs[LANGUAGE].let { if (it == "system") null else it },
      useDynamicColors = prefs[USE_DYNAMIC_COLORS] ?: default.useDynamicColors,
      themeHue = prefs[THEME_HUE] ?: default.themeHue,
      theme = prefs[THEME]?.let { AppTheme.fromKey(it) } ?: default.theme,
      contrast = prefs[CONTRAST]?.let { AppContrast.fromKey(it) } ?: default.contrast,
      // Behavior
      haptic = prefs[HAPTIC] ?: default.haptic,
      vibrationIntensity = prefs[VIBRATION_INTENSITY]?.let { VibrationIntensity.fromKey(it) }
        ?: default.vibrationIntensity,
      reduceAnim = prefs[REDUCE_ANIM] ?: default.reduceAnim,
      // Instrument Settings
      sound = prefs[SOUND]?.let { Sound.fromKey(it) } ?: default.sound,
      gain = prefs[GAIN] ?: default.gain,
      latency = prefs[LATENCY] ?: default.latency,
      beatMode = prefs[BEAT_MODE]?.let { BeatMode.fromKey(it) } ?: default.beatMode,
      flashlight = prefs[FLASHLIGHT]?.let { FlashStrength.fromKey(it) } ?: default.flashlight,
      flashScreen = prefs[FLASH_SCREEN]?.let { FlashStrength.fromKey(it) } ?: default.flashScreen,
      keepAwake = prefs[KEEP_AWAKE]?.let { KeepAwakeMode.fromKey(it) } ?: default.keepAwake,
      ignoreFocus = prefs[IGNORE_FOCUS] ?: default.ignoreFocus,
      // UI States
      showElapsed = prefs[SHOW_ELAPSED] ?: default.showElapsed,
      resetTimerOnStop = prefs[RESET_TIMER_ON_STOP] ?: default.resetTimerOnStop,
      permNotification = prefs[PERM_NOTIFICATION] ?: default.permNotification,
      activeBeat = prefs[ACTIVE_BEAT] ?: default.activeBeat,
      bigTimeText = prefs[BIG_TIME_TEXT] ?: default.bigTimeText,
      bigLogo = prefs[BIG_LOGO] ?: default.bigLogo,
      // App specific
      checkUnlockKey = prefs[CHECK_UNLOCK_KEY] ?: default.checkUnlockKey
    )
  }

  val metronomeConfig: Flow<MetronomeConfig> = dataStore.data.map { prefs ->
    val default = MetronomeConfig()
    MetronomeConfig(
      tempo = prefs[TEMPO] ?: default.tempo,
      beats = (prefs[BEATS] ?: default.beats.joinToString(","))
        .split(",").map { TickType.fromKey(it) },
      subdivisions = (prefs[SUBDIVISIONS] ?: default.subdivisions.joinToString(","))
        .split(",").map { TickType.fromKey(it) },
      usePolyrhythm = prefs[USE_POLYRHYTHM] ?: default.usePolyrhythm,
      countIn = prefs[COUNT_IN] ?: default.countIn,
      incrementalAmount = prefs[INCREMENTAL_AMOUNT] ?: default.incrementalAmount,
      incrementalInterval = prefs[INCREMENTAL_INTERVAL] ?: default.incrementalInterval,
      incrementalLimit = prefs[INCREMENTAL_LIMIT] ?: default.incrementalLimit,
      incrementalUnit = prefs[INCREMENTAL_UNIT]?.let { TimingUnit.fromKey(it) }
        ?: default.incrementalUnit,
      incrementalIncrease = prefs[INCREMENTAL_INCREASE] ?: default.incrementalIncrease,
      timerDuration = prefs[TIMER_DURATION] ?: default.timerDuration,
      timerUnit = prefs[TIMER_UNIT]?.let { TimingUnit.fromKey(it) }
        ?: default.timerUnit,
      mutePlay = prefs[MUTE_PLAY] ?: default.mutePlay,
      muteMute = prefs[MUTE_MUTE] ?: default.muteMute,
      muteUnit = prefs[MUTE_UNIT]?.let { TimingUnit.fromKey(it) }
        ?: default.muteUnit,
      muteRandom = prefs[MUTE_RANDOM] ?: default.muteRandom
    )
  }

  suspend fun updateSettings(settings: AppSettings) {
    dataStore.edit { prefs ->
      prefs.setIfChanged(LANGUAGE, settings.language ?: "system")
      prefs.setIfChanged(USE_DYNAMIC_COLORS, settings.useDynamicColors)
      prefs.setIfChanged(THEME_HUE, settings.themeHue)
      prefs.setIfChanged(THEME, settings.theme.key)
      prefs.setIfChanged(CONTRAST, settings.contrast.key)
      prefs.setIfChanged(HAPTIC, settings.haptic)
      prefs.setIfChanged(VIBRATION_INTENSITY, settings.vibrationIntensity.key)
      prefs.setIfChanged(REDUCE_ANIM, settings.reduceAnim)
      prefs.setIfChanged(SOUND, settings.sound.key)
      prefs.setIfChanged(GAIN, settings.gain)
      prefs.setIfChanged(LATENCY, settings.latency)
      prefs.setIfChanged(BEAT_MODE, settings.beatMode.key)
      prefs.setIfChanged(FLASHLIGHT, settings.flashlight.key)
      prefs.setIfChanged(FLASH_SCREEN, settings.flashScreen.key)
      prefs.setIfChanged(KEEP_AWAKE, settings.keepAwake.key)
      prefs.setIfChanged(IGNORE_FOCUS, settings.ignoreFocus)
      prefs.setIfChanged(SHOW_ELAPSED, settings.showElapsed)
      prefs.setIfChanged(RESET_TIMER_ON_STOP, settings.resetTimerOnStop)
      prefs.setIfChanged(PERM_NOTIFICATION, settings.permNotification)
      prefs.setIfChanged(ACTIVE_BEAT, settings.activeBeat)
      prefs.setIfChanged(BIG_TIME_TEXT, settings.bigTimeText)
      prefs.setIfChanged(BIG_LOGO, settings.bigLogo)
      prefs.setIfChanged(CHECK_UNLOCK_KEY, settings.checkUnlockKey)
    }
  }

  suspend fun updateMetronomeConfig(config: MetronomeConfig) {
    dataStore.edit { prefs ->
      prefs.setIfChanged(TEMPO, config.tempo)
      prefs.setIfChanged(BEATS, config.beats.joinToString(",") { it.key })
      prefs.setIfChanged(SUBDIVISIONS, config.subdivisions.joinToString(",") { it.key })
      prefs.setIfChanged(USE_POLYRHYTHM, config.usePolyrhythm)
      prefs.setIfChanged(COUNT_IN, config.countIn)
      prefs.setIfChanged(INCREMENTAL_AMOUNT, config.incrementalAmount)
      prefs.setIfChanged(INCREMENTAL_INCREASE, config.incrementalIncrease)
      prefs.setIfChanged(INCREMENTAL_INTERVAL, config.incrementalInterval)
      prefs.setIfChanged(INCREMENTAL_UNIT, config.incrementalUnit.key)
      prefs.setIfChanged(INCREMENTAL_LIMIT, config.incrementalLimit)
      prefs.setIfChanged(TIMER_DURATION, config.timerDuration)
      prefs.setIfChanged(TIMER_UNIT, config.timerUnit.key)
      prefs.setIfChanged(MUTE_PLAY, config.mutePlay)
      prefs.setIfChanged(MUTE_MUTE, config.muteMute)
      prefs.setIfChanged(MUTE_UNIT, config.muteUnit.key)
      prefs.setIfChanged(MUTE_RANDOM, config.muteRandom)
    }
  }

  suspend fun clearAll() {
    dataStore.edit { it.clear() }
  }

  private fun <T> MutablePreferences.setIfChanged(key: Preferences.Key<T>, value: T) {
    if (this[key] != value) {
      this[key] = value
    }
  }

  companion object {
    private val LANGUAGE = stringPreferencesKey("language")
    private val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    private val THEME_HUE = floatPreferencesKey("theme_hue")
    private val THEME = stringPreferencesKey("app_theme")
    private val CONTRAST = stringPreferencesKey("theme_contrast")
    private val HAPTIC = booleanPreferencesKey("haptic_feedback")
    private val VIBRATION_INTENSITY = stringPreferencesKey("vibration_intensity")
    private val REDUCE_ANIM = booleanPreferencesKey("reduce_animations")

    private val TEMPO = intPreferencesKey("tempo")
    private val BEATS = stringPreferencesKey("beats")
    private val SUBDIVISIONS = stringPreferencesKey("subdivisions")
    private val USE_POLYRHYTHM = booleanPreferencesKey("use_polyrhythm")
    private val BEAT_MODE = stringPreferencesKey("beat_mode")
    private val SHOW_ELAPSED = booleanPreferencesKey("show_elapsed")
    private val RESET_TIMER_ON_STOP = booleanPreferencesKey("reset_timer")
    private val FLASH_SCREEN = stringPreferencesKey("flash_screen_strength")
    private val FLASHLIGHT = stringPreferencesKey("flashlight_strength")
    private val KEEP_AWAKE = stringPreferencesKey("keep_screen_awake")
    private val SOUND = stringPreferencesKey("sound")
    private val LATENCY = longPreferencesKey("latency_ms")
    private val IGNORE_FOCUS = booleanPreferencesKey("ignore_focus")
    private val GAIN = intPreferencesKey("gain")
    private val PERM_NOTIFICATION = booleanPreferencesKey("permanent_notification")

    private val COUNT_IN = intPreferencesKey("count_in")
    private val INCREMENTAL_AMOUNT = intPreferencesKey("incremental_amount")
    private val INCREMENTAL_INCREASE = booleanPreferencesKey("incremental_increase")
    private val INCREMENTAL_INTERVAL = intPreferencesKey("incremental_interval")
    private val INCREMENTAL_UNIT = stringPreferencesKey("incremental_unit")
    private val INCREMENTAL_LIMIT = intPreferencesKey("incremental_limit")
    private val TIMER_DURATION = intPreferencesKey("timer_duration")
    private val TIMER_UNIT = stringPreferencesKey("timer_unit")
    private val MUTE_PLAY = intPreferencesKey("mute_play")
    private val MUTE_MUTE = intPreferencesKey("mute_mute")
    private val MUTE_UNIT = stringPreferencesKey("mute_unit")
    private val MUTE_RANDOM = booleanPreferencesKey("mute_random")

    private val ACTIVE_BEAT = booleanPreferencesKey("active_beat")
    private val BIG_TIME_TEXT = booleanPreferencesKey("big_time_text")
    private val BIG_LOGO = booleanPreferencesKey("big_logo")

    private val SONGS_ORDER = intPreferencesKey("songs_order")
    private val SONG_CURRENT_ID = stringPreferencesKey("current_song_id")
    private val PART_CURRENT_INDEX = intPreferencesKey("current_part_index")

    private val CHECK_UNLOCK_KEY = booleanPreferencesKey("check_unlock_key")
  }
}
