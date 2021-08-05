package xyz.zedler.patrick.tack.view;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
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
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.SystemUiUtil;

public class DottedCircleView extends View {

  private final static String TAG = DottedCircleView.class.getSimpleName();

  private final int waves;
  private final Paint paint;
  private final Path path;
  private float touchX, touchY;
  private final int colorDefault;
  private final float gradientRadius;
  private float gradientBlendRatio = 0;
  private float amplitude;
  private final float amplitudeDefault, amplitudeDrag;
  private final int[] colorsDrag;
  private AnimatorSet animatorSet;

  public DottedCircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    paint = new Paint();
    paint.setStyle(Style.FILL);
    paint.setColor(ContextCompat.getColor(context, R.color.picker));
    paint.setAntiAlias(true);
    paint.setPathEffect(new CornerPathEffect(SystemUiUtil.dpToPx(context, 9)));

    gradientRadius = SystemUiUtil.dpToPx(getContext(), 180);

    colorDefault = ContextCompat.getColor(context, R.color.picker);
    int colorDrag = ContextCompat.getColor(context, R.color.picker_dragged);

    colorsDrag = new int[]{
        colorDrag,
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.4f),
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.8f),
        colorDefault
    };

    amplitudeDefault = SystemUiUtil.dpToPx(context, 10);
    amplitudeDrag = SystemUiUtil.dpToPx(context, 13);
    amplitude = amplitudeDefault;

    path = new Path();

    waves = getResources().getInteger(R.integer.picker_waves);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    drawNewStar(canvas);
  }

  public void drawNewStar(Canvas canvas) {
    double section = 2 * Math.PI / waves;
    float cx = getPivotX();
    float cy = getPivotY();
    float radius = getPivotX();
    float innerRadius = radius - amplitude;

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
    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.cancel();
    }

    ValueAnimator animatorAmplitude = ValueAnimator.ofFloat(
        amplitude,
        dragged ? amplitudeDrag : amplitudeDefault
    );
    animatorAmplitude.addUpdateListener(
        animation -> amplitude = (float) animatorAmplitude.getAnimatedValue()
    );
    animatorAmplitude.setDuration(300);

    ValueAnimator alphaAnimator = ValueAnimator.ofFloat(gradientBlendRatio, dragged ? 0.7f : 0);
    alphaAnimator.addUpdateListener(animation -> {
      gradientBlendRatio = (float) alphaAnimator.getAnimatedValue();
      paint.setShader(getGradient());
      invalidate();
    });
    alphaAnimator.setDuration(750);

    animatorSet = new AnimatorSet();
    animatorSet.setInterpolator(new FastOutSlowInInterpolator());
    animatorSet.playTogether(alphaAnimator, animatorAmplitude);
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
        gradientRadius,
        new int[]{
            ColorUtils.blendARGB(colorDefault, colorsDrag[0], gradientBlendRatio),
            ColorUtils.blendARGB(colorDefault, colorsDrag[1], gradientBlendRatio),
            ColorUtils.blendARGB(colorDefault, colorsDrag[2], gradientBlendRatio),
            ColorUtils.blendARGB(colorDefault, colorsDrag[3], gradientBlendRatio)
        },
        new float[]{0, 0.33f, 0.73f, 1},
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