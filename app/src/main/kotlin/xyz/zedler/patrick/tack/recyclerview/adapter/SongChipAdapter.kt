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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.RowSongChipBinding

class SongChipAdapter(
  private val listener: OnSongClickListener,
  private var clickable: Boolean
) : ListAdapter<SongWithParts, SongChipAdapter.SongChipViewHolder>(
  SongWithPartsDiffCallback()
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongChipViewHolder {
    val binding = RowSongChipBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return SongChipViewHolder(binding)
  }

  override fun onBindViewHolder(holder: SongChipViewHolder, position: Int) {
    val songWithParts = getItem(position)
    holder.binding.textSong.text = songWithParts.song.name
    holder.binding.cardSong.isClickable = clickable
    if (clickable) {
      holder.binding.frameSong.setOnClickListener {
        listener.onSongClick(songWithParts)
      }
      holder.binding.cardSong.setOnClickListener {
        holder.binding.frameSong.callOnClick()
      }
      holder.binding.frameSong.setOnLongClickListener {
        listener.onSongLongClick(songWithParts)
        true
      }
      holder.binding.cardSong.setOnLongClickListener {
        listener.onSongLongClick(songWithParts)
        true
      }
    } else {
      holder.binding.frameSong.setOnClickListener(null)
      holder.binding.frameSong.setOnLongClickListener(null)
      holder.binding.cardSong.setOnClickListener(null)
      holder.binding.cardSong.setOnLongClickListener(null)
    }
  }

  fun setClickable(clickable: Boolean) {
    if (this.clickable != clickable) {
      this.clickable = clickable
      notifyItemRangeChanged(0, itemCount)
    }
  }

  class SongChipViewHolder(val binding: RowSongChipBinding)
    : RecyclerView.ViewHolder(binding.root)

  interface OnSongClickListener {
    fun onSongClick(song: SongWithParts)
    fun onSongLongClick(song: SongWithParts)
  }

  private class SongWithPartsDiffCallback : DiffUtil.ItemCallback<SongWithParts>() {

    override fun areItemsTheSame(
      oldItem: SongWithParts,
      newItem: SongWithParts
    ): Boolean {
      return oldItem.song.id == newItem.song.id
    }

    override fun areContentsTheSame(
      oldItem: SongWithParts,
      newItem: SongWithParts
    ): Boolean {
      return oldItem == newItem
    }
  }
}
