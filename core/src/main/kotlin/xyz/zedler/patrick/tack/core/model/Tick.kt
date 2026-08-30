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

package xyz.zedler.patrick.tack.core.model

data class Tick(
  val index: Long,
  val beat: Int,
  val subdivision: Int,
  val type: TickType,
  val isMuted: Boolean,
  val isPoly: Boolean
)

enum class TickType(val key: String) {
  NORMAL("normal"),
  STRONG("strong"),
  SUB("sub"),
  MUTED("muted"),
  BEAT_SUB("beat_sub"),
  BEAT_SUB_MUTED("beat_sub_muted");

  companion object {
    fun fromKey(key: String): TickType = entries.find {
      it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
    } ?: NORMAL
  }
}