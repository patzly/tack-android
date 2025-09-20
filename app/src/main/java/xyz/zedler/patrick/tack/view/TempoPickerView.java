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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.util.UiUtil;

public class TempoPickerView extends View implements View.OnTouchListener {

  private boolean isTouchable = true;
  private boolean isTouchStartedInCircle, isTouchStartedInCenter;
  private double currAngle = 0;
  private double prevAngle;
  private float degreeStorage = 0;
  private OnRotationListener onRotationListener;
  private OnPickListener onPickListener;
  private OnClickListener onClickListener;
  private final float ignoredCenterSize;
  private final GestureDetector gestureDetector;

  public TempoPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    prevAngle = 0;
    ignoredCenterSize = UiUtil.dpToPx(context, 40);
    gestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
      @Override
      public boolean onSingleTapUp(@NonNull MotionEvent event) {
        if (onClickListener != null) {
          onClickListener.onClick(TempoPickerView.this);
        }
        return true;
      }
    });

    setOnTouchListener(this);

    requestFocus();
  }

  public void setOnRotationListener(OnRotationListener listener) {
    this.onRotationListener = listener;
  }

  public void setOnPickListener(OnPickListener listener) {
    this.onPickListener = listener;
  }

  @Override
  public void setOnClickListener(OnClickListener listener) {
    this.onClickListener = listener;
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);
    setTouchable(visibility == VISIBLE);
  }

  public void setTouchable(boolean touchable) {
    isTouchable = touchable;
  }

  @Override
  public void onFocusChanged(
      boolean gainFocus,
      int direction,
      @Nullable Rect previouslyFocusedRect
  ) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    if (!gainFocus) {
      requestFocus(direction, previouslyFocusedRect);
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (!isTouchable) {
      return false;
    }

    final float xc = (float) getWidth() / 2;
    final float yc = (float) getHeight() / 2;
    final float x = event.getX();
    final float y = event.getY();
    boolean isTouchInsideCircle = isTouchInsideCircle(event.getX(), event.getY());
    boolean isTouchOutsideCenter = isTouchOutsideCenter(event.getX(), event.getY());

    double angleRaw = Math.toDegrees(Math.atan2(x - xc, yc - y));
    double angle = angleRaw >= 0 ? angleRaw : 180 + (180 - Math.abs(angleRaw));

    if (isTouchInsideCircle) {
      gestureDetector.onTouchEvent(event);
    }

    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      isTouchStartedInCircle = isTouchInsideCircle;
      // on back gesture edge or outside ring
      if (isTouchInsideCircle) {
        onPickListener.onPickDown(x, y);
        if (isTouchOutsideCenter) {
          isTouchStartedInCenter = false;
          currAngle = angle;
        } else {
          // Only follow touch if it's not directly in center
          isTouchStartedInCenter = true;
        }
      }
    } else if (event.getAction() == MotionEvent.ACTION_MOVE && isTouchStartedInCircle) {
      if (isTouchStartedInCenter && isTouchOutsideCenter) {
        isTouchStartedInCenter = false;
        currAngle = angle;
      }
      if (isTouchOutsideCenter) {
        prevAngle = currAngle;
        currAngle = angle;
        animate(prevAngle, currAngle);
      }
      onPickListener.onDrag(x, y);
    } else if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL
    ) {
      prevAngle = currAngle = 0;
      onPickListener.onPickUpOrCancel();
    }
    return true;
  }

  private void animate(double fromDegrees, double toDegrees) {
    final RotateAnimation rotate = new RotateAnimation(
        (float) fromDegrees,
        (float) toDegrees,
        RotateAnimation.RELATIVE_TO_SELF,
        0.5f,
        RotateAnimation.RELATIVE_TO_SELF,
        0.5f
    );
    rotate.setDuration(0);
    rotate.setFillEnabled(true);
    rotate.setFillAfter(true);
    startAnimation(rotate);

    float degreeDiff = (float) toDegrees - (float) fromDegrees;
    if (degreeDiff > 180) {
      degreeDiff = 360 - degreeDiff;
    }
    if (degreeDiff < -180) {
      degreeDiff = -360 + Math.abs(degreeDiff);
    }

    if (onRotationListener != null) {
      onRotationListener.onRotate(degreeDiff);
    }

    degreeStorage = degreeStorage + degreeDiff;
    if (degreeStorage > 12) {
      if (onRotationListener != null) {
        onRotationListener.onRotate(1);
      }
      degreeStorage = 0;
    } else if (degreeStorage < -12) {
      if (onRotationListener != null) {
        onRotationListener.onRotate(-1);
      }
      degreeStorage = 0;
    }
  }

  private boolean isTouchInsideCircle(float x, float y) {
    float radius = Math.min(getPivotX(), getPivotY());
    double centerX = getPivotX();
    double centerY = getPivotY();
    double distanceX = x - centerX;
    double distanceY = y - centerY;
    return (distanceX * distanceX) + (distanceY * distanceY) <= radius * radius;
  }

  private boolean isTouchOutsideCenter(float x, float y) {
    double centerX = getPivotX();
    double centerY = getPivotY();
    double radius = ignoredCenterSize / 2;
    double distanceSquared = (x - centerX) * (x - centerX) + (y - centerY) * (y - centerY);
    return distanceSquared > (radius * radius);
  }

  public interface OnRotationListener {

    void onRotate(int tempo);

    void onRotate(float degrees);
  }

  public interface OnPickListener {

    void onPickDown(float x, float y);

    void onDrag(float x, float y);

    void onPickUpOrCancel();
  }
}