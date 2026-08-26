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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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

  sealed class UiEvent {
    data class ShowToast(val messageResId: Int) : UiEvent()
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

  fun updateMetronomeConfig(config: MetronomeConfig) {
    viewModelScope.launch {
      metronomeRepository.updateMetronomeConfig(config)
    }
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

  fun togglePlay() {
    _service.value?.let { service ->
      if (service.engine.state.value.isPlaying) service.engine.stop() else service.engine.start()
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

  companion object {
    private val json = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
      prettyPrint = true
    }
  }
}
