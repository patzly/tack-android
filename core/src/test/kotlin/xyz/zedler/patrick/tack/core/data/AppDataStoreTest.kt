package xyz.zedler.patrick.tack.core.data

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
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
class AppDataStoreTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
  }

  @Test
  fun `test migration from shared preferences`() = runTest {
    val sharedPrefsName = "migration_test_prefs"
    val sharedPrefs = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
    sharedPrefs.edit()
      .putInt("tempo", 150)
      .putBoolean("haptic_feedback", false)
      .putString("beat_mode", "vibration")
      .putInt("count_in", 2)
      .putString("timer_unit", "seconds")
      .putInt("timer_duration", 30)
      .putString("beats", "strong,normal,normal")
      .putString("subdivisions", "sub,sub")
      .putInt("gain", 5)
      .putBoolean("permanent_notification", true)
      .commit()

    val dataStoreFile = File(context.filesDir, "test_datastore_migration.preferences_pb")
    dataStoreFile.delete()

    val dataStore = PreferenceDataStoreFactory.create(
      scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
      migrations = listOf(SharedPreferencesMigration(context, sharedPrefsName))
    ) { dataStoreFile }

    val appDataStore = AppDataStore(dataStore)
    val config = appDataStore.metronomeConfig.first()
    val settings = appDataStore.settings.first()

    assertEquals(150, config.tempo)
    assertEquals(false, settings.haptic)
    assertEquals("vibration", settings.beatMode)
    assertEquals(2, config.countIn)
    assertEquals("seconds", config.timerUnit)
    assertEquals(30, config.timerDuration)
    assertEquals(listOf("strong", "normal", "normal"), config.beats)
    assertEquals(listOf("sub", "sub"), config.subdivisions)
    assertEquals(5, settings.gain)
    assertEquals(true, settings.permNotification)
  }

  @Test
  fun `test default values`() = runTest {
    val dataStoreFile = File(context.filesDir, "test_datastore_defaults.preferences_pb")
    dataStoreFile.delete()

    val dataStore = PreferenceDataStoreFactory.create(
      scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    ) { dataStoreFile }

    val appDataStore = AppDataStore(dataStore)
    val config = appDataStore.metronomeConfig.first()
    val settings = appDataStore.settings.first()
    
    assertEquals(120, config.tempo)
    assertEquals(true, settings.haptic)
    assertEquals("all", settings.beatMode)
  }

  @Test
  fun `test update tempo`() = runTest {
    val dataStoreFile = File(context.filesDir, "test_datastore_update.preferences_pb")
    dataStoreFile.delete()

    val dataStore = PreferenceDataStoreFactory.create(
      scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    ) { dataStoreFile }

    val appDataStore = AppDataStore(dataStore)
    val config = appDataStore.metronomeConfig.first()
    appDataStore.updateMetronomeConfig(config.copy(tempo = 140))
    
    assertEquals(140, appDataStore.metronomeConfig.first().tempo)
  }
}
