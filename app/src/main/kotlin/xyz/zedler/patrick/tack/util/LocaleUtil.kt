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

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.Language
import java.util.Locale

object LocaleUtil {

  fun getLocale(languageCode: String?): Locale {
    if (languageCode == null) {
      return Resources.getSystem().configuration.locales[0]
    }
    return getLocaleFromCode(languageCode)
  }

  fun getLocaleName(languageCode: String?): String {
    val locale = getLocale(languageCode)
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

  fun applyLocale(context: Context, languageCode: String?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
      localeManager.applicationLocales = if (languageCode == null) {
        LocaleList.getEmptyLocaleList()
      } else {
        LocaleList.forLanguageTags(languageCode)
      }
    }
    // For older versions, the actual application happens via wrap() in attachBaseContext
  }

  fun resolveLanguageCode(context: Context, languageCode: String?): String? {
    if (languageCode.isNullOrBlank()) return null

    val supportedCodes = getLanguages(context).map { it.code }
    if (supportedCodes.contains(languageCode)) {
      return languageCode
    }
    val baseCode = languageCode.substringBefore("-")
    if (supportedCodes.contains(baseCode)) {
      return baseCode
    }
    return languageCode
  }

  fun wrap(context: Context, languageCode: String?): Context {
    val locale = getLocale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocales(LocaleList(locale))
    return context.createConfigurationContext(config)
  }

  private fun getLocaleFromCode(languageCode: String): Locale {
    return try {
      Locale.forLanguageTag(languageCode)
    } catch (_: Exception) {
      Locale.getDefault()
    }
  }
}
