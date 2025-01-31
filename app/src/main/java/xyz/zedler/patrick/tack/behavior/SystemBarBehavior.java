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

package xyz.zedler.patrick.tack.behavior;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.appbar.AppBarLayout;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class SystemBarBehavior {

  private static final String TAG = SystemBarBehavior.class.getSimpleName();

  private final Activity activity;
  private final Window window;
  int containerPaddingTop, containerPaddingBottom, containerPaddingLeft, containerPaddingRight;
  int scrollContentPaddingBottom;
  private AppBarLayout appBarLayout;
  private ViewGroup container;
  private NestedScrollView scrollView;
  private ViewGroup scrollContent;
  private boolean applyAppBarInsetOnContainer;
  private boolean applyStatusBarInsetOnContainer;
  private boolean applyCutoutInsetOnContainer;
  private boolean isScrollable;
  private int addBottomInset, cutoutInsetLeft, cutoutInsetRight;

  public SystemBarBehavior(@NonNull Activity activity) {
    this.activity = activity;
    window = activity.getWindow();

    // GOING EDGE TO EDGE
    UiUtil.layoutEdgeToEdge(window);
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      window.getAttributes().layoutInDisplayCutoutMode
          = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.getAttributes().layoutInDisplayCutoutMode
          = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
    }

    applyAppBarInsetOnContainer = true;
    applyStatusBarInsetOnContainer = true;
    applyCutoutInsetOnContainer = true;
    isScrollable = false;
  }

  public void setAppBar(AppBarLayout appBarLayout) {
    this.appBarLayout = appBarLayout;
  }

  public void setContainer(ViewGroup container) {
    this.container = container;
    containerPaddingTop = container.getPaddingTop();
    containerPaddingBottom = container.getPaddingBottom();
    containerPaddingLeft = container.getPaddingLeft();
    containerPaddingRight = container.getPaddingRight();
  }

  public void setScroll(@NonNull NestedScrollView scrollView, @NonNull ViewGroup scrollContent) {
    this.scrollView = scrollView;
    this.scrollContent = scrollContent;
    scrollContentPaddingBottom = scrollContent.getPaddingBottom();

    if (container == null) {
      setContainer(scrollView);
    }
  }

  public void setAdditionalBottomInset(int additional) {
    addBottomInset = additional;
  }

  public void setUp() {
    View root = window.getDecorView().findViewById(android.R.id.content);
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
      int containerPaddingTopExtra = 0;
      int containerPaddingBottomExtra = 0;
      int containerPaddingLeftExtra = 0;
      int containerPaddingRightExtra = 0;

      // TOP INSET
      if (appBarLayout != null) {
        int statusBarInset = insets.getInsets(Type.systemBars()).top;
        // STATUS BAR INSET
        appBarLayout.setPadding(0, statusBarInset, 0, appBarLayout.getPaddingBottom());
        appBarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // APP BAR INSET
        if (hasContainer() && applyAppBarInsetOnContainer) {
          ViewGroup.MarginLayoutParams params
              = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
          params.topMargin = appBarLayout.getMeasuredHeight();
          container.setLayoutParams(params);
        } else if (hasContainer() && applyStatusBarInsetOnContainer) {
          containerPaddingTopExtra += statusBarInset;
        }
      } else if (hasContainer() && applyStatusBarInsetOnContainer) {
        // STATUS BAR INSET
        // if no app bar exists, status bar inset is applied to container
        containerPaddingTopExtra += insets.getInsets(Type.systemBars()).top;
      }

      // CUTOUT INSET
      if (hasContainer() && applyCutoutInsetOnContainer) {
        cutoutInsetLeft = insets.getInsets(Type.displayCutout()).left;
        cutoutInsetRight = insets.getInsets(Type.displayCutout()).right;
        containerPaddingLeftExtra += cutoutInsetLeft;
        containerPaddingRightExtra += cutoutInsetRight;
      }

      // NAV BAR INSET
      boolean useBottomInset = UiUtil.isOrientationPortrait(activity)
          || UiUtil.isNavigationModeGesture(activity)
          || UiUtil.isLandTablet(activity);
      if (useBottomInset && hasContainer()) {
        int navBarInset = insets.getInsets(Type.systemBars()).bottom;
        if (hasScrollView()) {
          scrollContent.setPadding(
              scrollContent.getPaddingLeft(),
              scrollContent.getPaddingTop(),
              scrollContent.getPaddingRight(),
              scrollContentPaddingBottom + addBottomInset + navBarInset
          );
        } else {
          containerPaddingBottomExtra += addBottomInset + navBarInset;
        }
      } else if (hasContainer()) {
        root.setPadding(
            insets.getInsets(Type.systemBars()).left,
            root.getPaddingTop(),
            insets.getInsets(Type.systemBars()).right,
            root.getPaddingBottom()
        );
        // Add additional bottom inset
        if (hasScrollView()) {
          scrollContent.setPadding(
              scrollContent.getPaddingLeft(),
              scrollContent.getPaddingTop(),
              scrollContent.getPaddingRight(),
              scrollContentPaddingBottom + addBottomInset
          );
        } else {
          containerPaddingBottomExtra += addBottomInset;
        }
      }

      if (hasContainer()) {
        container.setPadding(
            containerPaddingLeft + containerPaddingLeftExtra,
            containerPaddingTop + containerPaddingTopExtra,
            containerPaddingRight + containerPaddingRightExtra,
            containerPaddingBottom + containerPaddingBottomExtra
        );
      }
      return insets;
    });

    if (hasScrollView()) {
      // call viewThreeObserver, this updates the system bar appearance
      measureScrollView();
    } else {
      // call directly because there won't be any changes caused by scroll content
      updateSystemBars();
    }
  }

  private void measureScrollView() {
    scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            int scrollViewWidth = scrollView.getWidth();
            scrollViewWidth -= scrollView.getPaddingLeft() + scrollView.getPaddingRight();
            int scrollContentWidth = scrollContent.getWidth() + UiUtil.dpToPx(activity, 16);
            if (applyCutoutInsetOnContainer && scrollContentWidth < scrollViewWidth) {
              // cutout insets not needed, remove them
              scrollView.setPadding(
                  scrollView.getPaddingLeft() - cutoutInsetLeft,
                  scrollView.getPaddingTop(),
                  scrollView.getPaddingRight() - cutoutInsetRight,
                  scrollView.getPaddingBottom()
              );
            }

            int scrollViewHeight = scrollView.getHeight();
            int scrollContentHeight = scrollContent.getHeight();
            isScrollable = scrollViewHeight - scrollContentHeight < 0;
            updateSystemBars();
            // Kill ViewTreeObserver
            if (scrollView.getViewTreeObserver().isAlive()) {
              scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
  }

  public void applyAppBarInsetOnContainer(boolean apply) {
    applyAppBarInsetOnContainer = apply;
  }

  public void applyStatusBarInsetOnContainer(boolean apply) {
    applyStatusBarInsetOnContainer = apply;
  }

  public void applyCutoutInsetOnContainer(boolean apply) {
    applyCutoutInsetOnContainer = apply;
  }

  public static void applyBottomInset(@NonNull View view) {
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    int marginBottom = params.bottomMargin;
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      params.bottomMargin = marginBottom + insets.getInsets(Type.systemBars()).bottom;
      view.setLayoutParams(params);
      return insets;
    });
  }

  private void updateSystemBars() {
    boolean isOrientationPortrait = UiUtil.isOrientationPortrait(activity);
    boolean isLandTablet = UiUtil.isLandTablet(activity);
    boolean isDarkModeActive = UiUtil.isDarkModeActive(activity);

    int colorScrim = ResUtil.getColor(activity, R.attr.colorSurface, 0.7f);

    if (Build.VERSION.SDK_INT >= VERSION_CODES.VANILLA_ICE_CREAM) { // 35
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
        if (!UiUtil.isNavigationModeGesture(activity)) {
          UiUtil.setLightNavigationBar(window.getDecorView(), true);
        }
      }
      window.setNavigationBarContrastEnforced(true);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 29
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
      }
      if (UiUtil.isNavigationModeGesture(activity)) {
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(true);
      } else {
        if (!isDarkModeActive) {
          UiUtil.setLightNavigationBar(window.getDecorView(), true);
        }
        if (isOrientationPortrait || isLandTablet) {
          window.setNavigationBarColor(
              isScrollable ? colorScrim : Color.parseColor("#01000000")
          );
        } else {
          window.setNavigationBarDividerColor(
              ResUtil.getColor(activity, R.attr.colorOutlineVariant)
          );
          window.setNavigationBarColor(
              ResUtil.getColor(activity, R.attr.colorSurface)
          );
        }
      }
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) { // 28
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
        UiUtil.setLightNavigationBar(window.getDecorView(), true);
      }
      if (isOrientationPortrait || isLandTablet) {
        window.setNavigationBarColor(isScrollable ? colorScrim : Color.TRANSPARENT);
      } else {
        window.setNavigationBarDividerColor(
            ResUtil.getColor(activity, R.attr.colorOutlineVariant)
        );
        window.setNavigationBarColor(
            ResUtil.getColor(activity, R.attr.colorSurface)
        );
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 26
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
      }
      if (isOrientationPortrait || isLandTablet) {
        window.setNavigationBarColor(isScrollable ? colorScrim : Color.TRANSPARENT);
        if (!isDarkModeActive) {
          UiUtil.setLightNavigationBar(window.getDecorView(), true);
        }
      } else {
        window.setNavigationBarColor(isDarkModeActive ? Color.BLACK : UiUtil.SCRIM);
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 23
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
      }
      if (isOrientationPortrait || isLandTablet) {
        window.setNavigationBarColor(
            isDarkModeActive ? (isScrollable ? colorScrim : Color.TRANSPARENT) : UiUtil.SCRIM
        );
      } else {
        window.setNavigationBarColor(isDarkModeActive ? colorScrim : UiUtil.SCRIM);
      }
    } else { // 21
      window.setStatusBarColor(isDarkModeActive ? Color.TRANSPARENT : UiUtil.SCRIM);
      if (isOrientationPortrait || isLandTablet) {
        window.setNavigationBarColor(
            isDarkModeActive ? (isScrollable ? colorScrim : Color.TRANSPARENT) : UiUtil.SCRIM
        );
      } else {
        window.setNavigationBarColor(Color.BLACK);
      }
    }
  }

  private boolean hasContainer() {
    return container != null;
  }

  private boolean hasScrollView() {
    return scrollView != null;
  }
}
