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

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

  private lateinit var repository: SettingsRepository
  private lateinit var appDataStore: AppDataStore
  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    val dataStoreFile = File(context.filesDir, "test_settings_repo.preferences_pb")
    dataStoreFile.delete()

    val dataStore = PreferenceDataStoreFactory.create(
      scope = TestScope(UnconfinedTestDispatcher()),
      produceFile = { dataStoreFile }
    )
    appDataStore = AppDataStore(dataStore)
    repository = SettingsRepository(appDataStore)
  }

  @Test
  fun `test settings returns default values initially`() = runTest {
    val settings = repository.settings.first()
    assertEquals(true, settings.useDynamicColors)
    assertEquals(200f, settings.themeHue)
    assertEquals(true, settings.haptic)
  }

  @Test
  fun `test updateSettings updates settings`() = runTest {
    val settings = repository.settings.first()
    repository.updateSettings(settings.copy(themeHue = 150f))
    val updated = repository.settings.first()
    assertEquals(150f, updated.themeHue)
  }
}
