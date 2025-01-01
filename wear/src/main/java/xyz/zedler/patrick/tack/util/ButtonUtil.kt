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
 * Copyright (c) 2020-2025 by Patrick Zedler
 */
package xyz.zedler.patrick.tack.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.wear.input.WearableButtons

class ButtonUtil(context: Context?, private val listener: OnPressListener) {

  private val hasMinTwoButtons = WearableButtons.getButtonCount(context!!) >= 2
  private var isDown = false
  private var nextRun = 400
  private var handler: Handler? = null
  private val runnable: Runnable = object : Runnable {
    override fun run() {
      listener.onFastPress()
      handler!!.postDelayed(this, nextRun.toLong())
      if (nextRun > 60) {
        nextRun = (nextRun * 0.9).toInt()
      }
    }
  }

  fun onPressDown() {
    if (isDown || !hasMinTwoButtons) {
      return
    }
    isDown = true
    if (handler != null) {
      handler!!.removeCallbacks(runnable)
    }
    listener.onPress()
    handler = Handler(Looper.getMainLooper())
    handler!!.postDelayed(runnable, 800)
  }

  fun onPressUp() {
    if (!hasMinTwoButtons || !isDown) {
      return
    }
    isDown = false
    if (handler != null) {
      handler!!.removeCallbacks(runnable)
    }
    handler = null
    nextRun = 400
  }

  interface OnPressListener {
    fun onPress()
    fun onFastPress()
  }
}
