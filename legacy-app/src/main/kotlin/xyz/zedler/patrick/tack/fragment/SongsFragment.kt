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

package xyz.zedler.patrick.tack.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.shape.MaterialShapes
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.ScrollBehavior
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.FragmentSongsBinding
import xyz.zedler.patrick.tack.drawable.ShapeDrawable
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListener
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListenerAdapter
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.NotificationUtil
import xyz.zedler.patrick.tack.util.requestSongsWidgetPin
import xyz.zedler.patrick.tack.util.sendSongsWidgetUpdate
import xyz.zedler.patrick.tack.util.sortSongsWithParts
import xyz.zedler.patrick.tack.util.dialog.BackupDialogUtil
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.edit
import xyz.zedler.patrick.tack.util.getDimension
import xyz.zedler.patrick.tack.util.isOrientationPortrait
import xyz.zedler.patrick.tack.util.isTablet
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showMenu

class SongsFragment : BaseFragment() {

  private var _binding: FragmentSongsBinding? = null
  private val binding get() = _binding!!

  private lateinit var dialogUtilWidgetPrompt: DialogUtil
  private lateinit var dialogUtilDelete: DialogUtil
  private lateinit var dialogUtilPermission: DialogUtil
  private lateinit var dialogUtilGain: DialogUtil
  private lateinit var backupDialogUtil: BackupDialogUtil
  private var songsWithParts: MutableList<SongWithParts> = mutableListOf()
  private var sortOrder: Int = 0
  private var songToDelete: Song? = null
  private var partsToDelete: MutableList<Part> = mutableListOf()
  private lateinit var adapter: SongAdapter
  private var metronomeListener: MetronomeListener? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSongsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    dialogUtilWidgetPrompt.dismiss()
    dialogUtilDelete.dismiss()
    backupDialogUtil.dismiss()
    dialogUtilPermission.dismiss()
    dialogUtilGain.dismiss()
    metronomeListener?.let { metronomeEngine?.removeListener(it) }
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val isPortrait = activity.isOrientationPortrait()
    val isTablet = activity.isTablet()

    SystemBarBehavior(activity).apply {
      setAppBar(binding.appBarSongs)
      setContainer(binding.constraintSongs)
      setRecycler(binding.recyclerSongs)
      val bottomInset = activity.getDimension(R.dimen.fab_margin_bottom) +
          activity.dpToPx(if (isPortrait || isTablet) 80f else 56f)
      additionalBottomInset = bottomInset
      setUp()
    }

    val bottomMargin = activity.getDimension(R.dimen.fab_margin_bottom)
    SystemBarBehavior.applyBottomInset(binding.fabSongs, bottomMargin)
    SystemBarBehavior.applyBottomInset(
      binding.songsEmpty.linearSongsEmptyContainer, bottomMargin
    )

    ScrollBehavior().apply {
      if (!isTablet) {
        setOnScrollChangedListener(object : ScrollBehavior.OnScrollChangedListener {
          override fun onScrollUp() = binding.fabSongs.extend()
          override fun onScrollDown() = binding.fabSongs.shrink()
          override fun onTopScroll() = binding.fabSongs.extend()
        })
      }
      setUpScroll(
        binding.appBarSongs,
        binding.recyclerSongs,
        ScrollBehavior.LIFT_ON_SCROLL
      )
    }

    binding.buttonSongsBack.setOnClickListener(getNavigationOnClickListener())
    binding.buttonSongsMenu.setOnClickListener { v ->
      performHapticClick()

      val itemClickListener = PopupMenu.OnMenuItemClickListener { item ->
        val id = item.itemId
        if (getViewUtil().isClickDisabled(id) || metronomeEngine == null) {
          return@OnMenuItemClickListener false
        }
        performHapticClick()
        when (id) {
          R.id.action_sort_name, R.id.action_sort_last_played, R.id.action_sort_most_played -> {
            if (item.isChecked) return@OnMenuItemClickListener false
            sortOrder = when (id) {
              R.id.action_sort_name -> SONGS_ORDER.NAME_ASC
              R.id.action_sort_last_played -> SONGS_ORDER.LAST_PLAYED_ASC
              else -> SONGS_ORDER.MOST_PLAYED_ASC
            }
            item.isChecked = true
            setSongsWithParts(null)
            metronomeEngine?.setSongsOrder(sortOrder)
            if (songsWithParts.isNotEmpty()) {
              sendSongsWidgetUpdate(activity)
            }
          }

          R.id.action_backup -> backupDialogUtil.show()
          R.id.action_settings -> activity.navigate(
            SongsFragmentDirections.actionSongsToSettings()
          )

          R.id.action_feedback -> activity.showFeedbackDialog()
          R.id.action_help -> activity.showHelpDialog()
        }
        true
      }

      v.showMenu(R.menu.menu_songs, itemClickListener) { menu ->
        val engine = metronomeEngine ?: return@showMenu
        sortOrder = engine.getSongsOrder()
        val itemId = when (sortOrder) {
          SONGS_ORDER.LAST_PLAYED_ASC -> R.id.action_sort_last_played
          SONGS_ORDER.MOST_PLAYED_ASC -> R.id.action_sort_most_played
          else -> R.id.action_sort_name
        }
        menu.findItem(itemId)?.isChecked = true
      }
    }

