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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.RowDialogRadioBinding

class PartDialogAdapter(
  private val listener: OnPartChangedListener
) : RecyclerView.Adapter<PartDialogAdapter.PartDialogViewHolder>() {

  private var songWithParts: SongWithParts? = null
  private var partIndex = 0
  private var partIndexPrev = 0

  companion object {
    private const val PAYLOAD_RADIO = "radio"
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartDialogViewHolder {
    val binding = RowDialogRadioBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return PartDialogViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PartDialogViewHolder, position: Int) {
    val adapterPosition = holder.bindingAdapterPosition
    val context = holder.binding.root.context

    val part = songWithParts?.parts?.get(adapterPosition) ?: return
    val name = part.name ?: context.getString(R.string.label_part_unnamed, adapterPosition + 1)
    holder.binding.textRowDialogName.text = name
    holder.binding.textRowDialogDescription.text = part.getTimerDurationString(context)

    holder.binding.radioRowDialog.isChecked = adapterPosition == partIndex
    holder.binding.radioRowDialog.jumpDrawablesToCurrentState()

    holder.binding.linearRowDialog.setOnClickListener {
      setPartIndex(adapterPosition, true)
    }
  }

  override fun onBindViewHolder(
    holder: PartDialogViewHolder,
    position: Int,
    payloads: List<Any>
  ) {
    if (payloads.contains(PAYLOAD_RADIO)) {
      val adapterPosition = holder.bindingAdapterPosition

      holder.binding.radioRowDialog.isChecked = adapterPosition == partIndexPrev
      holder.binding.radioRowDialog.jumpDrawablesToCurrentState()
      holder.binding.radioRowDialog.post {
        holder.binding.radioRowDialog.isChecked = adapterPosition == partIndex
      }
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun getItemCount(): Int = songWithParts?.parts?.size ?: 0

  fun setSongWithParts(songWithParts: SongWithParts?) {
    if (songWithParts == null && this.songWithParts == null) {
      return
    } else if (songWithParts != null) {
      val idNew = songWithParts.song.id
      this.songWithParts?.let {
        val idOld = it.song.id
        if (idNew == idOld && songWithParts.parts.size == it.parts.size) {
          return
        }
      }
    }
    this.songWithParts = songWithParts
    partIndex = 0
    partIndexPrev = 0
    notifyItemRangeChanged(0, itemCount)
  }

  fun setPartIndex(partIndex: Int, fromUser: Boolean = false) {
    if (partIndex == this.partIndex) {
      return
    }
    partIndexPrev = this.partIndex
    this.partIndex = partIndex
    if (fromUser) {
      notifyItemChanged(partIndexPrev, PAYLOAD_RADIO)
      notifyItemChanged(partIndex, PAYLOAD_RADIO)
    } else {
      notifyItemChanged(partIndexPrev)
      notifyItemChanged(partIndex)
    }
    listener.onPartChanged(partIndex, fromUser)
  }

  class PartDialogViewHolder(val binding: RowDialogRadioBinding) :
    RecyclerView.ViewHolder(binding.root)

  fun interface OnPartChangedListener {
    fun onPartChanged(partIndex: Int, fromUser: Boolean)
  }
}
