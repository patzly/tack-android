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

import java.util.*

class TempoTapUtil {

  private val intervals: Queue<Long> = LinkedList()
  private var previous: Long = 0

  fun tap(): Boolean {
    var enoughData = false
    val current = System.currentTimeMillis()
    if (previous > 0) {
      enoughData = true
      val interval = current - previous
      if (!intervals.isEmpty() && shouldReset(interval)) {
        intervals.clear()
        enoughData = false
      } else if (intervals.size >= MAX_TAPS) {
        intervals.poll()
      }
      intervals.offer(interval)
    }
    previous = current
    return enoughData
  }

  val tempo: Int
    get() = getTempo(average)

  private fun getTempo(interval: Long): Int {
    return if (interval > 0) {
      (60000 / interval).toInt()
    } else {
      0
    }
  }

  private val average: Long
    get() {
      var sum: Long = 0
      for (interval in intervals) {
        sum += interval
      }
      return if (!intervals.isEmpty()) {
        sum / intervals.size
      } else {
        0
      }
    }

  private fun shouldReset(interval: Long): Boolean {
    return getTempo(interval) >= tempo * (1 + TEMPO_FACTOR) || getTempo(
      interval
    ) <= tempo * (1 - TEMPO_FACTOR) || interval > average * INTERVAL_FACTOR
  }

  companion object {
    private const val MAX_TAPS = 20
    private const val TEMPO_FACTOR = 0.5
    private const val INTERVAL_FACTOR = 3
  }
}