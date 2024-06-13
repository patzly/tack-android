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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.tack.Constants.Pref;

public class BookmarkUtil {

  private final SharedPreferences sharedPrefs;

  public BookmarkUtil(Context context) {
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public int toggleBookmark(int tempo) {
    int bookmark = sharedPrefs.getInt(Pref.BOOKMARK, -1);
    if (bookmark == -1) {
      // Never used before
      bookmark = tempo;
    }
    sharedPrefs.edit().putInt(Pref.BOOKMARK, tempo).apply();
    return bookmark;
  }

  public void reportUsage(int tempo) {
    // TODO: implement bookmark algorithm with queue and least-frequently-used deletion method
  }
}
