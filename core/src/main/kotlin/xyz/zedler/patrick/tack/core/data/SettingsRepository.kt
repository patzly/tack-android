package xyz.zedler.patrick.tack.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

  val theme = dataStore.theme
  val contrast = dataStore.contrast
  val haptic = dataStore.haptic
  val vibrationIntensity = dataStore.vibrationIntensity
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
  val useDynamicColors = dataStore.useDynamicColors
  val themeHue = dataStore.themeHue

  val songsOrder = dataStore.songsOrder
  val currentSongId = dataStore.currentSongId
  val currentPartIndex = dataStore.currentPartIndex

  suspend fun updateTempo(tempo: Int) = dataStore.updateTempo(tempo)

  suspend fun updateUseDynamicColors(use: Boolean) = dataStore.updateUseDynamicColors(use)

  suspend fun updateThemeHue(hue: Float) = dataStore.updateThemeHue(hue)

  // TODO: Add other update functions
}
