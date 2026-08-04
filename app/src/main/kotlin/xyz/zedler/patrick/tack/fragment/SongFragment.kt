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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.os.BundleCompat
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.math.MathUtils
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.ScrollBehavior
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.databinding.FragmentSongBinding
import xyz.zedler.patrick.tack.model.MetronomeConfig
import xyz.zedler.patrick.tack.recyclerview.adapter.PartAdapter
import xyz.zedler.patrick.tack.recyclerview.decoration.PartItemDecoration
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.OptionsUtil
import xyz.zedler.patrick.tack.util.sendSongsWidgetUpdate
import xyz.zedler.patrick.tack.util.sortPartsByIndex
import xyz.zedler.patrick.tack.util.dialog.RenameDialogUtil
import xyz.zedler.patrick.tack.util.dialog.SongOptionsDialogUtil
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.getDimension
import xyz.zedler.patrick.tack.util.hideKeyboard
import xyz.zedler.patrick.tack.util.isLandTablet
import xyz.zedler.patrick.tack.util.isOrientationPortrait
import xyz.zedler.patrick.tack.util.isTablet
import xyz.zedler.patrick.tack.util.onGlobalLayout
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showKeyboard
import xyz.zedler.patrick.tack.util.showMenu
import java.util.LinkedList

class SongFragment : BaseFragment(), View.OnClickListener {

  private var _binding: FragmentSongBinding? = null
  private val binding get() = _binding!!

  private val args: SongFragmentArgs by navArgs()

  private lateinit var dialogUtilDiscard: DialogUtil
  private lateinit var dialogUtilDelete: DialogUtil
  private lateinit var songOptionsDialogUtil: SongOptionsDialogUtil
  private lateinit var renameDialogUtil: RenameDialogUtil
  private lateinit var optionsUtil: OptionsUtil
  private var onBackPressedCallback: OnBackPressedCallback? = null
  private lateinit var adapter: PartAdapter
  private var songSource: Song? = null
  private var songResult: Song = Song()
  private var songsExisting: List<Song> = LinkedList()
  private var partsSource: List<Part> = mutableListOf()
  private var partsResult: MutableList<Part> = LinkedList()
  private var isNewSong: Boolean = false
  private var hasUnsavedChanges: Boolean = true
  private var fabBaseY: Float = 0f

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSongBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    onBackPressedCallback?.remove()
    dialogUtilDiscard.dismiss()
    dialogUtilDelete.dismiss()
    songOptionsDialogUtil.dismiss()
    renameDialogUtil.dismiss()
    optionsUtil.dismiss()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val isPortrait = activity.isOrientationPortrait()
    val isTablet = activity.isTablet()

    val systemBarBehavior = SystemBarBehavior(activity).apply {
      setAppBar(binding.appBarSong)
      setContainer(binding.constraintSongContainer)
      setRecycler(binding.recyclerSongParts)
      val bottomInset = activity.getDimension(R.dimen.fab_margin_bottom) +
          activity.dpToPx(if (isPortrait || isTablet) 80f else 56f)
      additionalBottomInset = bottomInset
      setMultiColumnLayout(!isPortrait)
      setUp()
    }

    SystemBarBehavior.applyBottomInset(
      binding.fabSong, activity.getDimension(R.dimen.fab_margin_bottom)
    )

    ScrollBehavior().apply {
      if (!isTablet) {
        setOnScrollChangedListener(object : ScrollBehavior.OnScrollChangedListener {
          override fun onScrollUp() = binding.fabSong.extend()
          override fun onScrollDown() = binding.fabSong.shrink()
          override fun onTopScroll() = binding.fabSong.extend()
        })
      }
      val liftMode = if (isPortrait) ScrollBehavior.ALWAYS_LIFTED else ScrollBehavior.LIFT_ON_SCROLL
      setUpScroll(binding.appBarSong, binding.recyclerSongParts, liftMode)
    }

    binding.fabSong.onGlobalLayout {
      fabBaseY = binding.fabSong.y
    }

    setupImeAnimation(systemBarBehavior)

    binding.toolbarSong.isTitleCentered = isTablet || !isPortrait

