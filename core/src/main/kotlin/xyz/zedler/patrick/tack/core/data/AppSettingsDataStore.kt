package xyz.zedler.patrick.tack.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Default

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
  name = "settings",
  produceMigrations = { context ->
    listOf(SharedPreferencesMigration(
      context,
      context.packageName + "_preferences")
    )
  }
)

class AppSettingsDataStore(private val dataStore: DataStore<Preferences>) {

  constructor(context: Context) : this(context.dataStore)

  // General
  val theme = dataStore.data.map { it[THEME] ?: "system" }
  val contrast = dataStore.data.map { it[CONTRAST] ?: "standard" }
  val haptic = dataStore.data.map { it[HAPTIC] ?: true }
  val vibrationIntensity = dataStore.data.map { it[VIBRATION_INTENSITY] ?: "auto" }

  // Metronome
  val tempo = dataStore.data.map { it[TEMPO] ?: Default.TEMPO }
  val beats = dataStore.data.map { it[BEATS] ?: Default.BEATS.joinToString(",") }
  val subdivisions =
    dataStore.data.map { it[SUBDIVISIONS] ?: Default.SUBDIVISIONS.joinToString(",") }
  val usePolyrhythm = dataStore.data.map { it[USE_POLYRHYTHM] ?: Default.USE_POLYRHYTHM }
  val beatMode = dataStore.data.map { it[BEAT_MODE] ?: "all" }
  val showElapsed = dataStore.data.map { it[SHOW_ELAPSED] ?: false }
  val resetTimerOnStop = dataStore.data.map { it[RESET_TIMER_ON_STOP] ?: false }
  val flashScreen = dataStore.data.map { it[FLASH_SCREEN] ?: "off" }
  val flashlight = dataStore.data.map { it[FLASHLIGHT] ?: "off" }
  val keepAwake = dataStore.data.map { it[KEEP_AWAKE] ?: "while_playing" }
  val sound = dataStore.data.map { it[SOUND] ?: "sine" }
  val latency = dataStore.data.map { it[LATENCY] ?: 0L }
  val ignoreFocus = dataStore.data.map { it[IGNORE_FOCUS] ?: false }
  val gain = dataStore.data.map { it[GAIN] ?: 0 }
  val permNotification = dataStore.data.map { it[PERM_NOTIFICATION] ?: false }
  val useDynamicColors = dataStore.data.map { it[USE_DYNAMIC_COLORS] ?: true }
  val themeHue = dataStore.data.map { it[THEME_HUE] ?: 200f }

  // Options
  val countIn = dataStore.data.map { it[COUNT_IN] ?: Default.COUNT_IN }
  val incrementalAmount =
    dataStore.data.map { it[INCREMENTAL_AMOUNT] ?: Default.INCREMENTAL_AMOUNT }
  val incrementalIncrease =
    dataStore.data.map { it[INCREMENTAL_INCREASE] ?: Default.INCREMENTAL_INCREASE }
  val incrementalInterval =
    dataStore.data.map { it[INCREMENTAL_INTERVAL] ?: Default.INCREMENTAL_INTERVAL }
  val incrementalUnit =
    dataStore.data.map { it[INCREMENTAL_UNIT] ?: Default.INCREMENTAL_UNIT }
  val incrementalLimit =
    dataStore.data.map { it[INCREMENTAL_LIMIT] ?: Default.INCREMENTAL_LIMIT }
  val timerDuration = dataStore.data.map { it[TIMER_DURATION] ?: Default.TIMER_DURATION }
  val timerUnit = dataStore.data.map { it[TIMER_UNIT] ?: Default.TIMER_UNIT }
  val mutePlay = dataStore.data.map { it[MUTE_PLAY] ?: Default.MUTE_PLAY }
  val muteMute = dataStore.data.map { it[MUTE_MUTE] ?: Default.MUTE_MUTE }
  val muteUnit = dataStore.data.map { it[MUTE_UNIT] ?: Default.MUTE_UNIT }
  val muteRandom = dataStore.data.map { it[MUTE_RANDOM] ?: Default.MUTE_RANDOM }

  // Song library
  val songsOrder = dataStore.data.map { it[SONGS_ORDER] ?: 0 }
  val currentSongId = dataStore.data.map { it[SONG_CURRENT_ID] ?: "default" }
  val currentPartIndex = dataStore.data.map { it[PART_CURRENT_INDEX] ?: 0 }

  suspend fun updateTempo(tempo: Int) {
    dataStore.edit { it[TEMPO] = tempo }
  }

  suspend fun updateUseDynamicColors(use: Boolean) {
    dataStore.edit { it[USE_DYNAMIC_COLORS] = use }
  }

  suspend fun updateThemeHue(hue: Float) {
    dataStore.edit { it[THEME_HUE] = hue }
  }

  // ... other update functions will be added as needed

  companion object {
    private val THEME = stringPreferencesKey("app_theme")
    private val CONTRAST = stringPreferencesKey("theme_contrast")
    private val HAPTIC = booleanPreferencesKey("haptic_feedback")
    private val VIBRATION_INTENSITY = stringPreferencesKey("vibration_intensity")

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
    private val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    private val THEME_HUE = floatPreferencesKey("theme_hue")

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

    private val SONGS_ORDER = intPreferencesKey("songs_order")
    private val SONG_CURRENT_ID = stringPreferencesKey("current_song_id")
    private val PART_CURRENT_INDEX = intPreferencesKey("current_part_index")
  }
}
