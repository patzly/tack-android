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
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.google.android.material.motion.MotionUtils
import com.google.android.material.shape.MaterialShapes
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.*
import kotlin.math.*

class CircleView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val paintFill = Paint().apply {
    style = Paint.Style.FILL
  }
  private val path = Path()
  private val matrix = Matrix()
  private val morph: Morph
  private val colorDefault = context.getAttrColor(R.attr.colorPrimaryContainer)
  private val colorDrag1 = context.getAttrColor(R.attr.colorTertiaryContainer)
  private val colorDrag2 = context.getAttrColor(R.attr.colorPrimaryContainer)
  private val colorDrag3 = context.getAttrColor(R.attr.colorSecondaryContainer)

  private var morphFactor = 0f
  private var colorFraction = 0f
  private var touchX = 0f
  private var touchY = 0f
  private var reduceAnimations = false
  private var onDragAnimListener: OnDragAnimListener? = null
  private var springAnimationMorph: SpringAnimation? = null
  private var springAnimationColor: SpringAnimation? = null

  init {
    paintFill.color = colorDefault

    @SuppressLint("RestrictedApi")
    morph = Morph(
      normalize(
        MaterialShapes.COOKIE_12,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      normalize(
        MaterialShapes.SOFT_BURST,
        true,
        RectF(-1f, -1f, 1f, 1f)
      )
    )

    updateShape()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawPath(path, paintFill)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    updateShape()
  }

  fun setOnDragAnimListener(listener: OnDragAnimListener?) {
    onDragAnimListener = listener
  }

  private fun updateShape() {
    path.rewind()
    morph.toPath(morphFactor, path)
    matrix.reset()
    matrix.setScale(width / 2f, height / 2f)
    matrix.postTranslate(width / 2f, height / 2f)
    path.transform(matrix)
  }

  @SuppressLint("PrivateResource")
  fun setDragged(dragged: Boolean, x: Float, y: Float) {
    if (dragged) {
      touchX = x
      touchY = y
    }
    springAnimationMorph?.cancel()
    springAnimationColor?.cancel()

    if (!reduceAnimations) {
      if (springAnimationMorph == null) {
        springAnimationMorph = SpringAnimation(this, MORPH_FACTOR).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringDefaultSpatial,
            R.style.Motion_Material3_Spring_Standard_Default_Spatial
          )
          minimumVisibleChange = 0.01f
        }
      }
      if (springAnimationColor == null) {
        springAnimationColor = SpringAnimation(this, COLOR_FRACTION).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringDefaultEffects,
            R.style.Motion_Material3_Spring_Standard_Default_Effects
          )
          minimumVisibleChange = 0.01f
        }
      }
      springAnimationMorph?.animateToFinalPosition(if (dragged) 1f else 0f)
      springAnimationColor?.animateToFinalPosition(if (dragged) 0.85f else 0f)
    } else {
      setMorphFactor(0f)
      setColorFraction(0f)
    }
  }

  fun onDrag(x: Float, y: Float) {
    touchX = x
    touchY = y
    if (!reduceAnimations) {
      paintFill.shader = getGradient()
    }
    invalidate()
  }

  private fun getGradient(): Shader {
    val pointF = getRotatedPoint(
      touchX, touchY, pivotX, pivotY, -rotation
    )
    return RadialGradient(
      pointF.x,
      pointF.y,
      width.toFloat(),
      intArrayOf(
        ColorUtils.blendARGB(colorDefault, colorDrag1, colorFraction),
        ColorUtils.blendARGB(colorDefault, colorDrag1, colorFraction),
        ColorUtils.blendARGB(colorDefault, colorDrag2, colorFraction),
        ColorUtils.blendARGB(colorDefault, colorDrag3, colorFraction)
      ),
      floatArrayOf(0f, 0.1f, 0.5f, 0.9f),
      Shader.TileMode.CLAMP
    )
  }

  private fun getRotatedPoint(x: Float, y: Float, cx: Float, cy: Float, degrees: Float): PointF {
    val radians = Math.toRadians(degrees.toDouble())
    val x1 = x - cx
    val y1 = y - cy
    val x2 = (x1 * cos(radians) - y1 * sin(radians)).toFloat()
    val y2 = (x1 * sin(radians) + y1 * cos(radians)).toFloat()
    return PointF(x2 + cx, y2 + cy)
  }

  fun getMorphFactor(): Float = morphFactor

  fun setMorphFactor(factor: Float) {
    morphFactor = factor
    updateShape()
    invalidate()
  }

  fun getColorFraction(): Float = colorFraction

  fun setColorFraction(fraction: Float) {
    colorFraction = fraction
    if (width > 0) {
      paintFill.shader = getGradient()
    }
    invalidate()
    onDragAnimListener?.onDragAnim(fraction)
  }

  fun setReduceAnimations(reduce: Boolean) {
    reduceAnimations = reduce
  }

  interface OnDragAnimListener {
    fun onDragAnim(fraction: Float)
  }

  companion object {
    private val MORPH_FACTOR = object : FloatPropertyCompat<CircleView>("morphFactor") {
      override fun getValue(delegate: CircleView): Float = delegate.getMorphFactor()
      override fun setValue(delegate: CircleView, value: Float) = delegate.setMorphFactor(value)
    }
    private val COLOR_FRACTION = object : FloatPropertyCompat<CircleView>("colorFraction") {
      override fun getValue(delegate: CircleView): Float = delegate.getColorFraction()
      override fun setValue(delegate: CircleView, value: Float) = delegate.setColorFraction(value)
    }
  }
}
