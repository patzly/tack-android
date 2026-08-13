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

package xyz.zedler.patrick.tack.presentation.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Route : Parcelable {
  @Parcelize data object Main : Route
  @Parcelize data object About : Route
  @Parcelize data object Settings : Route
  @Parcelize data object Log : Route
  @Parcelize data object Songs : Route
  @Parcelize data class Song(val songId: String) : Route
}
