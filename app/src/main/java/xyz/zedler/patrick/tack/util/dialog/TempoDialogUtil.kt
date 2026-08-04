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

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.PartialDialogTempoBinding
import xyz.zedler.patrick.tack.fragment.MainFragment
import xyz.zedler.patrick.tack.util.*
import java.util.*
import androidx.core.view.isGone

class TempoDialogUtil(
  private val activity: MainActivity,
  private val fragment: MainFragment,
  private val listener: TempoDialogListener?
) {

  private val binding = PartialDialogTempoBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "tempo")
  private val intervals: Queue<Long> = LinkedList()
  private var previous: Long = 0
  private var tempoOld = 0
  private var inputMethodKeyboard = false
  private var instantApply = false

  @SuppressLint("ClickableViewAccessibility")
  private val onTouchListener = View.OnTouchListener { v, event ->
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        binding.tempoTapTempo.setTouched(true)
        val enoughData = tap()
        if (enoughData) {
          if (binding.frameTempoTapTempo.isGone) {
            binding.frameTempoTapTempo.alpha = 0f
            binding.frameTempoTapTempo.isVisible = true
            binding.frameTempoTapTempo.animate()
              .alpha(1f)
              .setDuration(150)
              .start()
            binding.textTempoPlaceholder.animate()
              .alpha(0f)
              .setDuration(150)
              .withEndAction { binding.textTempoPlaceholder.isVisible = false }
              .start()
          }
          val tempoNew = getTapTempo()
          setTapTempoDisplay(tempoOld, tempoNew)
          tempoOld = tempoNew
          if (instantApply) {
            activity.metronomeEngine?.let { engine ->
              engine.setTempo(tempoNew)
              engine.maybeUpdateDefaultSong()
            }
          }
        }
        activity.performHapticHeavyClick()
        true
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        binding.tempoTapTempo.setTouched(false)
        v.performClick()
        true
      }

      else -> false
    }
  }

  init {
    binding.editTextTempo.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        if (isInputValid()) {
          activity.performHapticClick()
          setTempoFromInputAndDismiss()
        } else {
          activity.performHapticReject(binding.root)
          return@setOnEditorActionListener true
        }
      }
      false
    }
    binding.textInputTempo.helperText = activity.getString(
      R.string.label_tempo_input_help, Constants.TEMPO_MIN, Constants.TEMPO_MAX
    )

    binding.linearTempoInstant.setOnClickListener {
      binding.switchTempoInstant.toggle()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val variableTypeface = ResourcesCompat.getFont(
        activity, R.font.google_sans_flex_variable
      )
      binding.textTempoTapTempo.typeface = variableTypeface
      binding.textTempoTapTempo.fontVariationSettings = "'wght' 700, 'ROND' 100"
    }
    binding.textSwitcherTempoTapTempoTerm.setFactory {
      TextView(activity).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          activity.resources.getDimension(R.dimen.tempo_tap_label_text_size)
        )
        typeface = ResourcesCompat.getFont(activity, R.font.google_sans_flex_medium)
        setTextColor(activity.getAttrColor(R.attr.colorOnTertiaryContainer))
      }
    }

    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.action_change_tempo)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_apply, null)
      builder.setNegativeButton(R.string.action_cancel) { _, _ ->
        activity.performHapticClick()
      }
    }
    dialogUtil.setOnShowListener {
      overrideDialogActions()
      if (inputMethodKeyboard) {
        showKeyboard()
      }
    }

    binding.toggleTempoMethod.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) {
        activity.metronomeEngine?.let { engine ->
          activity.performHapticClick()
          val isKeyboard = checkedId == R.id.button_tempo_keyboard
          engine.tempoInputKeyboard = isKeyboard
          if (!isKeyboard) {
            binding.linearTempoContainer.focusedChild?.hideKeyboard()
            intervals.clear()
            previous = 0
          }
          update()
          if (isKeyboard) showKeyboard()
          overrideDialogActions()
        }
      }
    }

    binding.switchTempoInstant.setOnCheckedChangeListener { _, isChecked ->
      activity.metronomeEngine?.let { engine ->
        activity.performHapticClick()
        instantApply = isChecked
        engine.tempoTapInstant = isChecked
        if (isChecked) {
          val tapAverage = getTapAverage()
          if (tapAverage > 0) {
            val tempo = getTapTempo(tapAverage)
            listener?.onTempoChanged(tempo)
            engine.setTempo(tempo)
            engine.maybeUpdateDefaultSong()
          }
        }
        overrideDialogActions()
      }
    }

    updateDividerVisibility(activity.isOrientationPortrait().not())
  }

  private fun overrideDialogActions() {
    val dialog = dialogUtil.getDialog() ?: return
    val buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    val buttonNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    val showApplyButton = inputMethodKeyboard || !instantApply

    buttonPositive?.apply {
      text =
        activity.getString(if (showApplyButton) R.string.action_apply else R.string.action_close)
      setOnClickListener {
        if (inputMethodKeyboard) {
          if (isInputValid()) {
            activity.performHapticClick()
            setTempoFromInputAndDismiss()
          } else {
            activity.performHapticReject(binding.root)
          }
        } else {
          activity.performHapticClick()
          setTempoFromInputAndDismiss()
        }
      }
    }
    buttonNegative?.isVisible = showApplyButton
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
    intervals.clear()
    previous = 0
  }

  fun saveState(outState: Bundle) {
    dialogUtil.saveState(outState)
  }

  fun update() {
    val engine = activity.metronomeEngine ?: return
    inputMethodKeyboard = engine.tempoInputKeyboard
    instantApply = engine.tempoTapInstant

    binding.toggleTempoMethod.apply {
      check(if (inputMethodKeyboard) R.id.button_tempo_keyboard else R.id.button_tempo_tap)
    }

    if (inputMethodKeyboard) {
      setError(false)
      binding.editTextTempo.text = null
      binding.editTextTempo.requestFocus()
    } else {
      binding.switchTempoInstant.isChecked = instantApply
      binding.switchTempoInstant.jumpDrawablesToCurrentState()

      tempoOld = engine.config.tempo
      setTapTempoDisplay(tempoOld, tempoOld)
      binding.textSwitcherTempoTapTempoTerm.setCurrentText(fragment.getTempoTerm(tempoOld))
      binding.tempoTapTempo.setReduceAnimations(fragment.isReduceAnimations())

      binding.frameTempoTapTempo.isVisible = false
      binding.frameTempoTapTempo.alpha = 0f
      binding.textTempoPlaceholder.isVisible = true
      binding.textTempoPlaceholder.alpha = 1f
    }
    binding.frameTempoInputContainer.isVisible = inputMethodKeyboard
    binding.frameTempoTapContainer.isVisible = !inputMethodKeyboard
    binding.linearTempoInstant.isVisible = !inputMethodKeyboard

    measureScrollView()
  }

  private fun showKeyboard() {
    binding.editTextTempo.requestFocus()
    binding.editTextTempo.showKeyboard()
  }

  private fun isInputValid(): Boolean {
    val tempoString = binding.editTextTempo.text?.toString() ?: ""
    if (tempoString.isEmpty()) {
      setError(true)
      return false
    }
    return try {
      val tempo = tempoString.toInt()
      val valid = tempo in Constants.TEMPO_MIN..Constants.TEMPO_MAX
      setError(!valid)
      valid
    } catch (e: NumberFormatException) {
      setError(true)
      false
    }
  }

  private fun setTempoFromInputAndDismiss() {
    val engine = activity.metronomeEngine ?: return
    if (inputMethodKeyboard) {
      if (!isInputValid()) return
      val tempo = binding.editTextTempo.text.toString().toInt()
      listener?.onTempoChanged(tempo)
      engine.setTempo(tempo)
      engine.maybeUpdateDefaultSong()
      binding.editTextTempo.clearFocus()
    } else {
      val tapAverage = getTapAverage()
      if (tapAverage > 0) {
        val tempo = getTapTempo(tapAverage)
        listener?.onTempoChanged(tempo)
        engine.setTempo(tempo)
        engine.maybeUpdateDefaultSong()
      }
    }
    dismiss()
  }

  private fun setError(error: Boolean) {
    if (error) {
      binding.textInputTempo.error = activity.getString(R.string.msg_invalid_input)
    } else {
      binding.textInputTempo.isErrorEnabled = false
    }
  }

  private fun tap(): Boolean {
    var enoughData = false
    val current = System.currentTimeMillis()
    if (previous > 0) {
      enoughData = true
      val interval = current - previous
      if (intervals.isNotEmpty() && shouldTapReset(interval)) {
        intervals.clear()
        enoughData = false
      } else if (intervals.size >= MAX_TAPS) {
        intervals.poll()
      }
      intervals.offer(interval)
    }
    previous = current
    return enoughData
  }

  private fun setTapTempoDisplay(tempoOld: Int, tempoNew: Int) {
    if (!fragment.isAdded) return
    if (instantApply) {
      listener?.onTempoChanged(tempoNew)
    }
    binding.textTempoTapTempo.text = tempoNew.toString()
    val termNew = fragment.getTempoTerm(tempoNew)
    if (termNew != fragment.getTempoTerm(tempoOld)) {
      val isFaster = tempoNew > tempoOld
      binding.textSwitcherTempoTapTempoTerm.setInAnimation(
        activity,
        if (isFaster) R.anim.tempo_term_open_enter else R.anim.tempo_term_close_enter
      )
      binding.textSwitcherTempoTapTempoTerm.setOutAnimation(
        activity,
        if (isFaster) R.anim.tempo_term_open_exit else R.anim.tempo_term_close_exit
      )
      binding.textSwitcherTempoTapTempoTerm.setText(termNew)
    }
  }

  private fun getTapTempo(interval: Long = getTapAverage()): Int {
    return if (interval > 0) {
      (60000 / interval).toInt().coerceIn(Constants.TEMPO_MIN, Constants.TEMPO_MAX)
    } else 0
  }

  private fun getTapAverage(): Long {
    return if (intervals.isNotEmpty()) intervals.sum() / intervals.size else 0
  }

  private fun shouldTapReset(interval: Long): Boolean {
    val tapTempo = getTapTempo()
    val intervalTempo = getTapTempo(interval)
    return intervalTempo >= tapTempo * (1 + TEMPO_FACTOR) ||
        intervalTempo <= tapTempo * (1 - TEMPO_FACTOR) ||
        interval > getTapAverage() * INTERVAL_FACTOR
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun measureScrollView() {
    binding.scrollTempo.onGlobalLayout {
      val isScrollable = binding.scrollTempo.canScrollVertically(-1) ||
          binding.scrollTempo.canScrollVertically(1)
      if (isScrollable) {
        binding.tempoTapTempo.setOnTouchListener(onTouchListener)
        binding.frameTempoTapContainer.setOnTouchListener(null)
      } else {
        binding.tempoTapTempo.setOnTouchListener(null)
        binding.frameTempoTapContainer.setOnTouchListener(onTouchListener)
      }
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollTempo.setDividerVisibility(
      visible,
      binding.dividerTempoTop,
      binding.dividerTempoBottom,
      binding.linearTempoContainer
    )
  }

  fun interface TempoDialogListener {
    fun onTempoChanged(tempo: Int)
  }

  companion object {
    private const val MAX_TAPS = 20
    private const val TEMPO_FACTOR = 0.5
    private const val INTERVAL_FACTOR = 3
  }
}
