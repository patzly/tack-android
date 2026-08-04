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

import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.RowSongBinding
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.getLocale
import xyz.zedler.patrick.tack.util.setBackgroundSegmented
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showMenu
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.UUID

class SongAdapter(
  private val listener: OnSongClickListener
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

  private val songsWithParts = mutableListOf<SongWithParts>()
  private var sortOrder = 0
  private var currentSongId: String? = null
  private var isPlaying = false

  init {
    setHasStableIds(true)
  }

  enum class Payload {
    PLAY, SELECTION
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
    val binding = RowSongBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return SongViewHolder(binding)
  }

  override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
    val songWithParts = getItem(position)
    val context = holder.binding.root.context
    val binding = holder.binding

    val isSelected = songWithParts.song.id == currentSongId

    binding.linearSongContainer.setOnClickListener { listener.onSongClick(songWithParts) }
    binding.linearSongContainer.setBackgroundSegmented(position, itemCount, isSelected)

    binding.textSongName.text = songWithParts.song.name
    binding.textSongName.setTextColor(
      context.getAttrColor(
        if (isSelected) R.attr.colorOnSecondaryContainer else R.attr.colorOnSurface
      )
    )

    val colorFgSecondary = context.getAttrColor(
      if (isSelected) R.attr.colorOnSecondaryContainer else R.attr.colorOnSurfaceVariant
    )
    binding.textSongPartCount.setTextColor(colorFgSecondary)
    binding.imageSongDivider1.setColorFilter(colorFgSecondary)
    binding.textSongDuration.setTextColor(colorFgSecondary)
    binding.imageSongDivider2.setColorFilter(colorFgSecondary)
    binding.textSongLooped.setTextColor(colorFgSecondary)
    binding.textSongSortDetails.setTextColor(colorFgSecondary)

    // part count
    val partCount = songWithParts.parts.size
    binding.textSongPartCount.text = context.resources.getQuantityString(
      R.plurals.label_parts_count, partCount, partCount
    )

    // song duration
    val hasDuration = songWithParts.parts.all { it.timerDuration > 0 }
    binding.textSongDuration.text = if (hasDuration) {
      songWithParts.getDurationString()
    } else {
      context.getString(R.string.label_part_no_duration)
    }

    // looped
    binding.textSongLooped.text = context.getString(
      if (songWithParts.song.isLooped) R.string.label_song_looped else R.string.label_song_not_looped
    )

    // last/most played
    val sortDetailsEnabled = sortOrder == SONGS_ORDER.LAST_PLAYED_ASC ||
        sortOrder == SONGS_ORDER.MOST_PLAYED_ASC
    binding.textSongSortDetails.visibility = if (sortDetailsEnabled) View.VISIBLE else View.GONE
    if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      val lastPlayed = songWithParts.song.lastPlayed
      if (lastPlayed != 0L) {
        val locale = getLocale()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(locale)
          val dateTime = Instant.ofEpochMilli(lastPlayed)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
          binding.textSongSortDetails.text = context.getString(
            R.string.label_sort_last_played_date, dateTime.format(formatter)
          )
        } else {
          val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT, locale
          )
          binding.textSongSortDetails.text = context.getString(
            R.string.label_sort_last_played_date,
            dateFormat.format(Date(lastPlayed))
          )
        }
      } else {
        binding.textSongSortDetails.setText(R.string.label_sort_never_played)
      }
    } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      val playCount = songWithParts.song.playCount
      if (playCount > 0) {
        binding.textSongSortDetails.text = context.resources.getQuantityString(
          R.plurals.label_sort_most_played_times, playCount, playCount
        )
      } else {
        binding.textSongSortDetails.setText(R.string.label_sort_never_played)
      }
    }

    val isPlayingSong = isPlaying && isSelected
    binding.buttonSongPlay.visibility = if (isSelected) View.VISIBLE else View.GONE
    binding.buttonSongPlay.isChecked = isPlayingSong
    binding.buttonSongPlay.setIconResource(
      if (isPlayingSong) R.drawable.ic_rounded_stop else R.drawable.ic_rounded_play_arrow
    )
    binding.buttonSongPlay.setBackgroundColor(
      context.getAttrColor(
        if (isSelected) R.attr.colorSecondary else R.attr.colorSurfaceBright
      )
    )
    binding.buttonSongPlay.iconTint = ColorStateList.valueOf(
      context.getAttrColor(
        if (isSelected) R.attr.colorOnSecondary else R.attr.colorOnSurfaceVariant
      )
    )
    binding.buttonSongPlay.setOnClickListener { listener.onPlayStopClick() }

    binding.buttonSongMenu.iconTint = ColorStateList.valueOf(
      context.getAttrColor(
        if (isSelected) R.attr.colorOnSecondaryContainer else R.attr.colorOnSurface
      )
    )
    binding.buttonSongMenu.setOnClickListener { view ->
      listener.onMoreClick()
      view.showMenu(R.menu.menu_song_list, { item ->
        when (item.itemId) {
          R.id.action_play -> listener.onPlayClick(songWithParts)
          R.id.action_apply -> listener.onApplyClick(songWithParts)
          R.id.action_delete -> listener.onDeleteClick(songWithParts)
        }
        true
      }, { menu ->
        menu.findItem(R.id.action_play).isVisible = !isSelected
        menu.findItem(R.id.action_apply).isVisible = !isSelected
      })
    }

    binding.buttonSongPlay.setTooltipText(R.string.action_play)
    binding.buttonSongMenu.setTooltipText(R.string.action_more)
  }

  override fun onBindViewHolder(
    holder: SongViewHolder,
    position: Int,
    payloads: List<Any>
  ) {
    if (payloads.contains(Payload.PLAY)) {
      val isSelected = getItem(position).song.id == currentSongId
      val isPlayingSong = isPlaying && isSelected
      holder.binding.buttonSongPlay.isChecked = isPlayingSong
      holder.binding.buttonSongPlay.setIconResource(
        if (isPlayingSong) R.drawable.ic_rounded_stop else R.drawable.ic_rounded_play_arrow
      )
    } else if (payloads.contains(Payload.SELECTION)) {
      onBindViewHolder(holder, position) // Selection affects many things, full rebind is easier
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun getItemCount(): Int = songsWithParts.size

  fun getItem(position: Int): SongWithParts = songsWithParts[position]

  override fun getItemId(position: Int): Long {
    val songId = getItem(position).song.id
    val uuid = UUID.fromString(songId)
    return uuid.mostSignificantBits xor uuid.leastSignificantBits
  }

  fun setSortOrder(sortOrder: Int) {
    if (this.sortOrder != sortOrder) {
      this.sortOrder = sortOrder
      notifyItemRangeChanged(0, itemCount)
    }
  }

  fun setCurrentSongId(currentSongId: String?) {
    val oldSongId = this.currentSongId
    this.currentSongId = currentSongId
    for (i in 0 until itemCount) {
      val id = getItem(i).song.id
      if (id == oldSongId || id == currentSongId) {
        notifyItemChanged(i, Payload.SELECTION)
      }
    }
  }

  fun setPlaying(isPlaying: Boolean) {
    this.isPlaying = isPlaying
    for (i in 0 until itemCount) {
      if (getItem(i).song.id == currentSongId) {
        notifyItemChanged(i, Payload.PLAY)
        break
      }
    }
  }

  fun setSongsWithParts(newSongsWithParts: List<SongWithParts>) {
    val oldSongsWithParts = ArrayList(songsWithParts)
    val newSongsWithPartsCopy = ArrayList(newSongsWithParts)
    val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
      override fun getOldListSize(): Int = oldSongsWithParts.size
      override fun getNewListSize(): Int = newSongsWithPartsCopy.size

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldSongsWithParts[oldItemPosition].song.id ==
            newSongsWithPartsCopy[newItemPosition].song.id
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldSongWithParts = oldSongsWithParts[oldItemPosition]
        val newSongWithParts = newSongsWithPartsCopy[newItemPosition]
        if (oldSongWithParts != newSongWithParts) return false

        val oldRole = getItemRole(oldItemPosition, oldSongsWithParts.size)
        val newRole = getItemRole(newItemPosition, newSongsWithPartsCopy.size)
        return oldRole == newRole
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
    songsWithParts.clear()
    songsWithParts.addAll(newSongsWithPartsCopy)
    diffResult.dispatchUpdatesTo(this)
  }

  class SongViewHolder(val binding: RowSongBinding)
    : RecyclerView.ViewHolder(binding.root)

  interface OnSongClickListener {
    fun onSongClick(song: SongWithParts)
    fun onPlayClick(song: SongWithParts)
    fun onPlayStopClick()
    fun onMoreClick()
    fun onApplyClick(song: SongWithParts)
    fun onDeleteClick(song: SongWithParts)
  }
}
