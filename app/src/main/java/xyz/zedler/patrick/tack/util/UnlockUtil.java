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
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
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

  public static boolean isInstallerValid(@NonNull Context context) {
    if (VERSION.SDK_INT < VERSION_CODES.R) {
      return true;
    }
    PackageManager pm = context.getPackageManager();
    try {
      InstallSourceInfo sourceInfo = pm.getInstallSourceInfo(PACKAGE_KEY);
      String installer = sourceInfo.getInstallingPackageName();
      return installer != null && installer.equals("com.android.vending");
    } catch (NameNotFoundException e) {
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

  public static boolean isUnlocked(@NonNull Context context, boolean verifyKey) {
    if (verifyKey && isPlayStoreInstalled(context)) {
      return isKeyInstalled(context) && isInstallerValid(context);
    }
    return true;
  }

  public static void openPlayStore(@NonNull Context context) {
    context.startActivity(
        new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.app_vending_key)))
    );
  }
}
