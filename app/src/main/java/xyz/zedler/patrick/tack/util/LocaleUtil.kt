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

@file:JvmName("LocaleUtil")

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.model.Language
import java.util.Locale

fun followsSystem(): Boolean {
  return AppCompatDelegate.getApplicationLocales().isEmpty
}

fun getLocale(): Locale {
  return if (followsSystem()) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      Locale.getDefault()
    } else if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
      Resources.getSystem().configuration.locales[0]
    } else {
      @Suppress("DEPRECATION")
      Resources.getSystem().configuration.locale
    }
  } else {
    AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
  }
}

fun getLocaleName(): String {
  val locale = getLocale()
  return locale.getDisplayName(locale)
}

fun Context.getLanguages(): List<Language> {
  val languages = mutableListOf<Language>()
  val localesRaw = getRawText(R.raw.locales)
  if (localesRaw.trim().isEmpty()) {
    return languages
  }
  localesRaw.split("\n\n").forEach {
    languages.add(Language(it))
  }
  languages.sort()
  return languages
}

fun LocaleListCompat.getLanguageCode(): String? {
  return if (!isEmpty) get(0)?.toLanguageTag() else null
}

fun getLocaleFromCode(languageCode: String?): Locale {
  if (languageCode == null) {
    return Locale.getDefault()
  }
  return try {
    val codeParts = languageCode.split("-")
    if (codeParts.size > 1) {
      Locale.Builder().setLanguage(codeParts[0]).setRegion(codeParts[1]).build()
    } else {
      Locale.Builder().setLanguage(languageCode).build()
    }
  } catch (e: Exception) {
    Locale.getDefault()
  }
}

fun String.getLangFromLanguageCode(): String {
  val codeParts = split("-")
  return if (codeParts.size > 1) codeParts[0] else this
}
