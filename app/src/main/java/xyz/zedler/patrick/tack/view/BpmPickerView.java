package xyz.zedler.patrick.tack.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import xyz.zedler.patrick.tack.R;

public class BpmPickerView extends View {

    public final int dots;
    private final Paint paint;
    private final float ringWidth, dotSizeMin, dotSizeMax;
    private final boolean dotsVisible;

    public BpmPickerView(@NonNull Context context) {
        this(context, null);
    }

    public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BpmPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources resources = context.getResources();
        ringWidth = (float) resources.getDimensionPixelSize(R.dimen.dotted_ring_width);
        dotSizeMin = resources.getDimensionPixelSize(R.dimen.dotted_ring_dot_size_min);
        dotSizeMax = resources.getDimensionPixelSize(R.dimen.dotted_ring_dot_size_max);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(ContextCompat.getColor(context, R.color.bpm_picker));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dotSizeMin);
        paint.setAntiAlias(true);

        dots = resources.getInteger(R.integer.bpm_picker_dots);
        dotsVisible = true;

        requestFocus();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(dotsVisible) {
            float centerX = getPivotX();
            float centerY = getPivotY();
            float min = Math.min(getWidth(), getHeight());
            float f3 = (min / 2f) - ringWidth / 2;
            for (int i = 0; i < dots; i++) {
                double d = (((i * 2f) / dots)) * Math.PI;
                canvas.drawPoint(
                        ((float) Math.cos(d) * f3) + centerX,
                        ((float) Math.sin(d) * f3) + centerY,
                        paint
                );
            }
        }
    }

    public void setTouched(boolean touched) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(
                paint.getStrokeWidth(),
                touched ? dotSizeMax : dotSizeMin
        );
        valueAnimator.addUpdateListener(animation -> {
            paint.setStrokeWidth((float) valueAnimator.getAnimatedValue());
            invalidate();
        });
        valueAnimator.setDuration(200).start();
    }
}