package xyz.zedler.patrick.tack.viewmodel

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

class MainViewModel(
  private val settingsRepository: SettingsRepository,
  private val songRepository: SongRepository
) : ViewModel() {

  private val _service = MutableStateFlow<MetronomeService?>(null)
  
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

  fun onServiceConnected(service: MetronomeService) {
    _service.value = service
  }

  fun onServiceDisconnected() {
    _service.value = null
  }

  fun togglePlay() {
    _service.value?.let { service ->
      if (service.engine.state.value.isPlaying) {
        service.engine.stop()
      } else {
        service.engine.start()
      }
    }
  }

  fun updateTempo(tempo: Int) {
    viewModelScope.launch {
      settingsRepository.updateTempo(tempo)
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
