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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val isDark = remember(theme, darkTheme) {
    when (theme) {
      AppTheme.LIGHT -> false
      AppTheme.DARK -> true
      AppTheme.SYSTEM -> darkTheme
    }
  }

  DisposableEffect(isDark) {
    (context as? ComponentActivity)?.enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ) { isDark },
      navigationBarStyle = SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ) { isDark }
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
        scheme.copy(
          error = scheme.error.harmonize(scheme.primary),
          onError = scheme.onError.harmonize(scheme.primary),
          errorContainer = scheme.errorContainer.harmonize(scheme.primary),
          onErrorContainer = scheme.onErrorContainer.harmonize(scheme.primary)
        )
      }
    }
    else -> {
      val contrastLevel = remember(contrast) {
        when (contrast) {
          AppContrast.MEDIUM -> 0.5
          AppContrast.HIGH -> 1.0
          AppContrast.STANDARD -> 0.0
        }
      }
      val seedColor = remember(hue) {
        Hct.from(hue.toDouble(), 70.0, 60.0).toColor()
      }
      rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        contrastLevel = contrastLevel,
        modifyColorScheme = {
          it.copy(
            error = it.error.harmonize(it.primary),
            onError = it.onError.harmonize(it.primary),
            errorContainer = it.errorContainer.harmonize(it.primary),
            onErrorContainer = it.onErrorContainer.harmonize(it.primary)
          )
        }
      )
    }
  }

  val motionScheme = remember { MotionScheme.expressive() }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    typography = TackTypography,
    shapes = TackShapes,
    motionScheme = motionScheme,
    content = content
  )
}