    binding.buttonSongClose.setOnClickListener { v ->
      if (getViewUtil().isClickEnabled(v.id)) {
        performHapticClick()
        if (hasUnsavedChanges) {
          dialogUtilDiscard.show()
        } else {
          navigateUp()
        }
      }
    }
    binding.buttonSongSave.setOnClickListener { v ->
      if (getViewUtil().isClickDisabled(v.id)) return@setOnClickListener
      performHapticClick()
      if (hasUnsavedChanges) {
        if (isNewSong) {
          activity.songViewModel.insertSongWithParts(songResult, partsResult)
          sendSongsWidgetUpdate(activity)
        } else {
          activity.songViewModel.updateSongAndParts(
            songResult, partsResult, partsSource
          ) {
            metronomeEngine?.apply {
              reloadCurrentSong()
              updateShortcuts()
              sendSongsWidgetUpdate(activity)
            }
          }
        }
        navigateUp()
      }
    }
    binding.buttonSongMenu.setOnClickListener { v ->
      performHapticClick()
      val itemClickListener = PopupMenu.OnMenuItemClickListener { item ->
        if (getViewUtil().isClickDisabled(item.itemId)) return@OnMenuItemClickListener false
        performHapticClick()
        when (item.itemId) {
          R.id.action_delete -> dialogUtilDelete.show()
          R.id.action_feedback -> activity.showFeedbackDialog()
          R.id.action_help -> activity.showHelpDialog()
        }
        true
      }
      v.showMenu(R.menu.menu_song, itemClickListener) { menu ->
        menu.findItem(R.id.action_delete)?.isVisible = !isNewSong
      }
    }

    binding.buttonSongClose.setTooltipText(R.string.action_close)
    binding.buttonSongSave.setTooltipText(R.string.action_save)
    binding.buttonSongMenu.setTooltipText(R.string.action_more)

    binding.buttonSongSave.isEnabled = false

    adapter = PartAdapter(object : PartAdapter.OnPartItemClickListener {
      override fun onEditClick(part: Part) {
        performHapticClick()
        optionsUtil.setPart(part.copy(), false)
        optionsUtil.show()

        binding.editTextSongName.postDelayed({
          binding.editTextSongName.hideKeyboard()
          binding.editTextSongName.clearFocus()
        }, 200)
      }

      override fun onMoveUpClick(part: Part) {
        performHapticClick()
        val index = part.partIndex
        if (index > 0) {
          val partCurrent = part.copy()
          val partAbove = partsResult[index - 1].copy()
          partCurrent.partIndex = index - 1
          partAbove.partIndex = index
          partsResult[index - 1] = partCurrent
          partsResult[index] = partAbove
          sortParts()
          adapter.setParts(ArrayList(partsResult))
          updateResult()
        }
        binding.editTextSongName.hideKeyboard()
        binding.editTextSongName.clearFocus()
      }

      override fun onMoveDownClick(part: Part) {
        performHapticClick()
        val index = part.partIndex
        if (index < partsResult.size - 1) {
          val partCurrent = part.copy()
          val partBelow = partsResult[index + 1].copy()
          partCurrent.partIndex = index + 1
          partBelow.partIndex = index
          partsResult[index + 1] = partCurrent
          partsResult[index] = partBelow
          sortParts()
          adapter.setParts(ArrayList(partsResult))
          updateResult()
        }
        binding.editTextSongName.hideKeyboard()
        binding.editTextSongName.clearFocus()
      }

      override fun onMoreClick(part: Part) {
        performHapticClick()
        binding.editTextSongName.hideKeyboard()
        binding.editTextSongName.clearFocus()
      }

      override fun onRenameClick(part: Part) {
        performHapticClick()
        renameDialogUtil.setPart(part)
        renameDialogUtil.show()
      }

      override fun onDuplicateClick(part: Part) {
        performHapticClick()
        if (activity.isUnlocked() || partsResult.size < 2) {
          val partNew = part.copy().apply {
            setRandomId()
            partIndex = part.partIndex + 1
          }
          for (i in partNew.partIndex until partsResult.size) {
            partsResult[i] = partsResult[i].copy().apply { partIndex++ }
          }
          partsResult.add(partNew.partIndex, partNew)
          sortParts()
          adapter.setParts(ArrayList(partsResult))
          updateResult()
        } else {
          activity.showUnlockDialog()
        }
      }

      override fun onDeleteClick(part: Part) {
        performHapticClick()
        if (partsResult.size <= 1) return
        partsResult.removeAt(part.partIndex)
        partsResult.forEachIndexed { i, p -> p.partIndex = i }
        adapter.setParts(ArrayList(partsResult))
        updateResult()
      }
    })
    binding.recyclerSongParts.adapter = adapter
    binding.recyclerSongParts.layoutManager = WrapperLinearLayoutManager(activity)
    binding.recyclerSongParts.itemAnimator = DefaultItemAnimator()
    binding.recyclerSongParts.addItemDecoration(
      PartItemDecoration(activity.dpToPx(0f))
    )

