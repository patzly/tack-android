package xyz.zedler.patrick.tack.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.button.MaterialButton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
  private MaterialButton button;
  private String tickType;
  private boolean isSubdivision, reduceAnimations;
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

    button = new MaterialButton(context);
    int insetHorizontal = UiUtil.dpToPx(context, 4);
    try {
      button.setLayoutParams(
          new FrameLayout.LayoutParams(minSize, minSize)
      );
      Field buttonHelperField = MaterialButton.class.getDeclaredField("materialButtonHelper");
      buttonHelperField.setAccessible(true);
      Object materialButtonHelper = buttonHelperField.get(button);
      if (materialButtonHelper != null) {
        // Change horizontal insets to get icon-only ripple size
        Field insetLeft = materialButtonHelper.getClass().getDeclaredField("insetLeft");
        insetLeft.setAccessible(true);
        insetLeft.set(materialButtonHelper, insetHorizontal);
        Field insetRight = materialButtonHelper.getClass().getDeclaredField("insetRight");
        insetRight.setAccessible(true);
        insetRight.set(materialButtonHelper, insetHorizontal);
        // Call updatedBackground() in MaterialButtonHelper to apply changed insets
        Method updateBackground = materialButtonHelper.getClass().getDeclaredMethod(
            "updateBackground"
        );
        updateBackground.setAccessible(true);
        updateBackground.invoke(materialButtonHelper);
        button.requestLayout();
      }
    } catch (Exception e) {
      // If not possible, change size of whole button (and touch target, not good)
      FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
          minSize - insetHorizontal * 2, minSize
      );
      paramsButton.gravity = Gravity.CENTER;
      button.setLayoutParams(paramsButton);
    }
    button.setRippleColor(
        ContextCompat.getColorStateList(context, R.color.selector_tonal_button_ripple)
    );
    button.setBackgroundColor(Color.TRANSPARENT);
    setOnClickListener(null);
    addView(button);

    iconSizeDefault = UiUtil.dpToPx(context, 24);
    iconSizeBeat = UiUtil.dpToPx(context, 32);
    iconSizeNoBeat = iconSizeDefault;
    iconSizeMuted = UiUtil.dpToPx(context, 12);
    iconSize = iconSizeDefault;

    colorNormal = ResUtil.getColor(context, R.attr.colorPrimary);
    colorStrong = ResUtil.getColor(context, R.attr.colorError);
    colorSub = ResUtil.getColor(context, R.attr.colorOnSurfaceVariant);
    colorMuted = ResUtil.getColor(context, R.attr.colorOutline);

    imageView = new ImageView(context);
    FrameLayout.LayoutParams paramsIcon = new FrameLayout.LayoutParams(iconSize, iconSize);
    paramsIcon.gravity = Gravity.CENTER;
    imageView.setLayoutParams(paramsIcon);
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
      animatorSet.removeAllListeners();
      animatorSet.cancel();
      animatorSet = null;
    }
    if (!tickType.equals(TICK_TYPE.MUTED) && !reduceAnimations) {
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

  @Override
  public void setOnClickListener(@Nullable OnClickListener l) {
    if (l != null) {
      button.setOnClickListener(l);
    }
    button.setEnabled(l != null);
  }

  public void setReduceAnimations(boolean reduce) {
    reduceAnimations = reduce;
    iconSizeBeat = UiUtil.dpToPx(getContext(), reduce ? 40 : 32);
  }

  @NonNull
  @Override
  public String toString() {
    return tickType;
  }
}
