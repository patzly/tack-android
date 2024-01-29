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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package com.google.android.material.slider;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.google.android.material.shape.MaterialShapeDrawable;
import java.lang.reflect.Method;
import java.util.Objects;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class CustomSlider extends Slider {

  private static final String TAG = "CustomSlider";
  private static final float THUMB_WIDTH_PRESSED_RATIO = .5f;
  private static final long THUMB_WIDTH_ANIM_DURATION = 200;

  private final RectF clipRect = new RectF();
  private final RectF trackRect = new RectF();
  private final RectF cornerRect = new RectF();
  private final Path trackPath = new Path();
  private final Path cornerPath = new Path();
  private final Paint inactiveTrackPaint = new Paint();
  private final Paint activeTrackPaint = new Paint();
  private final Paint inactiveTicksPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint activeTicksPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint stopIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private ValueAnimator thumbWidthAnimator, thumbPositionAnimator;
  private MaterialShapeDrawable thumbDrawable;
  private int thumbWidth, thumbWidthAnim, minTickSpacing;
  private float normalizedValueAnim;
  private float[] ticksCoordinates;
  private boolean dirtyConfig;

  public CustomSlider(@NonNull Context context) {
    super(context);
    init(context);
  }

  public CustomSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public CustomSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    dirtyConfig = true;
    thumbDrawable = new MaterialShapeDrawable();
    thumbDrawable.setShadowCompatibilityMode(
        MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
    );
    thumbDrawable.setFillColor(getTrackActiveTintList());
    thumbWidth = UiUtil.dpToPx(context, 4);
    minTickSpacing = UiUtil.dpToPx(context, 4);
    setTrackActiveTintList(getTrackActiveTintList());
    setTrackInactiveTintList(getTrackInactiveTintList());
    setTickInactiveRadius(getTickInactiveRadius());
    setTickInactiveTintList(getTickInactiveTintList());
    setTickActiveRadius(getTickActiveRadius());
    setTickActiveTintList(getTickActiveTintList());
    setTrackStopIndicatorSize(getTrackStopIndicatorSize());

    inactiveTicksPaint.setStyle(Style.STROKE);
    inactiveTicksPaint.setStrokeCap(Cap.ROUND);

    activeTicksPaint.setStyle(Style.STROKE);
    activeTicksPaint.setStrokeCap(Cap.ROUND);

    stopIndicatorPaint.setStyle(Style.FILL);
    stopIndicatorPaint.setStrokeCap(Cap.ROUND);

    updateThumbWidth(false, false);
    addOnSliderTouchListener(new OnSliderTouchListener() {
      @Override
      public void onStartTrackingTouch(@NonNull Slider slider) {
        updateThumbWidth(true, true);
      }

      @Override
      public void onStopTrackingTouch(@NonNull Slider slider) {
        updateThumbWidth(false, true);
      }
    });
    addOnChangeListener((slider, value, fromUser) -> updateThumbPosition(
        value, fromUser && getStepSize() > 0
    ));
    getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            updateThumbPosition(getValue(), false);
            if (getViewTreeObserver().isAlive()) {
              getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    if (dirtyConfig) {
      validateConfigurationIfDirty();
      maybeCalculateTicksCoordinates();
    }

    int yCenter = calculateTrackCenter();
    drawInactiveTrack(canvas, yCenter);
    drawActiveTrack(canvas, yCenter);

    maybeDrawTicks(canvas);
    maybeDrawStopIndicator(canvas, yCenter);

    drawThumb(canvas, yCenter);
  }

  @Override
  public void setValueFrom(float valueFrom) {
    dirtyConfig = true;
    super.setValueFrom(valueFrom);
  }

  @Override
  public void setValueTo(float valueTo) {
    dirtyConfig = true;
    super.setValueTo(valueTo);
  }

  @Override
  public void setStepSize(float stepSize) {
    dirtyConfig = true;
    super.setStepSize(stepSize);
  }

  @Override
  public void setThumbWidth(@IntRange(from = 0) @Px int width) {
    super.setThumbWidth(thumbWidth);
  }

  @Override
  public void setThumbHeight(int height) {
    if (thumbDrawable != null) {
      thumbDrawable.setBounds(0, 0, thumbWidthAnim, height);
    }
    super.setThumbHeight(height);
  }

  private void drawThumb(@NonNull Canvas canvas, int yCenter) {
    canvas.save();
    canvas.translate(
        getTrackSidePadding()
            + normalizedValueAnim * getTrackWidth()
            - (thumbDrawable.getBounds().width() / 2f),
        yCenter - (thumbDrawable.getBounds().height() / 2f));
    thumbDrawable.draw(canvas);
    canvas.restore();
  }

  private void drawInactiveTrack(@NonNull Canvas canvas, int yCenter) {
    int trackWidth = getTrackWidth();
    int trackHeight = getTrackHeight();
    int trackSidePadding = getTrackSidePadding();
    int thumbTrackGapSize = getThumbTrackGapSize();

    float[] activeRange = getActiveRange();
    float left = trackSidePadding + activeRange[1] * trackWidth + thumbWidthAnim / 2f;

    if (thumbTrackGapSize > 0) {
      left += thumbTrackGapSize;
      float right = trackSidePadding + trackWidth + trackHeight / 2f;
      left = Math.min(
          left, right - trackHeight / 2f - thumbTrackGapSize - getTrackInsideCornerSize()
      );
      trackRect.set(
          left,
          yCenter - trackHeight / 2f,
          trackSidePadding + trackWidth + trackHeight / 2f,
          yCenter + trackHeight / 2f);

      float thumbPosition = trackSidePadding + activeRange[1] * trackWidth;
      clipRect.set(
          thumbPosition + thumbTrackGapSize + thumbWidthAnim / 2f,
          yCenter - trackHeight / 2f,
          getTrackSidePadding() + getTrackWidth() + trackHeight / 2f,
          yCenter + trackHeight / 2f
      );

      boolean isThumbAtEnd = activeRange[1] * trackWidth == trackWidth;
      if (!isThumbAtEnd) {
        updateTrack(canvas, inactiveTrackPaint, FullCornerDirection.RIGHT);
      }
    } else {
      inactiveTrackPaint.setStyle(Style.STROKE);
      inactiveTrackPaint.setStrokeCap(Cap.ROUND);
      canvas.drawLine(
          left, yCenter, trackSidePadding + trackWidth, yCenter, inactiveTrackPaint
      );
    }
  }

  private void drawActiveTrack(@NonNull Canvas canvas, int yCenter) {
    int trackWidth = getTrackWidth();
    int trackHeight = getTrackHeight();
    int trackSidePadding = getTrackSidePadding();
    int thumbTrackGapSize = getThumbTrackGapSize();

    float[] activeRange = getActiveRange();
    float left = trackSidePadding + activeRange[0] * trackWidth;
    float right = trackSidePadding + activeRange[1] * trackWidth - thumbWidthAnim / 2f;

    if (thumbTrackGapSize > 0) {
      FullCornerDirection direction;
      direction = isRtl() ? FullCornerDirection.RIGHT : FullCornerDirection.LEFT;

      if (isRtl()) { // Swap left right
        float temp = left;
        left = right;
        right = temp;
      }

      int trackInsideCornerSize = getTrackInsideCornerSize();
      switch (direction) {
        case LEFT:
          left -= trackHeight / 2f;
          right -= thumbTrackGapSize;
          right = Math.max(
              right, left + trackHeight / 2f + thumbTrackGapSize + trackInsideCornerSize
          );
          break;
        case RIGHT:
          left += thumbTrackGapSize;
          left = Math.max(left, trackHeight / 2f + thumbTrackGapSize);
          right += trackHeight / 2f;
          break;
        default:
          // fall through
      }
      float top = yCenter - trackHeight / 2f;
      float bottom = yCenter + trackHeight / 2f;
      trackRect.set(left, top, right, bottom);

      float thumbPosition = trackSidePadding + activeRange[1] * trackWidth;
      clipRect.set(
          left,
          yCenter - trackHeight / 2f,
          thumbPosition - thumbWidthAnim / 2f - thumbTrackGapSize,
          yCenter + trackHeight / 2f
      );

      boolean isThumbAtStart = activeRange[1] * trackWidth == 0;
      if (!isThumbAtStart) {
        // Only draw active track if thumb is not at start
        // Else the thumb and gaps won't cover the track entirely
        updateTrack(canvas, activeTrackPaint, direction);
      }
    } else {
      activeTrackPaint.setStyle(Style.STROKE);
      activeTrackPaint.setStrokeCap(Cap.ROUND);
      canvas.drawLine(left, yCenter, right, yCenter, activeTrackPaint);
    }
  }

  private void updateTrack(Canvas canvas, Paint paint, FullCornerDirection direction) {
    int trackHeight = getTrackHeight();
    int trackInsideCornerSize = getTrackInsideCornerSize();
    float leftCornerSize = trackHeight / 2f;
    float rightCornerSize = trackHeight / 2f;
    switch (direction) {
      case BOTH:
        break;
      case LEFT:
        rightCornerSize = trackInsideCornerSize;
        break;
      case RIGHT:
        leftCornerSize = trackInsideCornerSize;
        break;
      case NONE:
        leftCornerSize = trackInsideCornerSize;
        rightCornerSize = trackInsideCornerSize;
        break;
    }
    trackRect.left += leftCornerSize;
    trackRect.right -= rightCornerSize;

    // Build track path with rounded corners
    trackPath.reset();
    trackPath.addRect(trackRect, Direction.CW);
    addRoundedCorners(trackPath, trackRect, leftCornerSize, rightCornerSize);

    // Mask the track
    canvas.save();
    canvas.clipRect(clipRect);

    // Draw the track
    paint.setStyle(Style.FILL);
    paint.setStrokeCap(Cap.BUTT);
    paint.setAntiAlias(true);
    canvas.drawPath(trackPath, paint);

    canvas.restore();
  }

  private void addRoundedCorners(
      Path path, RectF bounds, float leftCornerSize, float rightCornerSize
  ) {
    cornerRect.set(
        bounds.left - leftCornerSize,
        bounds.top,
        bounds.left + leftCornerSize,
        bounds.bottom
    );
    cornerPath.reset();
    cornerPath.addRoundRect(cornerRect, leftCornerSize, leftCornerSize, Direction.CW);
    path.op(cornerPath, Path.Op.UNION);
    cornerRect.set(
        bounds.right - rightCornerSize,
        bounds.top,
        bounds.right + rightCornerSize,
        bounds.bottom
    );
    cornerPath.reset();
    cornerPath.addRoundRect(cornerRect, rightCornerSize, rightCornerSize, Direction.CW);
    path.op(cornerPath, Path.Op.UNION);
  }

  private void maybeDrawTicks(@NonNull Canvas canvas) {
    if (!isTickVisible() || getStepSize() <= 0.0f) {
      return;
    }

    float[] activeRange = getActiveRange();
    int leftPivotIndex = pivotIndex(ticksCoordinates, activeRange[0]);
    int rightPivotIndex = pivotIndex(ticksCoordinates, activeRange[1]);

    canvas.save();
    int trackHeight = getTrackHeight();
    int trackCenter = calculateTrackCenter();
    int gapSize = getThumbTrackGapSize();
    float thumbPosition = getTrackSidePadding() + activeRange[1] * getTrackWidth();
    clipRect.set(
        getTrackSidePadding() - trackHeight / 2f,
        trackCenter - trackHeight / 2f,
        thumbPosition - thumbWidthAnim / 2f - gapSize,
        trackCenter + trackHeight / 2f
    );
    canvas.clipRect(clipRect);

    // Draw active ticks.
    canvas.drawPoints(
        ticksCoordinates,
        leftPivotIndex * 2,
        rightPivotIndex * 2 - leftPivotIndex * 2,
        activeTicksPaint
    );
    canvas.restore();

    int length = ticksCoordinates.length - rightPivotIndex * 2;
    if (shouldDrawStopIndicator() && length > 0) {
      length -= 2; // reduce length so that the last tick is not drawn
    }

    canvas.save();
    clipRect.set(
        thumbPosition + gapSize + thumbWidthAnim / 2f,
        trackCenter - trackHeight / 2f,
        getTrackSidePadding() + getTrackWidth() + trackHeight / 2f,
        trackCenter + trackHeight / 2f
    );
    canvas.clipRect(clipRect);

    // Draw inactive ticks to the right of the thumb.
    canvas.drawPoints(
        ticksCoordinates,
        rightPivotIndex * 2,
        length,
        inactiveTicksPaint
    );

    canvas.restore();
  }

  private static int pivotIndex(float[] coordinates, float position) {
    return Math.round(position * (coordinates.length / 2f - 1));
  }

  private boolean shouldDrawStopIndicator() {
    return getTrackStopIndicatorSize() > 0 && normalizedValueAnim < normalizeValue(getValueTo());
  }

  private void maybeDrawStopIndicator(@NonNull Canvas canvas, int yCenter) {
    // Draw stop indicator at the end of the track.
    if (shouldDrawStopIndicator()) {
      float x = normalizeValue(getValueTo()) * getTrackWidth() + getTrackSidePadding();
      canvas.drawPoint(x, yCenter, stopIndicatorPaint);
    }
  }

  public void setTrackActiveTintList(@NonNull ColorStateList trackColor) {
    if (activeTrackPaint != null) {
      activeTrackPaint.setColor(getColorForState(trackColor));
    }
    if (stopIndicatorPaint != null) {
      stopIndicatorPaint.setColor(getColorForState(trackColor));
    }
    super.setTrackActiveTintList(trackColor);
  }

  @NonNull
  @Override
  public ColorStateList getTrackActiveTintList() {
    return new ColorStateList(
        new int[][] {
            new int[] {android.R.attr.state_enabled},
            new int[] {},
        },
        new int[] {
            ResUtil.getColor(getContext(), R.attr.colorPrimary),
            ResUtil.getColor(getContext(), R.attr.colorOnSurface, 0.38f)
        }
    );
  }

  @Override
  public void setTrackInactiveTintList(@NonNull ColorStateList trackColor) {
    if (inactiveTrackPaint != null) {
      inactiveTrackPaint.setColor(getColorForState(trackColor));
    }
    super.setTrackInactiveTintList(trackColor);
  }

  @NonNull
  @Override
  public ColorStateList getTrackInactiveTintList() {
    return new ColorStateList(
        new int[][] {
            new int[] {android.R.attr.state_enabled},
            new int[] {},
        },
        new int[] {
            ResUtil.getColor(getContext(), R.attr.colorPrimaryContainer),
            ResUtil.getColor(getContext(), R.attr.colorOnSurfaceVariant, 0.12f)
        }
    );
  }

  private void updateThumbWidth(boolean dragged, boolean animate) {
    if (thumbWidthAnimator != null) {
      thumbWidthAnimator.cancel();
      thumbWidthAnimator.removeAllUpdateListeners();
      thumbWidthAnimator.removeAllListeners();
    }
    if (thumbDrawable == null) {
      return;
    }
    int thumbWidthNew = dragged ? (int) (thumbWidth * THUMB_WIDTH_PRESSED_RATIO) : thumbWidth;
    if (animate) {
      thumbWidthAnimator = ValueAnimator.ofInt(thumbWidthAnim, thumbWidthNew);
      thumbWidthAnimator.addUpdateListener(animation -> {
        thumbWidthAnim = (int) animation.getAnimatedValue();
        thumbDrawable.setShapeAppearanceModel(
            thumbDrawable.getShapeAppearanceModel().withCornerSize(thumbWidthAnim / 2f)
        );
        thumbDrawable.setBounds(0, 0, thumbWidthAnim, getThumbHeight());
        invalidate();
      });
      thumbWidthAnimator.setDuration(THUMB_WIDTH_ANIM_DURATION);
      thumbWidthAnimator.start();
    } else {
      thumbWidthAnim = thumbWidthNew;
      thumbDrawable.setShapeAppearanceModel(
          thumbDrawable.getShapeAppearanceModel().withCornerSize(thumbWidthAnim / 2f)
      );
      thumbDrawable.setBounds(0, 0, thumbWidthAnim, getThumbHeight());
      invalidate();
    }
  }

  private void updateThumbPosition(float value, boolean animate) {
    if (thumbPositionAnimator != null) {
      thumbPositionAnimator.cancel();
      thumbPositionAnimator.removeAllUpdateListeners();
      thumbPositionAnimator.removeAllListeners();
    }
    float thumbPositionNew = normalizeValue(value);
    if (animate) {
      thumbPositionAnimator = ValueAnimator.ofFloat(normalizedValueAnim, thumbPositionNew);
      thumbPositionAnimator.addUpdateListener(animation -> {
        normalizedValueAnim = (float) animation.getAnimatedValue();
        invalidate();
      });
      long duration = 4L * UiUtil.dpFromPx(getContext(), getTickInterval(getTickCount()));
      thumbPositionAnimator.setDuration(Math.max(50, Math.min(duration, 200)));
      thumbPositionAnimator.start();
    } else {
      normalizedValueAnim = thumbPositionNew;
      invalidate();
    }
  }

  private float normalizeValue(float value) {
    float normalized = (value - getValueFrom()) / (getValueTo() - getValueFrom());
    if (isRtl()) {
      return 1 - normalized;
    }
    return normalized;
  }

  private int calculateTrackCenter() {
    try {
      Method method = BaseSlider.class.getDeclaredMethod("calculateTrackCenter");
      method.setAccessible(true);
      Integer result = (Integer) method.invoke(this);
      return Objects.requireNonNullElse(result, getHeight() / 2);
    } catch (Exception e) {
      return getHeight() / 2;
    }
  }

  private void validateConfigurationIfDirty() {
    try {
      Method method = BaseSlider.class.getDeclaredMethod("validateConfigurationIfDirty");
      method.setAccessible(true);
      method.invoke(this);
      dirtyConfig = false;
    } catch (Exception e) {
      Log.e(TAG, "validateConfigurationIfDirty: ", e);
    }
  }

  private void maybeCalculateTicksCoordinates() {
    if (getStepSize() <= 0.0f) {
      return;
    }
    validateConfigurationIfDirty();

    int tickCount = getTickCount();
    if (ticksCoordinates == null || ticksCoordinates.length != tickCount * 2) {
      ticksCoordinates = new float[tickCount * 2];
    }
    float interval = getTickInterval(tickCount);
    for (int i = 0; i < tickCount * 2; i += 2) {
      ticksCoordinates[i] = getTrackSidePadding() + i / 2f * interval;
      ticksCoordinates[i + 1] = calculateTrackCenter();
    }
  }

  private int getTickCount() {
    int tickCount = (int) ((getValueTo() - getValueFrom()) / getStepSize() + 1);
    // Limit the tickCount if they will be too dense.
    if (getTickInterval(tickCount) < minTickSpacing) {
      tickCount = tickCount / 2 + 1;
    }
    return tickCount;
  }

  private float getTickInterval(int tickCount) {
    return getTrackWidth() / (float) (tickCount - 1);
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    if (thumbDrawable.isStateful()) {
      thumbDrawable.setState(getDrawableState());
    }
    inactiveTicksPaint.setColor(getColorForState(getTickInactiveTintList()));
    activeTicksPaint.setColor(getColorForState(getTickActiveTintList()));
    stopIndicatorPaint.setColor(getColorForState(getTrackActiveTintList()));
    activeTrackPaint.setColor(getColorForState(getTrackActiveTintList()));
    inactiveTrackPaint.setColor(getColorForState(getTrackInactiveTintList()));
  }

  @Override
  public void setTickActiveRadius(@IntRange(from = 0) @Px int tickActiveRadius) {
    if (activeTicksPaint != null) {
      activeTicksPaint.setStrokeWidth(tickActiveRadius * 2);
    }
    super.setTickActiveRadius(tickActiveRadius);
  }

  public void setTickActiveTintList(@NonNull ColorStateList tickColor) {
    if (activeTicksPaint != null) {
      activeTicksPaint.setColor(getColorForState(tickColor));
    }
    super.setTickActiveTintList(tickColor);
  }

  @Override
  public void setTickInactiveRadius(@IntRange(from = 0) @Px int tickInactiveRadius) {
    if (inactiveTicksPaint != null) {
      inactiveTicksPaint.setStrokeWidth(tickInactiveRadius * 2);
    }
    super.setTickInactiveRadius(tickInactiveRadius);
  }

  public void setTickInactiveTintList(@NonNull ColorStateList tickColor) {
    if (inactiveTicksPaint != null) {
      inactiveTicksPaint.setColor(getColorForState(tickColor));
    }
    super.setTickInactiveTintList(tickColor);
  }

  public void setTrackStopIndicatorSize(@Px int trackStopIndicatorSize) {
    if (stopIndicatorPaint != null) {
      stopIndicatorPaint.setStrokeWidth(trackStopIndicatorSize);
    }
    super.setTrackStopIndicatorSize(trackStopIndicatorSize);
  }

  @ColorInt
  private int getColorForState(@NonNull ColorStateList colorStateList) {
    return colorStateList.getColorForState(getDrawableState(), colorStateList.getDefaultColor());
  }

  private float[] getActiveRange() {
    float left = normalizeValue(getValueFrom());
    float right = normalizedValueAnim;
    // In RTL we draw things in reverse, so swap the left and right range values
    return isRtl() ? new float[] {right, left} : new float[] {left, right};
  }

  private enum FullCornerDirection {
    BOTH,
    LEFT,
    RIGHT,
    NONE
  }
}