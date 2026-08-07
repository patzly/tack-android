package xyz.zedler.patrick.tack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun TackTheme(
  useDynamicColors: Boolean,
  hue: Float,
  theme: String,
  contrast: String,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val isDark = when (theme) {
    "light" -> false
    "dark" -> true
    else -> darkTheme
  }

  val colorScheme = when {
    useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    else -> {
      val contrastLevel = when (contrast) {
        "medium" -> 0.5
        "high" -> 1.0
        else -> 0.0
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
    content = content
  )
}
