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
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.wear.input.WearableButtons;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;

public class ButtonUtil {

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final OnPressListener listener;
  private final boolean hasMinTwoButtons;
  private boolean isFirstButtonPress;
  private boolean isDown;
  private int nextRun = 400;
  private Handler handler;
  private final Runnable runnable = new Runnable() {
    @Override
    public void run() {
      listener.onPress();
      handler.postDelayed(this, nextRun);
      if (nextRun > 60) {
        nextRun = (int) (nextRun * 0.9);
      }
    }
  };

  public ButtonUtil(Context context, @NonNull OnPressListener listener) {
    this.context = context;
    this.listener = listener;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    isFirstButtonPress = sharedPrefs.getBoolean(
        Constants.PREF.FIRST_PRESS, Constants.DEF.FIRST_PRESS
    );
    hasMinTwoButtons = WearableButtons.getButtonCount(context) >= 2;
    isDown = false;
  }

  public void onPressDown() {
    if (isDown || (!hasMinTwoButtons && !isFirstButtonPress)) {
      return;
    }
    isDown = true;
    if (isFirstButtonPress) {
      isFirstButtonPress = false;
      sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_PRESS, isFirstButtonPress).apply();
      Toast.makeText(
          context,
          hasMinTwoButtons ? R.string.msg_long_press : R.string.msg_one_button,
          Toast.LENGTH_LONG
      ).show();
    }
    if (handler != null) {
      handler.removeCallbacks(runnable);
    }
    listener.onPress();
    handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(runnable, 800);
  }

  public void onPressUp() {
    if (!hasMinTwoButtons || !isDown) {
      return;
    }
    isDown = false;
    if (handler != null) {
      handler.removeCallbacks(runnable);
    }
    handler = null;
    nextRun = 400;
  }

  public void otherButtonWasPressed() {
    isFirstButtonPress = false;
  }

  public interface OnPressListener {

    void onPress();
  }
}
