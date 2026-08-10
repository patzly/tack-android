package xyz.zedler.patrick.tack.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppTheme
import xyz.zedler.patrick.tack.core.model.MetronomeConfig

class SettingsRepository(private val dataStore: AppSettingsDataStore) {

  val metronomeConfig: Flow<MetronomeConfig> = combine(
    dataStore.tempo,
    dataStore.beats,
    dataStore.subdivisions,
    dataStore.usePolyrhythm,
    dataStore.countIn,
    dataStore.incrementalAmount,
    dataStore.incrementalInterval,
    dataStore.incrementalLimit,
    dataStore.incrementalUnit,
    dataStore.incrementalIncrease,
    dataStore.timerDuration,
    dataStore.timerUnit,
    dataStore.mutePlay,
    dataStore.muteMute,
    dataStore.muteUnit,
    dataStore.muteRandom
  ) { args: Array<Any?> ->
    MetronomeConfig(
      tempo = args[0] as Int,
      beats = (args[1] as String).split(","),
      subdivisions = (args[2] as String).split(","),
      usePolyrhythm = args[3] as Boolean,
      countIn = args[4] as Int,
      incrementalAmount = args[5] as Int,
      incrementalInterval = args[6] as Int,
      incrementalLimit = args[7] as Int,
      incrementalUnit = args[8] as String,
      incrementalIncrease = args[9] as Boolean,
      timerDuration = args[10] as Int,
      timerUnit = args[11] as String,
      mutePlay = args[12] as Int,
      muteMute = args[13] as Int,
      muteUnit = args[14] as String,
      muteRandom = args[15] as Boolean
    )
  }

  val useDynamicColors = dataStore.useDynamicColors
  val themeHue = dataStore.themeHue
  val theme = dataStore.theme
  val contrast = dataStore.contrast
  val haptic = dataStore.haptic
  val vibrationIntensity = dataStore.vibrationIntensity
  val reduceAnim = dataStore.reduceAnim
  val beatMode = dataStore.beatMode
  val showElapsed = dataStore.showElapsed
  val resetTimerOnStop = dataStore.resetTimerOnStop
  val flashScreen = dataStore.flashScreen
  val flashlight = dataStore.flashlight
  val keepAwake = dataStore.keepAwake
  val sound = dataStore.sound
  val latency = dataStore.latency
  val ignoreFocus = dataStore.ignoreFocus
  val gain = dataStore.gain
  val permNotification = dataStore.permNotification
  val activeBeat = dataStore.activeBeat
  val bigTimeText = dataStore.bigTimeText
  val bigLogo = dataStore.bigLogo
  val checkUnlockKey = dataStore.checkUnlockKey

  val songsOrder = dataStore.songsOrder
  val currentSongId = dataStore.currentSongId
  val currentPartIndex = dataStore.currentPartIndex


  suspend fun updateUseDynamicColors(use: Boolean) = dataStore.updateUseDynamicColors(use)

  suspend fun updateThemeHue(hue: Float) = dataStore.updateThemeHue(hue)

  suspend fun updateTheme(theme: AppTheme) = dataStore.updateTheme(theme)

  suspend fun updateContrast(contrast: AppContrast) = dataStore.updateContrast(contrast)

  suspend fun updateHaptic(enabled: Boolean) = dataStore.updateHaptic(enabled)

  suspend fun updateVibrationIntensity(intensity: String) =
    dataStore.updateVibrationIntensity(intensity)

  suspend fun updateReduceAnim(reduce: Boolean) = dataStore.updateReduceAnim(reduce)

  suspend fun updateTempo(tempo: Int) = dataStore.updateTempo(tempo)

  suspend fun updateSound(sound: String) = dataStore.updateSound(sound)

  suspend fun updateIgnoreFocus(ignore: Boolean) = dataStore.updateIgnoreFocus(ignore)

  suspend fun updateGain(gain: Int) = dataStore.updateGain(gain)

  suspend fun updateLatency(latency: Long) = dataStore.updateLatency(latency)

  suspend fun updateResetTimerOnStop(reset: Boolean) = dataStore.updateResetTimerOnStop(reset)

  suspend fun updateFlashScreen(flash: String) = dataStore.updateFlashScreen(flash)

  suspend fun updateFlashlight(flashlight: String) = dataStore.updateFlashlight(flashlight)

  suspend fun updateKeepAwake(keepAwake: String) = dataStore.updateKeepAwake(keepAwake)

  suspend fun updateActiveBeat(active: Boolean) = dataStore.updateActiveBeat(active)

  suspend fun updatePermNotification(perm: Boolean) = dataStore.updatePermNotification(perm)

  suspend fun updateShowElapsed(show: Boolean) = dataStore.updateShowElapsed(show)

  suspend fun updateBigTimeText(big: Boolean) = dataStore.updateBigTimeText(big)

  suspend fun updateBigLogo(big: Boolean) = dataStore.updateBigLogo(big)

  suspend fun updateCheckUnlockKey(check: Boolean) = dataStore.updateCheckUnlockKey(check)

  suspend fun clearAll() = dataStore.clearAll()
}
