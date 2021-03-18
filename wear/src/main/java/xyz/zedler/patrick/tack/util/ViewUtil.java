package xyz.zedler.patrick.tack.util;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.FontRes;
import androidx.annotation.IntegerRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

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

    public static void setAlpha(float alpha, View... views) {
        for (View view : views) {
            view.setAlpha(alpha);
        }
    }

    public static void setSize(@DimenRes int resId, View... views) {
        int size = views[0].getResources().getDimensionPixelSize(resId);
        for (View view : views) {
            view.getLayoutParams().width = size;
            view.getLayoutParams().height = size;
            view.requestLayout();
        }
    }

    public static void setTextSize(TextView textView, @DimenRes int resId) {
        textView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                textView.getResources().getDimension(resId)
        );
    }

    public static void setFontFamily(TextView textView, @FontRes int resId) {
        textView.setTypeface(
                ResourcesCompat.getFont(textView.getContext(), resId)
        );
    }

    public static LinearLayout.LayoutParams getParamsWeightHeight(
            Context context,
            @IntegerRes int resId
    ) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                context.getResources().getInteger(resId)
        );
    }

    public static void setMargins(
            View view,
            @DimenRes int left,
            @DimenRes int top,
            @DimenRes int right,
            @DimenRes int bottom
    ) {
        Resources resources = view.getResources();
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).setMargins(
                left == -1 ? 0 : resources.getDimensionPixelSize(left),
                top == -1 ? 0 : resources.getDimensionPixelSize(top),
                right == -1 ? 0 : resources.getDimensionPixelSize(right),
                bottom == -1 ? 0 : resources.getDimensionPixelSize(bottom)
        );
        view.requestLayout();
    }

    public static void setMargin(View view, int margin) {
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).setMargins(
                margin, margin, margin, margin
        );
    }

    public static void setHorizontalMargins(View view, @DimenRes int left, @DimenRes int right) {
        setMargins(view, left, -1, right, -1);
    }

    public static void setMarginTop(View view, @DimenRes int resId) {
        setMargins(view, -1, resId, -1, -1);
    }

    public static void setMarginBottom(View view, @DimenRes int resId) {
        setMargins(view, -1, -1, -1, resId);
    }

    public static void animateAlpha(float alpha, View... views) {
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

    public static void startAnimatedIcon(ImageView imageView) {
        try {
            ((Animatable) imageView.getDrawable()).start();
        } catch (ClassCastException ignored) { }
    }
}
