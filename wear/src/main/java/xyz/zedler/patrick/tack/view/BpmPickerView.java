package xyz.zedler.patrick.tack.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
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

    private final int dots;
    private final Paint paint;
    private final float ringWidth;
    private final float edgeWidth;
    private final float dotSizeMin;
    private final float dotSizeMax;
    private boolean dotsVisible = true;
    private boolean isTouchStartedInRing;
    private double currAngle = 0;
    private double prevAngle;
    private float degreeStorage = 0;
    private OnRotaryInputListener onRotaryInputListener;
    private OnRotationListener onRotationListener;
    private OnPickListener onPickListener;

    public BpmPickerView(@NonNull Context context) {
        this(context, null);
    }

    public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BpmPickerView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes
    ) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources resources = getResources();
        ringWidth = resources.getDimensionPixelSize(R.dimen.dotted_ring_width);
        dotSizeMin = resources.getDimensionPixelSize(R.dimen.dotted_ring_dot_size_min);
        dotSizeMax = resources.getDimensionPixelSize(R.dimen.dotted_ring_dot_size_max);
        edgeWidth = resources.getDimensionPixelSize(R.dimen.edge_width);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(context.getColor(R.color.on_background_secondary));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dotSizeMin);
        paint.setAntiAlias(true);

        dots = 16;
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!dotsVisible) return;

        float centerX = getPivotX();
        float centerY = getPivotY();
        float min = Math.min(getWidth(), getHeight());
        float radius = (min / 2) - ringWidth / 2;
        for (int i = 0; i < dots; i++) {
            double d = (((i * 2f) / dots)) * Math.PI;
            canvas.drawPoint(
                    ((float) Math.cos(d) * radius) + centerX,
                    ((float) Math.sin(d) * radius) + centerY,
                    paint
            );
        }
    }

    public void setDotsVisible(boolean visible) {
        dotsVisible = visible;
        invalidate();
    }

    @Override
    public void onFocusChanged(
            boolean gainFocus,
            int direction,
            @Nullable Rect previouslyFocusedRect
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            requestFocus(direction, previouslyFocusedRect);
        }
    }

    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        if (!isRotaryEvent(event)) return false;
        float scrolled = RotaryEncoderHelper.getRotaryAxisValue(event);
        float factor = RotaryEncoderHelper.getScaledScrollFactor(getContext());
        float delta = -scrolled * (factor / 5);
        setRotation(getRotation() + delta);
        if (onRotaryInputListener != null) onRotaryInputListener.onRotate(-scrolled > 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!dotsVisible) return false;

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
            onPickListener.onPickDown(!isTouchInsideRing || x <= edgeWidth);
            currAngle = angle;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE
                && (isTouchInsideRing || isTouchStartedInRing)
        ) {
            prevAngle = currAngle;
            currAngle = angle;
            animate(prevAngle, currAngle);
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
        if (degreeDiff > 180) degreeDiff = 360 - degreeDiff;
        if (degreeDiff < -180) degreeDiff = -360 + Math.abs(degreeDiff);

        degreeStorage = degreeStorage + degreeDiff;
        if (degreeStorage > 12) {
            if (onRotationListener != null) onRotationListener.onRotate(1);
            degreeStorage = 0;
        } else if (degreeStorage < -12) {
            if (onRotationListener != null) onRotationListener.onRotate(-1);
            degreeStorage = 0;
        }
    }

    public void setTouched(boolean touched, boolean animated) {
        if (animated) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(
                    paint.getStrokeWidth(),
                    touched ? dotSizeMax : dotSizeMin
            );
            valueAnimator.addUpdateListener(animation -> {
                paint.setStrokeWidth((float) valueAnimator.getAnimatedValue());
                invalidate();
            });
            valueAnimator.setDuration(200).start();
        } else {
            paint.setStrokeWidth(touched ? dotSizeMax : dotSizeMin);
            invalidate();
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
    }

    public interface OnRotationListener {
        void onRotate(int change);
    }

    public interface OnPickListener {
        void onPickDown(boolean canBeDismiss);
        void onPickUpOrCancel();
    }
}