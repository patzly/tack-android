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

package xyz.zedler.patrick.tack.core.util

import java.util.Locale

object TimeUtil {
  fun getTimeStringFromSeconds(seconds: Int, forceHours: Boolean): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0 || forceHours) {
      String.format(
        Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60
      )
    } else {
      String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds % 60)
    }
  }
}
