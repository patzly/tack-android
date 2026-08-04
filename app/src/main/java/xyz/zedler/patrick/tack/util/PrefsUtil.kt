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

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import xyz.zedler.patrick.tack.Constants.BEAT_MODE
import xyz.zedler.patrick.tack.Constants.FLASH_SCREEN
import xyz.zedler.patrick.tack.Constants.KEEP_AWAKE
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.SongDatabase
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.model.MetronomeConfig
import java.util.concurrent.Executors

class PrefsUtil(private val context: Context) {

  private val tag = PrefsUtil::class.java.simpleName
  val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
  private val executorService = Executors.newSingleThreadExecutor()

  fun checkForMigrations(): PrefsUtil {
    migrateBookmarks()
    migrateBeatModeVibrateAndAlwaysVibrate()
    migrateFlashScreen()
    migrateKeepAwake()
    return this
  }

  private fun migrateBookmarks() {
    val bookmarksKey = "bookmarks"
    if (sharedPrefs.contains(bookmarksKey)) {
      // from String to Set<String>
      try {
        sharedPrefs.getStringSet(bookmarksKey, emptySet())
      } catch (e: Exception) {
        sharedPrefs.edit().apply {
          try {
            val prefBookmarks = sharedPrefs.getString(bookmarksKey, "") ?: ""
            val bookmarks = prefBookmarks.split(",").mapNotNull { it.toIntOrNull() }
            remove(bookmarksKey)
            putStringSet(bookmarksKey, bookmarks.map { it.toString() }.toSet())
          } catch (ignore: Exception) {
            remove(bookmarksKey)
          }
          apply()
        }
      }

      // from bookmarks to songs
      val bookmarks = sharedPrefs.getStringSet(
        bookmarksKey, emptySet()
      ) ?: emptySet()
      if (bookmarks.isNotEmpty()) {
        sharedPrefs.edit { remove(bookmarksKey) }
        val db = SongDatabase.getInstance(context.applicationContext)
        bookmarks.forEach { bookmark ->
          try {
            val tempo = bookmark.toInt()
            val songName = context.getString(R.string.label_bpm_value, tempo)
            val song = Song(name = songName)
            val config = MetronomeConfig().apply { this.tempo = tempo }
            val part = Part.fromConfig(null, song.id, 0, config)
            executorService.execute {
              db.songDao().insertSong(song)
              db.songDao().insertPart(part)
            }
            Log.i(tag, "migrateBookmarks: added $song for $bookmark")
          } catch (e: NumberFormatException) {
            Log.e(tag, "migrateBookmarks: bookmark to tempo: ", e)
          }
        }
        // Remove deprecated shortcuts
        ShortcutUtil(context).removeAllShortcuts()
      }
    }
  }

  private fun migrateBeatModeVibrateAndAlwaysVibrate() {
    val beatModeVibrateKeyOld = "beat_mode_vibrate"
    val alwaysVibrateKeyOld = "always_vibrate"
    if (sharedPrefs.contains(beatModeVibrateKeyOld)) {
      sharedPrefs.edit().apply {
        try {
          val currentBeatModeVibrate = sharedPrefs.getBoolean(
            beatModeVibrateKeyOld, false
          )
          val currentAlwaysVibrate = sharedPrefs.getBoolean(
            alwaysVibrateKeyOld, true
          )
          putString(
            PREF.BEAT_MODE,
            when {
              currentBeatModeVibrate -> BEAT_MODE.VIBRATION
              currentAlwaysVibrate -> BEAT_MODE.ALL
              else -> BEAT_MODE.SOUND
            }
          )
        } catch (ignored: ClassCastException) {
        } finally {
          remove(beatModeVibrateKeyOld)
          remove(alwaysVibrateKeyOld)
        }
        apply()
      }
    }
  }

  private fun migrateFlashScreen() {
    val flashScreenKeyOld = "flash_screen"
    if (sharedPrefs.contains(flashScreenKeyOld)) {
      sharedPrefs.edit().apply {
        try {
          val current = sharedPrefs.getBoolean(flashScreenKeyOld, false)
          putString(
            PREF.FLASH_SCREEN, if (current) FLASH_SCREEN.STRONG else FLASH_SCREEN.OFF
          )
        } catch (ignored: ClassCastException) {
        } finally {
          remove(flashScreenKeyOld)
        }
        apply()
      }
    }
  }

  private fun migrateKeepAwake() {
    val keepAwakeKeyOld = "keep_awake"
    if (sharedPrefs.contains(keepAwakeKeyOld)) {
      sharedPrefs.edit().apply {
        try {
          val current = sharedPrefs.getBoolean(keepAwakeKeyOld, true)
          putString(
            PREF.KEEP_AWAKE, if (current) KEEP_AWAKE.WHILE_PLAYING else KEEP_AWAKE.NEVER
          )
        } catch (ignored: ClassCastException) {
        } finally {
          remove(keepAwakeKeyOld)
        }
        apply()
      }
    }
  }

  private fun migrateString(keyOld: String, keyNew: String, def: String) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      sharedPrefs.edit().apply {
        try {
          val current = sharedPrefs.getString(keyOld, def)
          if (current != def) {
            remove(keyOld)
            putString(keyNew, current)
          }
        } catch (ignored: ClassCastException) {
          remove(keyOld)
        }
        apply()
      }
    }
  }

  private fun migrateBoolean(keyOld: String, keyNew: String, def: Boolean) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      sharedPrefs.edit().apply {
        try {
          val current = sharedPrefs.getBoolean(keyOld, def)
          if (current != def) {
            remove(keyOld)
            putBoolean(keyNew, current)
          }
        } catch (ignored: ClassCastException) {
          remove(keyOld)
        }
        apply()
      }
    }
  }

  private fun migrateInteger(keyOld: String, keyNew: String, def: Int) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      sharedPrefs.edit().apply {
        try {
          val current = sharedPrefs.getInt(keyOld, def)
          if (current != def) {
            remove(keyOld)
            putInt(keyNew, current)
          }
        } catch (ignored: ClassCastException) {
          remove(keyOld)
        }
        apply()
      }
    }
  }

  private fun migrateFloat(keyOld: String, keyNew: String, def: Float) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      sharedPrefs.edit().apply {
        try {
          val current = sharedPrefs.getFloat(keyOld, def)
          if (current != def) {
            remove(keyOld)
            putFloat(keyNew, current)
          }
        } catch (ignored: ClassCastException) {
          remove(keyOld)
        }
        apply()
      }
    }
  }

  private fun removePreference(key: String) {
    if (sharedPrefs.contains(key)) {
      sharedPrefs.edit { remove(key) }
    }
  }
}
