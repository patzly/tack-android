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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.isVisible
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogFeedbackBinding
import xyz.zedler.patrick.tack.util.*
import androidx.core.content.edit
import androidx.core.net.toUri

class FeedbackDialogUtil(
  private val activity: MainActivity,
  private val onSupportClick: () -> Unit
) : View.OnClickListener {

  private val binding = PartialDialogFeedbackBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "feedback")
  private val viewUtil = ViewUtil()

  init {
    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.title_feedback)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
      builder.setOnDismissListener {
        if (activity.sharedPrefs.getInt(PREF.FEEDBACK_POP_UP_COUNT, 1) != 0) {
          activity.sharedPrefs.edit { putInt(PREF.FEEDBACK_POP_UP_COUNT, 0) }
        }
      }
    }

    setOnClickListeners(
      this,
      binding.linearFeedbackRate,
      binding.linearFeedbackSupport,
      binding.linearFeedbackIssue,
      binding.linearFeedbackEmail,
      binding.linearFeedbackRecommend
    )

    updateDividerVisibility(activity.isOrientationPortrait().not())
  }

  override fun onClick(v: View) {
    if (viewUtil.isClickDisabled(v.id)) return
    activity.performHapticClick()

    when (v.id) {
      R.id.linear_feedback_rate -> {
        val uri = "market://details?id=${activity.packageName}".toUri()
        val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
          addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
          )
        }
        try {
          activity.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
          activity.startActivity(
            Intent(
              Intent.ACTION_VIEW,
              "http://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
            )
          )
        }
      }

      R.id.linear_feedback_support -> {
        Handler(Looper.getMainLooper()).postDelayed({ onSupportClick() }, 200)
      }

      R.id.linear_feedback_issue -> {
        val issues = "${activity.getString(R.string.app_github)}/issues"
        activity.startActivity(Intent(Intent.ACTION_VIEW, issues.toUri()))
      }

      R.id.linear_feedback_email -> {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
          data = "mailto:${
            activity.getString(R.string.app_mail)
          }?subject=${Uri.encode("Feedback@Tack")}".toUri()
        }
        activity.startActivity(
          Intent.createChooser(intent, activity.getString(R.string.action_send_feedback))
        )
      }

      R.id.linear_feedback_recommend -> {
        val text = activity.getString(
          R.string.msg_recommend,
          activity.getString(R.string.app_vending_app)
        )
        activity.share(text)
      }
    }
    dismiss()
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
    binding.scrollFeedback.scrollTo(0, 0)
    measureScrollView()

    val checkUnlockKey = activity.sharedPrefs.getBoolean(
      PREF.CHECK_UNLOCK_KEY, true
    )
    val isSupportVisible = checkUnlockKey &&
        isPlayStoreInstalled(activity) &&
        !isKeyInstalled(activity)
    binding.linearFeedbackSupport.isVisible = isSupportVisible
  }

  private fun measureScrollView() {
    binding.scrollFeedback.onGlobalLayout {
      val isScrollable = binding.scrollFeedback.canScrollVertically(-1) ||
          binding.scrollFeedback.canScrollVertically(1)
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollFeedback.setDividerVisibility(
      visible,
      binding.dividerFeedbackTop,
      binding.dividerFeedbackBottom,
      binding.linearFeedbackContainer
    )
  }
}
