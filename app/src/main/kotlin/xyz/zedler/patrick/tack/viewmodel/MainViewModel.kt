package xyz.zedler.patrick.tack.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.ui.navigation.Route

class MainViewModel(
  private val settingsRepository: SettingsRepository,
  private val songRepository: SongRepository
) : ViewModel() {

  private val _service = MutableStateFlow<MetronomeService?>(null)
  
  val backstack = mutableStateListOf<Route>(Route.Main)

  // Static config from DataStore
  val metronomeConfig: StateFlow<MetronomeConfig> = settingsRepository.metronomeConfig
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetronomeConfig())

  // Dynamic state from Engine (via Service)
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val metronomeState: StateFlow<MetronomeState> = _service
    .flatMapLatest { service ->
      service?.engine?.state ?: flowOf(MetronomeState())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetronomeState())

  // General Settings
  val useDynamicColors = settingsRepository.useDynamicColors
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
  val themeHue = settingsRepository.themeHue
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200f)
  val theme = settingsRepository.theme
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
  val contrast = settingsRepository.contrast
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "standard")
  val haptic = settingsRepository.haptic
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
  val vibrationIntensity = settingsRepository.vibrationIntensity
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")
  val reduceAnim = settingsRepository.reduceAnim
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  // Metronome Settings
  val sound = settingsRepository.sound
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sine")
  val ignoreFocus = settingsRepository.ignoreFocus
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
  val gain = settingsRepository.gain
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
  val latency = settingsRepository.latency
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
  val resetTimerOnStop = settingsRepository.resetTimerOnStop
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
  val flashScreen = settingsRepository.flashScreen
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "off")
  val flashlight = settingsRepository.flashlight
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "off")
  val keepAwake = settingsRepository.keepAwake
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "while_playing")

  // Controls Settings
  val activeBeat = settingsRepository.activeBeat
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
  val permNotification = settingsRepository.permNotification
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
  val showElapsed = settingsRepository.showElapsed
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
  val bigTimeText = settingsRepository.bigTimeText
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
  val bigLogo = settingsRepository.bigLogo
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  fun onServiceConnected(service: MetronomeService) {
    _service.value = service
  }

  fun onServiceDisconnected() {
    _service.value = null
  }

  fun navigateTo(route: Route) {
    backstack.add(route)
  }

  fun popBackstack(): Boolean {
    if (backstack.size > 1) {
      backstack.removeAt(backstack.size - 1)
      return true
    }
    return false
  }

  // Update functions
  fun updateTempo(tempo: Int) = viewModelScope.launch { settingsRepository.updateTempo(tempo) }
  fun updateUseDynamicColors(use: Boolean) = viewModelScope.launch { settingsRepository.updateUseDynamicColors(use) }
  fun updateThemeHue(hue: Float) = viewModelScope.launch { settingsRepository.updateThemeHue(hue) }
  fun updateTheme(theme: String) = viewModelScope.launch { settingsRepository.updateTheme(theme) }
  fun updateContrast(contrast: String) = viewModelScope.launch { settingsRepository.updateContrast(contrast) }
  fun updateHaptic(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateHaptic(enabled) }
  fun updateVibrationIntensity(intensity: String) = viewModelScope.launch { settingsRepository.updateVibrationIntensity(intensity) }
  fun updateReduceAnim(reduce: Boolean) = viewModelScope.launch { settingsRepository.updateReduceAnim(reduce) }
  fun updateSound(sound: String) = viewModelScope.launch { settingsRepository.updateSound(sound) }
  fun updateIgnoreFocus(ignore: Boolean) = viewModelScope.launch { settingsRepository.updateIgnoreFocus(ignore) }
  fun updateGain(gain: Int) = viewModelScope.launch { settingsRepository.updateGain(gain) }
  fun updateLatency(latency: Long) = viewModelScope.launch { settingsRepository.updateLatency(latency) }
  fun updateResetTimerOnStop(reset: Boolean) = viewModelScope.launch { settingsRepository.updateResetTimerOnStop(reset) }
  fun updateFlashScreen(flash: String) = viewModelScope.launch { settingsRepository.updateFlashScreen(flash) }
  fun updateFlashlight(flashlight: String) = viewModelScope.launch { settingsRepository.updateFlashlight(flashlight) }
  fun updateKeepAwake(keepAwake: String) = viewModelScope.launch { settingsRepository.updateKeepAwake(keepAwake) }
  fun updateActiveBeat(active: Boolean) = viewModelScope.launch { settingsRepository.updateActiveBeat(active) }
  fun updatePermNotification(perm: Boolean) = viewModelScope.launch { settingsRepository.updatePermNotification(perm) }
  fun updateShowElapsed(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowElapsed(show) }
  fun updateBigTimeText(big: Boolean) = viewModelScope.launch { settingsRepository.updateBigTimeText(big) }
  fun updateBigLogo(big: Boolean) = viewModelScope.launch { settingsRepository.updateBigLogo(big) }
  fun clearAll() = viewModelScope.launch { settingsRepository.clearAll() }

  fun togglePlay() {
    _service.value?.let { service ->
      if (service.engine.state.value.isPlaying) service.engine.stop() else service.engine.start()
    }
  }

  class Factory(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MainViewModel(settingsRepository, songRepository) as T
    }
  }
}
