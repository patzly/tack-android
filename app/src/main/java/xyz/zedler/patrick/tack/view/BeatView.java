package xyz.zedler.patrick.tack.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class BeatView extends FrameLayout {

  private static final int[] SHAPES = new int[]{
      R.drawable.ic_beat_star_anim,
      R.drawable.ic_beat_oval_anim,
      R.drawable.ic_beat_arrow_anim,
      R.drawable.ic_beat_clover_anim,
      R.drawable.ic_beat_pentagon_anim,
  };

  private AnimatorSet animatorSet;
  private int iconSize, iconSizeDefault, iconSizeBeat, iconSizeNoBeat, iconSizeMuted;
  private FastOutSlowInInterpolator interpolator;
  private ImageView imageView;
  private String tickType;
  private boolean isSubdivision;
  private int index;
  private int colorNormal, colorStrong, colorSub, colorMuted;

  public BeatView(Context context) {
    super(context);

    init(context);
  }

  public BeatView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    init(context);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.cancel();
    }
  }

  @SuppressLint("PrivateResource")
  private void init(Context context) {
    tickType = TICK_TYPE.NORMAL;

    int minSize = UiUtil.dpToPx(context, 48);
    setMinimumWidth(minSize);
    setMinimumHeight(minSize);
    setClickable(true);
    setBackgroundResource(R.drawable.ripple_beat_bg);

    iconSizeDefault = UiUtil.dpToPx(context, 24);
    iconSizeBeat = UiUtil.dpToPx(context, 32);
    iconSizeNoBeat = iconSizeDefault;
    iconSizeMuted = UiUtil.dpToPx(context, 12);
    iconSize = iconSizeDefault;

    colorNormal = ResUtil.getColorAttr(context, R.attr.colorPrimary);
    colorStrong = ResUtil.getColorAttr(context, R.attr.colorError);
    colorSub = ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant);
    colorMuted = ResUtil.getColorAttr(context, R.attr.colorOutline);

    imageView = new ImageView(context);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(iconSize, iconSize);
    params.gravity = Gravity.CENTER;
    imageView.setLayoutParams(params);
    imageView.setAdjustViewBounds(false);
    imageView.setImageResource(R.drawable.ic_beat_clover_anim);
    addView(imageView);

    interpolator = new FastOutSlowInInterpolator();

    setTickType(TICK_TYPE.NORMAL);
  }

  public void setIndex(int index) {
    this.index = index;
    imageView.setImageResource(SHAPES[index % SHAPES.length]);
  }

  public int getIndex() {
    return index;
  }

  public void setIsSubdivision(boolean isSubdivision) {
    this.isSubdivision = isSubdivision;
    setBackgroundResource(
        isSubdivision ? R.drawable.ripple_subdivision_bg : R.drawable.ripple_beat_bg
    );
    setTickType(TICK_TYPE.SUB);
  }

  public void setTickType(String tickType) {
    this.tickType = tickType;
    int color, iconSize;
    switch (tickType) {
      case TICK_TYPE.STRONG:
        color = colorStrong;
        iconSize = iconSizeNoBeat;
        break;
      case TICK_TYPE.MUTED:
        color = colorMuted;
        iconSize = iconSizeMuted;
        break;
      case TICK_TYPE.SUB:
        color = colorSub;
        iconSize = iconSizeNoBeat;
        break;
      default:
        color = colorNormal;
        iconSize = iconSizeNoBeat;
    }
    imageView.setImageTintList(ColorStateList.valueOf(color));
    iconSizeDefault = iconSize;
    updateIconSize(iconSizeDefault);
  }

  public String nextTickType() {
    String next;
    switch (tickType) {
      case TICK_TYPE.NORMAL:
        next = isSubdivision ? TICK_TYPE.MUTED : TICK_TYPE.STRONG;
        break;
      case TICK_TYPE.STRONG:
        next = TICK_TYPE.MUTED;
        break;
      case TICK_TYPE.SUB:
        next = TICK_TYPE.NORMAL;
        break;
      default:
        next = isSubdivision ? TICK_TYPE.SUB : TICK_TYPE.NORMAL;
    }
    if (isSubdivision && index == 0) {
      return TICK_TYPE.MUTED;
    } else {
      setTickType(next);
      beat();
      return next;
    }
  }

  public void beat() {
    if (animatorSet != null) {
      animatorSet.pause();
      animatorSet.cancel();
    }
    if (!tickType.equals(TICK_TYPE.MUTED)) {
      ViewUtil.startIcon(imageView);
    }
    ValueAnimator animatorSizeIn = ValueAnimator.ofInt(iconSize, iconSizeBeat);
    animatorSizeIn.addUpdateListener(
        animation -> updateIconSize((Integer) animation.getAnimatedValue())
    );
    animatorSizeIn.setInterpolator(interpolator);
    animatorSizeIn.setDuration(25);

    ValueAnimator animatorSizeOut = ValueAnimator.ofInt(iconSizeBeat, iconSizeDefault);
    animatorSizeOut.addUpdateListener(
      animation -> updateIconSize((Integer) animation.getAnimatedValue())
    );
    animatorSizeOut.setInterpolator(interpolator);
    animatorSizeOut.setDuration(375);

    animatorSet = new AnimatorSet();
    animatorSet.playSequentially(animatorSizeIn, animatorSizeOut);
    animatorSet.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        animatorSet = null;
      }
    });
    animatorSet.start();
  }

  private void updateIconSize(int size) {
    iconSize = size;
    imageView.getLayoutParams().width = size;
    imageView.getLayoutParams().height = size;
    imageView.invalidate();
    imageView.requestLayout();
  }
}
