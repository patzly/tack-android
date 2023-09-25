package xyz.zedler.patrick.tack.fragment.dialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;
import com.google.android.material.bottomsheet.CustomBottomSheetDialog;
import com.google.android.material.bottomsheet.CustomBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class BaseBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

  private static final String TAG = "BaseBottomSheet";

  private MainActivity activity;
  private CustomBottomSheetDialog dialog;
  private View decorView;
  private ViewUtil viewUtil;
  private boolean lightNavBar;
  private int backgroundColor;
  private FullWidthListener fullWidthListener;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    activity = (MainActivity) requireActivity();
    viewUtil = new ViewUtil();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    dialog = new CustomBottomSheetDialog(requireContext());
    dialog.setDismissWithAnimation(true);

    if (dialog.getWindow() == null) {
      return dialog;
    }
    decorView = dialog.getWindow().getDecorView();
    decorView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            ViewGroup container = dialog.findViewById(R.id.container);
            View sheet = dialog.findViewById(R.id.design_bottom_sheet);
            if (container == null || sheet == null) {
              return;
            }

            backgroundColor = ResUtil.getColorAttr(
                requireContext(), R.attr.colorSurfaceContainerLow
            );
            PaintDrawable background = new PaintDrawable(backgroundColor);
            int radius = UiUtil.dpToPx(requireContext(), 28);
            setCornerRadius(background, radius);
            sheet.setBackground(background);

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            int peekHeightHalf = UiUtil.getDisplayHeight(requireContext()) / 2;
            behavior.setPeekHeight(peekHeightHalf);

            boolean isFullWidth
                = behavior.getMaxWidth() >= UiUtil.getDisplayWidth(requireContext());
            if (fullWidthListener != null) {
              fullWidthListener.onKnowIfFullWidth(isFullWidth);
            }

            ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, insets) -> {
              int insetTop = insets.getInsets(Type.systemBars()).top;
              int insetBottom = insets.getInsets(Type.systemBars()).bottom;

              layoutEdgeToEdge(dialog.getWindow(), insetBottom);

              if (lightNavBar) {
                // Below API 30 it does not work for non-gesture if we take the normal method
                UiUtil.setLightNavigationBar(sheet, true);
              }

              applyBottomInset(insetBottom);

              if (!isFullWidth) {
                // Don't let the sheet go below the status bar
                container.setPadding(0, insetTop, 0, 0);
              }

              behavior.addBottomSheetCallback(new BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                  updateCornerRadii(bottomSheet, insetTop, isFullWidth, radius, background);
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                  updateCornerRadii(bottomSheet, insetTop, isFullWidth, radius, background);
                }
              });

              return insets;
            });

            ViewCompat.setOnApplyWindowInsetsListener(sheet, (view, insets) -> {
              int insetImeBottom = insets.getInsets(Type.ime()).bottom;
              int insetNavBottom = insets.getInsets(Type.systemBars()).bottom;
              boolean isImeVisible = insets.isVisible(Type.ime());
              view.setPadding(
                  view.getPaddingLeft(),
                  view.getPaddingTop(),
                  view.getPaddingRight(),
                  isImeVisible ? insetImeBottom - insetNavBottom : insetImeBottom
              );
              behavior.setSkipCollapsed(isImeVisible);
              behavior.setPeekHeight(
                  isImeVisible ? BottomSheetBehavior.PEEK_HEIGHT_AUTO : peekHeightHalf
              );
              return insets;
            });

            if (decorView.getViewTreeObserver().isAlive()) {
              ViewUtil.removeOnGlobalLayoutListener(decorView, this);
            }
          }
        });

    return dialog;
  }

  private void updateCornerRadii(
      View bottomSheet, int insetTop, boolean isFullWidth, int radius, PaintDrawable background
  ) {
    if (bottomSheet.getTop() < insetTop && isFullWidth) {
      float fraction = (float) bottomSheet.getTop() / (float) insetTop;
      setCornerRadius(background, radius * fraction);
    } else if (bottomSheet.getTop() != 0 && isFullWidth) {
      setCornerRadius(background, radius);
    }
  }

  private void setCornerRadius(PaintDrawable drawable, float radius) {
    drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
  }

  private void layoutEdgeToEdge(Window window, int insetBottom) {
    boolean isOrientationPortraitOrNavAtBottom =
        UiUtil.isOrientationPortrait(requireContext()) || insetBottom > 0;
    boolean isDarkModeActive = UiUtil.isDarkModeActive(requireContext());

    int colorScrim = ColorUtils.setAlphaComponent(backgroundColor, (int) (0.7f * 255));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 29
      window.setStatusBarColor(Color.TRANSPARENT);
      if (UiUtil.isNavigationModeGesture(requireContext())) {
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(true);
      } else {
        lightNavBar = !isDarkModeActive && isOrientationPortraitOrNavAtBottom;
        if (isOrientationPortraitOrNavAtBottom) {
          window.setNavigationBarColor(colorScrim);
        } else {
          window.setNavigationBarColor(
              isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
          );
          window.setNavigationBarDividerColor(ResUtil.getColorOutlineSecondary(activity));
        }
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 28
      window.setStatusBarColor(Color.TRANSPARENT);
      lightNavBar = !isDarkModeActive && isOrientationPortraitOrNavAtBottom;
      if (isOrientationPortraitOrNavAtBottom) {
        window.setNavigationBarColor(colorScrim);
      } else {
        window.setNavigationBarColor(
            isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
        );
        window.setNavigationBarDividerColor(ResUtil.getColorOutlineSecondary(activity));
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 26
      window.setStatusBarColor(Color.TRANSPARENT);
      lightNavBar = !isDarkModeActive && isOrientationPortraitOrNavAtBottom;
      if (isOrientationPortraitOrNavAtBottom) {
        window.setNavigationBarColor(colorScrim);
      } else {
        window.setNavigationBarColor(
            isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
        );
      }
    } else  { // down to 21
      if (isOrientationPortraitOrNavAtBottom) {
        window.setNavigationBarColor(isDarkModeActive ? colorScrim : UiUtil.SCRIM);
      } else {
        window.setNavigationBarColor(
            isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
        );
      }
    }
  }

  public SharedPreferences getSharedPrefs() {
    return activity.getSharedPrefs();
  }

  public ViewUtil getViewUtil() {
    return viewUtil;
  }

  public void performHapticClick() {
    activity.performHapticClick();
  }

  public void performHapticHeavyClick() {
    activity.performHapticHeavyClick();
  }

  public void applyBottomInset(int bottom) {
  }

  public void setFullWidthListener(FullWidthListener listener) {
    fullWidthListener = listener;
  }

  public interface FullWidthListener {
    void onKnowIfFullWidth(boolean isFullWidth);
  }
}
