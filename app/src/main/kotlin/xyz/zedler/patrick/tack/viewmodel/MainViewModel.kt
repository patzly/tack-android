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
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.core.model.UnlockState
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.ui.navigation.Route

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

  val metronomeConfig: StateFlow<MetronomeConfig> = metronomeRepository.metronomeConfig.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    MetronomeConfig()
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

  // general UI

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

  // settings

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
          R.string.msg_restore_duplicate_name,originalName, counter
        )
      }
        .onSuccess { _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_success)) }
        .onFailure { _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_error)) }
    }
  }

  // metronome

  fun onServiceConnected(service: MetronomeService) {
    _service.value = service
  }

  fun onServiceDisconnected() {
    _service.value = null
  }

  fun updateMetronomeConfig(config: MetronomeConfig) {
    viewModelScope.launch {
      metronomeRepository.updateMetronomeConfig(config)
    }
  }

  fun stopMetronome() {
    _service.value?.engine?.stop()
  }

  fun startMetronome() {
    neverStartedWithGain = false
    _service.value?.engine?.start()
  }

  fun requestTogglePlay(hasPermission: Boolean): Boolean {
    if (metronomeState.value.isPlaying) {
      stopMetronome()
      return false
    } else {
      // 1. Gain-Check
      if (settings.value.gain > 0 && neverStartedWithGain) {
        _dialogState.value = MainDialog.GainWarning
        return false
      }
      // 2. Permission-Check
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

    // Nach dem Gain-Dialog direkt die Permissions evaluieren
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
