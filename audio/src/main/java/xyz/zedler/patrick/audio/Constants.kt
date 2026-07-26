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
package xyz.zedler.patrick.audio

object Constants {

  object Def {
    const val SOUND: String = Sound.SINE
    const val GAIN: Int = 0
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
    const val BEAT_SUB_MUTED: String = "beat_sub_muted"
  }
}
