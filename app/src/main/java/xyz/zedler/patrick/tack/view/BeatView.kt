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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapes
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.dpToPx
import java.util.Random

class BeatView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  private val path = Path()
  private val matrix = Matrix()
  private val random = Random()
  private val interpolator = FastOutSlowInInterpolator()
  private val button: MaterialButton
  private val paintFill = Paint().apply { style = Paint.Style.FILL }
  private val paintStroke = Paint().apply {
    style = Paint.Style.STROKE
    strokeWidth = context.dpToPx(2f).toFloat()
  }

  private val shapeScaleSub: Float
  private val shapeScaleBeatSub: Float
  private val shapeScaleNoBeat: Float
  private val shapeScaleMuted: Float
  private val colorActive = context.getAttrColor(R.attr.colorOutline)

  private var animatorSet: AnimatorSet? = null
  private var strokeAnimator: ValueAnimator? = null
  private var springAnimationTickType: SpringAnimation? = null
  private var morph: Morph = MORPHS[0]
  private var tickType: String = TICK_TYPE.NORMAL
  private var isSubdivision = false
  private var reduceAnimations = false
  private var isActive = false
  private var morphFactor = 0f
  private var tickTypeFraction = 0f
  var shapeScaleBeat: Float
  var shapeScale0: Float
  var shapeScale1: Float
  internal var index = 0

  private var colorFillSource = 0
  private var colorFillTarget = 0
  private var colorStrokeSource = 0
  private var colorStrokeTarget = 0
  private var shapeScale0Source = 0f
  private var shapeScale1Source = 0f
  private var shapeScale0Target = 0f
  private var shapeScale1Target = 0f

  init {
    setWillNotDraw(false)

    val minSize = context.dpToPx(48f)
    minimumWidth = minSize
    minimumHeight = minSize

    button = MaterialButton(
      context, null, R.attr.materialIconButtonStyle
    ).apply {
      strokeWidth = context.dpToPx(1f)
      strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
      setOnClickListener(null)
    }
    addView(button)

    shapeScaleNoBeat = 0.25f
    shapeScaleBeat = 0.75f
    shapeScaleSub = shapeScaleBeat
    shapeScaleBeatSub = 0.4f
    shapeScaleMuted = 0.1f
    shapeScale0 = shapeScaleNoBeat
    shapeScale1 = shapeScaleBeat

    setTickType(TICK_TYPE.NORMAL, false)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    animatorSet?.apply {
      pause()
      removeAllListeners()
      cancel()
    }
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    updateShape()
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawPath(path, paintFill)
    canvas.drawPath(path, paintStroke)
  }

  fun setIndex(index: Int) {
    this.index = index
    setTickType(tickType, false)
  }

  fun getIndex(): Int = index

  fun setIsSubdivision(isSubdivision: Boolean) {
    this.isSubdivision = isSubdivision
    setTickType(if (isSubdivision) TICK_TYPE.SUB else TICK_TYPE.NORMAL, false)
  }

  fun setTickType(tickType: String, animated: Boolean) {
    this.tickType = tickType

    val colorNormalPrimary = context.getAttrColor(R.attr.colorPrimary)
    val colorNormal = if (isColorRed(colorNormalPrimary)) {
      context.getAttrColor(R.attr.colorTertiary)
    } else {
      colorNormalPrimary
    }
    val colorStrong = context.getAttrColor(R.attr.colorError)
    val colorSub = context.getAttrColor(R.attr.colorOnSurfaceVariant)
    val colorMuted = context.getAttrColor(R.attr.colorOutline)

    colorFillSource = paintFill.color
    colorStrokeSource = paintStroke.color
    shapeScale0Source = shapeScale0
    shapeScale1Source = shapeScale1

    val colorTarget: Int
    val alphaTarget: Int
    when (tickType) {
      TICK_TYPE.STRONG -> {
        colorTarget = colorStrong
        alphaTarget = 255
        shapeScale0Target = shapeScaleNoBeat
        shapeScale1Target = shapeScaleBeat
      }

      TICK_TYPE.MUTED, TICK_TYPE.BEAT_SUB_MUTED -> {
        colorTarget = colorMuted
        alphaTarget = 255
        shapeScale0Target = shapeScaleMuted
        shapeScale1Target = shapeScaleNoBeat
      }

      TICK_TYPE.SUB -> {
        colorTarget = colorSub
        alphaTarget = 0
        shapeScale0Target = shapeScaleNoBeat
        shapeScale1Target = shapeScaleSub
      }

      TICK_TYPE.BEAT_SUB -> {
        colorTarget = colorMuted
        alphaTarget = 255
        shapeScale0Target = shapeScaleNoBeat
        shapeScale1Target = shapeScaleBeatSub
      }

      else -> {
        colorTarget = colorNormal
        alphaTarget = (0.3f * 255).toInt()
        shapeScale0Target = shapeScaleNoBeat
        shapeScale1Target = shapeScaleBeat
      }
    }
    colorFillTarget = ColorUtils.setAlphaComponent(colorTarget, alphaTarget)
    colorStrokeTarget = colorTarget

    springAnimationTickType?.cancel()
    if (animated) {
      tickTypeFraction = 0f
      if (springAnimationTickType == null) {
        springAnimationTickType = SpringAnimation(
          this, TICK_TYPE_FRACTION
        ).apply {
          spring = SpringForce().apply {
            stiffness = if (TEST_ANIMATIONS) 20f else 1400f
            dampingRatio = if (TEST_ANIMATIONS) 0.3f else 0.6f
          }
          minimumVisibleChange = 0.01f
        }
      }
      springAnimationTickType?.animateToFinalPosition(1f)
    } else {
      setTickTypeFraction(1f)
    }
  }

  fun nextTickType(): String {
    val next = when (tickType) {
      TICK_TYPE.NORMAL -> if (isSubdivision) TICK_TYPE.MUTED else Constants.TICK_TYPE.STRONG
      TICK_TYPE.STRONG -> TICK_TYPE.MUTED
      TICK_TYPE.SUB -> TICK_TYPE.NORMAL
      TICK_TYPE.BEAT_SUB -> TICK_TYPE.BEAT_SUB_MUTED
      TICK_TYPE.BEAT_SUB_MUTED -> TICK_TYPE.BEAT_SUB
      else -> if (isSubdivision) TICK_TYPE.SUB else Constants.TICK_TYPE.NORMAL
    }
    setTickType(next, true)
    return next
  }

  fun beat() {
    animatorSet?.apply {
      pause()
      removeAllListeners()
      cancel()
    }
    animatorSet = null

    if (reduceAnimations) return

    morph = if (tickType == TICK_TYPE.MUTED ||
      tickType == TICK_TYPE.BEAT_SUB ||
      tickType == TICK_TYPE.BEAT_SUB_MUTED
    ) {
      MORPHS[0]
    } else {
      MORPHS[1 + random.nextInt(MORPHS.size - 1)]
    }

    val animatorIn = ValueAnimator.ofFloat(0f, 1f).apply {
      addUpdateListener { setMorphFactor(it.animatedValue as Float) }
      interpolator = this@BeatView.interpolator
      duration = 10
    }

    val animatorOut = ValueAnimator.ofFloat(1f, 0f).apply {
      addUpdateListener { setMorphFactor(it.animatedValue as Float) }
      interpolator = this@BeatView.interpolator
      duration = 300
      startDelay = 30
    }

    animatorSet = AnimatorSet().apply {
      playSequentially(animatorIn, animatorOut)
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          animatorSet = null
        }
      })
      start()
    }
  }

  override fun setOnClickListener(l: OnClickListener?) {
    button.setOnClickListener(l)
    button.isEnabled = l != null
  }

  fun setReduceAnimations(reduce: Boolean) {
    reduceAnimations = reduce
  }

  fun setActive(active: Boolean) {
    if (isActive == active) return
    isActive = active
    shapeScaleBeat = if (active) 0.6f else 0.75f
    shapeScale1 = if (tickType == TICK_TYPE.MUTED) shapeScaleMuted else shapeScaleBeat

    strokeAnimator?.apply {
      pause()
      removeAllListeners()
      cancel()
    }
    strokeAnimator = ValueAnimator.ofArgb(
      button.strokeColor.defaultColor,
      if (active) colorActive else Color.TRANSPARENT
    ).apply {
      addUpdateListener {
        button.setStrokeColor(ColorStateList.valueOf(it.animatedValue as Int))
      }
      interpolator = this@BeatView.interpolator
      duration = if (active) 25 else 300
      start()
    }
  }

  private fun updateShape() {
    path.rewind()
    morph.toPath(morphFactor, path)
    matrix.reset()
    val scale = shapeScale0 + morphFactor * (shapeScale1 - shapeScale0)
    matrix.setScale(width / 2f * scale, height / 2f * scale)
    matrix.postTranslate(width / 2f, height / 2f)
    path.transform(matrix)
  }

  private fun setMorphFactor(factor: Float) {
    morphFactor = factor
    updateShape()
    invalidate()
  }

  private fun setTickTypeFraction(fraction: Float) {
    tickTypeFraction = fraction
    val colorFraction = fraction.coerceIn(0f, 1f)
    paintFill.color = ColorUtils.blendARGB(
      colorFillSource, colorFillTarget, colorFraction
    )
    paintStroke.color = ColorUtils.blendARGB(
      colorStrokeSource, colorStrokeTarget, colorFraction
    )
    shapeScale0 = shapeScale0Source + fraction * (shapeScale0Target - shapeScale0Source)
    shapeScale1 = shapeScale1Source + fraction * (shapeScale1Target - shapeScale1Source)
    updateShape()
    invalidate()
  }

  fun getTickTypeFraction(): Float = tickTypeFraction

  override fun toString(): String = tickType

  companion object {
    @SuppressLint("RestrictedApi")
    private val SHAPES = arrayOf<RoundedPolygon>(
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.CIRCLE,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.SQUARE,
        false,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.SLANTED_SQUARE,
        false,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.OVAL,
        false,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.PILL,
        false,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.DIAMOND,
        false,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.PENTAGON,
        false,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.VERY_SUNNY,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.SUNNY,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.COOKIE_4,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.COOKIE_6,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.COOKIE_7,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.COOKIE_9,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.BURST,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.SOFT_BURST,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.BOOM,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.SOFT_BOOM,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      xyz.zedler.patrick.tack.util.normalize(
        MaterialShapes.FLOWER,
        true,
        RectF(-1f, -1f, 1f, 1f)
      )
    )

    private val MORPHS = Array(SHAPES.size) { i -> Morph(SHAPES[0], SHAPES[i]) }

    private const val TEST_ANIMATIONS = false

    @JvmStatic
    fun isColorRed(color: Int): Boolean {
      val tolerance = 30
      val red = Color.red(color)
      val green = Color.green(color)
      val blue = Color.blue(color)
      return red > green + tolerance && red > blue + tolerance
    }

    private val TICK_TYPE_FRACTION = object
      : FloatPropertyCompat<BeatView>("tickTypeFraction") {
      override fun getValue(delegate: BeatView): Float = delegate.getTickTypeFraction()
      override fun setValue(delegate: BeatView, value: Float) {
        delegate.setTickTypeFraction(value)
      }
    }
  }
}
