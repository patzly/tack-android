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
import xyz.zedler.patrick.tack.databinding.PartialDialogSongOptionsBinding
import xyz.zedler.patrick.tack.fragment.SongFragment
import xyz.zedler.patrick.tack.util.*

class SongOptionsDialogUtil(
  private val activity: MainActivity,
  private val fragment: SongFragment
) {

  private val binding = PartialDialogSongOptionsBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "song_options")
  private var looped = false
  private var speed = 100

  init {
    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.label_song_options_dialog)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_apply) { _, _ ->
        activity.performHapticClick()
        apply()
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ ->
        activity.performHapticClick()
      }
    }

    binding.linearSongOptionsLooped.setOnClickListener {
      binding.switchSongOptionsLooped.toggle()
    }

    binding.switchSongOptionsLooped.setOnCheckedChangeListener { _, isChecked ->
      activity.performHapticClick()
      looped = isChecked
    }

    binding.sliderSongOptionsSpeed.addOnChangeListener { slider, value, fromUser ->
      if (fromUser) {
        activity.performHapticSegmentTick(slider, true)
        speed = value.toInt()
        updateSpeedDisplay()
      }
    }

    updateDividerVisibility(activity.isOrientationPortrait().not())
  }

  fun show() {
    update()
    dialogUtil.show()
  }

  fun showIfWasShown(state: Bundle?) {
    state?.let {
      looped = it.getBoolean(LOOPED)
      speed = it.getInt(SPEED, 100)
    }
    update()
    dialogUtil.showIfWasShown(state)
  }

  fun dismiss() {
    dialogUtil.dismiss()
  }

  fun saveState(outState: Bundle) {
    dialogUtil.saveState(outState)
    outState.putBoolean(LOOPED, looped)
    outState.putInt(SPEED, speed)
  }

  fun update() {
    binding.switchSongOptionsLooped.isChecked = looped
    binding.switchSongOptionsLooped.jumpDrawablesToCurrentState()

    binding.sliderSongOptionsSpeed.configureSafely(
      5, 100, 5, speed
    )
    updateSpeedDisplay()

    measureScrollView()
  }

  fun setSongOptions(looped: Boolean, speed: Int) {
    this.looped = looped
    this.speed = speed
    update()
  }

  private fun apply() {
    fragment.setSongOptions(looped, speed)
  }

  private fun updateSpeedDisplay() {
    binding.textSongOptionsSpeed.text = if (speed == 100) {
      activity.getString(R.string.label_song_speed_description_original)
    } else {
      activity.getString(R.string.label_song_speed_description, speed)
    }
  }

  private fun measureScrollView() {
    binding.scrollSongOptions.onGlobalLayout {
      val isScrollable = binding.scrollSongOptions.canScrollVertically(-1) ||
          binding.scrollSongOptions.canScrollVertically(1)
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollSongOptions.setDividerVisibility(
      visible,
      binding.dividerSongOptionsTop,
      binding.dividerSongOptionsBottom,
      binding.linearSongOptionsContainer
    )
  }

  companion object {
    private const val LOOPED = "looped_dialog"
    private const val SPEED = "speed_dialog"
  }
}
