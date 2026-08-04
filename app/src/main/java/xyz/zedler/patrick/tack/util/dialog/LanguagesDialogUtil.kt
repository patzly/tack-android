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
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogLanguagesTitleBinding
import xyz.zedler.patrick.tack.databinding.PartialDialogRecyclerBinding
import xyz.zedler.patrick.tack.recyclerview.adapter.LanguageDialogAdapter
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.getLanguageCode
import xyz.zedler.patrick.tack.util.getLanguages
import xyz.zedler.patrick.tack.util.onGlobalLayout
import androidx.core.net.toUri

class LanguagesDialogUtil(private val activity: MainActivity) {

  private val titleBinding = PartialDialogLanguagesTitleBinding.inflate(activity.layoutInflater)
  private val binding = PartialDialogRecyclerBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "languages")
  private val adapter: LanguageDialogAdapter

  init {
    binding.recyclerDialog.layoutManager = WrapperLinearLayoutManager(activity)
    adapter = LanguageDialogAdapter(
      activity.getLanguages()
    ) { languageCode, fromUser ->
      val previous = AppCompatDelegate.getApplicationLocales()
      val selected = LocaleListCompat.forLanguageTags(languageCode)
      if (previous != selected) {
        if (fromUser) {
          activity.performHapticClick()
          setLanguageCode(languageCode)
        }
        Handler(Looper.getMainLooper()).postDelayed({
          dismiss()
          AppCompatDelegate.setApplicationLocales(selected)
        }, 300)
      }
    }
    binding.recyclerDialog.adapter = adapter

    dialogUtil.createDialog { builder ->
      builder.setCustomTitle(titleBinding.root)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
      builder.setNeutralButton(R.string.action_learn_more) { _, _ ->
        activity.performHapticClick()
        activity.startActivity(
          Intent(
            Intent.ACTION_VIEW,
            activity.getString(R.string.app_translate).toUri()
          )
        )
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
    adapter.setLanguageCode(
      AppCompatDelegate.getApplicationLocales().getLanguageCode()
    )
    maybeShowDividers()
  }

  private fun setLanguageCode(languageCode: String?) {
    adapter.setLanguageCode(languageCode)
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
