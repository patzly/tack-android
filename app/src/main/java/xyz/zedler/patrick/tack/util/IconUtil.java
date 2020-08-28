package xyz.zedler.patrick.tack.util;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

public class IconUtil {

    private final static String TAG = IconUtil.class.getSimpleName();
    private final static boolean DEBUG = false;

    public static void start(Activity activity, @IdRes int viewId) {
        if(activity == null) return;
        View view = activity.findViewById(viewId);
        if(view == null) return;
        try {
            ImageView imageView = (ImageView) view;
            start(imageView.getDrawable());
        } catch (ClassCastException e) {
            if(DEBUG) Log.e(TAG, "start() requires ImageView");
        }
    }

    public static void start(View context, @IdRes int viewId) {
        if(context == null) return;
        View view = context.findViewById(viewId);
        if(view == null) return;
        try {
            ImageView imageView = (ImageView) view;
            start(imageView.getDrawable());
        } catch (ClassCastException e) {
            if(DEBUG) Log.e(TAG, "start() requires ImageView");
        }
    }

    public static void start(ImageView imageView) {
        if(imageView == null) return;
        start(imageView.getDrawable());
    }

    public static void start(Drawable drawable) {
        if(drawable == null) return;
        try {
            ((Animatable) drawable).start();
        } catch (ClassCastException cla) {
            if(DEBUG) Log.e(TAG, "start() requires AnimVectorDrawable");
        }
    }
}
