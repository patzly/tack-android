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

import static java.lang.Math.min;

import android.annotation.SuppressLint;
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
import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.graphics.shapes.Morph;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.Shapes_androidKt;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.shape.MaterialShapes;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;

public class CircleView extends View {

  private final static String TAG = CircleView.class.getSimpleName();

  private final Paint paintFill;
  private final Path path;
  private final Matrix matrix;
  private final Morph morph;
  private final int colorDefault, colorDrag;
  private float morphFactor, colorFraction;
  private boolean reduceAnimations;
  private OnDragAnimListener onDragAnimListener;
  private SpringAnimation springAnimationMorph, springAnimationColor;

  @SuppressLint("RestrictedApi")
  public CircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    colorDefault = ResUtil.getColor(context, R.attr.colorPrimaryContainer);
    colorDrag = ResUtil.getColor(context, R.attr.colorTertiaryContainer);

    paintFill = new Paint();
    paintFill.setStyle(Style.FILL);
    paintFill.setColor(colorDefault);

    morph = new Morph(
        normalize(
            MaterialShapes.COOKIE_12, true, new RectF(-1, -1, 1, 1)
        ),
        normalize(
            MaterialShapes.BURST, true, new RectF(-1, -1, 1, 1)
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
  public void setDragged(boolean dragged) {
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
      springAnimationColor.animateToFinalPosition(dragged ? 1 : 0);
    } else {
      setMorphFactor(0);
      setColorFraction(0);
      invalidate();
    }
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
    paintFill.setColor(ColorUtils.blendARGB(colorDefault, colorDrag, fraction));
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