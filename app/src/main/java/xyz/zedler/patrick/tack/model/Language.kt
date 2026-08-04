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

package xyz.zedler.patrick.tack.model

import xyz.zedler.patrick.tack.util.getLocaleFromCode
import java.util.Locale

data class Language(
  val code: String,
  val translators: String,
  val name: String,
) : Comparable<Language> {

  constructor(codeTranslators: String) : this(
    code = codeTranslators.split("\n")[0],
    translators = codeTranslators.split("\n")[1],
    name = getLocaleFromCode(
      codeTranslators.split("\n")[0]
    ).let { locale ->
      val displayName = locale.getDisplayName(locale)
      displayName.take(1).uppercase(Locale.getDefault()) + displayName.drop(1)
    }
  )

  override fun compareTo(other: Language): Int {
    return name.lowercase(Locale.getDefault())
      .compareTo(other.name.lowercase(Locale.getDefault()))
  }
}
