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

import static java.lang.Math.min;

import android.graphics.Matrix;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.Shapes_androidKt;

public class ShapeUtil {

  @NonNull
  public static RoundedPolygon normalize(
      @NonNull RoundedPolygon shape, boolean radial, @NonNull RectF dstBounds
  ) {
    float[] srcBoundsArray = new float[4];
    if (radial) {
      // This calculates the axis-aligned bounds of the shape and returns that rectangle. It
      // determines the max dimension of the shape (by calculating the distance from its center to
      // the start and midpoint of each curve) and returns a square which can be used to hold the
      // object in any rotation.
      shape.calculateMaxBounds(srcBoundsArray);
    } else {
      // This calculates the bounds of the shape without rotating the shape.
      shape.calculateBounds(srcBoundsArray);
    }
    RectF srcBounds =
        new RectF(srcBoundsArray[0], srcBoundsArray[1], srcBoundsArray[2], srcBoundsArray[3]);
    float scale =
        min(dstBounds.width() / srcBounds.width(), dstBounds.height() / srcBounds.height());
    // Scales the shape with pivot point at its original center then moves it to align its original
    // center with the destination bounds center.
    Matrix transform = new Matrix();
    transform.setScale(scale, scale);
    transform.preTranslate(-srcBounds.centerX(), -srcBounds.centerY());
    transform.postTranslate(dstBounds.centerX(), dstBounds.centerY());
    return Shapes_androidKt.transformed(shape, transform);
  }
}