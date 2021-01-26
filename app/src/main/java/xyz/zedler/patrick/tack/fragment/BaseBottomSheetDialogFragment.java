package xyz.zedler.patrick.tack.fragment;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


/**
 * This is an extended BottomSheetDialogFragment class. The one overridden method fixes the
 * weird behavior of bottom sheets in landscape mode. All bottom sheets in this app should use this
 * extended class to apply the fix.
 */
public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation != Configuration.ORIENTATION_LANDSCAPE) return;
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if (dialog == null) return;

            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) return;

            Activity activity = getActivity();
            if (activity == null) return;

            BottomSheetBehavior.from(sheet).setPeekHeight(getHalfHeight(activity));
        });
    }

    private static int getHalfHeight(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()
            );
            return (windowMetrics.getBounds().height() - insets.top - insets.bottom) / 2;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels / 2;
        }
    }
}
