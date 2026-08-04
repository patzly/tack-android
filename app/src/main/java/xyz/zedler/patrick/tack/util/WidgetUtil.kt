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

@file:JvmName("WidgetUtil")

package xyz.zedler.patrick.tack.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import xyz.zedler.patrick.tack.widget.SongsWidgetProvider

fun sendSongsWidgetUpdate(context: Context) {
  val intent = Intent(context, SongsWidgetProvider::class.java).apply {
    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
  }

  val appWidgetManager = AppWidgetManager.getInstance(context)
  val componentName = ComponentName(context, SongsWidgetProvider::class.java)
  val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

  intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
  context.sendBroadcast(intent)
}

fun requestSongsWidgetPin(context: Context) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isRequestPinAppWidgetSupported(context)) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val widgetProvider = ComponentName(context, SongsWidgetProvider::class.java)
    appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
  }
}

fun isRequestPinAppWidgetSupported(context: Context): Boolean {
  val appWidgetManager = AppWidgetManager.getInstance(context)
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    appWidgetManager.isRequestPinAppWidgetSupported
  } else {
    false
  }
}
