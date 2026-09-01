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

package xyz.zedler.patrick.tack.ui.util

import android.graphics.Matrix
import android.graphics.RectF
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.transformed
import kotlin.math.min

fun normalize(
  shape: RoundedPolygon,
  radial: Boolean,
  dstBounds: RectF
): RoundedPolygon {
  val srcBoundsArray = FloatArray(4)
  if (radial) {
    // This calculates the axis-aligned bounds of the shape and returns that rectangle. It
    // determines the max dimension of the shape (by calculating the distance from its center to
    // the start and midpoint of each curve) and returns a square which can be used to hold the
    // object in any rotation.
    shape.calculateMaxBounds(srcBoundsArray)
  } else {
    // This calculates the bounds of the shape without rotating the shape.
    shape.calculateBounds(srcBoundsArray)
  }
  val srcBounds = RectF(
    srcBoundsArray[0],
    srcBoundsArray[1],
    srcBoundsArray[2],
    srcBoundsArray[3]
  )
  val scale = min(
    dstBounds.width() / srcBounds.width(),
    dstBounds.height() / srcBounds.height()
  )
  // Scales the shape with pivot point at its original center then moves it to align its original
  // center with the destination bounds center.
  val transform = Matrix().apply {
    setScale(scale, scale)
    preTranslate(-srcBounds.centerX(), -srcBounds.centerY())
    postTranslate(dstBounds.centerX(), dstBounds.centerY())
  }
  return shape.transformed(transform)
}