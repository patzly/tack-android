package xyz.zedler.patrick.tack.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.graphics.shapes.CornerRounding;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.ShapesKt;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;

public class CircleView extends View {

  private final static String TAG = CircleView.class.getSimpleName();

  private final int waves;
  private final Paint paint;
  private final Path path;
  private final RectF bounds;
  private final Matrix matrix;
  private final RoundedPolygon.Companion companion;
  private final CornerRounding cornerRounding;
  private final float innerRadiusDefault, innerRadiusDrag;
  private final int colorDefault;
  private final int[] colorsDrag;
  private float touchX, touchY;
  private float gradientBlendRatio = 0;
  private float innerRadius;
  private AnimatorSet animatorSet;

  public CircleView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    colorDefault = ResUtil.getColorAttr(context, R.attr.colorSecondaryContainer);
    int colorDrag = ResUtil.getColorAttr(context, R.attr.colorTertiary);

    paint = new Paint();
    paint.setStyle(Style.FILL);
    paint.setColor(colorDefault);

    colorsDrag = new int[]{
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.5f),
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.7f),
        ColorUtils.blendARGB(colorDrag, colorDefault, 0.9f),
        colorDefault
    };

    companion = RoundedPolygon.Companion;
    cornerRounding = new CornerRounding(0.5f, 0);

    waves = getResources().getInteger(R.integer.picker_waves);
    innerRadiusDefault = 0.84f;
    innerRadiusDrag = 0.78f;
    innerRadius = innerRadiusDefault;

    path = new Path();
    bounds = new RectF();
    matrix = new Matrix();
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawPath(path, paint);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    updateShape();
  }

  private void updateShape() {
    path.set(ShapesKt.star(companion, waves, 1, innerRadius, cornerRounding).toPath());
    path.computeBounds(bounds, false);
    float scaleX = getWidth() / (bounds.width()) + .3f;
    float scaleY = getHeight() / bounds.height();
    matrix.reset();
    matrix.postScale(scaleX, scaleY);
    matrix.postTranslate(-bounds.left * scaleX, -bounds.top * scaleY);
    path.transform(matrix);
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
    // inner radius
    ValueAnimator animatorAmplitude = ValueAnimator.ofFloat(
        innerRadius, dragged ? innerRadiusDrag : innerRadiusDefault
    );
    animatorAmplitude.addUpdateListener(animation -> {
      innerRadius = (float) animatorAmplitude.getAnimatedValue();
      updateShape();
    });
    // shader color
    ValueAnimator alphaAnimator = ValueAnimator.ofFloat(gradientBlendRatio, dragged ? 0.8f : 0);
    alphaAnimator.addUpdateListener(animation -> {
      gradientBlendRatio = (float) alphaAnimator.getAnimatedValue();
      paint.setShader(getGradient());
      invalidate();
    });
    animatorSet = new AnimatorSet();
    animatorSet.playTogether(alphaAnimator, animatorAmplitude);
    animatorSet.setInterpolator(new FastOutSlowInInterpolator());
    animatorSet.setDuration(Constants.ANIM_DURATION_LONG);
    animatorSet.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        animatorSet = null;
      }
    });
    animatorSet.start();
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
        getWidth(),
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