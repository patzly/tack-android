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
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Unit
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

  private lateinit var repository: SettingsRepository
  private lateinit var appSettings: AppSettingsDataStore
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
    appSettings = AppSettingsDataStore(dataStore)
    repository = SettingsRepository(appSettings)
  }

  @Test
  fun `test metronomeConfig returns default values initially`() = runTest {
    val config = repository.metronomeConfig.first()
    assertEquals(120, config.tempo)
    assertEquals(listOf("strong", "normal", "normal", "normal"), config.beats)
    assertEquals(0, config.countIn)
    assertEquals(Unit.BARS, config.timerUnit)
  }

  @Test
  fun `test updateTempo updates metronomeConfig`() = runTest {
    repository.updateTempo(145)
    val config = repository.metronomeConfig.first()
    assertEquals(145, config.tempo)
  }
}
