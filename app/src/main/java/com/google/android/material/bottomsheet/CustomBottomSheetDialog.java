/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package com.google.android.material.bottomsheet;

import static android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
import static com.google.android.material.color.MaterialColors.isColorLight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.material.R;
import com.google.android.material.motion.MaterialBackOrchestrator;
import com.google.android.material.shape.MaterialShapeDrawable;
import xyz.zedler.patrick.tack.util.UiUtil;

/**
 * Base class for {@link android.app.Dialog}s styled as a bottom sheet.
 *
 * <p>Edge to edge window flags are automatically applied if the {@link
 * android.R.attr#navigationBarColor} is transparent or translucent and {@code enableEdgeToEdge} is
 * true. These can be set in the theme that is passed to the constructor, or will be taken from the
 * theme of the context (ie. your application or activity theme).
 *
 * <p>In edge to edge mode, padding will be added automatically to the top when sliding under the
 * status bar. Padding can be applied automatically to the left, right, or bottom if any of
 * `paddingBottomSystemWindowInsets`, `paddingLeftSystemWindowInsets`, or
 * `paddingRightSystemWindowInsets` are set to true in the style.
 */
@SuppressLint("RestrictedApi")
public class CustomBottomSheetDialog extends AppCompatDialog {

  private BottomSheetBehavior<FrameLayout> behavior;

  private FrameLayout container;
  private CoordinatorLayout coordinator;
  private FrameLayout bottomSheet;

  boolean dismissWithAnimation;

  boolean cancelable = true;
  private boolean canceledOnTouchOutside = true;
  private boolean canceledOnTouchOutsideSet;
  private EdgeToEdgeCallback edgeToEdgeCallback;
  @Nullable
  private MaterialBackOrchestrator backOrchestrator;

  public CustomBottomSheetDialog(@NonNull Context context) {
    this(context, 0);
  }

