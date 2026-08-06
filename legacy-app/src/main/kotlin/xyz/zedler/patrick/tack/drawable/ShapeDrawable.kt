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

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import xyz.zedler.patrick.tack.util.normalize
import androidx.core.graphics.withClip

class ShapeDrawable(
  context: Context,
  shape: RoundedPolygon,
  @DrawableRes drawableResId: Int
) : Drawable() {

  private val path = Path()
  private val matrix = Matrix()
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val contentDrawable: Drawable? = AppCompatResources.getDrawable(context, drawableResId)

  init {
    val normalized = normalize(
      shape, true, RectF(-1f, -1f, 1f, 1f)
    )
    normalized.toPath(path)
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)

    matrix.reset()
    matrix.setScale(bounds.width() / 2f, bounds.height() / 2f)
    matrix.postTranslate(bounds.width() / 2f, bounds.height() / 2f)
    path.transform(matrix)

    contentDrawable?.bounds = bounds
  }

  override fun draw(canvas: Canvas) {
    canvas.withClip(path) {
      contentDrawable?.draw(canvas)
    }
  }

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
    contentDrawable?.alpha = alpha
    invalidateSelf()
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.colorFilter = colorFilter
    contentDrawable?.colorFilter = colorFilter
    invalidateSelf()
  }

  @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }
}
