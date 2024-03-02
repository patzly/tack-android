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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.TempoTapUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

  private lateinit var metronomeUtil: MetronomeUtil
  private lateinit var tempoTapUtil: TempoTapUtil
  private lateinit var viewModel: MainViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()

    super.onCreate(savedInstanceState)

    setTheme(android.R.style.Theme_DeviceDefault)

    tempoTapUtil = TempoTapUtil()
    metronomeUtil = MetronomeUtil(this)
    metronomeUtil.addListener(object : MetronomeUtil.MetronomeListener {
      override fun onMetronomeStart() {
        viewModel.onPlayingChange(true)
      }
      override fun onMetronomeStop() {
        viewModel.onPlayingChange(false)
      }
      override fun onMetronomeTick(tick: MetronomeUtil.Tick?) {}
    })

    viewModel = MainViewModel(metronomeUtil)

    setContent {
      TackApp(viewModel = viewModel)
    }
  }
}