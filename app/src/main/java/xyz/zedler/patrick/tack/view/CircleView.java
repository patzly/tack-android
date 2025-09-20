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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.graphics.shapes.Morph;
import androidx.graphics.shapes.Shapes_androidKt;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.shape.MaterialShapes;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ShapeUtil;

public class CircleView extends View {

  private final static String TAG = CircleView.class.getSimpleName();

  private final Paint paintFill;
  private final Path path;
  private final Matrix matrix;
  private final Morph morph;
  private final int colorDefault, colorDrag1, colorDrag2, colorDrag3;
  private float morphFactor, colorFraction, touchX, touchY;
  private boolean reduceAnimations;
  private OnDragAnimListener onDragAnimListener;
  private SpringAnimation springAnimationMorph, springAnimationColor;

  @SuppressLint("RestrictedApi")
  public CircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    colorDefault = ResUtil.getColor(context, R.attr.colorPrimaryContainer);
    colorDrag1 = ResUtil.getColor(context, R.attr.colorTertiaryContainer);
    colorDrag2 = ResUtil.getColor(context, R.attr.colorPrimaryContainer);
    colorDrag3 = ResUtil.getColor(context, R.attr.colorSecondaryContainer);

    paintFill = new Paint();
    paintFill.setStyle(Style.FILL);
    paintFill.setColor(colorDefault);

    morph = new Morph(
        ShapeUtil.normalize(
            MaterialShapes.COOKIE_12, true, new RectF(-1, -1, 1, 1)
        ),
        ShapeUtil.normalize(
            MaterialShapes.SOFT_BURST, true, new RectF(-1, -1, 1, 1)
        )
    );

    path = new Path();
    matrix = new Matrix();

    updateShape();
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawPath(path, paintFill);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    updateShape();
  }

  public void setOnDragAnimListener(OnDragAnimListener listener) {
    onDragAnimListener = listener;
  }

  private void updateShape() {
    path.rewind();
    Shapes_androidKt.toPath(morph, morphFactor, path);
    matrix.reset();
    matrix.setScale(getWidth() / 2f, getHeight() / 2f);
    matrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
    path.transform(matrix);
  }

  @SuppressLint("PrivateResource")
  public void setDragged(boolean dragged, float x, float y) {
    if (dragged) {
      touchX = x;
      touchY = y;
    }
    if (springAnimationMorph != null) {
      springAnimationMorph.cancel();
    }
    if (springAnimationColor != null) {
      springAnimationColor.cancel();
    }
    if (!reduceAnimations) {
      if (springAnimationMorph == null) {
        springAnimationMorph =
            new SpringAnimation(this, MORPH_FACTOR)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringDefaultSpatial,
                        R.style.Motion_Material3_Spring_Standard_Default_Spatial)
                ).setMinimumVisibleChange(0.01f);
      }
      if (springAnimationColor == null) {
        springAnimationColor =
            new SpringAnimation(this, COLOR_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringDefaultEffects,
                        R.style.Motion_Material3_Spring_Standard_Default_Effects)
                ).setMinimumVisibleChange(0.01f);
      }
      springAnimationMorph.animateToFinalPosition(dragged ? 1 : 0);
      springAnimationColor.animateToFinalPosition(dragged ? 0.85f : 0);
    } else {
      setMorphFactor(0);
      setColorFraction(0);
    }
  }

  public void onDrag(float x, float y) {
    touchX = x;
    touchY = y;
    if (!reduceAnimations) {
      paintFill.setShader(getGradient());
    }
    invalidate();
  }

  private Shader getGradient() {
    PointF pointF = getRotatedPoint(touchX, touchY, getPivotX(), getPivotY(), -getRotation());
    return new RadialGradient(
        pointF.x,
        pointF.y,
        getWidth(),
        new int[]{
            ColorUtils.blendARGB(colorDefault, colorDrag1, colorFraction),
            ColorUtils.blendARGB(colorDefault, colorDrag1, colorFraction),
            ColorUtils.blendARGB(colorDefault, colorDrag2, colorFraction),
            ColorUtils.blendARGB(colorDefault, colorDrag3, colorFraction)
        },
        new float[]{0, 0.1f, 0.5f, 0.9f},
        TileMode.CLAMP
    );
  }

  private PointF getRotatedPoint(float x, float y, float cx, float cy, float degrees) {
    double radians = Math.toRadians(degrees);
    float x1 = x - cx;
    float y1 = y - cy;
    float x2 = (float) (x1 * Math.cos(radians) - y1 * Math.sin(radians));
    float y2 = (float) (x1 * Math.sin(radians) + y1 * Math.cos(radians));
    return new PointF(x2 + cx, y2 + cy);
  }

  private float getMorphFactor() {
    return morphFactor;
  }

  private void setMorphFactor(float factor) {
    morphFactor = factor;
    updateShape();
    invalidate();
  }

  private float getColorFraction() {
    return colorFraction;
  }

  private void setColorFraction(float fraction) {
    colorFraction = fraction;

    paintFill.setShader(getGradient());

    invalidate();
    if (onDragAnimListener != null) {
      onDragAnimListener.onDragAnim(fraction);
    }
  }

  public void setReduceAnimations(boolean reduce) {
    reduceAnimations = reduce;
  }

  public interface OnDragAnimListener {
    void onDragAnim(float fraction);
  }

  private static final FloatPropertyCompat<CircleView> MORPH_FACTOR =
      new FloatPropertyCompat<>("morphFactor") {
        @Override
        public float getValue(CircleView delegate) {
          return delegate.getMorphFactor();
        }

        @Override
        public void setValue(CircleView delegate, float value) {
          delegate.setMorphFactor(value);
        }
      };
  private static final FloatPropertyCompat<CircleView> COLOR_FRACTION =
      new FloatPropertyCompat<>("colorFraction") {
        @Override
        public float getValue(CircleView delegate) {
          return delegate.getColorFraction();
        }

        @Override
        public void setValue(CircleView delegate, float value) {
          delegate.setColorFraction(value);
        }
      };
}