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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import xyz.zedler.patrick.tack.R;

public class LogoUtil {

  private final static String TAG = LogoUtil.class.getSimpleName();

  private final RotateDrawable pointer;
  private AnimatorSet animatorSet;
  private boolean isLeft = true;

  public LogoUtil(ImageView imageView) {
    LayerDrawable layers = (LayerDrawable) imageView.getDrawable();
    pointer = (RotateDrawable) layers.findDrawableByLayerId(R.id.logo_pointer);
    pointer.setLevel(0);
  }

  public void nextBeat(long interval) {
    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.cancel();
    }
    animatorSet = new AnimatorSet();
    animatorSet.play(getAnimator(interval, isLeft ? 10000 : 0));
    animatorSet.start();
    isLeft = !isLeft;
  }

  private ObjectAnimator getAnimator(long duration, int level) {
    ObjectAnimator animator = ObjectAnimator.ofInt(
        pointer, "level", pointer.getLevel(), level
    ).setDuration(duration);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    return animator;
  }
}
