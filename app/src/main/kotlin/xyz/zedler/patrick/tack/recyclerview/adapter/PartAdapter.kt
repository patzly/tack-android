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

package xyz.zedler.patrick.tack.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.databinding.RowPartBinding
import xyz.zedler.patrick.tack.util.setBackgroundSegmented
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showMenu

class PartAdapter(
  private val listener: OnPartItemClickListener
) : RecyclerView.Adapter<PartAdapter.PartViewHolder>() {

  private val parts = mutableListOf<Part>()

  enum class Payload {
    ROLE
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
    val binding = RowPartBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return PartViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
    val part = getItem(position)
    val context = holder.binding.root.context
    val binding = holder.binding

    binding.linearPartContainer.setBackgroundSegmented(position, itemCount)
    binding.buttonPartMoveUp.isEnabled = itemCount > 1 && position > 0
    binding.buttonPartMoveDown.isEnabled = itemCount > 1 && position < itemCount - 1

    // number
    binding.textPartNumber.text = context.getString(
      R.string.label_part_unnamed, position + 1
    )

    binding.buttonPartEdit.setOnClickListener { listener.onEditClick(part) }
    binding.buttonPartMoveUp.setOnClickListener { listener.onMoveUpClick(part) }
    binding.buttonPartMoveDown.setOnClickListener { listener.onMoveDownClick(part) }
    binding.buttonPartMenu.setOnClickListener { view ->
      listener.onMoreClick(part)
      view.showMenu(R.menu.menu_part, { item ->
        when (item.itemId) {
          R.id.action_rename -> listener.onRenameClick(part)
          R.id.action_duplicate -> listener.onDuplicateClick(part)
          R.id.action_delete -> listener.onDeleteClick(part)
        }
        true
      }, { menu ->
        menu.findItem(R.id.action_delete).isEnabled = itemCount > 1
      })
    }

    binding.buttonPartEdit.setTooltipText(R.string.action_edit)
    binding.buttonPartMoveUp.setTooltipText(R.string.action_move_up)
    binding.buttonPartMoveDown.setTooltipText(R.string.action_move_down)
    binding.buttonPartMenu.setTooltipText(R.string.action_more)

    // name
    val partName = part.name
    binding.textPartName.text = partName
    binding.linearPartName.visibility = if (partName != null) View.VISIBLE else View.GONE

    // tempo
    binding.textPartTempo.text = context.getString(R.string.label_bpm_value, part.tempo)

    // beats
    val beats = (part.beats ?: DEF.BEATS).split(",").toTypedArray()
    val beatsMaybeMuted = beats.clone()
    val subs = (part.subdivisions ?: DEF.SUBDIVISIONS).split(",").toTypedArray()
    if (subs.isNotEmpty() && subs[0] == TICK_TYPE.BEAT_SUB_MUTED) {
      beatsMaybeMuted.fill(TICK_TYPE.MUTED)
    }
    binding.beatsPartBeats.setBeats(beatsMaybeMuted)

    // subdivisions
    val subdivisions = (part.subdivisions ?: DEF.SUBDIVISIONS).split(",").toTypedArray()
    binding.beatsPartSubdivisions.setBeats(subdivisions)
    val showSubdivisions = subdivisions.size > 1 ||
        (subdivisions.size == 1 && subdivisions[0] == TICK_TYPE.BEAT_SUB_MUTED)
    binding.linearPartSubdivisions.visibility = if (showSubdivisions) View.VISIBLE else View.GONE
    binding.textPartSubdivisionsPolyrhythm.visibility =
      if (part.usePolyrhythm) View.VISIBLE else View.GONE

    // count in
    val isCountInActive = part.countIn > 0
    if (isCountInActive) {
      binding.textPartCountIn.text = context.resources.getQuantityString(
        R.plurals.options_count_in_description, part.countIn, part.countIn
      )
    }
    binding.linearPartCountIn.visibility = if (isCountInActive) View.VISIBLE else View.GONE

    // duration
    val timerDuration = part.timerDuration
    if (timerDuration > 0) {
      val durationResId = when (part.timerUnit) {
        UNIT.SECONDS -> R.plurals.options_timer_description_seconds
        UNIT.MINUTES -> R.plurals.options_timer_description_minutes
        else -> R.plurals.options_timer_description_bars
      }
      binding.textPartDuration.text = context.resources.getQuantityString(
        durationResId, timerDuration, timerDuration
      )
    }
    binding.linearPartDuration.visibility = if (timerDuration > 0) View.VISIBLE else View.GONE

    // incremental
    val isIncrementalActive = part.incrementalAmount > 0
    if (isIncrementalActive) {
      binding.textPartIncrementalAmount.text = context.getString(
        if (part.incrementalIncrease) {
          R.string.options_incremental_amount_increase
        } else {
          R.string.options_incremental_amount_decrease
        },
        part.incrementalAmount
      )

      val intervalResId = when (part.incrementalUnit) {
        UNIT.SECONDS -> R.plurals.options_incremental_interval_seconds
        UNIT.MINUTES -> R.plurals.options_incremental_interval_minutes
        else -> R.plurals.options_incremental_interval_bars
      }
      binding.textPartIncrementalInterval.text = context.resources.getQuantityString(
        intervalResId, part.incrementalInterval, part.incrementalInterval
      )

      if (part.incrementalLimit > 0) {
        binding.textPartIncrementalLimit.text = context.getString(
          if (part.incrementalIncrease) {
            R.string.options_incremental_max
          } else {
            R.string.options_incremental_min
          },
          part.incrementalLimit
        )
      } else {
        binding.textPartIncrementalLimit.text = context.getString(
          if (part.incrementalIncrease) {
            R.string.options_incremental_no_max
          } else {
            R.string.options_incremental_no_min
          }
        )
      }
    }
    binding.linearPartIncremental.visibility = if (isIncrementalActive) View.VISIBLE else View.GONE

    // muted
    val isMuteActive = if (part.muteUnit == UNIT.BEATS) {
      part.muteMute > 0
    } else {
      part.mutePlay > 0
    }
    if (isMuteActive) {
      val playVisibility = if (part.muteUnit == UNIT.BEATS) View.GONE else View.VISIBLE
      binding.textPartMutePlay.visibility = playVisibility
      binding.imagePartMuteMute.visibility = playVisibility
      if (part.muteUnit == UNIT.BEATS) {
        binding.textPartMuteMute.text = context.getString(
          R.string.options_mute_mute_beats, part.muteMute
        )
      } else {
        val resIdPlay = if (part.muteUnit == UNIT.SECONDS) {
          R.plurals.options_mute_play_seconds
        } else {
          R.plurals.options_mute_play_bars
        }
        val resIdMute = if (part.muteUnit == UNIT.SECONDS) {
          R.plurals.options_mute_mute_seconds
        } else {
          R.plurals.options_mute_mute_bars
        }
        binding.textPartMutePlay.text = context.resources.getQuantityString(
          resIdPlay, part.mutePlay, part.mutePlay
        )
        binding.textPartMuteMute.text = context.resources.getQuantityString(
          resIdMute, part.muteMute, part.muteMute
        )
      }
      val muteRandom = part.muteUnit != UNIT.BEATS && part.muteRandom
      binding.imagePartMuteRandom.visibility = if (muteRandom) View.VISIBLE else View.GONE
      binding.textPartMuteRandom.visibility = if (muteRandom) View.VISIBLE else View.GONE
    }
    binding.linearPartMute.visibility = if (isMuteActive) View.VISIBLE else View.GONE
  }

  override fun onBindViewHolder(
    holder: PartViewHolder,
    position: Int,
    payloads: List<Any>
  ) {
    if (payloads.contains(Payload.ROLE)) {
      holder.binding.linearPartContainer.setBackgroundSegmented(position, itemCount)
      holder.binding.buttonPartMoveUp.isEnabled = itemCount > 1 && position > 0
      holder.binding.buttonPartMoveDown.isEnabled = itemCount > 1 && position < itemCount - 1
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun getItemCount(): Int = parts.size

  fun getItem(position: Int): Part = parts[position]

  fun setParts(newParts: List<Part>) {
    val oldParts = ArrayList(parts)
    val newPartsCopy = ArrayList(newParts)
    val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
      override fun getOldListSize(): Int = oldParts.size
      override fun getNewListSize(): Int = newPartsCopy.size

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldParts[oldItemPosition].id == newPartsCopy[newItemPosition].id
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldPart = oldParts[oldItemPosition]
        val newPart = newPartsCopy[newItemPosition]
        if (oldPart != newPart) return false

        val oldRole = getItemRole(oldItemPosition, oldParts.size)
        val newRole = getItemRole(newItemPosition, newPartsCopy.size)
        return oldRole == newRole
      }

      override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldPart = oldParts[oldItemPosition]
        val newPart = newPartsCopy[newItemPosition]
        val oldRole = getItemRole(oldItemPosition, oldParts.size)
        val newRole = getItemRole(newItemPosition, newPartsCopy.size)
        return if (oldPart == newPart && oldRole != newRole) Payload.ROLE else null
      }

      private fun getItemRole(position: Int, size: Int): Int {
        return when {
          size == 1 -> -1
          position == 0 -> 0
          position == size - 1 -> 2
          else -> 1
        }
      }
    })
    parts.clear()
    parts.addAll(newPartsCopy)
    diffResult.dispatchUpdatesTo(this)
  }

  class PartViewHolder(val binding: RowPartBinding)
    : RecyclerView.ViewHolder(binding.root)

  interface OnPartItemClickListener {
    fun onEditClick(part: Part)
    fun onMoveUpClick(part: Part)
    fun onMoveDownClick(part: Part)
    fun onMoreClick(part: Part)
    fun onRenameClick(part: Part)
    fun onDuplicateClick(part: Part)
    fun onDeleteClick(part: Part)
  }
}
