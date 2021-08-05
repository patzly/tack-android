package xyz.zedler.patrick.tack.view;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class DottedCircleView extends View {

  private final static String TAG = DottedCircleView.class.getSimpleName();

  private final int dots;
  private final Paint paint;
  private final float pickerPadding;
  private final float dotSizeMin;
  private final float dotSizeMax;
  private boolean areDotsVisible = true;

  public DottedCircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    Resources resources = getResources();
    pickerPadding = resources.getDimensionPixelSize(R.dimen.picker_ring_padding);
    dotSizeMin = resources.getDimensionPixelSize(R.dimen.picker_dot_size);
    dotSizeMax = resources.getDimensionPixelSize(R.dimen.picker_dot_size_dragged);

    paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(context.getColor(R.color.on_background_secondary));
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeWidth(dotSizeMin);
    paint.setAntiAlias(true);
    paint.setPathEffect(new CornerPathEffect(ViewUtil.dpToPx(context, 9)));

    dots = getResources().getInteger(R.integer.picker_dots);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (!areDotsVisible) {
      return;
    }

    /*float centerX = getPivotX();
    float centerY = getPivotY();
    float min = Math.min(getWidth(), getHeight());
    float radius = (min / 2) - pickerPadding / 2;
    for (int i = 0; i < dots; i++) {
      double d = (((i * 2f) / dots)) * Math.PI;
      canvas.drawPoint(
          ((float) Math.cos(d) * radius) + centerX,
          ((float) Math.sin(d) * radius) + centerY,
          paint
      );
    }*/
    drawStar(canvas);
  }

  public void drawStar(Canvas canvas) {
    double section = 2 * Math.PI / dots;
    float cx = getPivotX();
    float cy = getPivotY();
    float radius = getPivotX() - pickerPadding / 2;
    float innerRadius = radius - ViewUtil.dpToPx(getContext(), 10);

    Path path = new Path();
    path.reset();
    path.moveTo((float) (cx + radius * Math.cos(0)), (float) (cy + radius * Math.sin(0)));
    path.lineTo(
        (float) (cx + innerRadius * Math.cos(section / 2)),
        (float) (cy + innerRadius * Math.sin(section / 2))
    );

    for (int i = 1; i < dots; i++) {
      path.lineTo(
          (float) (cx + radius * Math.cos(section * i)),
          (float) (cy + radius * Math.sin(section * i))
      );
      path.lineTo(
          (float) (cx + innerRadius * Math.cos(section * i + section / 2)),
          (float) (cy + innerRadius * Math.sin(section * i + section / 2))
      );
    }

    path.close();

    canvas.drawPath(path, paint);
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