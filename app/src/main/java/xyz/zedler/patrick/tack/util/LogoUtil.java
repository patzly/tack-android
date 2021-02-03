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
    private final static boolean DEBUG = false;

    private final AnimatorSet animatorSet;
    private final RotateDrawable pointer;
    private boolean isLeft = true;

    public LogoUtil(ImageView imageView) {
        LayerDrawable layers = (LayerDrawable) imageView.getDrawable();
        pointer = (RotateDrawable) layers.findDrawableByLayerId(R.id.logo_pointer);
        pointer.setLevel(0);
        animatorSet = new AnimatorSet();
    }

    public void nextBeat(long interval) {
        animatorSet.pause();
        animatorSet.cancel();
        ObjectAnimator animator = ObjectAnimator.ofInt(
                pointer,
                "level",
                pointer.getLevel(),
                isLeft ? 10000 : 0
        ).setDuration(interval);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.play(animator);
        animatorSet.start();
        isLeft = !isLeft;
    }
}
