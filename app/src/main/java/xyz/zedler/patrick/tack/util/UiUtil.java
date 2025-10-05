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

package xyz.zedler.patrick.tack.util;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.HarmonizedColorAttributes;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import xyz.zedler.patrick.tack.Constants.CONTRAST;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;

public class UiUtil {

  public static final int SCRIM = 0x55000000;
  public static final int SCRIM_DARK_DIALOG = 0xFF0c0c0e;
  public static final int SCRIM_LIGHT_DIALOG = 0xFF666666;

  public static void layoutEdgeToEdge(Window window) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      window.setDecorFitsSystemWindows(false);
    } else {
      int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
      View decorView = window.getDecorView();
      decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | flags);
    }
  }

  public static void setLightNavigationBar(@NonNull View view, boolean isLight) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.S && view.getWindowInsetsController() != null) {
      // API is already available in R but very buggy
      view.getWindowInsetsController().setSystemBarsAppearance(
          isLight ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0,
          WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
      );
    } else if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
      int flags = view.getSystemUiVisibility();
      if (isLight) {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      } else {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      }
      view.setSystemUiVisibility(flags);
    }
  }

  public static void setLightStatusBar(@NonNull View view, boolean isLight) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.S && view.getWindowInsetsController() != null) {
      // API is already available in R but very buggy
      view.getWindowInsetsController().setSystemBarsAppearance(
          isLight ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
      );
    } else {
      int flags = view.getSystemUiVisibility();
      if (isLight) {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      view.setSystemUiVisibility(flags);
    }
  }

  public static void setTheme(@NonNull Activity activity, @NonNull SharedPreferences sharedPrefs) {
    switch (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
      case THEME.RED:
        setContrastTheme(
            activity, sharedPrefs,
            R.style.Theme_Tack_Red,
            R.style.ThemeOverlay_Tack_Red_MediumContrast,
            R.style.ThemeOverlay_Tack_Red_HighContrast
        );
        break;
      case THEME.YELLOW:
        setContrastTheme(
            activity, sharedPrefs,
            R.style.Theme_Tack_Yellow,
            R.style.ThemeOverlay_Tack_Yellow_MediumContrast,
            R.style.ThemeOverlay_Tack_Yellow_HighContrast
        );
        break;
      case THEME.GREEN:
        setContrastTheme(
            activity, sharedPrefs,
            R.style.Theme_Tack_Green,
            R.style.ThemeOverlay_Tack_Green_MediumContrast,
            R.style.ThemeOverlay_Tack_Green_HighContrast
        );
        break;
      case THEME.BLUE:
        setContrastTheme(
            activity, sharedPrefs,
            R.style.Theme_Tack_Blue,
            R.style.ThemeOverlay_Tack_Blue_MediumContrast,
            R.style.ThemeOverlay_Tack_Blue_HighContrast
        );
        break;
      default:
        if (DynamicColors.isDynamicColorAvailable()) {
          DynamicColors.applyToActivityIfAvailable(
              activity,
              new DynamicColorsOptions.Builder().setOnAppliedCallback(
                  a -> HarmonizedColors.applyToContextIfAvailable(
                      a, HarmonizedColorsOptions.createMaterialDefaults()
                  )
              ).build()
          );
        } else {
          setContrastTheme(
              activity, sharedPrefs,
              R.style.Theme_Tack_Yellow,
              R.style.ThemeOverlay_Tack_Yellow_MediumContrast,
              R.style.ThemeOverlay_Tack_Yellow_HighContrast
          );
        }
        break;
    }
  }

  private static void setContrastTheme(
      Activity activity,
      SharedPreferences sharedPrefs,
      @StyleRes int resIdStandard,
      @StyleRes int resIdMedium,
      @StyleRes int resIdHigh
  ) {
    switch (sharedPrefs.getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)) {
      case CONTRAST.MEDIUM:
        activity.setTheme(resIdMedium);
        break;
      case CONTRAST.HIGH:
        activity.setTheme(resIdHigh);
        break;
      default:
        activity.setTheme(resIdStandard);
    }
  }

  public static void applyColorHarmonization(Context context) {
    int[] attrIds = new int[] {
        R.attr.colorError,
        R.attr.colorOnError,
        R.attr.colorErrorContainer,
        R.attr.colorOnErrorContainer,

        R.attr.colorCustomGreen,
        R.attr.colorOnCustomGreen,
        R.attr.colorCustomGreenContainer,
        R.attr.colorOnCustomGreenContainer
    };
    HarmonizedColorsOptions options = new HarmonizedColorsOptions.Builder()
        .setColorAttributes(HarmonizedColorAttributes.create(attrIds))
        .build();
    HarmonizedColors.applyToContextIfAvailable(context, options);
  }

  public static boolean isDarkModeActive(Context context) {
    int uiMode = context.getResources().getConfiguration().uiMode;
    return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  public static boolean isNavigationModeGesture(Context context) {
    final int NAV_GESTURE = 2;
    Resources resources = context.getResources();
    @SuppressLint("DiscouragedApi")
    int resourceId = resources.getIdentifier(
        "config_navBarInteractionMode", "integer", "android"
    );
    int mode = resourceId > 0 ? resources.getInteger(resourceId) : 0;
    return mode == NAV_GESTURE;
  }

  public static boolean isOrientationPortrait(Context context) {
    int orientation = context.getResources().getConfiguration().orientation;
    return orientation == Configuration.ORIENTATION_PORTRAIT;
  }

  public static boolean isTablet(Context context) {
    int width = context.getResources().getConfiguration().smallestScreenWidthDp;
    return width > 600;
  }

  public static boolean isLandTablet(Context context) {
    boolean isPortrait = isOrientationPortrait(context);
    int width = context.getResources().getConfiguration().smallestScreenWidthDp;
    return !isPortrait && width > 600;
  }

  public static boolean isLayoutRtl(Context context) {
    int direction = context.getResources().getConfiguration().getLayoutDirection();
    return direction == View.LAYOUT_DIRECTION_RTL;
  }

  public static boolean isFullWidth(Context context) {
    int maxWidth = context.getResources().getDimensionPixelSize(R.dimen.max_content_width);
    return maxWidth >= getDisplayWidth(context);
  }

  public static void keepScreenAwake(@NonNull Activity activity, boolean keepAwake) {
    Window window = activity.getWindow();
    if (window == null) {
      return;
    }
    if (keepAwake) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  // IME

  public static void showKeyboard(@NonNull View view) {
    InputMethodManager imm = getInputMethodManager(view);
    if (imm != null) {
      view.postDelayed(
          () -> imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT),
          100
      );
    }
  }

  public static void hideKeyboard(@NonNull View currentFocus) {
    InputMethodManager imm = getInputMethodManager(currentFocus);
    if (imm != null) {
      imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
    }
  }

  @Nullable
  private static InputMethodManager getInputMethodManager(@NonNull View view) {
    return getSystemService(view.getContext(), InputMethodManager.class);
  }

  // Unit conversions

  public static int dpToPx(@NonNull Context context, @Dimension(unit = Dimension.DP) float dp) {
    Resources r = context.getResources();
    return Math.round(dp * r.getDisplayMetrics().density);
  }

  public static int dpFromPx(@NonNull Context context, @Dimension float px) {
    Resources r = context.getResources();
    return (int) (px / r.getDisplayMetrics().density);
  }

  public static int spToPx(@NonNull Context context, @Dimension(unit = Dimension.SP) float sp) {
    Resources r = context.getResources();
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, r.getDisplayMetrics());
  }

  // Display metrics

  public static int getDisplayWidth(Context context) {
    return getDisplayMetrics(context, true);
  }

  public static int getDisplayHeight(Context context) {
    return getDisplayMetrics(context, false);
  }

  private static int getDisplayMetrics(Context context, boolean useWidth) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
      if (useWidth) {
        return windowMetrics.getBounds().width();
      } else {
        return windowMetrics.getBounds().height();
      }
    } else {
      DisplayMetrics displayMetrics = new DisplayMetrics();
      windowManager.getDefaultDisplay().getMetrics(displayMetrics);
      return useWidth ? displayMetrics.widthPixels : displayMetrics.heightPixels;
    }
  }
}
