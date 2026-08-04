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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.dpToPx

class PartBeatsView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val circleSize = context.dpToPx(10f)
  private val circleSizeMuted = context.dpToPx(5f)
  private val circleSpace = context.dpToPx(8f)

  private val colorNormal: Int
  private val colorStrong = context.getAttrColor(R.attr.colorError)
  private val colorSub = context.getAttrColor(R.attr.colorOnSurfaceVariant)
  private val colorMuted = context.getAttrColor(R.attr.colorOutline)

  private val paintSolid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
  }
  private val paintOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = context.dpToPx(2f).toFloat()
  }

  private var beats: Array<String> = emptyArray()

  init {
    val primary = context.getAttrColor(R.attr.colorPrimary)
    colorNormal = if (BeatView.isColorRed(primary)) {
      context.getAttrColor(R.attr.colorTertiary)
    } else {
      primary
    }
  }

  fun setBeats(beats: Array<String>) {
    this.beats = beats
    invalidate()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)

    val measuredWidth = if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
      widthSize
    } else {
      0
    }

    val measuredHeight = if (heightMode == MeasureSpec.EXACTLY) {
      heightSize
    } else {
      val h = circleSize + paddingTop + paddingBottom
      if (heightMode == MeasureSpec.AT_MOST) h.coerceAtMost(heightSize) else h
    }

    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val availableHeight = height - paddingTop - paddingBottom
    val diameter = circleSize.coerceAtMost(availableHeight)
    val radius = diameter / 2f

    var startX = paddingLeft + radius
    val centerY = paddingTop + radius
    val strokeWidthOffset = paintOutline.strokeWidth / 2

    for (beat in beats) {
      adjustPaint(beat)
      val radiusFinal = if (beat == TICK_TYPE.MUTED || beat == TICK_TYPE.BEAT_SUB_MUTED) {
        circleSizeMuted / 2f
      } else {
        radius
      }
      canvas.drawCircle(startX, centerY, radiusFinal - strokeWidthOffset, paintSolid)
      canvas.drawCircle(startX, centerY, radiusFinal - strokeWidthOffset, paintOutline)
      startX += circleSize + circleSpace
    }
  }

  private fun adjustPaint(beat: String) {
    when (beat) {
      TICK_TYPE.NORMAL -> {
        paintSolid.color = colorNormal
        paintSolid.alpha = (0.3 * 255).toInt()
        paintOutline.color = colorNormal
      }

      TICK_TYPE.STRONG -> {
        paintSolid.color = colorStrong
        paintSolid.alpha = 255
        paintOutline.color = colorStrong
      }

      TICK_TYPE.SUB -> {
        paintSolid.alpha = 0
        paintOutline.color = colorSub
      }

      TICK_TYPE.MUTED, TICK_TYPE.BEAT_SUB, TICK_TYPE.BEAT_SUB_MUTED -> {
        paintSolid.color = colorMuted
        paintSolid.alpha = 255
        paintOutline.color = colorMuted
      }
    }
  }
}
