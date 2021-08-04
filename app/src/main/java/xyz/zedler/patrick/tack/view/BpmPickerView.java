package xyz.zedler.patrick.tack.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BpmPickerView extends View implements View.OnTouchListener {

  private boolean isTouchable = true;
  private boolean isTouchStartedInCircle;
  private double currAngle = 0;
  private double prevAngle;
  private float degreeStorage = 0;
  private OnRotationListener onRotationListener;
  private OnPickListener onPickListener;

  public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    prevAngle = 0;

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

    double angleRaw = Math.toDegrees(Math.atan2(x - xc, yc - y));
    double angle = angleRaw >= 0 ? angleRaw : 180 + (180 - Math.abs(angleRaw));

    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      isTouchStartedInCircle = isTouchInsideCircle;
      // on back gesture edge or outside ring
      if (isTouchInsideCircle) {
        onPickListener.onPickDown(x, y);
        currAngle = angle;
      }
    } else if (event.getAction() == MotionEvent.ACTION_MOVE && isTouchStartedInCircle) {
      prevAngle = currAngle;
      currAngle = angle;
      animate(prevAngle, currAngle);
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

  public interface OnRotationListener {

    void onRotate(int change);

    void onRotate(float change);
  }

  public interface OnPickListener {

    void onPickDown(float x, float y);

    void onDrag(float x, float y);

    void onPickUpOrCancel();
  }
}