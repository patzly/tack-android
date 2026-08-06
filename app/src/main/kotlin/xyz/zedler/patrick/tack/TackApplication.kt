package xyz.zedler.patrick.tack

import android.app.Application
import xyz.zedler.patrick.tack.core.data.AppSettingsDataStore
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.database.SongDatabase

class TackApplication : Application() {

  val database by lazy { SongDatabase.getInstance(this) }
  val songRepository by lazy { SongRepository(database.songDao()) }

  private val dataStore by lazy { AppSettingsDataStore(this) }
  val settingsRepository by lazy { SettingsRepository(dataStore) }
}
