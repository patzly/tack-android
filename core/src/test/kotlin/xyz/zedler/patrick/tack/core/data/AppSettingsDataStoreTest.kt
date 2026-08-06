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
class AppSettingsDataStoreTest {

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

    val appSettings = AppSettingsDataStore(dataStore)

    assertEquals(150, appSettings.tempo.first())
    assertEquals(false, appSettings.haptic.first())
    assertEquals("vibration", appSettings.beatMode.first())
    assertEquals(2, appSettings.countIn.first())
    assertEquals("seconds", appSettings.timerUnit.first())
    assertEquals(30, appSettings.timerDuration.first())
    assertEquals("strong,normal,normal", appSettings.beats.first())
    assertEquals("sub,sub", appSettings.subdivisions.first())
    assertEquals(5, appSettings.gain.first())
    assertEquals(true, appSettings.permNotification.first())
  }

  @Test
  fun `test default values`() = runTest {
    val dataStoreFile = File(context.filesDir, "test_datastore_defaults.preferences_pb")
    dataStoreFile.delete()

    val dataStore = PreferenceDataStoreFactory.create(
      scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    ) { dataStoreFile }

    val appSettings = AppSettingsDataStore(dataStore)
    assertEquals(120, appSettings.tempo.first())
    assertEquals(true, appSettings.haptic.first())
    assertEquals("all", appSettings.beatMode.first())
  }

  @Test
  fun `test update tempo`() = runTest {
    val dataStoreFile = File(context.filesDir, "test_datastore_update.preferences_pb")
    dataStoreFile.delete()

    val dataStore = PreferenceDataStoreFactory.create(
      scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    ) { dataStoreFile }

    val appSettings = AppSettingsDataStore(dataStore)
    appSettings.updateTempo(140)
    assertEquals(140, appSettings.tempo.first())
  }
}
