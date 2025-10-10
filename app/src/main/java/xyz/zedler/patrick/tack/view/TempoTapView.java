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
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.graphics.shapes.Morph;
import androidx.graphics.shapes.Shapes_androidKt;
import com.google.android.material.shape.MaterialShapes;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ShapeUtil;

public class TempoTapView extends View {

  private final static String TAG = TempoTapView.class.getSimpleName();

  private final Paint paintFill;
  private final Path path;
  private final Matrix matrix;
  private final Morph morph;
  private final int colorGradient1, colorGradient2, colorGradient3;
  private float touchFactor;
  private RadialGradient gradient;
  private boolean reduceAnimations;
  private SpringAnimation springAnimationTouch, springAnimationRelease;

  @SuppressLint("RestrictedApi")
  public TempoTapView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    colorGradient1 = ResUtil.getColor(context, R.attr.colorSecondaryContainer);
    colorGradient2 = ResUtil.getColor(context, R.attr.colorPrimaryContainer);
    colorGradient3 = ResUtil.getColor(context, R.attr.colorTertiaryContainer);

    paintFill = new Paint();
    paintFill.setStyle(Style.FILL);
    paintFill.setColor(colorGradient3);

    morph = new Morph(
        ShapeUtil.normalize(
            MaterialShapes.VERY_SUNNY, true, new RectF(-1, -1, 1, 1)
        ),
        ShapeUtil.normalize(
            MaterialShapes.SUNNY, true, new RectF(-1, -1, 1, 1)
        )
    );

    path = new Path();
    matrix = new Matrix();

    updateShape();
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    if (gradient == null) {
      float blendFraction = 0.75f;
      gradient = new RadialGradient(
          getWidth(),
          0,
          getWidth() * 1.25f,
          new int[]{
              ColorUtils.blendARGB(colorGradient3, colorGradient1, blendFraction),
              ColorUtils.blendARGB(colorGradient3, colorGradient1, blendFraction),
              ColorUtils.blendARGB(colorGradient3, colorGradient2, blendFraction),
              ColorUtils.blendARGB(colorGradient3, colorGradient3, blendFraction)
          },
          new float[]{0, 0.1f, 0.4f, 0.8f},
          TileMode.CLAMP
      );
      paintFill.setShader(gradient);
    }

    canvas.drawPath(path, paintFill);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    updateShape();
  }

  private void updateShape() {
    path.rewind();
    Shapes_androidKt.toPath(morph, touchFactor, path);
    matrix.reset();
    matrix.setScale(getWidth() / 2f, getHeight() / 2f);
    matrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
    path.transform(matrix);
  }

  @SuppressLint("PrivateResource")
  public void setTouched(boolean touched) {
    if (springAnimationTouch != null) {
      springAnimationTouch.cancel();
    }
    if (springAnimationRelease != null) {
      springAnimationRelease.cancel();
    }
    if (!reduceAnimations) {
      if (springAnimationTouch == null) {
        springAnimationTouch =
            new SpringAnimation(this, TOUCH_FACTOR)
                .setSpring(new SpringForce().setStiffness(6000f).setDampingRatio(0.9f))
                .setMinimumVisibleChange(0.01f);
      }
      if (springAnimationRelease == null) {
        springAnimationRelease =
            new SpringAnimation(this, TOUCH_FACTOR)
                .setSpring(new SpringForce().setStiffness(1400).setDampingRatio(0.4f))
                .setMinimumVisibleChange(0.01f);
      }
      if (touched) {
        springAnimationTouch.animateToFinalPosition(1);
      } else {
        springAnimationRelease.animateToFinalPosition(0);
      }
    } else {
      setTouchFactor(0);
    }
  }

  private float getTouchFactor() {
    return touchFactor;
  }

  private void setTouchFactor(float factor) {
    touchFactor = factor;
    updateShape();
    invalidate();
  }

  public void setReduceAnimations(boolean reduce) {
    reduceAnimations = reduce;
  }

  private static final FloatPropertyCompat<TempoTapView> TOUCH_FACTOR =
      new FloatPropertyCompat<>("touchFactor") {
        @Override
        public float getValue(TempoTapView delegate) {
          return delegate.getTouchFactor();
        }

        @Override
        public void setValue(TempoTapView delegate, float value) {
          delegate.setTouchFactor(value);
        }
      };
}