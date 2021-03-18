package xyz.zedler.patrick.tack.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import xyz.zedler.patrick.tack.R;

public class LogoUtil {

    private final static String TAG = LogoUtil.class.getSimpleName();

    private final RotateDrawable pointer;
    private AnimatorSet animatorSet;
    private boolean isLeft = true;

    public LogoUtil(ImageView imageView) {
        LayerDrawable layers = (LayerDrawable) imageView.getDrawable();
        pointer = (RotateDrawable) layers.findDrawableByLayerId(R.id.logo_pointer);
        pointer.setLevel(0);
    }

    public void nextBeat(long interval) {
        if (animatorSet != null) {
            animatorSet.pause();
            animatorSet.cancel();
        }
        animatorSet = new AnimatorSet();
        animatorSet.play(getAnimator(interval, isLeft ? 10000 : 0));
        animatorSet.start();
        isLeft = !isLeft;
    }

    private ObjectAnimator getAnimator(long duration, int level) {
        ObjectAnimator animator = ObjectAnimator.ofInt(
                pointer, "level", pointer.getLevel(), level
        ).setDuration(duration);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }
}
