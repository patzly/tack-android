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
import kotlinx.serialization.json.Json
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.data.MetronomeRepository
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.database.relations.SongWithParts
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.ui.navigation.Route
import xyz.zedler.patrick.tack.util.UnlockUtil

class MainViewModel(
  private val settingsRepository: SettingsRepository,
  private val metronomeRepository: MetronomeRepository,
  private val songRepository: SongRepository
) : ViewModel() {

  private val _service = MutableStateFlow<MetronomeService?>(null)
  
  val backstack = mutableStateListOf<Route>(Route.Main)

  private val _isKeyInstalled = MutableStateFlow(false)
  val isKeyInstalled: StateFlow<Boolean> = _isKeyInstalled.asStateFlow()

  private val _isPlayStoreInstalled = MutableStateFlow(true)
  val isPlayStoreInstalled: StateFlow<Boolean> = _isPlayStoreInstalled.asStateFlow()

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

  fun init(context: android.content.Context) {
    _isKeyInstalled.value = UnlockUtil.isKeyInstalled(context)
    _isPlayStoreInstalled.value = UnlockUtil.isPlayStoreInstalled(context)
  }

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

  fun exportLibrary(context: android.content.Context, uri: android.net.Uri?) {
    viewModelScope.launch {
      if (uri == null) {
        _uiEvent.emit(UiEvent.ShowToast(R.string.msg_backup_directory_missing))
        return@launch
      }
      try {
        val songs = songRepository.getAllSongsWithPartsAsync().filter {
          it.song.id != SongRepository.SONG_ID_DEFAULT
        }
        val jsonString = json.encodeToString(songs)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
          outputStream.write(jsonString.toByteArray())
        }
        _uiEvent.emit(UiEvent.ShowToast(R.string.msg_backup_success))
      } catch (_: Exception) {
        _uiEvent.emit(UiEvent.ShowToast(R.string.msg_backup_error))
      }
    }
  }

  fun importLibrary(context: android.content.Context, uri: android.net.Uri?) {
    viewModelScope.launch {
      if (uri == null) {
        _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_file_missing))
        return@launch
      }
      try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val jsonString = inputStream.bufferedReader().readText()
          val songsWithParts: List<SongWithParts> = json.decodeFromString(jsonString)

          val existingSongs = songRepository.getAllSongsWithPartsAsync()
          val nameCountMap = mutableMapOf<String, Int>()
          val idNameMap = mutableMapOf<String, String>()

          for (existing in existingSongs) {
            idNameMap[existing.song.id] = existing.song.name ?: ""
            val name = existing.song.name ?: continue
            if (name.isNotEmpty()) {
              nameCountMap[name] = (nameCountMap[name] ?: 0) + 1
            }
          }

          for (songWithParts in songsWithParts) {
            val songId = songWithParts.song.id
            if (idNameMap.containsKey(songId)) {
              songWithParts.song.name = idNameMap[songId]
            } else {
              val originalName = songWithParts.song.name ?: ""
              var newName = originalName
              var counter = nameCountMap[originalName] ?: 0
              if (counter > 0) {
                do {
                  newName = context.getString(
                    R.string.msg_restore_duplicate_name, originalName, counter
                  )
                  counter++
                } while (nameCountMap.containsKey(newName))
              }
              songWithParts.song.name = newName
              nameCountMap[newName] = 1
            }
          }

          songRepository.insertSongsWithParts(songsWithParts)
          _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_success))
          // TODO: MetronomeService.updateShortcuts(context) when migrated
          // TODO: WidgetUtil.sendSongsWidgetUpdate(context) when migrated
        }
      } catch (_: Exception) {
        _uiEvent.emit(UiEvent.ShowToast(R.string.msg_restore_error))
      }
    }
  }

  fun togglePlay() {
    _service.value?.let { service ->
      if (service.engine.state.value.isPlaying) service.engine.stop() else service.engine.start()
    }
  }

  class Factory(
    private val settingsRepository: SettingsRepository,
    private val metronomeRepository: MetronomeRepository,
    private val songRepository: SongRepository
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MainViewModel(settingsRepository, metronomeRepository, songRepository) as T
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
