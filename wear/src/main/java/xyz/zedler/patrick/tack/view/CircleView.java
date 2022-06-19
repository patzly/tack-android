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
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class CircleView extends View {

  private final static String TAG = CircleView.class.getSimpleName();

  private final int waves;
  private final Paint paint;
  private final float pickerPadding;
  private final float strokeWidthMin;
  private final float strokeWidthMax;
  private boolean isPickerVisible = true;
  private final Path path;
  private float touchX, touchY;
  private int colorDefault;
  private final int colorOutline, colorTap;
  private float gradientBlendRatio = 0;
  private float amplitude;
  private final float amplitudeDefault, amplitudeDrag;
  private final int[] colorsDrag;
  private AnimatorSet animatorSet;
  private float rotaryRotation = 0;

  public CircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    Resources resources = getResources();
    pickerPadding = resources.getDimensionPixelSize(R.dimen.picker_ring_padding);
    strokeWidthMin = resources.getDimensionPixelSize(R.dimen.picker_width);
    strokeWidthMax = resources.getDimensionPixelSize(R.dimen.picker_width_dragged);

    colorOutline = ResUtil.getColorAttr(context, R.attr.colorOutline);
    colorTap = ResUtil.getColorAttr(context, R.attr.colorPrimary);

    colorDefault = colorOutline;
    int colorDrag = ResUtil.getColorAttr(context, R.attr.colorTertiary);

    colorsDrag = new int[]{
        colorDrag,
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.4f),
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.8f),
        colorDefault
    };

    amplitudeDefault = ViewUtil.dpToPx(context, 10);
    amplitudeDrag = ViewUtil.dpToPx(context, 13);
    amplitude = amplitudeDefault;

    paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(colorOutline);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeWidth(strokeWidthMin);
    paint.setAntiAlias(true);
    paint.setPathEffect(new CornerPathEffect(ViewUtil.dpToPx(context, 9)));
    paint.setShader(getGradient());

    path = new Path();

    waves = getResources().getInteger(R.integer.picker_waves);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (!isPickerVisible) {
      return;
    }

    drawStar(canvas);
  }

  public void drawStar(Canvas canvas) {
    double section = 2 * Math.PI / waves;
    float cx = getPivotX();
    float cy = getPivotY();
    float radius = getPivotX() - pickerPadding / 2;
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

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);

    setDotsVisibility(visibility == VISIBLE);
  }

  public void setDotsVisibility(boolean visible) {
    isPickerVisible = visible;
    invalidate();
  }

  public void changeRotaryRotation(float change) {
    // Fix for weird rotary rotation, gradient renders at other angle without it
    rotaryRotation += change;
  }

  public void setDragged(boolean dragged, float x, float y, boolean animated) {
    if (!animated) {
      paint.setStrokeWidth(dragged ? strokeWidthMax : strokeWidthMin);
      paint.setColor(dragged ? colorTap : colorDefault);
      paint.setShader(null);
      invalidate();
      return;
    }

    if (dragged) {
      touchX = x;
      touchY = y;
    }
    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.cancel();
    }

    // STROKE WIDTH

    ValueAnimator animatorWidth = ValueAnimator.ofFloat(
        paint.getStrokeWidth(), dragged ? strokeWidthMax : strokeWidthMin
    );
    animatorWidth.addUpdateListener(
        animation -> paint.setStrokeWidth((float) animatorWidth.getAnimatedValue())
    );

    // STROKE COLOR

    ValueAnimator animatorColor = ValueAnimator.ofObject(
        new ArgbEvaluator(),
        colorDefault,
        dragged ? colorTap : colorOutline
    );
    animatorColor.addUpdateListener(
        animation -> colorDefault = (int) animatorColor.getAnimatedValue()
    );

    // AMPLITUDE

    ValueAnimator animatorAmplitude = ValueAnimator.ofFloat(
        amplitude, dragged ? amplitudeDrag : amplitudeDefault
    );
    animatorAmplitude.addUpdateListener(
        animation -> amplitude = (float) animatorAmplitude.getAnimatedValue()
    );

    // GRADIENT ALPHA

    ValueAnimator animatorAlpha = ValueAnimator.ofFloat(gradientBlendRatio, dragged ? 0.7f : 0);
    animatorAlpha.addUpdateListener(animation -> {
      gradientBlendRatio = (float) animatorAlpha.getAnimatedValue();
      paint.setShader(getGradient());
      invalidate();
    });

    animatorSet = new AnimatorSet();
    animatorSet.setInterpolator(new FastOutSlowInInterpolator());
    animatorSet.setDuration(300);
    animatorSet.playTogether(animatorColor, animatorWidth, animatorAlpha, animatorAmplitude);
    animatorSet.start();

    paint.setShader(getGradient());
  }

  public void onDrag(float x, float y, boolean animated) {
    if (!animated) {
      return;
    }
    touchX = x;
    touchY = y;
    paint.setShader(getGradient());
    invalidate();
  }

  private Shader getGradient() {
    PointF pointF = getRotatedPoint(
        touchX, touchY, getPivotX(), getPivotY(), rotaryRotation - getRotation()
    );
    return new RadialGradient(
        pointF.x,
        pointF.y,
        getWidth() != 0 ? getWidth() * 0.8f : 100,
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