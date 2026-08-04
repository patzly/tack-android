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

package xyz.zedler.patrick.tack.util.dialog

import android.os.Bundle
import com.google.android.material.slider.Slider
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogLatencyBinding
import xyz.zedler.patrick.tack.fragment.SettingsFragment
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListenerAdapter
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick
import xyz.zedler.patrick.tack.util.*

class LatencyDialogUtil(
  private val activity: MainActivity,
  private val fragment: SettingsFragment
) {

  private val binding = PartialDialogLatencyBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "latency")
  private val colorBg = activity.getAttrColor(R.attr.colorSurfaceBright)
  private val colorBgFlash = activity.getAttrColor(R.attr.colorTertiaryContainer)
  private var flashScreen = false

  private val latencyListener = object : MetronomeListenerAdapter() {
    override fun onMetronomeTick(tick: Tick) {
      activity.runOnUiThread {
        if (flashScreen) {
          binding.linearLatencyFlash.setBackgroundColor(colorBgFlash)
          binding.linearLatencyFlash.postDelayed({
            binding.linearLatencyFlash.setBackgroundColor(colorBg)
          }, 100)
        }
      }
    }
  }

  init {
    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.settings_latency)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
    }

    binding.sliderLatency.addOnChangeListener { _, value, fromUser ->
      if (fromUser) {
        activity.metronomeEngine?.let { engine ->
          engine.latency = value.toLong()
          updateValueDisplay()
          fragment.updateLatencyDescription(value.toLong())
        }
      }
    }

    binding.sliderLatency.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
        flashScreen = true
        activity.metronomeEngine?.apply {
          savePlayingState()
          addListener(latencyListener)
          setUpLatencyCalibration()
        }
      }

      override fun onStopTrackingTouch(slider: Slider) {
        flashScreen = false
        activity.metronomeEngine?.apply {
          restorePlayingState()
          removeListener(latencyListener)
          setToPreferences()
        }
      }
    })

    updateDividerVisibility(activity.isOrientationPortrait().not())
  }

  fun show() {
    update()
    dialogUtil.show()
  }

  fun showIfWasShown(state: Bundle?) {
    update()
    dialogUtil.showIfWasShown(state)
  }

  fun dismiss() {
    dialogUtil.dismiss()
  }

  fun saveState(outState: Bundle) {
    dialogUtil.saveState(outState)
  }

  private fun update() {
    measureScrollView()
    updateValueDisplay()
    activity.metronomeEngine?.let { engine ->
      binding.sliderLatency.value = engine.latency.toFloat()
    }
  }

  private fun updateValueDisplay() {
    val engine = activity.metronomeEngine ?: return
    binding.textLatencyValue.text = activity.getString(
      R.string.label_ms, engine.latency.toString()
    )
  }

  private fun measureScrollView() {
    binding.scrollLatency.onGlobalLayout {
      val isScrollable = binding.scrollLatency.canScrollVertically(-1) ||
          binding.scrollLatency.canScrollVertically(1)
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollLatency.setDividerVisibility(
      visible,
      binding.dividerLatencyTop,
      binding.dividerLatencyBottom,
      binding.linearLatencyContainer
    )
  }
}
