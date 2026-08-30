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

package xyz.zedler.patrick.tack.ui.util

import androidx.annotation.StringRes
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.Sound

val Sound.titleRes: Int
  @StringRes get() = when (this) {
    Sound.SINE -> R.string.settings_sound_sine
    Sound.WOOD -> R.string.settings_sound_wood
    Sound.MECHANICAL -> R.string.settings_sound_mechanical
    Sound.BEATBOXING_1 -> R.string.settings_sound_beatboxing_1
    Sound.BEATBOXING_2 -> R.string.settings_sound_beatboxing_2
    Sound.HANDS -> R.string.settings_sound_hands
    Sound.FOLDING -> R.string.settings_sound_folding
  }
