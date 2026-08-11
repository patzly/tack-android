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
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppTheme
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
  val language = dataStore.data.map { it[LANGUAGE] }
  val useDynamicColors = dataStore.data.map { it[USE_DYNAMIC_COLORS] ?: true }
  val themeHue = dataStore.data.map { it[THEME_HUE] ?: 200f }
  val theme = dataStore.data.map { AppTheme.fromKey(it[THEME] ?: "system") }
  val contrast = dataStore.data.map { AppContrast.fromKey(it[CONTRAST] ?: "standard") }
  val haptic = dataStore.data.map { it[HAPTIC] ?: true }
  val vibrationIntensity = dataStore.data.map { it[VIBRATION_INTENSITY] ?: "auto" }
  val reduceAnim = dataStore.data.map { it[REDUCE_ANIM] ?: false }

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

  // Controls
  val activeBeat = dataStore.data.map { it[ACTIVE_BEAT] ?: false }
  val bigTimeText = dataStore.data.map { it[BIG_TIME_TEXT] ?: false }
  val bigLogo = dataStore.data.map { it[BIG_LOGO] ?: false }

  // Song library
  val songsOrder = dataStore.data.map { it[SONGS_ORDER] ?: 0 }
  val currentSongId = dataStore.data.map { it[SONG_CURRENT_ID] ?: "default" }
  val currentPartIndex = dataStore.data.map { it[PART_CURRENT_INDEX] ?: 0 }

  // App specific
  val checkUnlockKey = dataStore.data.map { it[CHECK_UNLOCK_KEY] ?: true }

  suspend fun updateLanguage(language: String?) {
    dataStore.edit {
      if (language == null) it.remove(LANGUAGE) else it[LANGUAGE] = language
    }
  }

  suspend fun updateUseDynamicColors(use: Boolean) {
    dataStore.edit { it[USE_DYNAMIC_COLORS] = use }
  }

  suspend fun updateThemeHue(hue: Float) {
    dataStore.edit { it[THEME_HUE] = hue }
  }

  suspend fun updateTheme(theme: AppTheme) {
    dataStore.edit { it[THEME] = theme.key }
  }

  suspend fun updateContrast(contrast: AppContrast) {
    dataStore.edit { it[CONTRAST] = contrast.key }
  }

  suspend fun updateHaptic(enabled: Boolean) {
    dataStore.edit { it[HAPTIC] = enabled }
  }

  suspend fun updateVibrationIntensity(intensity: String) {
    dataStore.edit { it[VIBRATION_INTENSITY] = intensity }
  }

  suspend fun updateReduceAnim(reduce: Boolean) {
    dataStore.edit { it[REDUCE_ANIM] = reduce }
  }

  suspend fun updateTempo(tempo: Int) {
    dataStore.edit { it[TEMPO] = tempo }
  }

  suspend fun updateSound(sound: String) {
    dataStore.edit { it[SOUND] = sound }
  }

  suspend fun updateIgnoreFocus(ignore: Boolean) {
    dataStore.edit { it[IGNORE_FOCUS] = ignore }
  }

  suspend fun updateGain(gain: Int) {
    dataStore.edit { it[GAIN] = gain }
  }

  suspend fun updateLatency(latency: Long) {
    dataStore.edit { it[LATENCY] = latency }
  }

  suspend fun updateResetTimerOnStop(reset: Boolean) {
    dataStore.edit { it[RESET_TIMER_ON_STOP] = reset }
  }

  suspend fun updateFlashScreen(flash: String) {
    dataStore.edit { it[FLASH_SCREEN] = flash }
  }

  suspend fun updateFlashlight(flashlight: String) {
    dataStore.edit { it[FLASHLIGHT] = flashlight }
  }

  suspend fun updateKeepAwake(keepAwake: String) {
    dataStore.edit { it[KEEP_AWAKE] = keepAwake }
  }

  suspend fun updateActiveBeat(active: Boolean) {
    dataStore.edit { it[ACTIVE_BEAT] = active }
  }

  suspend fun updatePermNotification(perm: Boolean) {
    dataStore.edit { it[PERM_NOTIFICATION] = perm }
  }

  suspend fun updateShowElapsed(show: Boolean) {
    dataStore.edit { it[SHOW_ELAPSED] = show }
  }

  suspend fun updateBigTimeText(big: Boolean) {
    dataStore.edit { it[BIG_TIME_TEXT] = big }
  }

  suspend fun updateBigLogo(big: Boolean) {
    dataStore.edit { it[BIG_LOGO] = big }
  }

  suspend fun updateCheckUnlockKey(check: Boolean) {
    dataStore.edit { it[CHECK_UNLOCK_KEY] = check }
  }

  suspend fun clearAll() {
    dataStore.edit { it.clear() }
  }

  companion object {
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
    private val LANGUAGE = stringPreferencesKey("language")
  }
}
