package xyz.zedler.patrick.tack.view;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import xyz.zedler.patrick.tack.R;

public class DottedCircleView extends View {

    private final static String TAG = DottedCircleView.class.getSimpleName();

    private final int dots;
    private final Paint paint;
    private final float ringWidth;
    private final float dotSizeMin;
    private final float dotSizeMax;
    private boolean areDotsVisible = true;

    public DottedCircleView(@NonNull Context context) {
        this(context, null);
    }

    public DottedCircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public DottedCircleView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DottedCircleView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes
    ) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources resources = getResources();
        ringWidth = resources.getDimensionPixelSize(R.dimen.picker_ring_width);
        dotSizeMin = resources.getDimensionPixelSize(R.dimen.picker_dot_size);
        dotSizeMax = resources.getDimensionPixelSize(R.dimen.picker_dot_size_dragged);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(context.getColor(R.color.on_background_secondary));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dotSizeMin);
        paint.setAntiAlias(true);

        dots = getResources().getInteger(R.integer.picker_dots);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!areDotsVisible) return;

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

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        setDotsVisibility(visibility == VISIBLE);
    }

    public void setDotsVisibility(boolean visible) {
        areDotsVisible = visible;
        invalidate();
    }

    public void setHighlighted(boolean highlighted, boolean animated) {
        if (animated) {
            ValueAnimator animatorSize = ValueAnimator.ofFloat(
                    paint.getStrokeWidth(),
                    highlighted ? dotSizeMax : dotSizeMin
            );
            animatorSize.addUpdateListener(animation -> {
                paint.setStrokeWidth((float) animatorSize.getAnimatedValue());
                invalidate();
            });

            ValueAnimator animatorColor = ValueAnimator.ofObject(
                    new ArgbEvaluator(),
                    paint.getColor(),
                    ContextCompat.getColor(
                            getContext(),
                            highlighted ? R.color.retro_dirt : R.color.on_background_secondary
                    )
            );
            animatorColor.addUpdateListener(animation -> {
                paint.setColor((int) animatorColor.getAnimatedValue());
                invalidate();
            });

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setInterpolator(new FastOutSlowInInterpolator());
            animatorSet.setDuration(200);
            animatorSet.playTogether(animatorSize, animatorColor);
            animatorSet.start();
        } else {
            paint.setStrokeWidth(highlighted ? dotSizeMax : dotSizeMin);
            paint.setColor(
                    ContextCompat.getColor(
                            getContext(),
                            highlighted ? R.color.retro_dirt : R.color.on_background_secondary
                    )
            );
            invalidate();
        }
    }
}