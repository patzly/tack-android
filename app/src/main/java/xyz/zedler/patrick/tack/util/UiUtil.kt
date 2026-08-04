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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

@file:JvmName("UiUtil")

package xyz.zedler.patrick.tack.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.HarmonizedColorAttributes
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import xyz.zedler.patrick.tack.Constants.CONTRAST
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.THEME
import xyz.zedler.patrick.tack.R

const val SCRIM = 0x55000000

fun Window.layoutEdgeToEdge() {
  if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
    setDecorFitsSystemWindows(false)
  } else {
    @Suppress("DEPRECATION")
    val flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    @Suppress("DEPRECATION")
    decorView.systemUiVisibility = decorView.systemUiVisibility or flags
  }
}

fun View.setLightNavigationBar(isLight: Boolean) {
  if (Build.VERSION.SDK_INT >= VERSION_CODES.S && windowInsetsController != null) {
    windowInsetsController?.setSystemBarsAppearance(
      if (isLight) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
      WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
    )
  } else if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
    @Suppress("DEPRECATION")
    var flags = systemUiVisibility
    flags = if (isLight) {
      flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    } else {
      flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
    }
    @Suppress("DEPRECATION")
    systemUiVisibility = flags
  }
}

fun View.setLightStatusBar(isLight: Boolean) {
  if (Build.VERSION.SDK_INT >= VERSION_CODES.S && windowInsetsController != null) {
    windowInsetsController?.setSystemBarsAppearance(
      if (isLight) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
      WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
    )
  } else {
    @Suppress("DEPRECATION")
    var flags = systemUiVisibility
    flags = if (isLight) {
      flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    } else {
      flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
    @Suppress("DEPRECATION")
    systemUiVisibility = flags
  }
}

fun Activity.setTheme(sharedPrefs: SharedPreferences) {
  when (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
    THEME.RED -> setContrastTheme(
      sharedPrefs,
      R.style.Theme_Tack_Red,
      R.style.ThemeOverlay_Tack_Red_MediumContrast,
      R.style.ThemeOverlay_Tack_Red_HighContrast
    )

    THEME.YELLOW -> setContrastTheme(
      sharedPrefs,
      R.style.Theme_Tack_Yellow,
      R.style.ThemeOverlay_Tack_Yellow_MediumContrast,
      R.style.ThemeOverlay_Tack_Yellow_HighContrast
    )

    THEME.GREEN -> setContrastTheme(
      sharedPrefs,
      R.style.Theme_Tack_Green,
      R.style.ThemeOverlay_Tack_Green_MediumContrast,
      R.style.ThemeOverlay_Tack_Green_HighContrast
    )

    THEME.BLUE -> setContrastTheme(
      sharedPrefs,
      R.style.Theme_Tack_Blue,
      R.style.ThemeOverlay_Tack_Blue_MediumContrast,
      R.style.ThemeOverlay_Tack_Blue_HighContrast
    )

    else -> if (DynamicColors.isDynamicColorAvailable()) {
      DynamicColors.applyToActivityIfAvailable(
        this,
        DynamicColorsOptions.Builder().setOnAppliedCallback { activity ->
          HarmonizedColors.applyToContextIfAvailable(
            activity, HarmonizedColorsOptions.createMaterialDefaults()
          )
        }.build()
      )
    } else {
      setContrastTheme(
        sharedPrefs,
        R.style.Theme_Tack_Yellow,
        R.style.ThemeOverlay_Tack_Yellow_MediumContrast,
        R.style.ThemeOverlay_Tack_Yellow_HighContrast
      )
    }
  }
}

private fun Activity.setContrastTheme(
  sharedPrefs: SharedPreferences,
  @StyleRes resIdStandard: Int,
  @StyleRes resIdMedium: Int,
  @StyleRes resIdHigh: Int
) {
  when (sharedPrefs.getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)) {
    CONTRAST.MEDIUM -> setTheme(resIdMedium)
    CONTRAST.HIGH -> setTheme(resIdHigh)
    else -> setTheme(resIdStandard)
  }
}

fun Context.applyColorHarmonization() {
  val attrIds = intArrayOf(
    R.attr.colorError,
    R.attr.colorOnError,
    R.attr.colorErrorContainer,
    R.attr.colorOnErrorContainer,
    R.attr.colorCustomGreen,
    R.attr.colorOnCustomGreen,
    R.attr.colorCustomGreenContainer,
    R.attr.colorOnCustomGreenContainer
  )
  val options = HarmonizedColorsOptions.Builder()
    .setColorAttributes(HarmonizedColorAttributes.create(attrIds))
    .build()
  HarmonizedColors.applyToContextIfAvailable(this, options)
}

fun Context.isDarkModeActive(): Boolean {
  val uiMode = resources.configuration.uiMode
  return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

fun Context.isNavigationModeGesture(): Boolean {
  val navGesture = 2

  @SuppressLint("DiscouragedApi")
  val resourceId = resources.getIdentifier(
    "config_navBarInteractionMode", "integer", "android"
  )
  val mode = if (resourceId > 0) resources.getInteger(resourceId) else 0
  return mode == navGesture
}

fun Context.isOrientationPortrait(): Boolean {
  return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

fun Context.isTablet(): Boolean {
  return resources.configuration.smallestScreenWidthDp > 600
}

fun Context.isLandTablet(): Boolean {
  return !isOrientationPortrait() && resources.configuration.smallestScreenWidthDp > 600
}

fun Context.isLayoutRtl(): Boolean {
  return resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}

fun Context.isFullWidth(): Boolean {
  val maxWidth = resources.getDimensionPixelSize(R.dimen.max_content_width)
  return maxWidth >= getDisplayWidth()
}

fun Activity.keepScreenAwake(keepAwake: Boolean) {
  window?.let {
    if (keepAwake) {
      it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }
}

// IME

fun View.showKeyboard() {
  val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
  imm?.let {
    postDelayed({
      it.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }, 100)
  }
}

fun View.hideKeyboard() {
  val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
  imm?.hideSoftInputFromWindow(windowToken, 0)
}

// Unit conversions

fun Context.dpToPx(@Dimension(unit = Dimension.DP) dp: Float): Int {
  return Math.round(dp * resources.displayMetrics.density)
}

fun Context.dpFromPx(@Dimension px: Float): Int {
  return (px / resources.displayMetrics.density).toInt()
}

fun Context.spToPx(@Dimension(unit = Dimension.SP) sp: Float): Int {
  return TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics
  ).toInt()
}

// Display metrics

fun Context.getDisplayWidth(): Int = getDisplayMetrics(true)

fun Context.getDisplayHeight(): Int = getDisplayMetrics(false)

private fun Context.getDisplayMetrics(useWidth: Boolean): Int {
  val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
  return if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
    val windowMetrics = windowManager.currentWindowMetrics
    if (useWidth) windowMetrics.bounds.width() else windowMetrics.bounds.height()
  } else {
    val displayMetrics = android.util.DisplayMetrics()
    @Suppress("DEPRECATION")
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    if (useWidth) displayMetrics.widthPixels else displayMetrics.heightPixels
  }
}
