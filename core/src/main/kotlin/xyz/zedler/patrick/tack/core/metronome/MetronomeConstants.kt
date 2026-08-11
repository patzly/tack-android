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

package xyz.zedler.patrick.tack.core.metronome

object MetronomeConstants {
  const val TEMPO_MIN = 1
  const val TEMPO_MAX = 600
  const val BEATS_MAX = 20
  const val SUBS_MAX = 10
  const val COUNT_IN_MAX = 4

  const val MUTE_MUTE_MIN = 1
  const val MUTE_MUTE_MIN_BEATS = 0
  const val MUTE_MUTE_MAX = 20
  const val MUTE_MUTE_MAX_BEATS = 100
  const val MUTE_MUTE_STEP_SIZE = 1
  const val MUTE_MUTE_STEP_SIZE_BEATS = 5

  object DurationUnit {
    const val BEATS = "beats"
    const val BARS = "bars"
    const val SECONDS = "seconds"
    const val MINUTES = "minutes"
  }
}
