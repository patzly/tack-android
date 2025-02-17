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
package xyz.zedler.patrick.tack

object Constants {
  const val ANIM_DURATION: Long = 250
  const val BEAT_ANIM_OFFSET: Long = 25
  const val FLASH_SCREEN_DURATION: Long = 100
  const val TEMPO_MIN: Int = 1
  const val TEMPO_MAX: Int = 600
  const val BEATS_MAX: Int = 20
  const val SUBS_MAX: Int = 10
  const val BOOKMARKS_MAX: Int = 10

  object Pref {
    const val TEMPO: String = "tempo"
    const val BEATS: String = "beats"
    const val SUBDIVISIONS: String = "subdivisions"
    const val BEAT_MODE_VIBRATE: String = "beat_mode_vibrate"
    const val ALWAYS_VIBRATE: String = "always_vibrate"
    const val STRONG_VIBRATION: String = "strong_vibration"
    const val FLASH_SCREEN: String = "flash_screen"
    const val KEEP_AWAKE: String = "keep_awake"
    const val REDUCE_ANIM: String = "reduce_animations"
    const val SOUND: String = "sound"
    const val LATENCY: String = "latency_offset"
    const val IGNORE_FOCUS: String = "ignore_focus"
    const val GAIN: String = "gain"
    const val BOOKMARKS: String = "bookmarks"
  }

  object Def {
    const val TEMPO: Int = 120
    const val BEATS: String =
      "${TickType.STRONG},${TickType.NORMAL},${TickType.NORMAL},${TickType.NORMAL}"
    const val SUBDIVISIONS: String = TickType.MUTED
    const val BEAT_MODE_VIBRATE: Boolean = false
    const val ALWAYS_VIBRATE: Boolean = true
    const val STRONG_VIBRATION: Boolean = false
    const val FLASH_SCREEN: Boolean = false
    const val KEEP_AWAKE: Boolean = true
    const val REDUCE_ANIM: Boolean = false
    const val SOUND: String = Sound.SINE
    const val LATENCY: Long = 100
    const val IGNORE_FOCUS: Boolean = false
    const val GAIN: Int = 0
    const val BOOKMARKS: String = ""
  }

  object Sound {
    const val SINE: String = "sine"
    const val WOOD: String = "wood"
    const val MECHANICAL: String = "mechanical"
    const val BEATBOXING_1: String = "beatboxing_1"
    const val BEATBOXING_2: String = "beatboxing_2"
    const val HANDS: String = "hands"
    const val FOLDING: String = "folding"
  }

  object TickType {
    const val NORMAL: String = "normal"
    const val STRONG: String = "strong"
    const val SUB: String = "sub"
    const val MUTED: String = "muted"
  }

  object Action {
    const val STOP: String = "xyz.zedler.patrick.tack.intent.action.STOP"
  }
}
