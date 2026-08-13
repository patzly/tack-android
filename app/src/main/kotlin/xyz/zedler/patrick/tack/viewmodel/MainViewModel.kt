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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.core.data.MetronomeRepository
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.model.*
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.presentation.navigation.Route
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

  val settings: StateFlow<AppSettings> = settingsRepository.settings
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

  val metronomeConfig: StateFlow<MetronomeConfig> = metronomeRepository.metronomeConfig
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetronomeConfig())

  // Dynamic state from Engine (via Service)
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val metronomeState: StateFlow<MetronomeState> = _service
    .flatMapLatest { service ->
      service?.engine?.state ?: flowOf(MetronomeState())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetronomeState())

  fun refreshUnlockStatus(context: android.content.Context) {
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

  fun clearAll() = viewModelScope.launch { settingsRepository.clearAll() }

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
}