    val songId = args.songId
    if (songId != null) {
      isNewSong = false
      activity.songViewModel.fetchSongWithParts(songId) { songWithParts ->
        activity.runOnUiThread {
          if (songWithParts != null) {
            songSource = songWithParts.song
            songResult = songSource!!.copy()
            savedInstanceState?.let {
              BundleCompat.getParcelable(
                it, KEY_SONG_RESULT, Song::class.java
              )?.let { song ->
                songResult = song
              }
            }
            binding.textInputSongName.isHintAnimationEnabled = false
            binding.editTextSongName.clearFocus()
            binding.editTextSongName.setText(songResult.name)
            binding.editTextSongName.post {
              binding.editTextSongName.hideKeyboard()
              binding.editTextSongName.clearFocus()
            }
            binding.textInputSongName.isHintAnimationEnabled = true
            setSongOptions(songResult.isLooped, songResult.speed)
            partsSource = songWithParts.parts
            partsResult = songWithParts.parts.map { it.copy() }.toMutableList()
            savedInstanceState?.let {
              BundleCompat.getParcelableArrayList(it, KEY_PARTS_RESULT, Part::class.java)
                ?.let { parts ->
                  partsResult = parts.toMutableList()
                }
            }
            sortParts()
            binding.recyclerSongParts.stopScroll()
            adapter.setParts(ArrayList(partsResult))
          } else {
            Log.e(TAG, "onViewCreated: song with id=$songId not found")
          }
          updateResult()
        }
      }
    } else {
      isNewSong = true
      songSource = Song()
      songResult = songSource!!.copy()
      savedInstanceState?.let {
        BundleCompat.getParcelable(it, KEY_SONG_RESULT, Song::class.java)?.let { song ->
          songResult = song.copy()
        }
      }
      binding.editTextSongName.clearFocus()
      binding.editTextSongName.setText(songResult.name)
      binding.editTextSongName.post {
        if (savedInstanceState == null && (isPortrait || activity.isLandTablet())) {
          binding.editTextSongName.requestFocus()
          binding.editTextSongName.showKeyboard()
        } else {
          binding.editTextSongName.hideKeyboard()
        }
      }
      setSongOptions(songResult.isLooped, songResult.speed)
      partsResult = mutableListOf(
        Part.fromConfig(
          null, songResult.id, 0,
          metronomeEngine?.config ?: MetronomeConfig()
        )
      )
      sortParts()
      adapter.setParts(ArrayList(partsResult))
      partsSource = partsResult.map { it.copy() }
      savedInstanceState?.let {
        BundleCompat.getParcelableArrayList(
          it, KEY_PARTS_RESULT, Part::class.java
        )?.let { parts ->
          partsResult = parts.toMutableList()
          sortParts()
          binding.recyclerSongParts.stopScroll()
          adapter.setParts(partsResult)
        }
      }
      updateResult()
      if (songResult.name.isNullOrEmpty()) clearError()
    }

    binding.toolbarSong.menu.findItem(R.id.action_delete)?.isVisible = !isNewSong

    activity.songViewModel.getAllSongsLive().observe(viewLifecycleOwner) { songs ->
      songsExisting = songs
    }

