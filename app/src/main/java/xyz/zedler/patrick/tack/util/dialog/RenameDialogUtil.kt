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
import android.view.inputmethod.EditorInfo
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.databinding.PartialDialogPartRenameBinding
import xyz.zedler.patrick.tack.fragment.SongFragment
import xyz.zedler.patrick.tack.util.*

class RenameDialogUtil(
  private val activity: MainActivity,
  private val fragment: SongFragment
) {

  private val binding = PartialDialogPartRenameBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "part_rename")
  private var partId: String? = null
  private var partNamePrev: String? = null
  private var partIndex = 0

  init {
    binding.editTextPartRename.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        activity.performHapticClick()
        rename()
      }
      false
    }

    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.action_rename_part)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_rename) { _, _ ->
        activity.performHapticClick()
        rename()
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ ->
        activity.performHapticClick()
      }
    }

    updateDividerVisibility(activity.isOrientationPortrait().not())
  }

  fun show() {
    update()
    dialogUtil.show()
    showKeyboard()
  }

  fun showIfWasShown(state: Bundle?) {
    state?.let {
      partId = it.getString(PART_ID)
      partNamePrev = it.getString(PART_NAME_PREV)
      partIndex = it.getInt(PART_INDEX)
    }
    update()
    if (dialogUtil.showIfWasShown(state)) {
      showKeyboard()
    }
  }

  fun dismiss() {
    dialogUtil.dismiss()
  }

  fun saveState(outState: Bundle) {
    dialogUtil.saveState(outState)
    outState.putString(PART_ID, partId)
    outState.putString(PART_NAME_PREV, partNamePrev)
    outState.putInt(PART_INDEX, partIndex)
  }

  fun update() {
    binding.editTextPartRename.setText(partNamePrev)
    val text = binding.editTextPartRename.text
    binding.editTextPartRename.setSelection(text?.length ?: 0)
    // placeholder
    binding.editTextPartRename.hint = activity.getString(
      R.string.label_part_unnamed,
      partIndex + 1
    )

    measureScrollView()
  }

  fun setPart(part: Part) {
    partId = part.id
    partNamePrev = part.name
    partIndex = part.partIndex
    update()
  }

  private fun rename() {
    val text = binding.editTextPartRename.text?.toString()?.trim()
    val name = if (text.isNullOrEmpty()) null else text
    if (partId != null) {
      fragment.renamePart(partId!!, name)
    }
  }

  private fun showKeyboard() {
    binding.editTextPartRename.requestFocus()
    binding.editTextPartRename.showKeyboard()
  }

  private fun measureScrollView() {
    binding.scrollPartRename.onGlobalLayout {
      val isScrollable = binding.scrollPartRename.canScrollVertically(-1) ||
          binding.scrollPartRename.canScrollVertically(1)
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollPartRename.setDividerVisibility(
      visible,
      binding.dividerPartRenameTop,
      binding.dividerPartRenameBottom,
      binding.linearPartRenameContainer
    )
  }

  companion object {
    private const val PART_ID = "part_id_dialog"
    private const val PART_NAME_PREV = "part_name_prev_dialog"
    private const val PART_INDEX = "part_index_dialog"
  }
}
