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

@get:StringRes
val Int.tempoTermResId: Int
  get() = when {
    this < 60 -> R.string.label_tempo_largo
    this < 66 -> R.string.label_tempo_larghetto
    this < 76 -> R.string.label_tempo_adagio
    this < 108 -> R.string.label_tempo_andante
    this < 120 -> R.string.label_tempo_moderato
    this < 168 -> R.string.label_tempo_allegro
    this < 200 -> R.string.label_tempo_presto
    else -> R.string.label_tempo_prestissimo
  }