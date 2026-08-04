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
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogGainBinding
import xyz.zedler.patrick.tack.fragment.SettingsFragment
import xyz.zedler.patrick.tack.util.*

class GainDialogUtil(
  private val activity: MainActivity,
  private val fragment: SettingsFragment
) {

  private val binding = PartialDialogGainBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "gain")

  init {
    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.settings_gain)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
    }

    binding.sliderGain.addOnChangeListener { slider, value, fromUser ->
      if (fromUser) {
        val engine = activity.metronomeEngine
        if (engine != null) {
          engine.setGain(value.toInt())
          activity.performHapticSegmentTick(slider, false)
          updateValueDisplay()
          fragment.updateGainDescription(value.toInt())
        }
      }
    }

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

    activity.metronomeEngine?.let { engine ->
      updateValueDisplay()
      binding.sliderGain.value = engine.getGain().toFloat()
    }
  }

  private fun updateValueDisplay() {
    val engine = activity.metronomeEngine ?: return
    val gain = engine.getGain()
    binding.textGainValue.text = activity.getString(
      R.string.label_db_signed,
      if (gain > 0) "+$gain" else gain.toString()
    )
  }

  private fun measureScrollView() {
    binding.scrollGain.onGlobalLayout {
      val isScrollable = binding.scrollGain.canScrollVertically(-1) ||
          binding.scrollGain.canScrollVertically(1)
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollGain.setDividerVisibility(
      visible,
      binding.dividerGainTop,
      binding.dividerGainBottom,
      binding.linearGainContainer
    )
  }
}
