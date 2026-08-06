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

package xyz.zedler.patrick.tack.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.google.android.material.shape.MaterialShapes
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.*

class TempoTapView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val paintFill = Paint().apply {
    style = Paint.Style.FILL
  }
  private val path = Path()
  private val matrix = Matrix()
  private val morph: Morph
  private val colorGradient1 = context.getAttrColor(R.attr.colorSecondaryContainer)
  private val colorGradient2 = context.getAttrColor(R.attr.colorPrimaryContainer)
  private val colorGradient3 = context.getAttrColor(R.attr.colorTertiaryContainer)

  private var touchFactor = 0f
  private var gradient: RadialGradient? = null
  private var reduceAnimations = false
  private var springAnimationTouch: SpringAnimation? = null
  private var springAnimationRelease: SpringAnimation? = null

  init {
    paintFill.color = colorGradient3

    @SuppressLint("RestrictedApi")
    morph = Morph(
      normalize(
        MaterialShapes.VERY_SUNNY,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      normalize(
        MaterialShapes.SUNNY,
        true,
        RectF(-1f, -1f, 1f, 1f)
      )
    )

    updateShape()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (gradient == null && width > 0) {
      val blendFraction = 0.75f
      gradient = RadialGradient(
        width.toFloat(),
        0f,
        width * 1.25f,
        intArrayOf(
          ColorUtils.blendARGB(colorGradient3, colorGradient1, blendFraction),
          ColorUtils.blendARGB(colorGradient3, colorGradient1, blendFraction),
          ColorUtils.blendARGB(colorGradient3, colorGradient2, blendFraction),
          ColorUtils.blendARGB(colorGradient3, colorGradient3, blendFraction)
        ),
        floatArrayOf(0f, 0.1f, 0.4f, 0.8f),
        Shader.TileMode.CLAMP
      )
      paintFill.shader = gradient
    }

    canvas.drawPath(path, paintFill)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    updateShape()
  }

  private fun updateShape() {
    path.rewind()
    morph.toPath(touchFactor, path)
    matrix.reset()
    matrix.setScale(width / 2f, height / 2f)
    matrix.postTranslate(width / 2f, height / 2f)
    path.transform(matrix)
  }

  @SuppressLint("PrivateResource")
  fun setTouched(touched: Boolean) {
    springAnimationTouch?.cancel()
    springAnimationRelease?.cancel()

    if (!reduceAnimations) {
      if (springAnimationTouch == null) {
        springAnimationTouch = SpringAnimation(this, TOUCH_FACTOR).apply {
          spring = SpringForce().apply {
            stiffness = 6000f
            dampingRatio = 0.9f
          }
          minimumVisibleChange = 0.01f
        }
      }
      if (springAnimationRelease == null) {
        springAnimationRelease = SpringAnimation(this, TOUCH_FACTOR).apply {
          spring = SpringForce().apply {
            stiffness = 1400f
            dampingRatio = 0.4f
          }
          minimumVisibleChange = 0.01f
        }
      }
      if (touched) {
        springAnimationTouch?.animateToFinalPosition(1f)
      } else {
        springAnimationRelease?.animateToFinalPosition(0f)
      }
    } else {
      setTouchFactor(0f)
    }
  }

  fun getTouchFactor(): Float = touchFactor

  fun setTouchFactor(factor: Float) {
    touchFactor = factor
    updateShape()
    invalidate()
  }

  fun setReduceAnimations(reduce: Boolean) {
    reduceAnimations = reduce
  }

  companion object {
    private val TOUCH_FACTOR = object : FloatPropertyCompat<TempoTapView>("touchFactor") {
      override fun getValue(delegate: TempoTapView): Float = delegate.getTouchFactor()
      override fun setValue(delegate: TempoTapView, value: Float) = delegate.setTouchFactor(value)
    }
  }
}
