package xyz.zedler.patrick.tack.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.presentation.navigation.Route
import xyz.zedler.patrick.tack.presentation.screen.AboutScreen
import xyz.zedler.patrick.tack.presentation.screen.LogScreen
import xyz.zedler.patrick.tack.presentation.screen.MainScreen
import xyz.zedler.patrick.tack.presentation.screen.SettingsScreen
import xyz.zedler.patrick.tack.presentation.screen.SongScreen
import xyz.zedler.patrick.tack.presentation.screen.SongsScreen
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity(), ServiceConnection {

  private val viewModel: MainViewModel by viewModels {
    val app = application as TackApplication
    MainViewModel.Factory(app.settingsRepository, app.songRepository)
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      val useDynamicColors by viewModel.useDynamicColors.collectAsState()
      val themeHue by viewModel.themeHue.collectAsState()
      val theme by viewModel.theme.collectAsState()
      val contrast by viewModel.contrast.collectAsState()
      val backstack = viewModel.backstack

      val windowSizeClass = calculateWindowSizeClass(this)
      val widthClass = windowSizeClass.widthSizeClass

      TackTheme(
        useDynamicColors = useDynamicColors,
        hue = themeHue,
        theme = theme,
        contrast = contrast
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          BackHandler(enabled = backstack.size > 1) {
            viewModel.popBackstack()
          }

          NavDisplay(
            backStack = backstack.toList(),
            onBack = { viewModel.popBackstack() },
            entryProvider = { route ->
              when (route) {
                is Route.Main -> NavEntry(route) {
                  MainScreen(
                    widthSizeClass = widthClass,
                    onNavigateToSettings = { viewModel.navigateTo(Route.About) }
                  )
                }
                is Route.Songs -> NavEntry(route) { SongsScreen(widthClass) }
                is Route.Song -> NavEntry(route) { SongScreen(route.songId, widthClass) }
                is Route.Settings -> NavEntry(route) {
                  SettingsScreen(viewModel, onBack = { viewModel.popBackstack() })
                }
                is Route.About -> NavEntry(route) {
                  AboutScreen(viewModel, onBack = { viewModel.popBackstack() })
                }
                is Route.Log -> NavEntry(route) { LogScreen() }
              }
            }
          )
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
