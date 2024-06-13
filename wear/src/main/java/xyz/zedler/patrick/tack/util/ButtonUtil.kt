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
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.wear.input.WearableButtons;

public class ButtonUtil {

  private final OnPressListener listener;
  private final boolean hasMinTwoButtons;
  private boolean isDown;
  private int nextRun = 400;
  private Handler handler;
  private final Runnable runnable = new Runnable() {
    @Override
    public void run() {
      listener.onFastPress();
      handler.postDelayed(this, nextRun);
      if (nextRun > 60) {
        nextRun = (int) (nextRun * 0.9);
      }
    }
  };

  public ButtonUtil(Context context, @NonNull OnPressListener listener) {
    this.listener = listener;
    hasMinTwoButtons = WearableButtons.getButtonCount(context) >= 2;
    isDown = false;
  }

  public void onPressDown() {
    if (isDown || !hasMinTwoButtons) {
      return;
    }
    isDown = true;
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

  public interface OnPressListener {

    void onPress();
    void onFastPress();
  }
}
