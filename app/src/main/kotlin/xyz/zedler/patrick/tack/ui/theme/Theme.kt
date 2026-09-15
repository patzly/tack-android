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

package xyz.zedler.patrick.tack.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.toColor
import com.materialkolor.rememberDynamicColorScheme
import xyz.zedler.patrick.tack.core.model.AppColor
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppTheme

@Composable
fun TackTheme(
  color: AppColor = AppColor.STATIC,
  hue: Float = 154f,
  theme: AppTheme = AppTheme.SYSTEM,
  contrast: AppContrast = AppContrast.STANDARD,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val isDark = when (theme) {
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    AppTheme.SYSTEM -> darkTheme
  }

  val activity = remember(context) { context.findActivity() }

  DisposableEffect(isDark, activity) {
    activity?.enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ) { isDark },
      navigationBarStyle = SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ) { isDark },
    )
    onDispose {}
  }

  val colorScheme = when {
    color == AppColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      remember(isDark, context) {
        val scheme = if (isDark) {
          dynamicDarkColorScheme(context)
        } else {
          dynamicLightColorScheme(context)
        }
        scheme.harmonizeError()
      }
    }
    else -> {
      val contrastLevel = when (contrast) {
        AppContrast.STANDARD -> 0.0
        AppContrast.MEDIUM -> 0.5
        AppContrast.HIGH -> 1.0
      }
      val seedColor = remember(hue) {
        Hct.from(hue.toDouble(), 70.0, 60.0).toColor()
      }
      rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        contrastLevel = contrastLevel,
        modifyColorScheme = ColorScheme::harmonizeError,
      )
    }
  }

  val motionScheme = remember { MotionScheme.expressive() }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    typography = TackTypography,
    shapes = TackShapes,
    motionScheme = motionScheme,
    content = content,
  )
}

private fun ColorScheme.harmonizeError(): ColorScheme = copy(
  error = error.harmonize(primary),
  onError = onError.harmonize(primary),
  errorContainer = errorContainer.harmonize(primary),
  onErrorContainer = onErrorContainer.harmonize(primary),
)

private fun Context.findActivity(): ComponentActivity? {
  var currentContext = this
  while (currentContext is ContextWrapper) {
    if (currentContext is ComponentActivity) {
      return currentContext
    }
    currentContext = currentContext.baseContext
  }
  return null
}
