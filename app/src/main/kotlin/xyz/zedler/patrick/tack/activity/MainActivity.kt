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

package xyz.zedler.patrick.tack.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.ui.navigation.Route
import xyz.zedler.patrick.tack.ui.screen.AboutScreen
import xyz.zedler.patrick.tack.ui.screen.LogScreen
import xyz.zedler.patrick.tack.ui.screen.MainScreen
import xyz.zedler.patrick.tack.ui.screen.SettingsScreen
import xyz.zedler.patrick.tack.ui.screen.SongScreen
import xyz.zedler.patrick.tack.ui.screen.SongsScreen
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.util.LocaleUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity(), ServiceConnection {

  private val viewModel: MainViewModel by viewModels {
    val app = application as TackApplication
    MainViewModel.Factory(
      app.settingsRepository,
      app.unlockRepository,
      app.metronomeRepository,
      app.songRepository,
      app.backupRepository
    )
  }
  private val metronomeIntent by lazy {
    Intent(this, MetronomeService::class.java)
  }

  override fun attachBaseContext(newBase: Context) {
    val app = newBase.applicationContext as? TackApplication
    val languageCode = runBlocking {
      app?.settingsRepository?.settings?.first()?.language
    }
    super.attachBaseContext(LocaleUtil.wrap(newBase, languageCode))
  }

  @OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalMaterial3AdaptiveApi::class
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val app = application as TackApplication
    val hapticProvider = app.hapticProvider

    setContent {
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      val metronomeState by viewModel.metronomeState.collectAsStateWithLifecycle()
      val backstack = viewModel.backstack

      LaunchedEffect(settings.haptic, settings.vibrationIntensity) {
        hapticProvider.isEnabled = settings.haptic
        hapticProvider.intensity = settings.vibrationIntensity
      }

      LaunchedEffect(metronomeState.isHapticPossible) {
        hapticProvider.isHapticPossible = metronomeState.isHapticPossible
      }

      var isInitialCompose by remember { mutableStateOf(true) }
      LaunchedEffect(settings.language) {
        if (isInitialCompose) {
          isInitialCompose = false
          return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          LocaleUtil.applyLocale(this@MainActivity, settings.language)
        } else {
          val currentLocale = resources.configuration.locales[0]
          val targetLocale = LocaleUtil.getLocale(settings.language)
          if (currentLocale.language != targetLocale.language ||
            currentLocale.country != targetLocale.country
          ) {
            recreate()
          }
        }
      }

      val windowSizeClass = calculateWindowSizeClass(this)

      TackTheme(
        color = settings.color,
        hue = settings.colorHue,
        theme = settings.theme,
        contrast = settings.contrast
      ) {
        CompositionLocalProvider(LocalHaptic provides hapticProvider) {
          Surface(modifier = Modifier.fillMaxSize()) {
            BackHandler(enabled = backstack.size > 1) {
              viewModel.popBackstack()
            }

            val scaleSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
            val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

            val listDetailSceneStrategy = rememberListDetailSceneStrategy<Route>()

            NavDisplay(
              backStack = backstack.toList(),
              onBack = { viewModel.popBackstack() },
              sceneStrategies = listOf(listDetailSceneStrategy),
              entryProvider = { route ->
                when (route) {
                  is Route.Main -> NavEntry(key = route) {
                    MainScreen(
                      viewModel = viewModel,
                      windowSizeClass = windowSizeClass
                    )
                  }

                  is Route.Songs -> NavEntry(
                    key = route,
                    metadata = ListDetailSceneStrategy.listPane()
                  ) {
                    SongsScreen(windowSizeClass.widthSizeClass)
                  }

                  is Route.Song -> NavEntry(
                    key = route,
                    metadata = ListDetailSceneStrategy.detailPane()
                  ) {
                    SongScreen(route.songId, windowSizeClass.widthSizeClass)
                  }

                  is Route.Settings -> NavEntry(key = route) { SettingsScreen(viewModel) }
                  is Route.About -> NavEntry(key = route) { AboutScreen(viewModel) }
                  is Route.Log -> NavEntry(key = route) { LogScreen(viewModel) }
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
              predictivePopTransitionSpec = { _ ->
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

  override fun onDestroy() {
    super.onDestroy()
    if (isFinishing) {
      stopService(metronomeIntent)
    }
  }

  override fun onStart() {
    super.onStart()
    try {
      startService(metronomeIntent)
      bindService(metronomeIntent, this, Context.BIND_IMPORTANT)
    } catch (e: Exception) {
      Log.e(TAG, "onStart: $e")
    }
  }

  override fun onStop() {
    super.onStop()
    unbindService(this)
    viewModel.onServiceDisconnected()
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    val binder = service as MetronomeService.MetronomeBinder
    val metronomeService = binder.getService()
    viewModel.onServiceConnected(metronomeService)

    metronomeService.engine.warmUpAudio()
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    viewModel.onServiceDisconnected()
  }

  companion object {
    private val TAG = MainActivity::class.java.simpleName
  }
}
