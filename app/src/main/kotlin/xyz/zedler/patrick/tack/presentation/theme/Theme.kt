package xyz.zedler.patrick.tack.presentation.theme

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
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.rememberDynamicColorScheme
import xyz.zedler.patrick.tack.core.model.AppContrast
import xyz.zedler.patrick.tack.core.model.AppTheme

@Composable
fun TackTheme(
  useDynamicColors: Boolean = false,
  hue: Float = 150f,
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
        seedColor = Hct.from(hue.toDouble(), 70.0, 60.0).toColor(),
        isDark = isDark,
        contrastLevel = contrastLevel
      )
    }
  }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    typography = TackTypography,
    shapes = TackShapes,
    motionScheme = MotionScheme.expressive(),
    content = content
  )
}
