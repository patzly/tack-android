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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogTextBinding
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.getRawText
import androidx.core.net.toUri

class TextDialogUtil(
  activity: MainActivity,
  @StringRes title: Int,
  @RawRes file: Int,
  highlights: Array<String>? = null,
  @StringRes link: Int = 0
) {

  private val binding = PartialDialogTextBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "text_$title").apply {
    createDialog { builder ->
      builder.setTitle(title)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
      if (link != 0) {
        builder.setNeutralButton(R.string.action_learn_more) { _, _ ->
          activity.performHapticClick()
          activity.startActivity(
            Intent(Intent.ACTION_VIEW, activity.getString(link).toUri())
          )
        }
      }
    }
  }

  init {
    binding.formattedText.setIsDialog(true)
    if (highlights != null) {
      binding.formattedText.setText(activity.getRawText(file), *highlights)
    } else {
      binding.formattedText.setText(activity.getRawText(file))
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

  private fun update() {
    binding.scrollText.scrollTo(0, 0)
  }
}
