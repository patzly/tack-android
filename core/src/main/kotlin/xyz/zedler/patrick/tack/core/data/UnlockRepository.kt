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

package xyz.zedler.patrick.tack.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import xyz.zedler.patrick.tack.core.hardware.UnlockProvider
import xyz.zedler.patrick.tack.core.model.UnlockState

class UnlockRepository(
  private val dataStore: AppDataStore,
  private val unlockProvider: UnlockProvider
) {
  private val refreshTrigger = MutableStateFlow(0)
  val unlockState: Flow<UnlockState> = combine(
    dataStore.unlockState,
    refreshTrigger
  ) { unlockState, _ ->
    val isKeyInstalled = unlockProvider.isKeyInstalled()
    val isPlayStoreInstalled = unlockProvider.isPlayStoreInstalled()

    UnlockState(
      isKeyInstalled = isKeyInstalled,
      isPlayStoreInstalled = isPlayStoreInstalled,
      checkUnlockKey = unlockState.checkUnlockKey,
      isUnlocked = if (unlockState.checkUnlockKey) unlockProvider.isUnlocked() else true
    )
  }

  fun refresh() {
    refreshTrigger.value++
  }

  suspend fun updateCheckUnlockKey(checkKey: Boolean) {
    dataStore.updateCheckUnlockKey(checkKey)
  }
}
