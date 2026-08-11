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
