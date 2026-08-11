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

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.Language
import java.util.Locale

object LocaleUtil {

  fun followsSystem(): Boolean {
    return AppCompatDelegate.getApplicationLocales().isEmpty
  }

  fun getLocale(): Locale {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    return if (appLocales.isEmpty) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Locale.getDefault()
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Resources.getSystem().configuration.locales[0]
      } else {
        @Suppress("DEPRECATION")
        Resources.getSystem().configuration.locale
      }
    } else {
      appLocales[0] ?: Locale.getDefault()
    }
  }

  fun getLocaleName(): String {
    val locale = getLocale()
    return locale.getDisplayName(locale).let {
      it.take(1).uppercase(Locale.getDefault()) + it.drop(1)
    }
  }

  fun getLanguages(context: Context): List<Language> {
    val languages = mutableListOf<Language>()
    val localesRaw = context.getRawText(R.raw.locales)
    if (localesRaw.trim().isEmpty()) {
      return languages
    }
    localesRaw.split("\n\n").forEach {
      languages.add(Language(it))
    }
    languages.sort()
    return languages
  }

  fun getLanguageCode(): String? {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (!locales.isEmpty) locales[0]?.toLanguageTag() else null
  }
}
