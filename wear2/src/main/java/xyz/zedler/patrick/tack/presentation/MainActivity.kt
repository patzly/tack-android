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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.presentation

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.NotificationUtil
import xyz.zedler.patrick.tack.util.TempoTapUtil
import xyz.zedler.patrick.tack.util.keepScreenAwake
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity(), ServiceConnection {

  companion object {
    private const val TAG = "MainActivity"
  }

  private lateinit var metronomeService: MetronomeService
  private lateinit var metronomeUtil: MetronomeUtil
  private lateinit var tempoTapUtil: TempoTapUtil
  private lateinit var viewModel: MainViewModel
  private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
  private var bound: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()

    super.onCreate(savedInstanceState)

    setTheme(android.R.style.Theme_DeviceDefault)

    tempoTapUtil = TempoTapUtil()
    metronomeUtil = MetronomeUtil(this, false)
    metronomeUtil.addListener(object : MetronomeUtil.MetronomeListener {
      override fun onMetronomeStart() {
        if (NotificationUtil.hasPermission(this@MainActivity)) {
          keepScreenAwake(this@MainActivity, getMetronomeUtil().keepAwake)
        } else {
          viewModel.changePlaying(false)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }
      override fun onMetronomeStop() {
        keepScreenAwake(this@MainActivity, false)
      }
      override fun onMetronomeTick(tick: MetronomeUtil.Tick) {}
    })

    requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
          viewModel.changeShowPermissionDialog(true)
        }
      }

    viewModel = MainViewModel(metronomeUtil)
    updateMetronomeUtil()

    setContent {
      TackApp(
        viewModel = viewModel,
        onPermissionRequestClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      )
    }
  }

  override fun onStart() {
    super.onStart()
    try {
      val intent = Intent(this, MetronomeService::class.java)
      startService(intent)
      // cannot use startForegroundService
      // would cause crash as notification is only displayed when metronome is playing
      bindService(intent, this, BIND_IMPORTANT)
    } catch (e: Exception) {
      Log.e(TAG, "onStart: could not bind metronome service", e)
    }
  }

  override fun onStop() {
    super.onStop()
    if (bound) {
      unbindService(this)
      bound = false
      updateMetronomeUtil()
    }
  }

  override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
    val binder: MetronomeService.MetronomeBinder = iBinder as MetronomeService.MetronomeBinder
    metronomeService = binder.service
    bound = true
    updateMetronomeUtil()
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    bound = false
    updateMetronomeUtil()
  }

  override fun onBindingDied(name: ComponentName?) {
    bound = false
    unbindService(this)
    try {
      val intent = Intent(this, MetronomeService::class.java)
      bindService(intent, this, BIND_AUTO_CREATE)
    } catch (e: IllegalStateException) {
      Log.e(TAG, "onBindingDied: cannot start MetronomeService because app is in background")
    }
  }

  private fun getMetronomeUtil(): MetronomeUtil {
    return if (bound) {
      metronomeService.getMetronomeUtil()
    } else {
      metronomeUtil
    }
  }

  private fun updateMetronomeUtil() {
    val listeners: MutableSet<MetronomeUtil.MetronomeListener> = HashSet(metronomeUtil.listeners)
    if (bound) {
      listeners.addAll(metronomeService.getMetronomeUtil().listeners)
    }
    getMetronomeUtil().addListeners(listeners)
    getMetronomeUtil().setToPreferences()
    viewModel.metronomeUtil = getMetronomeUtil()
    viewModel.mutableIsPlaying.value = getMetronomeUtil().isPlaying
  }
}