    binding.editTextSongName.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        binding.editTextSongName.hideKeyboard()
        binding.editTextSongName.clearFocus()
        updateResult()
      }
      false
    }
    binding.editTextSongName.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
      override fun afterTextChanged(s: Editable?) {
        if (binding.editTextSongName.hasFocus()) updateResult()
      }
    })

    binding.fabSong.setOnClickListener(this)
    binding.linearSongOptions.setOnClickListener(this)

    dialogUtilDiscard = DialogUtil(activity, "discard_changes")
    dialogUtilDiscard.createDialogError { builder ->
      builder.setTitle(R.string.msg_discard_changes)
      builder.setMessage(R.string.msg_discard_changes_description)
      builder.setPositiveButton(R.string.action_discard) { _, _ ->
        performHapticClick()
        activity.navigateUp()
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
    }
    dialogUtilDiscard.showIfWasShown(savedInstanceState)

    dialogUtilDelete = DialogUtil(activity, "delete")
    dialogUtilDelete.createDialogError { builder ->
      builder.setTitle(R.string.msg_delete_song)
      builder.setMessage(R.string.msg_delete_song_description)
      builder.setPositiveButton(R.string.action_delete) { _, _ ->
        performHapticClick()
        val source = songSource ?: run {
          Log.e(TAG, "onViewCreated: songSource cannot be null")
          return@setPositiveButton
        }
        val engine = metronomeEngine ?: return@setPositiveButton
        if (source.id == engine.currentSongId) {
          engine.setCurrentSong(Constants.SONG_ID_DEFAULT, 0)
        }
        activity.songViewModel.deleteSong(source) {
          metronomeEngine?.apply {
            activity.songViewModel.deleteParts(partsSource)
            updateShortcuts()
            sendSongsWidgetUpdate(activity)
          }
        }
        activity.navigateUp()
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
    }
    dialogUtilDelete.showIfWasShown(savedInstanceState)

    songOptionsDialogUtil = SongOptionsDialogUtil(activity, this)
    songOptionsDialogUtil.showIfWasShown(savedInstanceState)

    renameDialogUtil = RenameDialogUtil(activity, this)
    renameDialogUtil.showIfWasShown(savedInstanceState)

    optionsUtil = OptionsUtil(activity, object : OptionsUtil.OnPartEditListener {
      override fun onPartAdded(part: Part) {
        partsResult.add(part)
        sortParts()
        adapter.setParts(ArrayList(partsResult))
        updateResult()
      }

      override fun onPartUpdated(part: Part) {
        partsResult[part.partIndex] = part
        adapter.setParts(ArrayList(partsResult))
        updateResult()
      }
    })
    optionsUtil.showIfWasShown(savedInstanceState)

    onBackPressedCallback = object : OnBackPressedCallback(false) {
      override fun handleOnBackPressed() = dialogUtilDiscard.show()
    }.also {
      activity.onBackPressedDispatcher.addCallback(activity, it)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    dialogUtilDiscard.saveState(outState)
    dialogUtilDelete.saveState(outState)
    songOptionsDialogUtil.saveState(outState)
    renameDialogUtil.saveState(outState)
    optionsUtil.saveState(outState)
    outState.putParcelable(KEY_SONG_RESULT, songResult)
    outState.putParcelableArrayList(KEY_PARTS_RESULT, ArrayList(partsResult))
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.linear_song_options -> {
        performHapticClick()
        songOptionsDialogUtil.setSongOptions(songResult.isLooped, songResult.speed)
        songOptionsDialogUtil.show()
      }

      R.id.fab_song -> {
        performHapticClick()
        binding.editTextSongName.hideKeyboard()
        binding.editTextSongName.clearFocus()
        if (activity.isUnlocked() || partsResult.size < 2) {
          val part = Part.fromConfig(
            null, songResult.id,
            partsResult.size, MetronomeConfig()
          )
          optionsUtil.setPart(part, true)
          optionsUtil.show()
        } else {
          activity.showUnlockDialog()
        }
      }
    }
  }

  fun setSongOptions(looped: Boolean, speed: Int) {
    songResult.isLooped = looped
    songResult.speed = speed
    binding.textSongLooped.text = getString(
      if (looped) R.string.label_song_looped else R.string.label_song_not_looped
    )
    binding.textSongSpeed.text = if (speed == 100) {
      getString(R.string.label_song_speed_original)
    } else {
      getString(R.string.label_song_speed_short, speed)
    }
    updateResult()
  }

  fun renamePart(partId: String, name: String?) {
    partsResult.find { it.id == partId }?.let { part ->
      val partResult = part.copy().apply { this.name = name }
      partsResult[part.partIndex] = partResult
      adapter.setParts(ArrayList(partsResult))
      updateResult()
    }
  }

  private fun sortParts() {
    sortPartsByIndex(partsSource.toMutableList())
    sortPartsByIndex(partsResult)
  }

  private fun updateResult() {
    var isValid = true
    val songName = binding.editTextSongName.text?.toString()?.trim()
    if (!songName.isNullOrEmpty()) {
      val isUnique = songsExisting.none { song ->
        val isSameSong = songSource?.id == song.id
        !isSameSong && songName == song.name
      }
      if (isUnique) {
        songResult.name = songName
      } else {
        isValid = false
        setErrorSongName(true)
      }
    } else {
      isValid = false
      setErrorSongName(false)
      songResult.name = null
    }
    if (isValid) clearError()
    val hasChanges = songResult != songSource || partsResult != partsSource
    setHasUnsavedChanges(hasChanges, isValid)
  }

  private fun setHasUnsavedChanges(hasChanges: Boolean, isValid: Boolean) {
    hasUnsavedChanges = hasChanges
    onBackPressedCallback?.isEnabled = hasChanges
    binding.buttonSongSave.isEnabled = hasChanges && isValid
  }

  private fun setErrorSongName(notUnique: Boolean) {
    binding.textInputSongName.error = getString(
      if (notUnique) R.string.label_song_name_used else R.string.msg_invalid_input
    )
  }

  private fun clearError() {
    binding.textInputSongName.error = null
    binding.textInputSongName.isErrorEnabled = false
  }

  private fun setupImeAnimation(systemBarBehavior: SystemBarBehavior) {
    val callback = object : WindowInsetsAnimationCompat.Callback(
      DISPATCH_MODE_STOP
    ) {
      private var imeInsetStart = 0
      private var imeInsetEnd = 0
      private var yStart = 0f
      private var yEnd = 0f

      override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        imeInsetStart = systemBarBehavior.imeInset
        yStart = binding.fabSong.y
      }

      override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
      ): WindowInsetsAnimationCompat.BoundsCompat {
        imeInsetEnd = systemBarBehavior.imeInset
        systemBarBehavior.imeInset = imeInsetStart
        systemBarBehavior.refresh(false)
        yEnd = binding.fabSong.y
        binding.fabSong.y = yStart
        return bounds
      }

      override fun onProgress(
        insets: WindowInsetsCompat,
        animations: List<WindowInsetsAnimationCompat>
      ): WindowInsetsCompat {
        val animation = animations.firstOrNull() ?: return insets
        val fraction = animation.interpolatedFraction
        systemBarBehavior.imeInset = MathUtils.lerp(
          imeInsetStart.toFloat(), imeInsetEnd.toFloat(), fraction
        ).toInt()
        systemBarBehavior.refresh(false)
        binding.fabSong.y = MathUtils.lerp(yStart, yEnd, fraction)
        return insets
      }
    }
    ViewCompat.setOnApplyWindowInsetsListener(binding.constraintSongContainer) { _, insets ->
      val bottomInsetIme = insets.getInsets(Type.ime()).bottom
      systemBarBehavior.imeInset = bottomInsetIme
      systemBarBehavior.refresh(false)
      if (insets.isVisible(Type.ime())) {
        val bottomInsetNav = insets.getInsets(Type.systemBars()).bottom
        binding.fabSong.translationY = (-bottomInsetIme + bottomInsetNav).toFloat()
      } else {
        binding.fabSong.y = fabBaseY
      }
      insets
    }
    ViewCompat.setWindowInsetsAnimationCallback(binding.constraintSongContainer, callback)
  }

  companion object {
    private val TAG = SongFragment::class.java.simpleName
    private const val KEY_SONG_RESULT = "song_result"
    private const val KEY_PARTS_RESULT = "parts_result"
  }
}
