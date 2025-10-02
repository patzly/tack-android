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

package xyz.zedler.patrick.tack.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.Shapes_androidKt;
import xyz.zedler.patrick.tack.util.ShapeUtil;

public class ShapeDrawable extends Drawable {

  private final Paint paint = new Paint();
  private final Path path = new Path();
  private final Matrix matrix = new Matrix();

  public ShapeDrawable(RoundedPolygon shape, @ColorInt int color) {
    paint.setColor(color);
    RoundedPolygon normalized = ShapeUtil.normalize(
        shape, true, new RectF(-1, -1, 1, 1)
    );
    Shapes_androidKt.toPath(normalized, path);
  }

  @Override
  protected void onBoundsChange(@NonNull Rect bounds) {
    matrix.reset();
    matrix.setScale(bounds.width() / 2f, bounds.height() / 2f);
    matrix.postTranslate(bounds.width() / 2f, bounds.height() / 2f);
    path.transform(matrix);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    canvas.drawPath(path, paint);
  }

  @Deprecated
  @Override
  public void setAlpha(int alpha) {}

  @Deprecated
  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
