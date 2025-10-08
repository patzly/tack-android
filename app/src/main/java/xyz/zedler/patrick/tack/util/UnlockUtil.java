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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.tack.R;

public class UnlockUtil {

  private final static String PACKAGE_KEY = "xyz.zedler.patrick.tack.unlock";

  public static boolean isKeyInstalled(@NonNull Context context) {
    try {
      context.getPackageManager().getPackageInfo(PACKAGE_KEY, 0);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isPlayStoreInstalled(@NonNull Context context){
    try {
      context.getPackageManager().getPackageInfo("com.android.vending", 0);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isUnlocked(@NonNull Context context) {
    if (isPlayStoreInstalled(context)) {
      return isKeyInstalled(context);
    }
    return true;
  }

  public static void openPlayStore(@NonNull Context context) {
    context.startActivity(
        new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.app_vending_key)))
    );
  }
}
