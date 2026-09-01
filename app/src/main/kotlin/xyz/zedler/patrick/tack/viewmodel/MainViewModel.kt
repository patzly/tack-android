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

package xyz.zedler.patrick.tack.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.data.BackupRepository
import xyz.zedler.patrick.tack.core.data.MetronomeRepository
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.data.UnlockRepository
import xyz.zedler.patrick.tack.core.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeConstants
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.core.model.UnlockState
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.ui.navigation.Route
import kotlin.math.roundToInt

sealed interface UiEvent {
  data class ShowToast(val messageResId: Int) : UiEvent
}

sealed interface MainDialog {
  data object GainWarning : MainDialog
  data object NotificationPermission : MainDialog
}

class MainViewModel(
  private val settingsRepository: SettingsRepository,
  private val unlockRepository: UnlockRepository,
  private val metronomeRepository: MetronomeRepository,
  private val songRepository: SongRepository,
  private val backupRepository: BackupRepository
) : ViewModel() {

  private val _service = MutableStateFlow<MetronomeService?>(null)

  val backstack = mutableStateListOf<Route>(Route.Main)

  private val _uiEvent = MutableSharedFlow<UiEvent>()
  val uiEvent = _uiEvent.asSharedFlow()

  private val _dialogState = MutableStateFlow<MainDialog?>(null)
  val dialogState: StateFlow<MainDialog?> = _dialogState.asStateFlow()

  private var neverStartedWithGain = true

  // Session State (Single Source of Truth)
  private val _activeSongId = MutableStateFlow<String?>(null)
  val activeSongId: StateFlow<String?> = _activeSongId.asStateFlow()

  private val _activePartIndex = MutableStateFlow(0)
  val activePartIndex: StateFlow<Int> = _activePartIndex.asStateFlow()

  private val _metronomeConfig = MutableStateFlow(MetronomeConfig())
  val metronomeConfig: StateFlow<MetronomeConfig> = _metronomeConfig.asStateFlow()

  init {
    viewModelScope.launch {
      val lastSongId = metronomeRepository.activeSongId.first()
      val lastPartIndex = metronomeRepository.activePartIndex.first()

      if (lastSongId != null) {
        loadSongPart(lastSongId, lastPartIndex)
      } else {
        val savedConfig = metronomeRepository.metronomeConfig.first()
        _metronomeConfig.value = savedConfig
        _service.value?.engine?.applyConfig(savedConfig)
      }
    }
  }

  val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    AppSettings()
  )

  val unlockState: StateFlow<UnlockState> = unlockRepository.unlockState.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    UnlockState()
  )

  // Dynamic state from Engine (via Service)
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val metronomeState: StateFlow<MetronomeState> = _service
    .flatMapLatest { service ->
      service?.engine?.state ?: flowOf(MetronomeState())
    }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      MetronomeState()
    )

  // General UI

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

  fun onDismissDialog() {
    _dialogState.value = null
  }

  // Settings / Backup

  fun updateSettings(settings: AppSettings) {
    viewModelScope.launch {
      settingsRepository.updateSettings(settings)
    }
  }

  fun updateCheckUnlockKey(checkKey: Boolean) {
    viewModelScope.launch {
      unlockRepository.updateCheckUnlockKey(checkKey)
    }
  }

  fun refreshUnlockState() {
    unlockRepository.refresh()
  }

  fun resetAll() {
    viewModelScope.launch {
      _service.value?.engine?.stop()
      settingsRepository.clearAll()
      songRepository.deleteAllSongs()
      metronomeRepository.updateActiveSong(null)
      // TODO: ShortcutUtil(context).removeAllShortcuts() when migrated
    }
  }

  fun exportLibrary(uri: Uri?) {
    viewModelScope.launch {
      if (uri == null) {
        _uiEvent.emit(
          UiEvent.ShowToast(R.string.msg_backup_directory_missing)
        )
        return@launch
      }
      backupRepository.exportLibrary(uri)
        .onSuccess {
          _uiEvent.emit(UiEvent.ShowToast(R.string.msg_backup_success))
        }
        .onFailure {
          _uiEvent.emit(UiEvent.ShowToast(R.string.msg_backup_error))
        }
    }
  }

  fun importLibrary(context: Context, uri: Uri?) {
    viewModelScope.launch {
      if (uri == null) {
        _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_file_missing))
        return@launch
      }
      backupRepository.importLibrary(uri) { originalName, counter ->
        context.getString(
          R.string.msg_restore_duplicate_name, originalName, counter
        )
      }
        .onSuccess {
          _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_success))
        }
        .onFailure {
          _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_error))
        }
    }
  }

  // Metronome / Engine

  fun onServiceConnected(service: MetronomeService) {
    _service.value = service
    val engine = service.engine

    val activeSongId = _activeSongId.value
    if (activeSongId != null) {
      loadSongPart(activeSongId, _activePartIndex.value)
    } else {
      engine.applyConfig(_metronomeConfig.value)
      engine.setPlaylist(emptyList(), false)
    }

    viewModelScope.launch {
      engine.engineEvent.collect { event ->
        when (event) {
          is MetronomeEngine.EngineEvent.AutoTempoChange -> {
            val current = _metronomeConfig.value
            _metronomeConfig.value = current.copy(tempo = event.newTempo)

            if (_activeSongId.value == null) {
              metronomeRepository.updateMetronomeConfig(_metronomeConfig.value)
            }
          }
          is MetronomeEngine.EngineEvent.PartChange -> {
            _activePartIndex.value = event.partIndex
            _metronomeConfig.value = event.config

            _activeSongId.value?.let { songId ->
              viewModelScope.launch {
                metronomeRepository.updateActiveSong(songId, event.partIndex)
              }
            }
          }
          is MetronomeEngine.EngineEvent.PlaylistEnd -> {}
        }
      }
    }
  }

  fun onServiceDisconnected() {
    _service.value = null
  }

  fun loadSongPart(songId: String, partIndex: Int) {
    viewModelScope.launch {
      val songWithParts = songRepository.getSongWithPartsAsync(songId)

      if (songWithParts != null && songWithParts.parts.isNotEmpty()) {
        val safeIndex = partIndex.coerceIn(0, songWithParts.parts.size - 1)

        val speedModifier = songWithParts.song.speed / 100.0
        val playlist = songWithParts.parts.map { part ->
          val config = part.toConfig()
          val effectiveTempo = (config.tempo * speedModifier).roundToInt()
            .coerceIn(MetronomeConstants.TEMPO_MIN, MetronomeConstants.TEMPO_MAX)
          config.copy(tempo = effectiveTempo)
        }

        _activeSongId.value = songId
        _activePartIndex.value = safeIndex

        _metronomeConfig.value = playlist[safeIndex]

        _service.value?.engine?.setPlaylist(
          configs = playlist,
          isLooped = songWithParts.song.isLooped,
          partIndex = safeIndex,
          startPlaying = false
        )

        metronomeRepository.updateActiveSong(songId, safeIndex)
      }
    }
  }

  fun unloadSong() {
    viewModelScope.launch {
      _activeSongId.value = null
      _activePartIndex.value = 0
      metronomeRepository.updateActiveSong(null)

      val fallbackConfig = metronomeRepository.metronomeConfig.first()
      _metronomeConfig.value = fallbackConfig

      _service.value?.engine?.applyConfig(fallbackConfig)
      _service.value?.engine?.setPlaylist(emptyList(), false)
    }
  }

  fun updateMetronomeConfig(config: MetronomeConfig) {
    _metronomeConfig.value = config
    _service.value?.engine?.applyConfig(config)

    if (_activeSongId.value == null) {
      viewModelScope.launch {
        metronomeRepository.updateMetronomeConfig(config)
      }
    }
  }

  fun changeTempo(delta: Int): Boolean {
    val currentConfig = _metronomeConfig.value
    val currentTempo = currentConfig.tempo

    val newTempo = (currentTempo + delta).coerceIn(
      MetronomeConstants.TEMPO_MIN, MetronomeConstants.TEMPO_MAX
    )

    return if (newTempo != currentTempo) {
      updateMetronomeConfig(currentConfig.copy(tempo = newTempo))
      true
    } else {
      false
    }
  }

  fun startMetronome() {
    neverStartedWithGain = false
    _service.value?.engine?.start()
  }

  fun stopMetronome() {
    _service.value?.engine?.stop()
  }

  fun requestTogglePlay(hasPermission: Boolean): Boolean {
    if (metronomeState.value.isPlaying) {
      stopMetronome()
      return false
    } else {
      if (settings.value.gain > 0 && neverStartedWithGain) {
        _dialogState.value = MainDialog.GainWarning
        return false
      }
      if (hasPermission || settings.value.notificationPermissionDenied) {
        startMetronome()
        return true
      } else {
        _dialogState.value = MainDialog.NotificationPermission
        return false
      }
    }
  }

  fun onConfirmGainWarning(hasPermission: Boolean, deactivateGain: Boolean) {
    _dialogState.value = null
    neverStartedWithGain = false
    if (deactivateGain) {
      updateSettings(settings.value.copy(gain = 0))
    }

    if (hasPermission || settings.value.notificationPermissionDenied) {
      startMetronome()
    } else {
      _dialogState.value = MainDialog.NotificationPermission
    }
  }

  fun onDismissPermissionDialog(proceedAnyway: Boolean, dontAskAgain: Boolean) {
    _dialogState.value = null
    if (dontAskAgain) {
      updateSettings(settings.value.copy(notificationPermissionDenied = true))
    }
    if (proceedAnyway) {
      startMetronome()
    }
  }

  class Factory(
    private val settingsRepository: SettingsRepository,
    private val unlockRepository: UnlockRepository,
    private val metronomeRepository: MetronomeRepository,
    private val songRepository: SongRepository,
    private val backupRepository: BackupRepository
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MainViewModel(
        settingsRepository,
        unlockRepository,
        metronomeRepository,
        songRepository,
        backupRepository
      ) as T
    }
  }
}
