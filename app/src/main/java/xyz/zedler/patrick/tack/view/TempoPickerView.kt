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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.RotateAnimation
import xyz.zedler.patrick.tack.util.dpToPx
import kotlin.math.*

class TempoPickerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs), View.OnTouchListener {

  private var isTouchable = true
  private var isTouchStartedInCircle = false
  private var isTouchStartedInCenter = false
  private var currAngle = 0.0
  private var prevAngle = 0.0
  private var degreeStorage = 0f
  private var onRotationListener: OnRotationListener? = null
  private var onPickListener: OnPickListener? = null
  private var onClickListener: OnClickListener? = null
  private val ignoredCenterSize = context.dpToPx(40f).toFloat()
  private val gestureDetector = GestureDetector(
    context,
    object : SimpleOnGestureListener() {
      override fun onSingleTapUp(event: MotionEvent): Boolean {
        onClickListener?.onClick(this@TempoPickerView)
        return true
      }
    })

  init {
    setOnTouchListener(this)
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()
  }

  fun setOnRotationListener(listener: OnRotationListener?) {
    onRotationListener = listener
  }

  fun setOnPickListener(listener: OnPickListener?) {
    onPickListener = listener
  }

  override fun setOnClickListener(listener: OnClickListener?) {
    onClickListener = listener
  }

  override fun setVisibility(visibility: Int) {
    super.setVisibility(visibility)
    setTouchable(visibility == VISIBLE)
  }

  fun setTouchable(touchable: Boolean) {
    isTouchable = touchable
  }

  override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    if (!gainFocus) {
      requestFocus(direction, previouslyFocusedRect)
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(v: View, event: MotionEvent): Boolean {
    if (!isTouchable) return false

    val xc = width / 2f
    val yc = height / 2f
    val x = event.x
    val y = event.y
    val isTouchInsideCircle = isTouchInsideCircle(x, y)
    val isTouchOutsideCenter = isTouchOutsideCenter(x, y)

    val angleRaw = Math.toDegrees(atan2((x - xc).toDouble(), (yc - y).toDouble()))
    val angle = if (angleRaw >= 0) angleRaw else 180 + (180 - abs(angleRaw))

    if (isTouchInsideCircle) {
      gestureDetector.onTouchEvent(event)
    }

    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        isTouchStartedInCircle = isTouchInsideCircle
        if (isTouchInsideCircle) {
          onPickListener?.onPickDown(x, y)
          if (isTouchOutsideCenter) {
            isTouchStartedInCenter = false
            currAngle = angle
          } else {
            isTouchStartedInCenter = true
          }
        }
      }

      MotionEvent.ACTION_MOVE -> {
        if (isTouchStartedInCircle) {
          if (isTouchStartedInCenter && isTouchOutsideCenter) {
            isTouchStartedInCenter = false
            currAngle = angle
          }
          if (isTouchOutsideCenter) {
            prevAngle = currAngle
            currAngle = angle
            animateRotation(prevAngle, currAngle)
          }
          onPickListener?.onDrag(x, y)
        }
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        currAngle = 0.0
        prevAngle = 0.0
        onPickListener?.onPickUpOrCancel()
      }
    }
    return true
  }

  private fun animateRotation(fromDegrees: Double, toDegrees: Double) {
    val rotate = RotateAnimation(
      fromDegrees.toFloat(),
      toDegrees.toFloat(),
      RotateAnimation.RELATIVE_TO_SELF,
      0.5f,
      RotateAnimation.RELATIVE_TO_SELF,
      0.5f
    ).apply {
      duration = 0
      isFillEnabled = true
      fillAfter = true
    }
    startAnimation(rotate)

    var degreeDiff = (toDegrees - fromDegrees).toFloat()
    if (degreeDiff > 180) {
      degreeDiff -= 360f
    } else if (degreeDiff < -180) {
      degreeDiff += 360f
    }

    onRotationListener?.onRotate(degreeDiff)

    degreeStorage += degreeDiff
    if (degreeStorage > 12) {
      onRotationListener?.onRotate(1)
      degreeStorage = 0f
    } else if (degreeStorage < -12) {
      onRotationListener?.onRotate(-1)
      degreeStorage = 0f
    }
  }

  private fun isTouchInsideCircle(x: Float, y: Float): Boolean {
    val radius = min(pivotX, pivotY)
    val centerX = pivotX.toDouble()
    val centerY = pivotY.toDouble()
    val distanceX = x - centerX
    val distanceY = y - centerY
    return (distanceX * distanceX) + (distanceY * distanceY) <= radius * radius
  }

  private fun isTouchOutsideCenter(x: Float, y: Float): Boolean {
    val centerX = pivotX.toDouble()
    val centerY = pivotY.toDouble()
    val radius = (ignoredCenterSize / 2).toDouble()
    val distanceSquared = (x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)
    return distanceSquared > (radius * radius)
  }

  interface OnRotationListener {
    fun onRotate(tempo: Int)
    fun onRotate(degrees: Float)
  }

  interface OnPickListener {
    fun onPickDown(x: Float, y: Float)
    fun onDrag(x: Float, y: Float)
    fun onPickUpOrCancel()
  }
}
