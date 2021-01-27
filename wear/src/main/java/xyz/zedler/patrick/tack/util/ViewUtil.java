package xyz.zedler.patrick.tack.util;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

public class ViewUtil {

    public static void setOnClickListeners(View.OnClickListener listener, View... views) {
        for (View view : views) {
            view.setOnClickListener(listener);
        }
    }

    public static void setOnCheckedChangedListeners(
            CompoundButton.OnCheckedChangeListener listener,
            CompoundButton... compoundButtons
    ) {
        for (CompoundButton compoundButton : compoundButtons) {
            compoundButton.setOnCheckedChangeListener(listener);
        }
    }

    public static void setVisibility(int visibility, View... views) {
        for (View view : views) {
            view.setVisibility(visibility);
        }
    }

    public static void startAnimatedIcon(ImageView imageView) {
        try {
            ((Animatable) imageView.getDrawable()).start();
        } catch (ClassCastException ignored) { }
    }

    public static void setViewsAlpha(float alpha, View... views) {
        for (View view : views) {
            view.setAlpha(alpha);
        }
    }

    public static void setViewsSize(float size, View... views) {
        for (View view : views) {
            view.getLayoutParams().width = (int) size;
            view.getLayoutParams().height = (int) size;
            view.requestLayout();
        }
    }

    public static void animateViewsAlpha(float alpha, View... views) {
        for (View view : views) {
            view.animate().alpha(alpha).setDuration(300).start();
        }
    }

    public static void animateBackgroundTint(View view, @ColorRes int color) {
        ColorStateList background = view.getBackgroundTintList();
        if (background == null || view.getBackground() == null) return;
        int colorFrom = background.getDefaultColor();
        int colorTo = ContextCompat.getColor(view.getContext(), color);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), colorFrom, colorTo
        );
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(
                animator -> view.getBackground().setTint((int) animator.getAnimatedValue())
        );
        colorAnimation.start();
    }
}