    binding.buttonSongsBack.setTooltipText(R.string.action_back)
    binding.buttonSongsMenu.setTooltipText(R.string.action_more)

    metronomeListener = object : MetronomeListenerAdapter() {
      override fun onMetronomeStart() {
        activity.runOnUiThread { adapter.setPlaying(true) }
      }

      override fun onMetronomeStop() {
        activity.runOnUiThread { adapter.setPlaying(false) }
      }

      override fun onMetronomeSongOrPartChanged(song: SongWithParts?, partIndex: Int) {
        activity.runOnUiThread {
          adapter.setCurrentSongId(song?.song?.id)
        }
      }

      override fun onMetronomePermissionMissing() {
        activity.runOnUiThread { activity.requestNotificationPermission(true) }
      }
    }

    adapter = SongAdapter(object : SongAdapter.OnSongClickListener {
      override fun onSongClick(song: SongWithParts) {
        performHapticClick()
        val action = SongsFragmentDirections.actionSongsToSong()
        action.arguments.putString("songId", song.song.id)
        activity.navigate(action)
      }

      override fun onPlayClick(song: SongWithParts) {
        val engine = metronomeEngine ?: return
        if (engine.getGain() > 0 && engine.neverStartedWithGainBefore()) {
          dialogUtilGain.show()
        } else {
          val permissionDenied = sharedPrefs.getBoolean(
            PREF.PERMISSION_DENIED, false
          )
          engine.setCurrentSong(song.song.id, 0, false) {
            if (NotificationUtil.hasPermission(activity) || permissionDenied) {
              engine.start()
            } else {
              dialogUtilPermission.show()
            }
          }
        }
        performHapticClick()
      }

      override fun onPlayStopClick() {
        val engine = metronomeEngine ?: return
        if (engine.isPlaying()) {
          performHapticClick()
          engine.stop()
        } else {
          if (engine.getGain() > 0 && engine.neverStartedWithGainBefore()) {
            dialogUtilGain.show()
          } else {
            val permissionDenied = sharedPrefs.getBoolean(
              PREF.PERMISSION_DENIED, false
            )
            if (NotificationUtil.hasPermission(activity) || permissionDenied) {
              engine.start()
            } else {
              dialogUtilPermission.show()
            }
          }
          performHapticClick()
        }
      }

      override fun onMoreClick() = performHapticClick()

      override fun onApplyClick(song: SongWithParts) {
        metronomeEngine?.let {
          performHapticClick()
          it.setCurrentSong(song.song.id, 0)
        }
      }

      override fun onDeleteClick(song: SongWithParts) {
        performHapticClick()
        songToDelete = song.song
        partsToDelete = song.parts.toMutableList()
        dialogUtilDelete.show()
      }
    })

    binding.recyclerSongs.adapter = adapter
    binding.recyclerSongs.layoutManager = WrapperLinearLayoutManager(activity)
    binding.recyclerSongs.itemAnimator = DefaultItemAnimator().apply {
      changeDuration = 0
    }

    activity.songViewModel.allSongsWithPartsLive.observe(viewLifecycleOwner) { songs ->
      val list = songs.filter { it.song.id != Constants.SONG_ID_DEFAULT }
      setSongsWithParts(list.toMutableList())
    }

    updateMetronomeControls(true)

