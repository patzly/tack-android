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

package xyz.zedler.patrick.tack

import android.app.Application
import xyz.zedler.patrick.tack.core.data.AppDataStore
import xyz.zedler.patrick.tack.core.data.BackupRepository
import xyz.zedler.patrick.tack.core.data.MetronomeRepository
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.data.UnlockRepository
import xyz.zedler.patrick.tack.core.database.SongDatabase
import xyz.zedler.patrick.tack.hardware.HapticProviderImpl
import xyz.zedler.patrick.tack.hardware.UnlockProviderImpl

class TackApplication : Application() {

  private val database by lazy { SongDatabase.getInstance(this) }
  val songRepository by lazy { SongRepository(database.songDao()) }
  val backupRepository by lazy { BackupRepository(this, songRepository) }

  val hapticProvider by lazy { HapticProviderImpl(this) }
  val unlockProvider by lazy { UnlockProviderImpl(this) }

  private val dataStore by lazy { AppDataStore(this) }

  val settingsRepository by lazy { SettingsRepository(dataStore, hapticProvider) }
  val unlockRepository by lazy { UnlockRepository(dataStore, unlockProvider) }
  val metronomeRepository by lazy { MetronomeRepository(dataStore) }
}