  public CustomBottomSheetDialog(@NonNull Context context, @StyleRes int theme) {
    super(context, getThemeResId(context, theme));
    // We hide the title bar for any style configuration. Otherwise, there will be a gap
    // above the bottom sheet when it is expanded.
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  protected CustomBottomSheetDialog(
      @NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
    super(context, cancelable, cancelListener);
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    this.cancelable = cancelable;
  }

  @Override
  public void setContentView(@LayoutRes int layoutResId) {
    super.setContentView(wrapInBottomSheet(layoutResId, null, null));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Window window = getWindow();
    if (window != null) {
      // The status bar should always be transparent because of the window animation.
      window.setStatusBarColor(0);

      window.addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      if (VERSION.SDK_INT < VERSION_CODES.M) {
        // It can be transparent for API 23 and above because we will handle switching the status
        // bar icons to light or dark as appropriate. For API 21 and API 22 we just set the
        // translucent status bar.
        window.addFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS);
      }
      if (!UiUtil.isDarkModeActive(getContext()) && !UiUtil.isNavigationModeGesture(getContext())) {
        UiUtil.setLightNavigationBar(window.getDecorView(), true);
      }
      window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
  }

  @Override
  public void setContentView(@NonNull View view) {
    super.setContentView(wrapInBottomSheet(0, view, null));
  }

  @Override
  public void setContentView(@NonNull View view, ViewGroup.LayoutParams params) {
    super.setContentView(wrapInBottomSheet(0, view, params));
  }

  @Override
  public void setCancelable(boolean cancelable) {
    super.setCancelable(cancelable);
    if (this.cancelable != cancelable) {
      this.cancelable = cancelable;
      if (behavior != null) {
        behavior.setHideable(cancelable);
      }
      if (getWindow() != null) {
        updateListeningForBackCallbacks();
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (behavior != null && behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
      behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    Window window = getWindow();
    if (window != null) {
      // If the navigation bar is transparent at all the BottomSheet should be edge to edge.
      boolean drawEdgeToEdge = Color.alpha(window.getNavigationBarColor()) < 255;
      if (container != null) {
        container.setFitsSystemWindows(!drawEdgeToEdge);
      }
      if (coordinator != null) {
        coordinator.setFitsSystemWindows(!drawEdgeToEdge);
      }
      WindowCompat.setDecorFitsSystemWindows(window, !drawEdgeToEdge);
      updateListeningForBackCallbacks();
    }
  }

  @Override
  public void onDetachedFromWindow() {
    if (backOrchestrator != null) {
      backOrchestrator.stopListeningForBackCallbacks();
    }
  }

  /**
   * This function can be called from a few different use cases, including Swiping the dialog down
   * or calling `dismiss()` from a `BottomSheetDialogFragment`, tapping outside a dialog, etc...
   *
   * <p>The default animation to dismiss this dialog is a fade-out transition through a
   * windowAnimation. Call {@link #setDismissWithAnimation(boolean)} if you want to utilize the
   * BottomSheet animation instead.
   *
   * <p>If this function is called from a swipe down interaction, or dismissWithAnimation is false,
   * then keep the default behavior.
   *
   * <p>Else, since this is a terminal event which will finish this dialog, we override the attached
   * {@link BottomSheetBehavior.BottomSheetCallback} to call this function, after {@link
   * BottomSheetBehavior#STATE_HIDDEN} is set. This will enforce the swipe down animation before
   * canceling this dialog.
   */
  @Override
  public void cancel() {
    BottomSheetBehavior<FrameLayout> behavior = getBehavior();

    if (!dismissWithAnimation || behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
      super.cancel();
    } else {
      behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
  }

  @Override
  public void setCanceledOnTouchOutside(boolean cancel) {
    super.setCanceledOnTouchOutside(cancel);
    if (cancel && !cancelable) {
      cancelable = true;
    }
    canceledOnTouchOutside = cancel;
    canceledOnTouchOutsideSet = true;
  }

  @NonNull
  public BottomSheetBehavior<FrameLayout> getBehavior() {
    if (behavior == null) {
      // The content hasn't been set, so the behavior doesn't exist yet. Let's create it.
      ensureContainerAndBehavior();
    }
    return behavior;
  }

  /**
   * Set to perform the swipe down animation when dismissing instead of the window animation for the
   * dialog.
   *
   * @param dismissWithAnimation True if swipe down animation should be used when dismissing.
   */
  public void setDismissWithAnimation(boolean dismissWithAnimation) {
    this.dismissWithAnimation = dismissWithAnimation;
  }

  /**
   * Returns if dismissing will perform the swipe down animation on the bottom sheet, rather than
   * the window animation for the dialog.
   */
  public boolean getDismissWithAnimation() {
    return dismissWithAnimation;
  }

  /** Creates the container layout which must exist to find the behavior */
  private void ensureContainerAndBehavior() {
    if (container == null) {
      container = (FrameLayout) View.inflate(
          getContext(),
          xyz.zedler.patrick.tack.R.layout.dialog_bottom_sheet_custom, null
      );

      coordinator = container.findViewById(R.id.coordinator);
      bottomSheet = container.findViewById(R.id.design_bottom_sheet);

      behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.addBottomSheetCallback(bottomSheetCallback);
      behavior.setHideable(cancelable);
      backOrchestrator = new MaterialBackOrchestrator(behavior, bottomSheet);
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private View wrapInBottomSheet(
      int layoutResId, @Nullable View view, @Nullable ViewGroup.LayoutParams params) {
    ensureContainerAndBehavior();
    CoordinatorLayout coordinator = container.findViewById(R.id.coordinator);
    if (layoutResId != 0 && view == null) {
      view = getLayoutInflater().inflate(layoutResId, coordinator, false);
    }

    ViewCompat.setOnApplyWindowInsetsListener(
        bottomSheet,
        (v, insets) -> {
          if (edgeToEdgeCallback != null) {
            behavior.removeBottomSheetCallback(edgeToEdgeCallback);
          }

          edgeToEdgeCallback = new EdgeToEdgeCallback(bottomSheet, insets);
          behavior.addBottomSheetCallback(edgeToEdgeCallback);

          return insets;
        });

    bottomSheet.removeAllViews();
    if (params == null) {
      bottomSheet.addView(view);
    } else {
      bottomSheet.addView(view, params);
    }
    // We treat the CoordinatorLayout as outside the dialog though it is technically inside
    coordinator
        .findViewById(R.id.touch_outside)
        .setOnClickListener(v -> {
          if (cancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
            cancel();
          }
        });
    // Handle accessibility events
    ViewCompat.setAccessibilityDelegate(
        bottomSheet,
        new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (cancelable) {
              info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS);
              info.setDismissable(true);
            } else {
              info.setDismissable(false);
            }
          }

          @Override
          public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
            if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS && cancelable) {
              cancel();
              return true;
            }
            return super.performAccessibilityAction(host, action, args);
          }
        });
    bottomSheet.setOnTouchListener((v, event) -> {
      // Consume the event and prevent it from falling through
      return true;
    });
    return container;
  }

  private void updateListeningForBackCallbacks() {
    if (backOrchestrator == null) {
      return;
    }
    if (cancelable) {
      backOrchestrator.startListeningForBackCallbacks();
    } else {
      backOrchestrator.stopListeningForBackCallbacks();
    }
  }

  boolean shouldWindowCloseOnTouchOutside() {
    if (!canceledOnTouchOutsideSet) {
      TypedArray a =
          getContext().obtainStyledAttributes(new int[] {android.R.attr.windowCloseOnTouchOutside});
      canceledOnTouchOutside = a.getBoolean(0, true);
      a.recycle();
      canceledOnTouchOutsideSet = true;
    }
    return canceledOnTouchOutside;
  }

  @SuppressLint("PrivateResource")
  private static int getThemeResId(@NonNull Context context, int themeId) {
    if (themeId == 0) {
      // If the provided theme is 0, then retrieve the dialogTheme from our theme
      TypedValue outValue = new TypedValue();
      if (context.getTheme().resolveAttribute(R.attr.bottomSheetDialogTheme, outValue, true)) {
        themeId = outValue.resourceId;
      } else {
        // bottomSheetDialogTheme is not provided; we default to our light theme
        themeId = R.style.Theme_Design_Light_BottomSheetDialog;
      }
    }
    return themeId;
  }

  void removeDefaultCallback() {
    behavior.removeBottomSheetCallback(bottomSheetCallback);
  }

  @NonNull
  private final BottomSheetBehavior.BottomSheetCallback bottomSheetCallback =
      new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(
            @NonNull View bottomSheet, @BottomSheetBehavior.State int newState) {
          if (newState == BottomSheetBehavior.STATE_HIDDEN) {
            cancel();
          }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
      };

  private static class EdgeToEdgeCallback extends BottomSheetBehavior.BottomSheetCallback {

    private final boolean lightBottomSheet;
    private final boolean lightStatusBar;
    private final WindowInsetsCompat insetsCompat;
    private final boolean isFullWidth;

    private EdgeToEdgeCallback(
        @NonNull final View bottomSheet, @NonNull WindowInsetsCompat insetsCompat) {
      this.insetsCompat = insetsCompat;
      lightStatusBar = VERSION.SDK_INT >= VERSION_CODES.M
          && (bottomSheet.getSystemUiVisibility() & SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0;

      BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

      // Try to find the background color to automatically change the status bar icons so they will
      // still be visible when the bottomSheet slides underneath the status bar.
      ColorStateList backgroundTint;
      MaterialShapeDrawable msd = behavior.getMaterialShapeDrawable();
      if (msd != null) {
        backgroundTint = msd.getFillColor();
      } else {
        backgroundTint = ViewCompat.getBackgroundTintList(bottomSheet);
      }

      isFullWidth = behavior.getMaxWidth() >= UiUtil.getDisplayWidth(bottomSheet.getContext());

      if (backgroundTint != null) {
        // First check for a tint
        lightBottomSheet = isColorLight(backgroundTint.getDefaultColor());
      } else if (bottomSheet.getBackground() instanceof ColorDrawable) {
        // Then check for the background color
        lightBottomSheet = isColorLight(((ColorDrawable) bottomSheet.getBackground()).getColor());
      } else {
        // Otherwise don't change the status bar color
        lightBottomSheet = lightStatusBar;
      }
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
      setPaddingForPosition(bottomSheet);
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
      setPaddingForPosition(bottomSheet);
    }

    @Override
    void onLayout(@NonNull View bottomSheet) {
      setPaddingForPosition(bottomSheet);
    }

    private void setPaddingForPosition(View bottomSheet) {
      if (bottomSheet.getTop() < insetsCompat.getInsets(Type.systemBars()).top && isFullWidth) {
        // If the bottomSheet is light, we should set light status bar so the icons are visible
        // since the bottomSheet is now under the status bar.
        setLightStatusBar(bottomSheet, lightBottomSheet);
        // Smooth transition into status bar when drawing edge to edge.
        bottomSheet.setPadding(
            bottomSheet.getPaddingLeft(),
            insetsCompat.getInsets(Type.systemBars()).top - bottomSheet.getTop(),
            bottomSheet.getPaddingRight(),
            0
        );
      } else if (bottomSheet.getTop() != 0) {
        // Reset the status bar icons to the original color because the bottomSheet is not under the
        // status bar.
        setLightStatusBar(bottomSheet, lightStatusBar);
        bottomSheet.setPadding(
            bottomSheet.getPaddingLeft(),
            0,
            bottomSheet.getPaddingRight(),
            0
        );
      }
    }
  }

  public static void setLightStatusBar(@NonNull View view, boolean isLight) {
    if (VERSION.SDK_INT >= VERSION_CODES.R && view.getWindowInsetsController() != null) {
      view.getWindowInsetsController().setSystemBarsAppearance(
          isLight ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
      );
    } else if (VERSION.SDK_INT >= VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      if (isLight) {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      view.setSystemUiVisibility(flags);
    }
  }
}
