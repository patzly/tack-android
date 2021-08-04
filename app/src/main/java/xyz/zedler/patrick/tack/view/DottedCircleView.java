package xyz.zedler.patrick.tack.view;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.SystemUiUtil;

public class DottedCircleView extends View {

  private final static String TAG = DottedCircleView.class.getSimpleName();

  private final int waves;
  private final Paint paint;
  private final Path path;
  private final float pickerPadding;
  private final float dotSizeMin;
  private final float dotSizeMax;
  private float touchX, touchY;

  public DottedCircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    Resources resources = getResources();
    pickerPadding = resources.getDimensionPixelSize(R.dimen.picker_ring_padding);
    dotSizeMin = resources.getDimensionPixelSize(R.dimen.picker_dot_size);
    dotSizeMax = resources.getDimensionPixelSize(R.dimen.picker_dot_size_dragged);

    paint = new Paint();
    paint.setStyle(Style.FILL);
    paint.setColor(ContextCompat.getColor(context, R.color.retro_dirt));
    paint.setAntiAlias(true);
    paint.setPathEffect(new CornerPathEffect(SystemUiUtil.dpToPx(context, 9)));

    path = new Path();

    waves = getResources().getInteger(R.integer.picker_waves);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    drawNewStar(canvas, getPivotY(), getPivotY() - SystemUiUtil.dpToPx(getContext(), 10));
  }

  public void drawNewStar(Canvas canvas, float radius, float innerRadius) {
    double section = 2 * Math.PI / waves;
    float cx = getPivotX();
    float cy = getPivotY();

    path.reset();
    path.moveTo((float) (cx + radius * Math.cos(0)), (float) (cy + radius * Math.sin(0)));
    path.lineTo(
        (float) (cx + innerRadius * Math.cos(section / 2)),
        (float) (cy + innerRadius * Math.sin(section / 2))
    );

    for (int i = 1; i < waves; i++) {
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

  public void setDragged(boolean dragged, float x, float y) {
    if (dragged) {
      touchX = x;
      touchY = y;
    }

    ValueAnimator animatorSize = ValueAnimator.ofFloat(
        paint.getStrokeWidth(),
        dragged ? dotSizeMax : dotSizeMin
    );
    animatorSize.addUpdateListener(animation -> {
      /*paint.setStrokeWidth((float) animatorSize.getAnimatedValue());
      invalidate();*/
    });

    ValueAnimator animatorColor = ValueAnimator.ofObject(
        new ArgbEvaluator(),
        paint.getColor(),
        ContextCompat.getColor(
            getContext(),
            dragged ? R.color.picker_dragged : R.color.retro_dirt
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


    paint.setShader(getGradient());
    invalidate();
  }

  public void onDrag(float x, float y) {
    touchX = x;
    touchY = y;
    paint.setShader(getGradient());
    invalidate();
  }

  private Shader getGradient() {
    PointF pointF = getRotatedPoint(touchX, touchY, getPivotX(), getPivotY(), -getRotation());
    return new RadialGradient(
        pointF.x,
        pointF.y,
        500,
        ContextCompat.getColor(getContext(), R.color.retro_red),
        ContextCompat.getColor(getContext(), R.color.retro_dirt),
        TileMode.CLAMP
    );
  }

  private PointF getRotatedPoint(float x, float y, float cx, float cy, float degrees) {
    double radians = Math.toRadians(degrees);

    float x1 = x - cx;
    float y1 = y - cy;

    float x2 = (float) (x1 * Math.cos(radians) - y1 * Math.sin(radians));
    float y2 = (float) (x1 * Math.sin(radians) + y1 * Math.cos(radians));

    return new PointF(x2 + cx, y2 + cy);
  }
}