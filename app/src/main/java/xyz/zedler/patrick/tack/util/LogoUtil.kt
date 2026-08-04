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

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import xyz.zedler.patrick.tack.R

class LogoUtil(imageView: ImageView) {

  private val pointer: RotateDrawable
  private var animator: Animator? = null
  private var isLeft = true

  init {
    val layers = imageView.drawable as LayerDrawable
    pointer = layers.findDrawableByLayerId(R.id.logo_pointer) as RotateDrawable
    pointer.level = 0
  }

  fun nextBeat(interval: Long) {
    animator?.apply {
      pause()
      cancel()
    }

    animator = ObjectAnimator.ofInt(
      pointer, "level", pointer.level, if (isLeft) 10000 else 0
    ).apply {
      duration = interval
      interpolator = AccelerateDecelerateInterpolator()
      start()
    }

    isLeft = !isLeft
  }
}
