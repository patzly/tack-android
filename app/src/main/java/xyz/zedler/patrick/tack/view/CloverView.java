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

package xyz.zedler.patrick.tack.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.graphics.shapes.CornerRounding;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.ShapesKt;
import androidx.graphics.shapes.Shapes_androidKt;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class CloverView extends View {

  private final static String TAG = CloverView.class.getSimpleName();

  private final Paint paintFill, paintStroke;
  private final Path path;
  private final RectF bounds;
  private final Matrix matrix;
  private final RoundedPolygon.Companion companion;
  private final CornerRounding cornerRounding;
  private final float innerRadiusDefault, innerRadiusTap;
  private float innerRadius;
  private ValueAnimator animator;
  private boolean reduceAnimations;

  public CloverView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    paintFill = new Paint();
    paintFill.setStyle(Style.FILL);
    paintFill.setColor(ResUtil.getColor(context, R.attr.colorTertiaryContainer));
    paintStroke = new Paint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setStrokeWidth(UiUtil.dpToPx(context, 1));
    paintStroke.setColor(ResUtil.getColor(context, R.attr.colorTertiary));

    companion = RoundedPolygon.Companion;
    cornerRounding = new CornerRounding(0.32f, 0);

    innerRadiusDefault = .352f;
    innerRadiusTap = .2f;
    innerRadius = innerRadiusDefault;

    path = new Path();
    bounds = new RectF();
    matrix = new Matrix();
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawPath(path, paintFill);
    canvas.drawPath(path, paintStroke);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    updateShape();
  }

  private void updateShape() {
    RoundedPolygon star = ShapesKt.star(
        companion, 4, 1, innerRadius, cornerRounding
    );
    path.set(Shapes_androidKt.toPath(star));
    path.computeBounds(bounds, false);
    float scaleX = getWidth() / bounds.width() - 20;
    matrix.reset();
    matrix.preScale(1.25f, 1.25f);
    matrix.postScale(scaleX, scaleX);
    matrix.postTranslate(-bounds.left * scaleX, -bounds.top * scaleX);
    matrix.postRotate(45, getWidth() / 2f, getHeight() / 2f);
    path.transform(matrix);
    invalidate();
  }

  public void setTapped(boolean dragged) {
    if (animator != null) {
      animator.pause();
      animator.cancel();
    }
    if (reduceAnimations) {
      innerRadius = innerRadiusDefault;
      updateShape();
      return;
    }
    animator = ValueAnimator.ofFloat(
        innerRadius, dragged ? innerRadiusTap : innerRadiusDefault
    );
    animator.addUpdateListener(animationIn -> {
      innerRadius = (float) animationIn.getAnimatedValue();
      updateShape();
    });
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        animator = null;
      }
    });
    animator.setInterpolator(new FastOutSlowInInterpolator());
    animator.setDuration(dragged ? 40 : 300);
    animator.start();
  }

  public void setReduceAnimations(boolean reduce) {
    reduceAnimations = reduce;
  }
}