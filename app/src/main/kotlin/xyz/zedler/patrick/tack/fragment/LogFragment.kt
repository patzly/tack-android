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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.ScrollBehavior
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.databinding.FragmentLogBinding
import xyz.zedler.patrick.tack.util.setOnClickListeners
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.start
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LogFragment : BaseFragment(), View.OnClickListener {

  private var _binding: FragmentLogBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentLogBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val systemBarBehavior = SystemBarBehavior(activity)
    systemBarBehavior.setAppBar(binding.appBarLog)
    systemBarBehavior.setScroll(
      binding.scrollLog, binding.linearLogContainer
    )
    systemBarBehavior.setUp()

    ScrollBehavior().setUpScroll(
      binding.appBarLog,
      binding.scrollLog,
      ScrollBehavior.LIFT_ON_SCROLL
    )

    binding.buttonLogBack.setOnClickListener(getNavigationOnClickListener())
    binding.buttonLogReload.setOnClickListener {
      binding.buttonLogReload.icon?.start()
      loadLogcat { log -> binding.textLog.text = log }
    }
    binding.buttonLogBack.setTooltipText(R.string.action_back)
    binding.buttonLogReload.setTooltipText(R.string.action_reload)

    setOnClickListeners(
      this,
      binding.buttonLogCopy,
      binding.buttonLogFeedback
    )

    Handler(Looper.getMainLooper()).postDelayed({
      loadLogcat { log -> binding.textLog.text = log }
    }, 10)
  }

  override fun onClick(v: View) {
    val id = v.id
    if (getViewUtil().isClickDisabled(id)) {
      return
    }
    performHapticClick()

    when (id) {
      R.id.button_log_copy -> {
        val logcat = binding.textLog.text.toString()
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(logcat, logcat))
        activity.showSnackbar(
          activity.getSnackbar(R.string.msg_copied_to_clipboard, Snackbar.LENGTH_SHORT)
        )
      }

      R.id.button_log_feedback -> {
        activity.showFeedbackDialog()
      }
    }
  }

  private fun loadLogcat(onLogLoaded: (String) -> Unit) {
    lifecycleScope.launch {
      val log = withContext(Dispatchers.IO) {
        val logBuilder = StringBuilder()
        try {
          val process = Runtime.getRuntime().exec("logcat -d *:E -t 300")
          BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
              logBuilder.append(line).append('\n')
            }
          }
          process.destroy()
        } catch (ignored: IOException) {
        }
        logBuilder.toString()
      }
      onLogLoaded(log)
    }
  }

  companion object {
    private val TAG = LogFragment::class.java.simpleName
  }
}
