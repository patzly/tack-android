package xyz.zedler.patrick.tack.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.SystemUiUtil;
import xyz.zedler.patrick.tack.util.UnitUtil;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Tack_BottomSheetDialog);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
    if (dialog == null) {
      return;
    }

    view.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            View sheet = dialog.findViewById(R.id.design_bottom_sheet);
            if (sheet == null) {
              return;
            }

            PaintDrawable background = new PaintDrawable(
                ContextCompat.getColor(activity, R.color.surface)
            );
            int radius = UnitUtil.getDp(activity, 16);
            background.setCornerRadii(
                new float[]{
                    radius, radius,
                    radius, radius,
                    0, 0,
                    0, 0
                }
            );
            sheet.setBackground(background);

            updateSystemBars(activity, dialog.getWindow(), sheet);

            BottomSheetBehavior.from(sheet).setPeekHeight(
                UnitUtil.getDisplayHeight(activity) / 2
            );

            if (view.getViewTreeObserver().isAlive()) {
              view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });
  }

  private void updateSystemBars(Context context, Window window, View sheet) {
    boolean isOrientationPortrait = SystemUiUtil.isOrientationPortrait(context);
    boolean isDarkModeActive = SystemUiUtil.isDarkModeActive(context);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 29
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        SystemUiUtil.setLightStatusBar(window);
      }
      if (SystemUiUtil.isNavigationModeGesture(context)) {
        window.setNavigationBarColor(ContextCompat.getColor(context, R.color.surface));
        window.setNavigationBarDividerColor(
            ContextCompat.getColor(context, R.color.stroke_secondary)
        );
        window.setNavigationBarContrastEnforced(true);
      } else {
        if (!isDarkModeActive) {
          SystemUiUtil.setLightNavigationBar(window);
        }
        if (isOrientationPortrait) {
          window.setNavigationBarColor(ContextCompat.getColor(context, R.color.surface));
          window.setNavigationBarDividerColor(
              ContextCompat.getColor(context, R.color.stroke_secondary)
          );
        } else {
          // TODO: NavBar not transparent in button navigation mode in landscape
          window.setNavigationBarColor(Color.TRANSPARENT);
          window.setNavigationBarDividerColor(
              Color.TRANSPARENT
          );
        }
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 28
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        SystemUiUtil.setLightStatusBar(window);
        SystemUiUtil.setLightNavigationBar(window);
      }
      if (isOrientationPortrait) {
        window.setNavigationBarColor(ContextCompat.getColor(context, R.color.surface));
        window.setNavigationBarDividerColor(
            ContextCompat.getColor(context, R.color.stroke_secondary)
        );
      } else {
        window.setNavigationBarColor(Color.TRANSPARENT);
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // 27
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        SystemUiUtil.setLightStatusBar(window);
      }
      if (isOrientationPortrait) {
        addCompatNavigationBarDivider(context, sheet);
        window.setNavigationBarColor(ContextCompat.getColor(context, R.color.surface));
        if (!isDarkModeActive) {
          SystemUiUtil.setLightNavigationBar(window);
        }
      } else {
        window.setNavigationBarColor(Color.BLACK);
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 23
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        SystemUiUtil.setLightStatusBar(window);
      }
      if (isOrientationPortrait) {
        if (isDarkModeActive) {
          addCompatNavigationBarDivider(context, sheet);
          window.setNavigationBarColor(ContextCompat.getColor(context, R.color.surface));
        } else {
          window.setNavigationBarColor(SystemUiUtil.COLOR_SCRIM_OPAQUE);
        }
      } else {
        window.setNavigationBarColor(Color.BLACK);
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 21
      window.setStatusBarColor(Color.TRANSPARENT);
      if (isOrientationPortrait) {
        if (isDarkModeActive) {
          addCompatNavigationBarDivider(context, sheet);
          window.setNavigationBarColor(ContextCompat.getColor(context, R.color.surface));
        } else {
          window.setNavigationBarColor(SystemUiUtil.COLOR_SCRIM_OPAQUE);
        }
      } else {
        window.setNavigationBarColor(Color.BLACK);
      }
    }
  }

  private static void addCompatNavigationBarDivider(Context context, View sheet) {
    FrameLayout container = new FrameLayout(context);
    container.setLayoutParams(
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    );

    View divider = new View(context);
    divider.setLayoutParams(
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, UnitUtil.getDp(context, 1)
        )
    );
    ((FrameLayout.LayoutParams) divider.getLayoutParams()).gravity = Gravity.BOTTOM;
    divider.setBackgroundResource(R.color.stroke_secondary);

    container.addView(divider);

    ((ViewGroup) sheet.getParent().getParent()).addView(container);
    container.bringToFront();
  }
}
