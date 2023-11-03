package xyz.zedler.patrick.tack.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.wearable.input.RotaryEncoderHelper;
import xyz.zedler.patrick.tack.R;

public class BpmPickerView extends View
    implements View.OnGenericMotionListener, View.OnTouchListener {

  private final static String TAG = BpmPickerView.class.getSimpleName();

  private final float ringWidth;
  private final float edgeWidth;
  private boolean isTouchable = true;
  private boolean isTouchStartedInRing;
  private double currAngle = 0;
  private double prevAngle;
  private float degreeStorage = 0;
  private float rotaryStorage = 0;
  private OnRotaryInputListener onRotaryInputListener;
  private OnRotationListener onRotationListener;
  private OnPickListener onPickListener;

  public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    Resources resources = getResources();
    ringWidth = resources.getDimensionPixelSize(R.dimen.picker_ring_width);
    edgeWidth = resources.getDimensionPixelSize(R.dimen.edge_width);

    prevAngle = 0;

    setOnGenericMotionListener(this);
    setOnTouchListener(this);

    requestFocus();
  }

  public void setOnRotaryInputListener(OnRotaryInputListener listener) {
    this.onRotaryInputListener = listener;
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
  public boolean onGenericMotion(View v, MotionEvent event) {
    if (!isRotaryEvent(event)) {
      return false;
    }

    float scrolled = RotaryEncoderHelper.getRotaryAxisValue(event);
    float factor = RotaryEncoderHelper.getScaledScrollFactor(getContext());
    float delta = -scrolled * (factor / 4);
    float rotaryThreshold = 0.065f;

    rotaryStorage = rotaryStorage - scrolled;
    if (rotaryStorage > rotaryThreshold) {
      if (onRotaryInputListener != null) {
        onRotaryInputListener.onRotate(1);
      }
      rotaryStorage = 0;
    } else if (rotaryStorage < -rotaryThreshold) {
      if (onRotaryInputListener != null) {
        onRotaryInputListener.onRotate(-1);
      }
      rotaryStorage = 0;
    }

    setRotation(getRotation() + delta);
    if (onRotaryInputListener != null) {
      onRotaryInputListener.onRotate(delta);
    }

    return true;
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
    boolean isTouchInsideRing = isTouchInsideRing(event.getX(), event.getY());

    double angleRaw = Math.toDegrees(Math.atan2(x - xc, yc - y));
    double angle = angleRaw >= 0 ? angleRaw : 180 + (180 - Math.abs(angleRaw));

    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      isTouchStartedInRing = isTouchInsideRing;
      // on back gesture edge or outside ring
      onPickListener.onPickDown(
          x, y,
          isTouchInsideRing,
          !isTouchInsideRing || x <= edgeWidth
      );
      currAngle = angle;
    } else if (event.getAction() == MotionEvent.ACTION_MOVE
        && (isTouchInsideRing || isTouchStartedInRing)
    ) {
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

  private boolean isTouchInsideRing(float x, float y) {
    float radius = (Math.min(getWidth(), getHeight()) / 2f) - ringWidth;
    double centerX = getPivotX();
    double centerY = getPivotY();
    double distanceX = x - centerX;
    double distanceY = y - centerY;
    return !((distanceX * distanceX) + (distanceY * distanceY) <= radius * radius);
  }

  private boolean isRotaryEvent(MotionEvent event) {
    return event.getAction() == MotionEvent.ACTION_SCROLL
        && RotaryEncoderHelper.isFromRotaryEncoder(event);
  }

  public interface OnRotaryInputListener {

    void onRotate(int change);

    void onRotate(float change);
  }

  public interface OnRotationListener {

    void onRotate(int change);

    void onRotate(float change);
  }

  public interface OnPickListener {

    void onPickDown(float x, float y, boolean isOnRing, boolean canBeDismiss);

    void onDrag(float x, float y);

    void onPickUpOrCancel();
  }
}