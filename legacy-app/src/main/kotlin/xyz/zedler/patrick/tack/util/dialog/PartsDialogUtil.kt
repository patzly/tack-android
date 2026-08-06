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
import androidx.core.view.isVisible
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogPartsTitleBinding
import xyz.zedler.patrick.tack.databinding.PartialDialogRecyclerBinding
import xyz.zedler.patrick.tack.recyclerview.adapter.PartDialogAdapter
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.onGlobalLayout

class PartsDialogUtil(private val activity: MainActivity) {

  private val titleBinding = PartialDialogPartsTitleBinding.inflate(activity.layoutInflater)
  private val binding = PartialDialogRecyclerBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "parts")
  private val adapter: PartDialogAdapter

  init {
    binding.recyclerDialog.layoutManager = WrapperLinearLayoutManager(activity)
    adapter = PartDialogAdapter { partIndex, fromUser ->
      if (fromUser) {
        activity.metronomeEngine?.let { engine ->
          activity.performHapticClick()
          engine.setCurrentPartIndex(partIndex)
        }
      }
    }
    binding.recyclerDialog.adapter = adapter

    dialogUtil.createDialog { builder ->
      builder.setCustomTitle(titleBinding.root)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
    }
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

  fun update() {
    val metronomeEngine = activity.metronomeEngine ?: return
    val songWithParts = metronomeEngine.currentSongWithParts
    if (songWithParts != null) {
      titleBinding.textDialogPartsTitle.text = songWithParts.song.name
      // part count
      val partCount = songWithParts.parts.size
      titleBinding.textDialogPartsCount.text = activity.resources.getQuantityString(
        R.plurals.label_parts_count, partCount, partCount
      )
      // song duration
      val hasDuration = songWithParts.parts.none { it.timerDuration == 0 }
      titleBinding.textDialogPartsDuration.text = if (hasDuration) {
        songWithParts.getDurationString()
      } else {
        activity.getString(R.string.label_part_no_duration)
      }
      // looped
      titleBinding.textDialogPartsLooped.text = activity.getString(
        if (songWithParts.song.isLooped) R.string.label_song_looped else R.string.label_song_not_looped
      )
      // speed
      val speed = songWithParts.song.speed
      titleBinding.textDialogPartsSpeed.text = if (speed == 100) {
        activity.getString(R.string.label_song_speed_original)
      } else {
        activity.getString(R.string.label_song_speed_short, speed)
      }
    } else {
      // Don't show dialog if no song is selected
      dismiss()
    }

    adapter.setSongWithParts(songWithParts)
    adapter.setPartIndex(metronomeEngine.getCurrentPartIndex())
    maybeShowDividers()
  }

  private fun maybeShowDividers() {
    binding.recyclerDialog.onGlobalLayout {
      val isScrollable = binding.recyclerDialog.canScrollVertically(-1) ||
          binding.recyclerDialog.canScrollVertically(1)
      binding.dividerDialogTop.isVisible = isScrollable
      binding.dividerDialogBottom.isVisible = isScrollable
    }
  }
}
