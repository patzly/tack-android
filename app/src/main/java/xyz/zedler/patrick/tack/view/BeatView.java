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
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.graphics.shapes.Morph;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.Shapes_androidKt;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.MaterialShapes;
import java.util.Random;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ShapeUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class BeatView extends FrameLayout {

  @SuppressLint("RestrictedApi")
  private static final RoundedPolygon[] SHAPES =
      new RoundedPolygon[] {
          ShapeUtil.normalize(
              MaterialShapes.CIRCLE, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.SQUARE, false, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.SLANTED_SQUARE, false, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.OVAL, false, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.PILL, false, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.DIAMOND, false, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.PENTAGON, false, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.VERY_SUNNY, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.SUNNY, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.COOKIE_4, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.COOKIE_6, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.COOKIE_7, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.COOKIE_9, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.BURST, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.SOFT_BURST, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.BOOM, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.SOFT_BOOM, true, new RectF(-1, -1, 1, 1)
          ),
          ShapeUtil.normalize(
              MaterialShapes.FLOWER, true, new RectF(-1, -1, 1, 1)
          ),
      };

  private static final Morph[] MORPHS = new Morph[SHAPES.length];
  static {
    for (int i = 0; i < SHAPES.length; i++) {
      MORPHS[i] = new Morph(SHAPES[0], SHAPES[i]);
    }
  }
  private static final boolean TEST_ANIMATIONS = false;

  private final Path path = new Path();
  private final Matrix matrix = new Matrix();
  private final Random random = new Random();
  private final FastOutSlowInInterpolator interpolator;
  private final MaterialButton button;
  private final Paint paintFill, paintStroke;
  private final float shapeScaleSub, shapeScaleBeatSub, shapeScaleNoBeat, shapeScaleMuted;
  private final int colorActive;
  private AnimatorSet animatorSet;
  private ValueAnimator strokeAnimator;
  private SpringAnimation springAnimationTickType;
  private Morph morph;
  private String tickType;
  private boolean isSubdivision, reduceAnimations, isActive;
  private float morphFactor, tickTypeFraction, shapeScaleBeat, shapeScale0, shapeScale1;
  private int index;
  private int colorFillSource, colorFillTarget, colorStrokeSource, colorStrokeTarget;
  private float shapeScale0Source, shapeScale1Source, shapeScale0Target, shapeScale1Target;

  public BeatView(Context context) {
    super(context);

    setWillNotDraw(false);

    tickType = TICK_TYPE.NORMAL;

    int minSize = UiUtil.dpToPx(context, 48);
    setMinimumWidth(minSize);
    setMinimumHeight(minSize);

    button = new MaterialButton(context, null, R.attr.materialIconButtonStyle);
    button.setStrokeWidth(UiUtil.dpToPx(context, 1));
    button.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
    setOnClickListener(null);
    addView(button);

    shapeScaleNoBeat = 0.25f;
    shapeScaleBeat = 0.75f;
    shapeScaleSub = shapeScaleBeat;
    shapeScaleBeatSub = 0.4f;
    shapeScaleMuted = 0.1f;
    shapeScale0 = shapeScaleNoBeat;
    shapeScale1 = shapeScaleBeat;

    colorActive = ResUtil.getColor(context, R.attr.colorOutline);

    paintFill = new Paint();
    paintFill.setStyle(Style.FILL);

    paintStroke = new Paint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setStrokeWidth(UiUtil.dpToPx(context, 2));

    interpolator = new FastOutSlowInInterpolator();

    morph = MORPHS[0];

    setTickType(TICK_TYPE.NORMAL, false);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.removeAllListeners();
      animatorSet.cancel();
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    updateShape();
    invalidate();
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    canvas.drawPath(path, paintFill);
    canvas.drawPath(path, paintStroke);
  }

  public void setIndex(int index) {
    this.index = index;
    setTickType(tickType, false);
  }

  public int getIndex() {
    return index;
  }

  public void setIsSubdivision(boolean isSubdivision) {
    this.isSubdivision = isSubdivision;
    setTickType(TICK_TYPE.SUB, false);
  }

  public void setTickType(String tickType, boolean animated) {
    this.tickType = tickType;

    Context context = getContext();
    int colorNormal = ResUtil.getColor(context, R.attr.colorPrimary);
    if (isColorRed(colorNormal)) {
      colorNormal = ResUtil.getColor(context, R.attr.colorTertiary);
    }
    int colorStrong = ResUtil.getColor(context, R.attr.colorError);
    int colorSub = ResUtil.getColor(context, R.attr.colorOnSurfaceVariant);
    int colorMuted = ResUtil.getColor(context, R.attr.colorOutline);

    colorFillSource = paintFill.getColor();
    colorStrokeSource = paintStroke.getColor();
    shapeScale0Source = shapeScale0;
    shapeScale1Source = shapeScale1;
    int colorTarget, alphaTarget;
    switch (tickType) {
      case TICK_TYPE.STRONG:
        colorTarget = colorStrong;
        alphaTarget = 255;
        shapeScale0Target = shapeScaleNoBeat;
        shapeScale1Target = shapeScaleBeat;
        break;
      case TICK_TYPE.MUTED:
      case TICK_TYPE.BEAT_SUB_MUTED:
        colorTarget = colorMuted;
        alphaTarget = 255;
        shapeScale0Target = shapeScaleMuted;
        shapeScale1Target = shapeScaleNoBeat;
        break;
      case TICK_TYPE.SUB:
        colorTarget = colorSub;
        alphaTarget = 0;
        shapeScale0Target = shapeScaleNoBeat;
        shapeScale1Target = shapeScaleSub;
        break;
      case TICK_TYPE.BEAT_SUB:
        colorTarget = colorMuted;
        alphaTarget = 255;
        shapeScale0Target = shapeScaleNoBeat;
        shapeScale1Target = shapeScaleBeatSub;
        break;
      default:
        colorTarget = colorNormal;
        alphaTarget = (int) (0.3f * 255);
        shapeScale0Target = shapeScaleNoBeat;
        shapeScale1Target = shapeScaleBeat;
    }
    colorFillTarget = ColorUtils.setAlphaComponent(colorTarget, alphaTarget);
    colorStrokeTarget = colorTarget;

    if (springAnimationTickType != null) {
      springAnimationTickType.cancel();
    }
    if (animated) {
      tickTypeFraction = 0;
      if (springAnimationTickType == null) {
        springAnimationTickType =
            new SpringAnimation(this, TICK_TYPE_FRACTION)
                .setSpring(new SpringForce().setStiffness(1400f).setDampingRatio(0.6f))
                .setMinimumVisibleChange(0.01f);
      }
      if (TEST_ANIMATIONS) {
        springAnimationTickType.setSpring(
            new SpringForce().setStiffness(20f).setDampingRatio(0.3f)
        );
      }
      springAnimationTickType.animateToFinalPosition(1);
    } else {
      setTickTypeFraction(1);
    }
  }

  public String nextTickType() {
    String next;
    switch (tickType) {
      case TICK_TYPE.NORMAL:
        next = isSubdivision ? TICK_TYPE.MUTED : Constants.TICK_TYPE.STRONG;
        break;
      case TICK_TYPE.STRONG:
        next = TICK_TYPE.MUTED;
        break;
      case TICK_TYPE.SUB:
        next = TICK_TYPE.NORMAL;
        break;
      case TICK_TYPE.BEAT_SUB:
        next = TICK_TYPE.BEAT_SUB_MUTED;
        break;
      case TICK_TYPE.BEAT_SUB_MUTED:
        next = TICK_TYPE.BEAT_SUB;
        break;
      default:
        next = isSubdivision ? TICK_TYPE.SUB : Constants.TICK_TYPE.NORMAL;
    }
    setTickType(next, true);
    return next;
  }

  public void beat() {
    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.removeAllListeners();
      animatorSet.cancel();
      animatorSet = null;
    }

    if (reduceAnimations) {
      return;
    }

    if (tickType.equals(TICK_TYPE.MUTED)
        || tickType.equals(TICK_TYPE.BEAT_SUB)
        ||  tickType.equals(TICK_TYPE.BEAT_SUB_MUTED)
    ) {
      morph = MORPHS[0];
    } else {
      int index = 1 + random.nextInt(MORPHS.length - 1);
      morph = MORPHS[index];
    }

    ValueAnimator animatorIn = ValueAnimator.ofFloat(0, 1);
    animatorIn.addUpdateListener(
        animation -> setMorphFactor((float) animation.getAnimatedValue())
    );
    animatorIn.setInterpolator(interpolator);
    animatorIn.setDuration(25);

    ValueAnimator animatorOut = ValueAnimator.ofFloat(1, 0);
    animatorOut.addUpdateListener(
        animation -> setMorphFactor((float) animation.getAnimatedValue())
    );
    animatorOut.setInterpolator(interpolator);
    animatorOut.setDuration(375);

    animatorSet = new AnimatorSet();
    animatorSet.playSequentially(animatorIn, animatorOut);
    animatorSet.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        animatorSet = null;
      }
    });
    animatorSet.start();
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener l) {
    if (l != null) {
      button.setOnClickListener(l);
    }
    button.setEnabled(l != null);
  }

  public void setReduceAnimations(boolean reduce) {
    reduceAnimations = reduce;
  }

  public void setActive(boolean active) {
    if (isActive == active) {
      return;
    }
    isActive = active;
    // update beat scale for surrounding circle
    shapeScaleBeat = active ? 0.6f : 0.75f;
    shapeScale1 = tickType.equals(TICK_TYPE.MUTED) ? shapeScaleMuted : shapeScaleBeat;
    if (strokeAnimator != null) {
      strokeAnimator.pause();
      strokeAnimator.removeAllListeners();
      strokeAnimator.cancel();
      strokeAnimator = null;
    }
    strokeAnimator = ValueAnimator.ofArgb(
        button.getStrokeColor().getDefaultColor(),
        active ? colorActive : Color.TRANSPARENT
    );
    strokeAnimator.addUpdateListener(animation -> {
      int color = (int) animation.getAnimatedValue();
      button.setStrokeColor(ColorStateList.valueOf(color));
    });
    strokeAnimator.setInterpolator(interpolator);
    strokeAnimator.setDuration(active ? 25 : 300);
    strokeAnimator.start();
  }

  private void updateShape() {
    path.rewind();
    Shapes_androidKt.toPath(morph, morphFactor, path);
    matrix.reset();
    float scale = shapeScale0 + morphFactor * (shapeScale1 - shapeScale0);
    matrix.setScale(getWidth() / 2f * scale, getHeight() / 2f * scale);
    matrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
    path.transform(matrix);
  }

  private void setMorphFactor(float factor) {
    morphFactor = factor;
    updateShape();
    invalidate();
  }

  private void setTickTypeFraction(float fraction) {
    tickTypeFraction = fraction;
    float colorFraction = Math.min(Math.max(fraction, 0), 1);
    paintFill.setColor(ColorUtils.blendARGB(colorFillSource, colorFillTarget, colorFraction));
    paintStroke.setColor(ColorUtils.blendARGB(colorStrokeSource, colorStrokeTarget, colorFraction));
    shapeScale0 = shapeScale0Source + fraction * (shapeScale0Target - shapeScale0Source);
    shapeScale1 = shapeScale1Source + fraction * (shapeScale1Target - shapeScale1Source);
    updateShape();
    invalidate();
  }

  public float getTickTypeFraction() {
    return tickTypeFraction;
  }

  @NonNull
  @Override
  public String toString() {
    return tickType;
  }

  public static boolean isColorRed(int color) {
    int tolerance = 30;
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return red > green + tolerance && red > blue + tolerance;
  }

  private static final FloatPropertyCompat<BeatView> TICK_TYPE_FRACTION =
      new FloatPropertyCompat<>("tickTypeFraction") {
        @Override
        public float getValue(BeatView delegate) {
          return delegate.getTickTypeFraction();
        }

        @Override
        public void setValue(BeatView delegate, float value) {
          delegate.setTickTypeFraction(value);
        }
      };
}
