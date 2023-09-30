package xyz.zedler.patrick.tack.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.Op;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class BeatsBgDrawable extends Drawable {

  private static final float ALPHA_FG_BASE_LIGHT = 0.08f;
  private static final float ALPHA_FG_BASE_DARK = 0.12f;

  private final Paint paintFg = new Paint();
  private final Paint paintBg = new Paint();
  private final RectF rectFg = new RectF();
  private final RectF rectBg = new RectF();
  private final Path pathFg = new Path();
  private final Path pathBg = new Path();
  private final float alphaBase, progressThreshold;
  private float fraction, alpha;
  private ValueAnimator progressAnimator, alphaAnimator;

  public BeatsBgDrawable(Context context) {
    paintBg.setColor(ResUtil.getColorAttr(context, R.attr.colorSurfaceContainerHigh));
    paintFg.setColor(ResUtil.getColorAttr(context, R.attr.colorOnSurface));
    alphaBase = UiUtil.isDarkModeActive(context) ? ALPHA_FG_BASE_DARK : ALPHA_FG_BASE_LIGHT;
    progressThreshold = 1;
    setProgress(0, false);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    float radius = getBounds().height() / 2f;
    rectBg.set(0, 0, getBounds().width(), getBounds().height());
    pathBg.reset();
    pathBg.addRoundRect(rectBg, radius, radius, Direction.CW);
    canvas.drawPath(pathBg, paintBg);

    rectFg.set(rectBg);
    rectFg.right = getBounds().width() * fraction;
    pathFg.reset();
    pathFg.addRect(rectFg, Direction.CW);
    pathFg.op(pathBg, Op.INTERSECT);

    float interpolated = alphaBase;
    if (progressThreshold < 1 && fraction > progressThreshold) {
      interpolated = alphaBase * (1 - (fraction - progressThreshold) / (1 - progressThreshold));
    }
    paintFg.setAlpha((int) (interpolated * alpha * 255));
    canvas.drawPath(pathFg, paintFg);
  }

  @Deprecated
  @Override
  public void setAlpha(int alpha) {}

  @Deprecated
  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  private void setFraction(float fraction) {
    this.fraction = fraction;
    invalidateSelf();
  }

  private void setProgressAlpha(float alpha) {
    this.alpha = alpha;
    invalidateSelf();
  }

  public void setProgress(float fraction, boolean animated) {
    setProgress(fraction, Constants.ANIM_DURATION_LONG, animated);
  }

  public void setProgress(float fraction, long duration, boolean animated) {
    if (progressAnimator != null) {
      progressAnimator.pause();
      progressAnimator.cancel();
      progressAnimator = null;
    }
    if (animated) {
      progressAnimator = ValueAnimator.ofFloat(this.fraction, fraction);
      progressAnimator.addUpdateListener(
          animation -> setFraction((float) animation.getAnimatedValue())
      );
      progressAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (progressThreshold == 1) {
            setProgressVisible(false, 1500, true);
          }
        }
      });
      progressAnimator.setInterpolator(new LinearInterpolator());
      progressAnimator.setDuration(duration);
      progressAnimator.start();
    } else {
      setFraction(fraction);
    }
  }

  public void setProgressVisible(boolean visible, boolean animated) {
    setProgressVisible(visible, Constants.ANIM_DURATION_LONG, animated);
  }

  public void setProgressVisible(boolean visible, long duration, boolean animated) {
    if (alphaAnimator != null) {
      alphaAnimator.pause();
      alphaAnimator.cancel();
      alphaAnimator = null;
    }
    if (animated) {
      alphaAnimator = ValueAnimator.ofFloat(this.alpha, visible ? 1 : 0);
      alphaAnimator.addUpdateListener(
          animation -> setProgressAlpha((float) animation.getAnimatedValue())
      );
      alphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
      alphaAnimator.setDuration(duration);
      alphaAnimator.start();
    } else {
      setProgressAlpha(visible ? 1 : 0);
    }
  }

  public void reset() {
    setProgress(0, false);
    setProgressVisible(true, false);
  }
}
