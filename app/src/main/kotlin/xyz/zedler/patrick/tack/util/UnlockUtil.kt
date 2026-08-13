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

object UnlockUtil {
  private const val PACKAGE_KEY = "xyz.zedler.patrick.tack.unlock"

  fun isKeyInstalled(context: Context): Boolean {
    return try {
      context.packageManager.getPackageInfo(PACKAGE_KEY, 0)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun isPlayStoreInstalled(context: Context): Boolean {
    return try {
      context.packageManager.getPackageInfo("com.android.vending", 0)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun isUnlocked(context: Context): Boolean {
    return if (isPlayStoreInstalled(context)) {
      isKeyInstalled(context)
    } else {
      true
    }
  }
}
