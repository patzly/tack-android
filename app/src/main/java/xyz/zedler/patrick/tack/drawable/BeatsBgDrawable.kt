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

package xyz.zedler.patrick.tack.drawable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.graphics.Path.Op
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.getDimension
import xyz.zedler.patrick.tack.util.isDarkModeActive
import xyz.zedler.patrick.tack.util.isLayoutRtl

class BeatsBgDrawable(context: Context) : Drawable() {

  private val paintFg = Paint()
  private val paintBg = Paint()
  private val rectFg = RectF()
  private val rectBg = RectF()
  private val pathFg = Path()
  private val pathBg = Path()
  private val alphaBase: Float
  private val progressThreshold: Float
  private val radii: FloatArray
  private val rtl: Boolean
  private var fraction = 0f
  private var alphaVal = 0f
  private var progressAnimator: ValueAnimator? = null
  private var alphaAnimator: ValueAnimator? = null

  init {
    rtl = context.isLayoutRtl()
    val topRadius = context.getDimension(R.dimen.segmented_large_corner_size).toFloat()
    val bottomRadius = context.dpToPx(4f).toFloat()
    radii = floatArrayOf(
      topRadius, topRadius,
      topRadius, topRadius,
      bottomRadius, bottomRadius,
      bottomRadius, bottomRadius
    )
    paintBg.color = context.getAttrColor(R.attr.colorSurfaceContainerHigh)
    paintFg.color = context.getAttrColor(R.attr.colorOnSurface)
    alphaBase = if (context.isDarkModeActive()) ALPHA_FG_BASE_DARK else ALPHA_FG_BASE_LIGHT
    progressThreshold = 1f
    setProgress(0f, 0)
  }

  override fun draw(canvas: Canvas) {
    rectBg.set(
      0f, 0f, bounds.width().toFloat(), bounds.height().toFloat()
    )
    pathBg.reset()
    pathBg.addRoundRect(rectBg, radii, Direction.CW)
    canvas.drawPath(pathBg, paintBg)

    rectFg.set(rectBg)
    if (rtl) {
      rectFg.left = bounds.width() * (1 - fraction)
    } else {
      rectFg.right = bounds.width() * fraction
    }
    pathFg.reset()
    pathFg.addRect(rectFg, Direction.CW)
    pathFg.op(pathBg, Op.INTERSECT)

    var interpolated = alphaBase
    if (progressThreshold < 1 && fraction > progressThreshold) {
      interpolated = alphaBase * (1 - (fraction - progressThreshold) / (1 - progressThreshold))
    }
    paintFg.alpha = (interpolated * alphaVal * 255).toInt()
    canvas.drawPath(pathFg, paintFg)
  }

  @Deprecated("Deprecated in Java", ReplaceWith(""))
  override fun setAlpha(alpha: Int) {
  }

  @Deprecated("Deprecated in Java", ReplaceWith(""))
  override fun setColorFilter(colorFilter: ColorFilter?) {
  }

  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  private fun setFraction(fraction: Float) {
    this.fraction = fraction
    invalidateSelf()
  }

  private fun setProgressAlpha(alpha: Float) {
    this.alphaVal = alpha
    invalidateSelf()
  }

  fun setProgress(fraction: Float, animationDuration: Long) {
    progressAnimator?.let {
      it.pause()
      it.removeAllUpdateListeners()
      it.removeAllListeners()
      it.cancel()
    }
    progressAnimator = null
    if (animationDuration > 0) {
      val animator = ValueAnimator.ofFloat(this.fraction, fraction)
      animator.addUpdateListener { animation ->
        setFraction(animation.animatedValue as Float)
      }
      animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          if (progressThreshold == 1f) {
            setProgressVisible(false, true, 1500L)
          }
        }
      })
      animator.interpolator = LinearInterpolator()
      animator.duration = animationDuration
      animator.start()
      progressAnimator = animator
    } else {
      setFraction(fraction)
    }
  }

  @JvmOverloads
  fun setProgressVisible(
    visible: Boolean,
    animated: Boolean,
    duration: Long = Constants.ANIM_DURATION_LONG
  ) {
    alphaAnimator?.let {
      it.pause()
      it.removeAllUpdateListeners()
      it.removeAllListeners()
      it.cancel()
    }
    alphaAnimator = null
    if (animated) {
      val animator = ValueAnimator.ofFloat(this.alphaVal, if (visible) 1f else 0f)
      animator.addUpdateListener { animation ->
        setProgressAlpha(animation.animatedValue as Float)
      }
      animator.interpolator = FastOutSlowInInterpolator()
      animator.duration = duration
      animator.start()
      alphaAnimator = animator
    } else {
      setProgressAlpha(if (visible) 1f else 0f)
    }
  }

  fun reset() {
    setProgress(0f, 0)
    setProgressVisible(true, false)
  }

  companion object {
    private const val ALPHA_FG_BASE_LIGHT = 0.08f
    private const val ALPHA_FG_BASE_DARK = 0.12f
  }
}
