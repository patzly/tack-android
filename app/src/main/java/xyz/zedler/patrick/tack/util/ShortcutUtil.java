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
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.SongActivity;

public class ShortcutUtil {

  private static final String TAG = ShortcutUtil.class.getSimpleName();

  private final Context context;
  private ShortcutManager manager;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public ShortcutUtil(Context context) {
    this.context = context;
    if (isSupported()) {
      manager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
    }
  }

  public void addShortcut(@NonNull ShortcutInfo shortcutInfo) {
    if (!isSupported()) {
      return;
    }
    hasShortcutAsync(shortcutInfo.getId(), hasShortcut -> {
      if (isSupported() && !hasShortcut) {
        if (manager.getDynamicShortcuts().size() < getMaxShortcutCount()) {
          manager.addDynamicShortcuts(Collections.singletonList(shortcutInfo));
        }
      }
    });
  }

  public void addAllShortcuts(@NonNull List<ShortcutInfo> shortcuts) {
    if (isSupported()) {
      // Ensure we do not exceed the maximum number of shortcuts else it will throw an exception
      manager.addDynamicShortcuts(
          shortcuts.subList(0, Math.min(shortcuts.size(), getMaxShortcutCount()))
      );
    }
  }

  public void removeShortcut(@NonNull String shortcutId) {
    hasShortcutAsync(shortcutId, hasShortcut -> {
      if (isSupported() && hasShortcut) {
        manager.removeDynamicShortcuts(Collections.singletonList(shortcutId));
      }
    });
  }

  public void removeAllShortcuts() {
    if (isSupported()) {
      manager.removeAllDynamicShortcuts();
    }
  }

  public void reportUsage(@NonNull String shortcutId) {
    hasShortcutAsync(shortcutId, hasShortcut -> {
      if (isSupported() && hasShortcut) {
        manager.reportShortcutUsed(shortcutId);
      }
    });
  }

  public int getMaxShortcutCount() {
    return isSupported() ? manager.getMaxShortcutCountPerActivity() : 0;
  }

  /**
   * Asynchronous because there was an ANR reported caused by manager.getDynamicShortcuts()
   */
  private void hasShortcutAsync(@Nullable String shortcutId, ShortcutCallback callback) {
    if (isSupported()) {
      executorService.execute(() -> {
        boolean result = false;
        try {
          for (ShortcutInfo info : manager.getDynamicShortcuts()) {
            if (Objects.equals(shortcutId, info.getId())) {
              result = true;
              break;
            }
          }
        } catch (Exception e) {
          Log.e(TAG, "hasShortcutAsync: ", e);
        }
        final boolean finalResult = result;
        mainHandler.post(() -> callback.onResult(finalResult));
      });
    } else {
      mainHandler.post(() -> callback.onResult(false));
    }
  }

  @RequiresApi(api = VERSION_CODES.N_MR1)
  public ShortcutInfo getShortcutInfo(@NonNull String id, @Nullable String name) {
    ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, id);
    builder.setShortLabel(name != null ? name : context.getString(R.string.label_song_name));
    builder.setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut));
    builder.setIntent(
        new Intent(context, SongActivity.class)
            .setAction(ACTION.APPLY_SONG)
            .putExtra(EXTRA.SONG_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
    );
    return builder.build();
  }

  public static boolean isSupported() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
  }

  private interface ShortcutCallback {
    void onResult(boolean hasShortcut);
  }
}
