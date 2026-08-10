package xyz.zedler.patrick.tack.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppTheme

@Composable
fun TackTheme(
  useDynamicColors: Boolean = false,
  hue: Float = 200f,
  theme: AppTheme = AppTheme.SYSTEM,
  contrast: AppContrast = AppContrast.STANDARD,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val isDark = when (theme) {
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    AppTheme.SYSTEM -> darkTheme
  }

  val colorScheme = when {
    useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    else -> {
      val contrastLevel = when (contrast) {
        AppContrast.MEDIUM -> 0.5
        AppContrast.HIGH -> 1.0
        AppContrast.STANDARD -> 0.0
      }
      rememberDynamicColorScheme(
        seedColor = Color.hsv(hue, 0.4f, 0.4f),
        isDark = isDark,
        contrastLevel = contrastLevel
      )
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = TackTypography,
    shapes = TackShapes,
    content = content
  )
}
