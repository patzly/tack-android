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
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import xyz.zedler.patrick.tack.widget.SongsWidgetProvider;

public class WidgetUtil {

  public static void sendSongsWidgetUpdate(Context context) {
    Intent intent = new Intent(context, SongsWidgetProvider.class);
    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    ComponentName componentName = new ComponentName(context, SongsWidgetProvider.class);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
    context.sendBroadcast(intent);
  }

  public static void requestSongsWidgetPin(Context context) {
    if (VERSION.SDK_INT >= VERSION_CODES.O && isRequestPinAppWidgetSupported(context)) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
      ComponentName widgetProvider = new ComponentName(context, SongsWidgetProvider.class);
      appWidgetManager.requestPinAppWidget(widgetProvider, null, null);
    }
  }

  public static boolean isRequestPinAppWidgetSupported(Context context) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      return appWidgetManager.isRequestPinAppWidgetSupported();
    }
    return false;
  }
}
