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
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import xyz.zedler.patrick.tack.Constants.ACTION
import xyz.zedler.patrick.tack.Constants.EXTRA
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.SongActivity
import java.util.concurrent.Executors

class ShortcutUtil(private val context: Context) {

  private var manager: ShortcutManager? = null
  private val executorService = Executors.newSingleThreadExecutor()
  private val mainHandler = Handler(Looper.getMainLooper())

  init {
    if (isSupported) {
      manager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
    }
  }

  fun addShortcut(shortcutInfo: ShortcutInfo) {
    if (!isSupported) return
    hasShortcutAsync(shortcutInfo.id) { hasShortcut ->
      if (isSupported && !hasShortcut) {
        manager?.let {
          if (it.dynamicShortcuts.size < maxShortcutCount) {
            it.addDynamicShortcuts(listOf(shortcutInfo))
          }
        }
      }
    }
  }

  fun addAllShortcuts(shortcuts: List<ShortcutInfo>) {
    if (isSupported) {
      manager?.addDynamicShortcuts(
        shortcuts.take(maxShortcutCount)
      )
    }
  }

  fun removeShortcut(shortcutId: String) {
    hasShortcutAsync(shortcutId) { hasShortcut ->
      if (isSupported && hasShortcut) {
        manager?.removeDynamicShortcuts(listOf(shortcutId))
      }
    }
  }

  fun removeAllShortcuts() {
    if (isSupported) {
      manager?.removeAllDynamicShortcuts()
    }
  }

  fun reportUsage(shortcutId: String) {
    hasShortcutAsync(shortcutId) { hasShortcut ->
      if (isSupported && hasShortcut) {
        manager?.reportShortcutUsed(shortcutId)
      }
    }
  }

  val maxShortcutCount: Int
    get() = if (isSupported) manager?.maxShortcutCountPerActivity ?: 0 else 0

  private fun hasShortcutAsync(shortcutId: String?, callback: (Boolean) -> Unit) {
    if (isSupported) {
      executorService.execute {
        var result = false
        try {
          manager?.let {
            for (info in it.dynamicShortcuts) {
              if (shortcutId == info.id) {
                result = true
                break
              }
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "hasShortcutAsync: ", e)
        }
        mainHandler.post { callback(result) }
      }
    } else {
      mainHandler.post { callback(false) }
    }
  }

  @RequiresApi(Build.VERSION_CODES.N_MR1)
  fun getShortcutInfo(id: String, name: String?): ShortcutInfo {
    return ShortcutInfo.Builder(context, id).apply {
      setShortLabel(name ?: context.getString(R.string.label_song_name))
      setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut))
      setIntent(
        Intent(context, SongActivity::class.java).apply {
          action = ACTION.APPLY_SONG
          putExtra(EXTRA.SONG_ID, id)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
      )
    }.build()
  }

  companion object {
    private val TAG = ShortcutUtil::class.java.simpleName

    @JvmStatic
    val isSupported: Boolean
      get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
  }
}
