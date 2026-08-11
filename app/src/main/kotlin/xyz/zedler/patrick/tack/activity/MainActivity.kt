package xyz.zedler.patrick.tack.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.hardware.HapticProviderImpl
import xyz.zedler.patrick.tack.presentation.navigation.Route
import xyz.zedler.patrick.tack.presentation.screen.AboutScreen
import xyz.zedler.patrick.tack.presentation.screen.LogScreen
import xyz.zedler.patrick.tack.presentation.screen.MainScreen
import xyz.zedler.patrick.tack.presentation.screen.SettingsScreen
import xyz.zedler.patrick.tack.presentation.screen.SongScreen
import xyz.zedler.patrick.tack.presentation.screen.SongsScreen
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.presentation.util.LocalHaptic
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.util.LocaleUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity(), ServiceConnection {

  private val viewModel: MainViewModel by viewModels {
    val app = application as TackApplication
    MainViewModel.Factory(app.settingsRepository, app.songRepository)
  }

  override fun attachBaseContext(newBase: Context) {
    val app = newBase.applicationContext as? TackApplication
    val languageCode = runBlocking {
      app?.settingsRepository?.language?.first()
    }
    super.attachBaseContext(LocaleUtil.wrap(newBase, languageCode))
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      val useDynamicColors by viewModel.useDynamicColors.collectAsState()
      val themeHue by viewModel.themeHue.collectAsState()
      val theme by viewModel.theme.collectAsState()
      val contrast by viewModel.contrast.collectAsState()
      val languageCode by viewModel.language.collectAsState()
      val hapticEnabled by viewModel.haptic.collectAsState()
      val vibrationIntensity by viewModel.vibrationIntensity.collectAsState()
      val backstack = viewModel.backstack

      val hapticProvider = remember { HapticProviderImpl(this) }

      LaunchedEffect(hapticEnabled, vibrationIntensity) {
        hapticProvider.isEnabled = hapticEnabled
        hapticProvider.intensity = vibrationIntensity
      }

      // Handle manual language change recreation for older APIs
      var isInitialCompose by remember { mutableStateOf(true) }
      LaunchedEffect(languageCode) {
        if (isInitialCompose) {
          // Prevent screen flicker on initial compose
          isInitialCompose = false
          return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          LocaleUtil.applyLocale(this@MainActivity, languageCode)
        } else {
          val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
          } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
          }
          val targetLocale = LocaleUtil.getLocale(languageCode)
          if (currentLocale.language != targetLocale.language ||
            currentLocale.country != targetLocale.country
          ) {
            recreate()
          }
        }
      }

      val windowSizeClass = calculateWindowSizeClass(this)
      val widthClass = windowSizeClass.widthSizeClass

      TackTheme(
        useDynamicColors = useDynamicColors,
        hue = themeHue,
        theme = theme,
        contrast = contrast
      ) {
        CompositionLocalProvider(LocalHaptic provides hapticProvider) {
          Surface(modifier = Modifier.fillMaxSize()) {
            BackHandler(enabled = backstack.size > 1) {
              viewModel.popBackstack()
            }

            val scaleSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
            val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

            NavDisplay(
              backStack = backstack.toList(),
              onBack = { viewModel.popBackstack() },
              entryProvider = { route ->
                when (route) {
                  is Route.Main -> NavEntry(route) {
                    MainScreen(widthSizeClass = widthClass)
                  }

                  is Route.Songs -> NavEntry(route) {
                    SongsScreen(widthClass)
                  }

                  is Route.Song -> NavEntry(route) {
                    SongScreen(route.songId, widthClass)
                  }

                  is Route.Settings -> NavEntry(route) { SettingsScreen(viewModel) }

                  is Route.About -> NavEntry(route) { AboutScreen(viewModel) }

                  is Route.Log -> NavEntry(route) { LogScreen() }
                }
              },
              transitionSpec = {
                (fadeIn(animationSpec = fadeSpec) +
                    scaleIn(initialScale = 0.9f, animationSpec = scaleSpec)) togetherWith
                    (fadeOut(animationSpec = fadeSpec) +
                        scaleOut(targetScale = 1.1f, animationSpec = scaleSpec))
              },
              popTransitionSpec = {
                (fadeIn(animationSpec = fadeSpec) +
                    scaleIn(initialScale = 1.1f, animationSpec = scaleSpec)) togetherWith
                    (fadeOut(animationSpec = fadeSpec) +
                        scaleOut(targetScale = 0.9f, animationSpec = scaleSpec))
              },
              predictivePopTransitionSpec = {
                (fadeIn(animationSpec = fadeSpec) +
                    scaleIn(initialScale = 1.1f, animationSpec = scaleSpec)) togetherWith
                    (fadeOut(animationSpec = fadeSpec) +
                        scaleOut(targetScale = 0.9f, animationSpec = scaleSpec))
              }
            )
          }
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    Intent(this, MetronomeService::class.java).also { intent ->
      bindService(intent, this, BIND_AUTO_CREATE)
    }
  }

  override fun onStop() {
    super.onStop()
    unbindService(this)
    viewModel.onServiceDisconnected()
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    val binder = service as MetronomeService.MetronomeBinder
    viewModel.onServiceConnected(binder.getService())
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    viewModel.onServiceDisconnected()
  }
}