    dialogUtilWidgetPrompt = DialogUtil(activity, "widget_prompt")
    dialogUtilWidgetPrompt.createDialog { builder ->
      builder.setTitle(R.string.msg_widget_prompt)
      builder.setMessage(R.string.msg_widget_prompt_description)
      builder.setPositiveButton(R.string.action_apply) { _, _ ->
        performHapticClick()
        requestSongsWidgetPin(activity)
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
    }
    dialogUtilWidgetPrompt.showIfWasShown(savedInstanceState)

    if (savedInstanceState == null) {
      val visitCount = sharedPrefs.getInt(PREF.SONGS_VISIT_COUNT, 0)
      if (visitCount >= 5) {
        sharedPrefs.edit { putInt(PREF.SONGS_VISIT_COUNT, -1) }
        dialogUtilWidgetPrompt.show()
      }
    }

    savedInstanceState?.let {
      if (it.containsKey(KEY_SONG_TO_DELETE)) {
        songToDelete = BundleCompat.getParcelable(it, KEY_SONG_TO_DELETE, Song::class.java)
      }
      BundleCompat.getParcelableArrayList(
        it, KEY_PARTS_TO_DELETE, Part::class.java
      )?.let { parts ->
        partsToDelete = parts.toMutableList()
      }
    }
    dialogUtilDelete = DialogUtil(activity, "delete")
    dialogUtilDelete.createDialogError { builder ->
      builder.setTitle(R.string.msg_delete_song)
      builder.setMessage(R.string.msg_delete_song_description)
      builder.setPositiveButton(R.string.action_delete) { _, _ ->
        performHapticClick()
        val song = songToDelete
        if (song == null) {
          Log.e(TAG, "No song to delete set")
          return@setPositiveButton
        } else if (partsToDelete.isEmpty()) {
          Log.e(TAG, "No parts to delete set")
          return@setPositiveButton
        }
        val engine = metronomeEngine ?: return@setPositiveButton

        if (song.id == engine.currentSongId) {
          engine.setCurrentSong(Constants.SONG_ID_DEFAULT, 0)
        }
        activity.songViewModel.deleteSong(song) {
          metronomeEngine?.let {
            activity.songViewModel.deleteParts(partsToDelete)
            it.updateShortcuts()
            sendSongsWidgetUpdate(activity)
          }
        }
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
    }
    dialogUtilDelete.showIfWasShown(savedInstanceState)

    backupDialogUtil = BackupDialogUtil(activity, this)
    backupDialogUtil.showIfWasShown(savedInstanceState)

    dialogUtilPermission = DialogUtil(activity, "notification_permission")
    dialogUtilPermission.createDialog { builder ->
      builder.setTitle(R.string.msg_notification_permission)
      builder.setMessage(R.string.msg_notification_permission_description)
      builder.setPositiveButton(R.string.action_next) { _, _ ->
        metronomeEngine?.let {
          performHapticClick()
          it.start()
        }
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
    }
    dialogUtilPermission.showIfWasShown(savedInstanceState)

    dialogUtilGain = DialogUtil(activity, "gain")
    dialogUtilGain.createDialogError { builder ->
      builder.setTitle(R.string.msg_gain)
      builder.setMessage(R.string.msg_gain_description)
      builder.setPositiveButton(R.string.action_play) { _, _ ->
        metronomeEngine?.let {
          performHapticClick()
          it.start()
        }
      }
      builder.setNegativeButton(R.string.action_deactivate) { _, _ ->
        metronomeEngine?.let {
          performHapticClick()
          it.setGain(0)
          it.start()
        }
      }
    }
    dialogUtilGain.showIfWasShown(savedInstanceState)

    binding.fabSongs.setOnClickListener {
      performHapticClick()
      if (activity.isUnlocked() || songsWithParts.size < 3) {
        activity.navigate(SongsFragmentDirections.actionSongsToSong())
      } else {
        activity.showUnlockDialog()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    dialogUtilWidgetPrompt.saveState(outState)
    dialogUtilDelete.saveState(outState)
    backupDialogUtil.saveState(outState)
    dialogUtilPermission.saveState(outState)
    dialogUtilGain.saveState(outState)

    outState.putParcelable(KEY_SONG_TO_DELETE, songToDelete)
    outState.putParcelableArrayList(KEY_PARTS_TO_DELETE, ArrayList(partsToDelete))
  }

  override fun updateMetronomeControls(init: Boolean) {
    val engine = metronomeEngine ?: return
    metronomeListener?.let { engine.addListener(it) }

    adapter.setCurrentSongId(engine.currentSongId)
    adapter.setPlaying(engine.isPlaying())
  }

  @SuppressLint("RestrictedApi")
  private fun setSongsWithParts(songsWithParts: MutableList<SongWithParts>?) {
    if (songsWithParts != null) {
      this.songsWithParts = songsWithParts
      binding.songsEmpty.root.visibility = if (songsWithParts.isEmpty()) View.VISIBLE else View.GONE
      if (songsWithParts.isEmpty()) {
        binding.songsEmpty.imageSongsEmpty.setImageDrawable(
          ShapeDrawable(
            activity,
            MaterialShapes.COOKIE_7,
            R.drawable.illustration_songs_empty
          )
        )
      }
    }
    sortSongsWithParts(this.songsWithParts, sortOrder)
    adapter.setSongsWithParts(ArrayList(this.songsWithParts))
    adapter.setSortOrder(sortOrder)
  }

  companion object {
    private val TAG = SongsFragment::class.java.simpleName
    private const val KEY_SONG_TO_DELETE = "song_to_delete"
    private const val KEY_PARTS_TO_DELETE = "parts_to_delete"
  }
}
