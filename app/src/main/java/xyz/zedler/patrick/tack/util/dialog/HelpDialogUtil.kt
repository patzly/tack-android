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
import android.view.View
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogHelpBinding
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.setOnClickListeners
import androidx.core.net.toUri

class HelpDialogUtil(private val activity: MainActivity) : View.OnClickListener {

  private val binding = PartialDialogHelpBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "help")

  init {
    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.title_help)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
    }

    setOnClickListeners(
      this,
      binding.linearHelpQuestion1,
      binding.linearHelpQuestion2,
      binding.linearHelpQuestion3,
      binding.linearHelpQuestion4,
      binding.linearHelpQuestion5,
      binding.linearHelpQuestion6,
      binding.linearHelpQuestion7,
      binding.linearHelpQuestion8,
      binding.linearHelpQuestion9,
      binding.buttonHelpTranslate
    )
  }

  override fun onClick(v: View) {
    activity.performHapticClick()

    when (v.id) {
      R.id.linear_help_question1 -> toggleAnswerVisibility(binding.textHelpAnswer1)
      R.id.linear_help_question2 -> toggleAnswerVisibility(binding.textHelpAnswer2)
      R.id.linear_help_question3 -> toggleAnswerVisibility(binding.textHelpAnswer3)
      R.id.linear_help_question4 -> toggleAnswerVisibility(binding.textHelpAnswer4)
      R.id.linear_help_question5 -> toggleAnswerVisibility(binding.textHelpAnswer5)
      R.id.linear_help_question6 -> toggleAnswerVisibility(binding.textHelpAnswer6)
      R.id.linear_help_question7 -> toggleAnswerVisibility(binding.textHelpAnswer7)
      R.id.linear_help_question8 -> toggleAnswerVisibility(binding.textHelpAnswer8)
      R.id.linear_help_question9 -> {
        toggleAnswerVisibility(binding.textHelpAnswer9)
        binding.buttonHelpTranslate.isVisible = !binding.buttonHelpTranslate.isVisible
      }

      R.id.button_help_translate -> {
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

  private fun update() {
    binding.scrollHelp.scrollTo(0, 0)

    binding.textHelpAnswer1.isVisible = false
    binding.textHelpAnswer2.isVisible = false
    binding.textHelpAnswer3.isVisible = false
    binding.textHelpAnswer4.isVisible = false
    binding.textHelpAnswer5.isVisible = false
    binding.textHelpAnswer6.isVisible = false
    binding.textHelpAnswer7.isVisible = false
    binding.textHelpAnswer8.isVisible = false
    binding.textHelpAnswer9.isVisible = false
    binding.buttonHelpTranslate.isVisible = false
  }

  private fun toggleAnswerVisibility(answerView: View) {
    val transition = AutoTransition().apply {
      duration = Constants.ANIM_DURATION_SHORT
    }
    TransitionManager.beginDelayedTransition(binding.linearHelpContainer, transition)
    answerView.isVisible = !answerView.isVisible
  }
}
