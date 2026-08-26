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

package xyz.zedler.patrick.tack.hardware

import android.content.Context
import xyz.zedler.patrick.tack.core.hardware.UnlockProvider

class UnlockProviderImpl(private val context: Context): UnlockProvider {

  override fun isKeyInstalled(): Boolean {
    return try {
      context.packageManager.getPackageInfo(PACKAGE_KEY, 0)
      true
    } catch (e: Exception) {
      false
    }
  }

  override fun isPlayStoreInstalled(): Boolean {
    return try {
      context.packageManager.getPackageInfo("com.android.vending", 0)
      true
    } catch (_: Exception) {
      false
    }
  }

  override fun isUnlocked(): Boolean {
    return if (isPlayStoreInstalled()) {
      isKeyInstalled()
    } else {
      true
    }
  }

  companion object {
    const val PACKAGE_KEY = "xyz.zedler.patrick.tack.unlock"
  }
}