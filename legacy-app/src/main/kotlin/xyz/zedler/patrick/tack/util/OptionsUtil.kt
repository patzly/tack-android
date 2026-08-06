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

package xyz.zedler.patrick.tack.util

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener
import com.google.android.material.slider.Slider.OnSliderTouchListener
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.databinding.FragmentMainBinding
import xyz.zedler.patrick.tack.databinding.PartialDialogOptionsBinding
import xyz.zedler.patrick.tack.databinding.PartialOptionsBinding
import xyz.zedler.patrick.tack.model.MetronomeConfig
import xyz.zedler.patrick.tack.view.BeatView

class OptionsUtil : OnClickListener, OnButtonCheckedListener, OnChangeListener,
  OnSliderTouchListener {

  private val activity: MainActivity
  private val useDialog: Boolean
  private val editPart: Boolean
  private var onOptionsListener: OnOptionsListener? = null
  private var onPartEditListener: OnPartEditListener? = null
  private var isCountInActive = false
  private var isIncrementalActive = false
  private var isTimerActive = false
  private var isMuteActive = false
  private var usePolyrhythm = false
  private var isNew = false
  private var isInitialized = false
  private var dialogUtil: DialogUtil? = null
  private var binding: PartialOptionsBinding? = null
  private var bindingDialog: PartialDialogOptionsBinding? = null
  private var part: Part? = null
  private var config: MetronomeConfig? = null
  private val ticksMaxPerRange: Int

  constructor(
    activity: MainActivity,
    fragmentBinding: FragmentMainBinding,
    listener: OnOptionsListener
  ) {
    this.activity = activity
    this.onOptionsListener = listener

    editPart = false
    useDialog = !activity.isLandTablet()
    if (useDialog) {
      bindingDialog = PartialDialogOptionsBinding.inflate(activity.layoutInflater)
      dialogUtil = DialogUtil(activity, "options")
    }
    binding = if (useDialog) bindingDialog?.partialOptions else fragmentBinding.partialOptions

    binding?.let {
      it.sliderOptionsCountIn.addOnSliderTouchListener(this)
      it.sliderOptionsIncrementalAmount.addOnSliderTouchListener(this)
      it.sliderOptionsIncrementalInterval.addOnSliderTouchListener(this)
      it.sliderOptionsIncrementalLimit.addOnSliderTouchListener(this)
      it.sliderOptionsTimerDuration.addOnSliderTouchListener(this)
      it.sliderOptionsMutePlay.addOnSliderTouchListener(this)
      it.sliderOptionsMuteMute.addOnSliderTouchListener(this)
    }

    ticksMaxPerRange = if (activity.isTablet()) 50 else 20

    if (useDialog) {
      dialogUtil?.createDialog { builder ->
        builder.setTitle(R.string.title_options)
        builder.setView(bindingDialog?.root)
        builder.setPositiveButton(R.string.action_close) { _, _ ->
          activity.performHapticClick()
        }
      }
    }
  }

  constructor(activity: MainActivity, onPartEditListener: OnPartEditListener) {
    this.activity = activity
    this.onPartEditListener = onPartEditListener

    editPart = true
    useDialog = true
    dialogUtil = DialogUtil(activity, "edit_part")
    bindingDialog = PartialDialogOptionsBinding.inflate(activity.layoutInflater)
    binding = bindingDialog?.partialOptions

    ticksMaxPerRange = if (activity.isTablet()) 50 else 20
  }

  fun maybeInit() {
    val config = configInternal ?: return
    if (editPart || isInitialized) {
      return
    }
    isCountInActive = config.isCountInActive()
    isIncrementalActive = config.isIncrementalActive()
    isTimerActive = config.isTimerActive()
    isMuteActive = config.isMuteActive()
    isInitialized = true
  }

  fun show() {
    update()
    if (useDialog) {
      dialogUtil?.show()
    }
  }

  fun showIfWasShown(state: Bundle?) {
    if (editPart) {
      part = state?.getParcelable(PART)
      isNew = state?.getBoolean(IS_NEW, false) ?: false
      val p = part
      if (p != null) {
        setPart(p, isNew)
        update()
        dialogUtil?.showIfWasShown(state)
      }
    } else {
      update()
      if (useDialog) {
        dialogUtil?.showIfWasShown(state)
      }
    }
  }

  fun dismiss() {
    if (useDialog) {
      dialogUtil?.dismiss()
    }
  }

  fun saveState(outState: Bundle) {
    if (useDialog && dialogUtil != null) {
      dialogUtil?.saveState(outState)
      if (editPart && part != null) {
        config?.let { part?.setConfig(it) }
        outState.putParcelable(PART, part)
        outState.putBoolean(IS_NEW, isNew)
      }
    }
  }

  fun setPart(part: Part, isNew: Boolean) {
    this.part = part
    this.isNew = isNew

    config = part.toConfig()

    bindingDialog = PartialDialogOptionsBinding.inflate(activity.layoutInflater)
    binding = bindingDialog?.partialOptions

    dialogUtil?.createDialog { builder ->
      var title = activity.getString(R.string.label_part_edit, part.partIndex + 1)
      if (isNew) {
        title = activity.getString(R.string.action_add_part)
      }
      builder.setTitle(title)
      builder.setView(bindingDialog?.root)
      builder.setPositiveButton(
        if (isNew) R.string.action_add else R.string.action_apply
      ) { _, _ ->
        activity.performHapticClick()
        onPartEditListener?.let { listener ->
          val partResult = part.copy()
          config?.let { partResult.setConfig(it) }
          if (isNew) {
            listener.onPartAdded(partResult)
          } else {
            listener.onPartUpdated(partResult)
          }
        }
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ ->
        activity.performHapticClick()
      }
    }

    update()
  }

  fun update() {
    val b = binding ?: return
    b.linearOptionsEditPartContainer.visibility = if (editPart) View.VISIBLE else View.GONE
    b.linearOptionsUseCurrentConfig.setOnClickListener(this)
    updateTempo()
    updateBeats(false)
    updateSubdivisions(false)
    updateCountIn()
    updateIncremental()
    updateTimer()
    updateMute()
    updateSwing()
    updatePolyrhythm()
  }

  private fun updateTempo() {
    val config = configInternal ?: return
    val b = binding ?: return
    val tempo = config.tempo
    b.textOptionsTempo.text = activity.getString(R.string.label_bpm_value, tempo)

    val tempoFactor = (tempo - 1) / ticksMaxPerRange
    val tempoFromNew = 1 + tempoFactor * ticksMaxPerRange
    val tempoToNew = tempoFromNew + ticksMaxPerRange - 1

    b.buttonOptionsTempoDecrease.isEnabled = tempoFromNew > Constants.TEMPO_MIN
    b.buttonOptionsTempoDecrease.setOnClickListener(this)
    b.buttonOptionsTempoDecrease.setTooltipText(R.string.action_decrease)

    b.buttonOptionsTempoIncrease.isEnabled = tempoToNew < Constants.TEMPO_MAX
    b.buttonOptionsTempoIncrease.setOnClickListener(this)
    b.buttonOptionsTempoIncrease.setTooltipText(R.string.action_increase)

    b.sliderOptionsTempo.removeOnChangeListener(this)
    b.sliderOptionsTempo.configureSafely(
      tempoFromNew, tempoToNew, 1, tempo
    )
    b.sliderOptionsTempo.addOnChangeListener(this)
    b.sliderOptionsTempo.setLabelFormatter { value ->
      activity.getString(R.string.label_bpm_value, value.toInt())
    }
  }

  private fun updateBeats(firstSubChanged: Boolean) {
    val config = configInternal ?: return
    val b = binding ?: return
    val beats = config.beats
    val isFirstSubMuted = config.isFirstSubdivisionMuted()

    if (firstSubChanged) {
      for (i in 0 until b.linearOptionsBeats.childCount) {
        val beatView = b.linearOptionsBeats.getChildAt(i) as BeatView
        if (usePolyrhythm) {
          val muted = isFirstSubMuted && i == 0
          beatView.setTickType(if (muted) TICK_TYPE.MUTED else beats[i], true)
        } else {
          beatView.setTickType(if (isFirstSubMuted) TICK_TYPE.MUTED else beats[i], true)
        }
      }
      return
    }

    val beatsMaybeMuted = beats.clone()
    if (isFirstSubMuted) {
      beatsMaybeMuted.fill(TICK_TYPE.MUTED)
    }
    val currentBeats = Array(b.linearOptionsBeats.childCount) {
      b.linearOptionsBeats.getChildAt(it).toString()
    }

    if (beatsMaybeMuted.contentEquals(currentBeats)) {
      return
    } else if (beatsMaybeMuted.size == currentBeats.size) {
      for (i in beatsMaybeMuted.indices) {
        val beatView = b.linearOptionsBeats.getChildAt(i) as BeatView
        beatView.setTickType(beatsMaybeMuted[i], false)
      }
    } else {
      b.linearOptionsBeats.removeAllViews()
      for (i in beatsMaybeMuted.indices) {
        val beatView = getNewBeatView(false)
        beatView.setTickType(beatsMaybeMuted[i], false)
        beatView.setIndex(i)
        b.linearOptionsBeats.addView(beatView)
      }
    }

    b.linearOptionsBeats.post {
      b.scrollHorizOptionsBeats.centerScrollContentIfNotFullWidth()
    }

    updateBeatControls()
  }

  private fun updateBeatControls() {
    val config = configInternal ?: return
    val b = binding ?: return
    val beatsCount = config.getBeatsCount()
    b.textOptionsBeats.text = activity.resources.getQuantityString(
      R.plurals.options_beats_description, beatsCount, beatsCount
    )
    b.buttonOptionsBeatsAdd.setOnClickListener(this)
    b.buttonOptionsBeatsAdd.isEnabled = beatsCount < Constants.BEATS_MAX
    b.buttonOptionsBeatsRemove.setOnClickListener(this)
    b.buttonOptionsBeatsRemove.isEnabled = beatsCount > 1
  }

  private fun updateSubdivisions(firstSubChanged: Boolean) {
    val config = configInternal ?: return
    val b = binding ?: return
    val subdivisions = config.subdivisions
    val isFirstSubMuted = config.isFirstSubdivisionMuted()

    if (firstSubChanged) {
      val beatView = b.linearOptionsSubs.getChildAt(0) as BeatView
      beatView.setTickType(
        if (isFirstSubMuted) TICK_TYPE.BEAT_SUB_MUTED else TICK_TYPE.BEAT_SUB, true
      )
      return
    }

    val currentSubs = Array(b.linearOptionsSubs.childCount) {
      b.linearOptionsSubs.getChildAt(it).toString()
    }
    if (subdivisions.contentEquals(currentSubs)) {
      return
    } else if (subdivisions.size == currentSubs.size) {
      for (i in subdivisions.indices) {
        val beatView = b.linearOptionsSubs.getChildAt(i) as BeatView
        beatView.setTickType(subdivisions[i], false)
      }
    } else {
      b.linearOptionsSubs.removeAllViews()
      for (i in subdivisions.indices) {
        val beatView = getNewBeatView(true)
        var tickType = subdivisions[i]
        if (i == 0 && tickType == TICK_TYPE.MUTED) {
          tickType = TICK_TYPE.BEAT_SUB
        }
        beatView.setTickType(tickType, false)
        beatView.setIndex(i)
        b.linearOptionsSubs.addView(beatView)
      }
    }

    b.linearOptionsSubs.post {
      b.scrollHorizOptionsSubs.centerScrollContentIfNotFullWidth()
    }

    updateSubControls()
  }

  private fun updateSubControls() {
    val config = configInternal ?: return
    val b = binding ?: return
    val subdivisionsCount = config.getSubdivisionsCount()
    val isSubdivisionActive = config.isSubdivisionActive()
    if (isSubdivisionActive) {
      b.textOptionsSubs.text = activity.resources.getQuantityString(
        R.plurals.options_subdivisions_description, subdivisionsCount, subdivisionsCount
      )
    } else if (config.isFirstSubdivisionMuted()) {
      b.textOptionsSubs.setText(R.string.options_subdivisions_beats_muted)
    } else {
      b.textOptionsSubs.setText(R.string.options_inactive)
    }
    b.buttonOptionsSubsAdd.setOnClickListener(this)
    b.buttonOptionsSubsAdd.isEnabled = subdivisionsCount < Constants.SUBS_MAX
    b.buttonOptionsSubsRemove.setOnClickListener(this)
    b.buttonOptionsSubsRemove.isEnabled = subdivisionsCount > 1
  }

  private fun getNewBeatView(isSubdivision: Boolean): BeatView {
    val beatView = BeatView(activity)
    beatView.setIsSubdivision(isSubdivision)
    beatView.setOnClickListener {
      val config = configInternal ?: return@setOnClickListener
      activity.performHapticClick()

      if (isSubdivision) {
        config.setSubdivision(beatView.index, beatView.nextTickType())
        if (beatView.index == 0) {
          updateBeats(true)
        }
      } else {
        if (config.isFirstSubdivisionMuted()) {
          config.setSubdivision(0, TICK_TYPE.BEAT_SUB)
          updateBeats(true)
          updateSubdivisions(true)
        } else {
          config.setBeat(beatView.index, beatView.nextTickType())
        }
      }
      updateSubControls()
    }
    return beatView
  }

  private fun updateCountIn() {
    val config = configInternal ?: return
    val b = binding ?: return
    val isCountInActive = config.isCountInActive()
    if (this.isCountInActive != isCountInActive) {
      this.isCountInActive = isCountInActive
      onOptionsListener?.onModifiersCountChanged()
    }
    val countIn = config.countIn
    b.sliderOptionsCountIn.removeOnChangeListener(this)
    b.sliderOptionsCountIn.configureSafely(
      0, Constants.COUNT_IN_MAX, 1, countIn
    )
    b.sliderOptionsCountIn.addOnChangeListener(this)
    b.sliderOptionsCountIn.setLabelFormatter { value ->
      activity.resources.getQuantityString(
        R.plurals.options_unit_bars, value.toInt(), value.toInt()
      )
    }
    if (config.isCountInActive()) {
      b.textOptionsCountIn.text = activity.resources.getQuantityString(
        R.plurals.options_count_in_description, countIn, countIn
      )
    } else {
      b.textOptionsCountIn.setText(R.string.options_inactive)
    }

    b.linearOptionsCountInContainer.setBackgroundResource(
      if (editPart) R.drawable.ripple_list_item_bg_segmented_middle
      else R.drawable.ripple_list_item_bg_segmented_first
    )
  }

  private fun updateIncremental() {
    val config = configInternal ?: return
    val b = binding ?: return
    val incrementalAmount = config.incrementalAmount
    val incrementalIncrease = config.incrementalIncrease
    val isIncrementalActive = config.isIncrementalActive()
    if (this.isIncrementalActive != isIncrementalActive) {
      this.isIncrementalActive = isIncrementalActive
      onOptionsListener?.onModifiersCountChanged()
    }
    if (isIncrementalActive) {
      b.textOptionsIncrementalAmount.text = activity.getString(
        if (incrementalIncrease) R.string.options_incremental_amount_increase
        else R.string.options_incremental_amount_decrease,
        incrementalAmount
      )
    } else {
      b.textOptionsIncrementalAmount.setText(R.string.options_inactive)
    }

    val factorAmount = incrementalAmount / ticksMaxPerRange
    val valueFromNewAmount = factorAmount * ticksMaxPerRange
    val valueToNewAmount = minOf(
      valueFromNewAmount + ticksMaxPerRange - 1, Constants.INCREMENTAL_AMOUNT_MAX
    )

    b.buttonOptionsIncrementalAmountDecrease.isEnabled = valueFromNewAmount > 0
    b.buttonOptionsIncrementalAmountDecrease.setOnClickListener(this)
    b.buttonOptionsIncrementalAmountDecrease.setTooltipText(R.string.action_decrease)

    b.buttonOptionsIncrementalAmountIncrease.isEnabled =
      valueToNewAmount < Constants.INCREMENTAL_AMOUNT_MAX
    b.buttonOptionsIncrementalAmountIncrease.setOnClickListener(this)
    b.buttonOptionsIncrementalAmountIncrease.setTooltipText(R.string.action_increase)

    b.sliderOptionsIncrementalAmount.removeOnChangeListener(this)
    b.sliderOptionsIncrementalAmount.configureSafely(
      valueFromNewAmount, valueToNewAmount, 1, incrementalAmount
    )
    b.sliderOptionsIncrementalAmount.addOnChangeListener(this)
    b.sliderOptionsIncrementalAmount.setLabelFormatter { value ->
      activity.getString(R.string.label_bpm_value, value.toInt())
    }

    val visibilityOld = b.linearOptionsIncrementalContainer.visibility
    val visibilityNew = if (isIncrementalActive || !useDialog) View.VISIBLE else View.GONE
    if (visibilityOld != visibilityNew) {
      val transition = AutoTransition().apply {
        duration = Constants.ANIM_DURATION_SHORT
      }
      TransitionManager.beginDelayedTransition(b.linearOptionsContainer, transition)
      b.linearOptionsIncrementalContainer.visibility = visibilityNew
    }

    b.toggleOptionsIncrementalDirection.removeOnButtonCheckedListener(this)
    b.toggleOptionsIncrementalDirection.check(
      if (incrementalIncrease) R.id.button_options_incremental_increase
      else R.id.button_options_incremental_decrease
    )
    b.toggleOptionsIncrementalDirection.addOnButtonCheckedListener(this)
    b.toggleOptionsIncrementalDirection.isEnabled = isIncrementalActive

    val incrementalInterval = config.incrementalInterval
    val incrementalUnit = config.incrementalUnit
    val (intervalResId, checkedId) = when (incrementalUnit) {
      UNIT.SECONDS -> R.plurals.options_incremental_interval_seconds to
          R.id.button_options_incremental_unit_seconds

      UNIT.MINUTES -> R.plurals.options_incremental_interval_minutes to
          R.id.button_options_incremental_unit_minutes

      else -> R.plurals.options_incremental_interval_bars to
          R.id.button_options_incremental_unit_bars
    }
    b.textOptionsIncrementalInterval.text = activity.resources.getQuantityString(
      intervalResId, incrementalInterval, incrementalInterval
    )
    b.textOptionsIncrementalInterval.alpha = if (isIncrementalActive) 1f else 0.5f

    val intervalFactor = (incrementalInterval - 1) / ticksMaxPerRange
    val intervalFromNew = 1 + intervalFactor * ticksMaxPerRange
    val intervalToNew = minOf(
      intervalFromNew + ticksMaxPerRange - 1, Constants.INCREMENTAL_INTERVAL_MAX
    )

    b.buttonOptionsIncrementalIntervalDecrease.isEnabled =
      isIncrementalActive && intervalFromNew > 1
    b.buttonOptionsIncrementalIntervalDecrease.setOnClickListener(this)
    b.buttonOptionsIncrementalIntervalDecrease.setTooltipText(R.string.action_decrease)

    b.buttonOptionsIncrementalIntervalIncrease.isEnabled =
      isIncrementalActive && intervalToNew < Constants.INCREMENTAL_INTERVAL_MAX
    b.buttonOptionsIncrementalIntervalIncrease.setOnClickListener(this)
    b.buttonOptionsIncrementalIntervalIncrease.setTooltipText(R.string.action_increase)

    b.sliderOptionsIncrementalInterval.removeOnChangeListener(this)
    b.sliderOptionsIncrementalInterval.configureSafely(
      intervalFromNew, intervalToNew, 1, incrementalInterval
    )
    b.sliderOptionsIncrementalInterval.addOnChangeListener(this)
    b.sliderOptionsIncrementalInterval.setLabelFormatter { value ->
      val resId = when (incrementalUnit) {
        UNIT.SECONDS -> R.plurals.options_unit_seconds
        UNIT.MINUTES -> R.plurals.options_unit_minutes
        else -> R.plurals.options_unit_bars
      }
      val interval = value.toInt()
      activity.resources.getQuantityString(resId, interval, interval)
    }
    b.sliderOptionsIncrementalInterval.isEnabled = isIncrementalActive

    b.toggleOptionsIncrementalUnit.removeOnButtonCheckedListener(this)
    b.toggleOptionsIncrementalUnit.check(checkedId)
    b.toggleOptionsIncrementalUnit.addOnButtonCheckedListener(this)
    b.toggleOptionsIncrementalUnit.isEnabled = isIncrementalActive

    val incrementalLimit = config.incrementalLimit
    if (incrementalLimit > 0) {
      b.textOptionsIncrementalLimit.text = activity.resources.getString(
        if (incrementalIncrease) R.string.options_incremental_max
        else R.string.options_incremental_min,
        incrementalLimit
      )
    } else {
      b.textOptionsIncrementalLimit.setText(
        if (incrementalIncrease) R.string.options_incremental_no_max
        else R.string.options_incremental_no_min
      )
    }
    b.textOptionsIncrementalLimit.alpha = if (isIncrementalActive) 1f else 0.5f

    val factor = incrementalLimit / ticksMaxPerRange
    val valueFromNew = factor * ticksMaxPerRange
    val valueToNew = minOf(valueFromNew + ticksMaxPerRange - 1, Constants.TEMPO_MAX - 1)

    b.buttonOptionsIncrementalLimitDecrease.isEnabled = isIncrementalActive && valueFromNew > 0
    b.buttonOptionsIncrementalLimitDecrease.setOnClickListener(this)
    b.buttonOptionsIncrementalLimitDecrease.setTooltipText(R.string.action_decrease)

    b.buttonOptionsIncrementalLimitIncrease.isEnabled =
      isIncrementalActive && valueToNew < Constants.TEMPO_MAX - 1
    b.buttonOptionsIncrementalLimitIncrease.setOnClickListener(this)
    b.buttonOptionsIncrementalLimitIncrease.setTooltipText(R.string.action_increase)

    b.sliderOptionsIncrementalLimit.removeOnChangeListener(this)
    b.sliderOptionsIncrementalLimit.configureSafely(
      valueFromNew, valueToNew, 1, incrementalLimit
    )
    b.sliderOptionsIncrementalLimit.addOnChangeListener(this)
    b.sliderOptionsIncrementalLimit.setLabelFormatter { value ->
      activity.getString(R.string.label_bpm_value, value.toInt())
    }
    b.sliderOptionsIncrementalLimit.isEnabled = isIncrementalActive
  }

  private fun updateTimer() {
    val config = configInternal ?: return
    val b = binding ?: return
    val timerDuration = config.timerDuration
    val isTimerActive = config.isTimerActive()
    if (this.isTimerActive != isTimerActive) {
      this.isTimerActive = isTimerActive
      onOptionsListener?.onModifiersCountChanged()
    }
    val timerUnit = config.timerUnit
    val (durationResId, checkedId) = when (timerUnit) {
      UNIT.SECONDS -> R.plurals.options_timer_description_seconds to
          R.id.button_options_timer_unit_seconds

      UNIT.MINUTES -> R.plurals.options_timer_description_minutes to
          R.id.button_options_timer_unit_minutes

      else -> R.plurals.options_timer_description_bars to
          R.id.button_options_timer_unit_bars
    }
    if (isTimerActive) {
      b.textOptionsTimerDuration.text = activity.resources.getQuantityString(
        durationResId, timerDuration, timerDuration
      )
    } else {
      b.textOptionsTimerDuration.setText(R.string.options_inactive)
    }

    val factor = timerDuration / ticksMaxPerRange
    val valueFromNew = factor * ticksMaxPerRange
    val valueToNew = minOf(valueFromNew + ticksMaxPerRange - 1, Constants.TIMER_MAX)

    b.buttonOptionsTimerDecrease.isEnabled = valueFromNew > 0
    b.buttonOptionsTimerDecrease.setOnClickListener(this)
    b.buttonOptionsTimerDecrease.setTooltipText(R.string.action_decrease)

    b.buttonOptionsTimerIncrease.isEnabled = valueToNew < Constants.TIMER_MAX
    b.buttonOptionsTimerIncrease.setOnClickListener(this)
    b.buttonOptionsTimerIncrease.setTooltipText(R.string.action_increase)

    b.sliderOptionsTimerDuration.removeOnChangeListener(this)
    b.sliderOptionsTimerDuration.configureSafely(
      valueFromNew, valueToNew, 1, timerDuration
    )
    b.sliderOptionsTimerDuration.addOnChangeListener(this)
    b.sliderOptionsTimerDuration.setLabelFormatter { value ->
      val resId = when (timerUnit) {
        UNIT.SECONDS -> R.plurals.options_unit_seconds
        UNIT.MINUTES -> R.plurals.options_unit_minutes
        else -> R.plurals.options_unit_bars
      }
      val interval = value.toInt()
      activity.resources.getQuantityString(resId, interval, interval)
    }

    val visibilityOld = b.linearOptionsTimerContainer.visibility
    val visibilityNew = if (isTimerActive || !useDialog) View.VISIBLE else View.GONE
    if (visibilityOld != visibilityNew) {
      val transition = AutoTransition().apply {
        duration = Constants.ANIM_DURATION_SHORT
      }
      TransitionManager.beginDelayedTransition(b.linearOptionsContainer, transition)
      b.linearOptionsTimerContainer.visibility = visibilityNew
    }

    b.toggleOptionsTimerUnit.removeOnButtonCheckedListener(this)
    b.toggleOptionsTimerUnit.check(checkedId)
    b.toggleOptionsTimerUnit.addOnButtonCheckedListener(this)
    b.toggleOptionsTimerUnit.isEnabled = isTimerActive
  }

  private fun updateMute() {
    val config = configInternal ?: return
    val b = binding ?: return
    val mutePlay = config.mutePlay
    val muteMute = config.muteMute
    val muteUnit = config.muteUnit
    val isUnitBeats = muteUnit == UNIT.BEATS
    val muteRandom = config.muteRandom
    val isMuteActive = config.isMuteActive()
    if (this.isMuteActive != isMuteActive) {
      this.isMuteActive = isMuteActive
      onOptionsListener?.onModifiersCountChanged()
    }

    val showPlay = !isUnitBeats
    val visibilityPlayOld = b.linearOptionsMutePlay.visibility
    val visibilityPlayNew = if (showPlay) View.VISIBLE else View.GONE
    val visibilityPlayChanged = visibilityPlayOld != visibilityPlayNew

    val (resIdPlay, resIdLabelPlay) = if (muteUnit == UNIT.SECONDS) {
      R.plurals.options_mute_play_seconds to R.plurals.options_unit_seconds
    } else {
      R.plurals.options_mute_play_bars to R.plurals.options_unit_bars
    }
    if (showPlay) {
      if (isMuteActive) {
        b.textOptionsMutePlay.text = activity.resources.getQuantityString(
          resIdPlay, mutePlay, mutePlay
        )
      } else {
        b.textOptionsMutePlay.setText(R.string.options_inactive)
      }
    }

    b.sliderOptionsMutePlay.removeOnChangeListener(this)
    b.sliderOptionsMutePlay.configureSafely(
      0, Constants.MUTE_PLAY_MAX, 1, mutePlay
    )
    b.sliderOptionsMutePlay.addOnChangeListener(this)
    b.sliderOptionsMutePlay.setLabelFormatter { value ->
      val play = value.toInt()
      activity.resources.getQuantityString(resIdLabelPlay, play, play)
    }

    val showMute = isUnitBeats || isMuteActive || !useDialog
    val visibilityMuteOld = b.linearOptionsMuteMute.visibility
    val visibilityMuteNew = if (showMute) View.VISIBLE else View.GONE
    val visibilityMuteChanged = visibilityMuteOld != visibilityMuteNew

    val (resIdMute, resIdLabelMute) = when (muteUnit) {
      UNIT.SECONDS -> R.plurals.options_mute_mute_seconds to R.plurals.options_unit_seconds
      UNIT.BARS -> R.plurals.options_mute_mute_bars to R.plurals.options_unit_bars
      else -> 0 to R.plurals.options_unit_beats
    }
    if (isUnitBeats && isMuteActive) {
      b.textOptionsMuteMute.text = activity.getString(
        R.string.options_mute_mute_beats, muteMute
      )
    } else if (isUnitBeats) {
      b.textOptionsMuteMute.setText(R.string.options_inactive)
    } else {
      b.textOptionsMuteMute.text = activity.resources.getQuantityString(
        resIdMute, muteMute, muteMute
      )
    }
    b.textOptionsMuteMute.alpha = if (isUnitBeats || isMuteActive) 1f else 0.5f

    b.sliderOptionsMuteMute.removeOnChangeListener(this)
    b.sliderOptionsMuteMute.configureSafely(
      if (isUnitBeats) Constants.MUTE_MUTE_MIN_BEATS else Constants.MUTE_MUTE_MIN,
      if (isUnitBeats) Constants.MUTE_MUTE_MAX_BEATS else Constants.MUTE_MUTE_MAX,
      if (isUnitBeats) Constants.MUTE_MUTE_STEP_SIZE_BEATS
      else Constants.MUTE_MUTE_STEP_SIZE,
      muteMute
    )
    b.sliderOptionsMuteMute.addOnChangeListener(this)
    b.sliderOptionsMuteMute.setLabelFormatter { value ->
      val mute = value.toInt()
      if (isUnitBeats) {
        activity.getString(R.string.options_mute_mute_beats, mute)
      } else {
        activity.resources.getQuantityString(resIdLabelMute, mute, mute)
      }
    }
    b.sliderOptionsMuteMute.isEnabled = isUnitBeats || isMuteActive

    val showUnit = isMuteActive || !useDialog
    val visibilityUnitOld = b.scrollHorizOptionsMuteUnit.visibility
    val visibilityUnitNew = if (showUnit) View.VISIBLE else View.GONE
    val visibleUnitChanged = visibilityUnitOld != visibilityUnitNew

    val checkedId = when (muteUnit) {
      UNIT.SECONDS -> R.id.button_options_mute_unit_seconds
      UNIT.BARS -> R.id.button_options_mute_unit_bars
      else -> R.id.button_options_mute_unit_beats
    }
    b.toggleOptionsMuteUnit.removeOnButtonCheckedListener(this)
    b.toggleOptionsMuteUnit.check(checkedId)
    b.toggleOptionsMuteUnit.addOnButtonCheckedListener(this)
    b.toggleOptionsMuteUnit.isEnabled = isMuteActive

    val showRandom = !isUnitBeats && (isMuteActive || !useDialog)
    val visibilityRandomOld = b.linearOptionsMuteRandom.visibility
    val visibilityRandomNew = if (showRandom) View.VISIBLE else View.GONE
    val visibilityRandomChanged = visibilityRandomOld != visibilityRandomNew

    b.linearOptionsMuteRandom.setOnClickListener(this)
    b.linearOptionsMuteRandom.isEnabled = isMuteActive
    b.linearOptionsMuteRandom.setBackgroundResource(
      if (useDialog) R.drawable.ripple_list_item_surface_bright
      else R.drawable.ripple_list_item_bg
    )
    b.textOptionsMuteRandom.alpha = if (isMuteActive) 1f else 0.5f
    b.switchOptionsMuteRandom.setOnCheckedChangeListener(null)
    b.switchOptionsMuteRandom.isChecked = muteRandom
    b.switchOptionsMuteRandom.setOnCheckedChangeListener { _, isChecked ->
      activity.performHapticClick()
      if (editPart && configInternal != null) {
        configInternal?.muteRandom = isChecked
      } else {
        activity.metronomeEngine?.let { engine ->
          engine.setMuteRandom(isChecked)
          engine.maybeUpdateDefaultSong()
        }
      }
      updateMute()
    }
    b.switchOptionsMuteRandom.isEnabled = isMuteActive

    if (visibilityPlayChanged || visibilityMuteChanged
      || visibleUnitChanged || visibilityRandomChanged
    ) {
      val transition = AutoTransition().apply {
        duration = Constants.ANIM_DURATION_SHORT
      }
      TransitionManager.beginDelayedTransition(b.linearOptionsContainer, transition)
    }

    if (visibilityPlayChanged) b.linearOptionsMutePlay.visibility = visibilityPlayNew
    if (visibilityMuteChanged) b.linearOptionsMuteMute.visibility = visibilityMuteNew
    if (visibleUnitChanged) b.scrollHorizOptionsMuteUnit.visibility = visibilityUnitNew
    if (visibilityRandomChanged) b.linearOptionsMuteRandom.visibility = visibilityRandomNew
  }

  fun updateSwing() {
    val config = configInternal ?: return
    val b = binding ?: return
    val isSwingActive = config.isSwingActive()

    b.textOptionsSwing.setText(
      if (isSwingActive) R.string.options_swing_description else R.string.options_inactive
    )

    b.toggleOptionsSwing.removeOnButtonCheckedListener(this)
    when {
      config.isSwing3() -> b.toggleOptionsSwing.check(R.id.button_options_swing_3)
      config.isSwing5() -> b.toggleOptionsSwing.check(R.id.button_options_swing_5)
      config.isSwing7() -> b.toggleOptionsSwing.check(R.id.button_options_swing_7)
      else -> b.toggleOptionsSwing.clearChecked()
    }
    b.toggleOptionsSwing.addOnButtonCheckedListener(this)
  }

  private fun updatePolyrhythm() {
    val config = configInternal ?: return
    val b = binding ?: return
    val usePolyrhythm = config.usePolyrhythm
    if (this.usePolyrhythm != usePolyrhythm) {
      this.usePolyrhythm = usePolyrhythm
      onOptionsListener?.onModifiersCountChanged()
    }

    b.linearOptionsPolyrhythm.setOnClickListener(this)
    b.switchOptionsPolyrhythm.setOnCheckedChangeListener(null)
    b.switchOptionsPolyrhythm.isChecked = usePolyrhythm
    b.switchOptionsPolyrhythm.setOnCheckedChangeListener { _, isChecked ->
      activity.performHapticClick()
      if (editPart && configInternal != null) {
        configInternal?.usePolyrhythm = isChecked
      } else {
        activity.metronomeEngine?.let { engine ->
          engine.setUsePolyrhythm(isChecked)
          engine.maybeUpdateDefaultSong()
          engine.restartIfPlaying(false)
        }
      }
      updatePolyrhythm()
      if (!editPart) {
        onOptionsListener?.onBeatsChanged()
      }
      updateBeats(true)
    }
  }

  override fun onClick(v: View) {
    val config = configInternal ?: return
    val metronomeEngine = activity.metronomeEngine ?: return
    val b = binding ?: return
    val id = v.id
    when (id) {
      R.id.linear_options_use_current_config -> {
        activity.performHapticClick()
        this.config = MetronomeConfig(metronomeEngine.config)
        update()
      }

      R.id.button_options_tempo_decrease -> {
        activity.performHapticClick()
        val valueFrom = b.sliderOptionsTempo.valueFrom.toInt()
        val valueTo = b.sliderOptionsTempo.valueTo.toInt()
        val range = valueTo - valueFrom
        val decreasedTempo = config.tempo - range - 1
        if (editPart) {
          config.tempo = decreasedTempo
        } else {
          metronomeEngine.setTempo(decreasedTempo)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateTempo()
        b.buttonOptionsTempoDecrease.icon.start()
      }

      R.id.button_options_tempo_increase -> {
        activity.performHapticClick()
        val valueFrom = b.sliderOptionsTempo.valueFrom.toInt()
        val valueTo = b.sliderOptionsTempo.valueTo.toInt()
        val range = valueTo - valueFrom
        val increasedTempo = config.tempo + range + 1
        if (editPart) {
          config.tempo = increasedTempo
        } else {
          metronomeEngine.setTempo(increasedTempo)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateTempo()
        b.buttonOptionsTempoIncrease.icon.start()
      }

      R.id.button_options_beats_add -> {
        activity.performHapticClick()
        b.buttonOptionsBeatsAdd.icon.start()
        if (config.addBeat()) {
          val transition = AutoTransition().apply {
            duration = Constants.ANIM_DURATION_SHORT
          }
          TransitionManager.beginDelayedTransition(b.linearOptionsBeats, transition)

          b.scrollHorizOptionsBeats.centerScrollContentIfNotFullWidth(
            activity.dpToPx(48f)
          )

          val beatView = getNewBeatView(false)
          val isFirstSubMuted = config.isFirstSubdivisionMuted()
          beatView.setTickType(
            if (isFirstSubMuted) TICK_TYPE.MUTED else TICK_TYPE.NORMAL, false
          )
          beatView.setIndex(b.linearOptionsBeats.childCount)
          b.linearOptionsBeats.addView(beatView)
          updateBeatControls()
        }
      }

      R.id.button_options_beats_remove -> {
        activity.performHapticClick()
        b.buttonOptionsBeatsRemove.icon.start()
        if (config.removeBeat()) {
          val transition = ChangeBounds().apply {
            duration = Constants.ANIM_DURATION_SHORT
          }
          TransitionManager.beginDelayedTransition(b.linearOptionsBeats, transition)

          b.scrollHorizOptionsBeats.centerScrollContentIfNotFullWidth(
            -activity.dpToPx(48f)
          )

          b.linearOptionsBeats.removeViewAt(b.linearOptionsBeats.childCount - 1)
          updateBeatControls()
        }
      }

      R.id.button_options_subs_add -> {
        activity.performHapticClick()
        b.buttonOptionsSubsAdd.icon.start()
        if (config.addSubdivision()) {
          val transition = AutoTransition().apply {
            duration = Constants.ANIM_DURATION_SHORT
          }
          TransitionManager.beginDelayedTransition(b.linearOptionsSubs, transition)

          b.scrollHorizOptionsSubs.centerScrollContentIfNotFullWidth(
            activity.dpToPx(48f)
          )

          val beatView = getNewBeatView(true)
          beatView.setIndex(b.linearOptionsSubs.childCount)
          b.linearOptionsSubs.addView(beatView)
          updateSubControls()
        }
      }

      R.id.button_options_subs_remove -> {
        activity.performHapticClick()
        b.buttonOptionsSubsRemove.icon.start()
        if (config.removeSubdivision()) {
          val transition = ChangeBounds().apply {
            duration = Constants.ANIM_DURATION_SHORT
          }
          TransitionManager.beginDelayedTransition(b.linearOptionsSubs, transition)

          b.scrollHorizOptionsSubs.centerScrollContentIfNotFullWidth(
            -activity.dpToPx(48f)
          )

          b.linearOptionsSubs.removeViewAt(b.linearOptionsSubs.childCount - 1)
          updateSubControls()
        }
      }

      R.id.button_options_incremental_amount_decrease -> {
        activity.performHapticClick()
        val decreasedAmount = config.incrementalAmount - ticksMaxPerRange
        if (editPart) {
          config.incrementalAmount = decreasedAmount
        } else {
          metronomeEngine.setIncrementalAmount(decreasedAmount)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
        b.buttonOptionsIncrementalAmountDecrease.icon.start()
      }

      R.id.button_options_incremental_amount_increase -> {
        activity.performHapticClick()
        val increasedAmount = config.incrementalAmount + ticksMaxPerRange
        if (editPart) {
          config.incrementalAmount = increasedAmount
        } else {
          metronomeEngine.setIncrementalAmount(increasedAmount)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
        b.buttonOptionsIncrementalAmountIncrease.icon.start()
      }

      R.id.button_options_incremental_interval_decrease -> {
        activity.performHapticClick()
        val decreasedInterval = config.incrementalInterval - ticksMaxPerRange
        if (editPart) {
          config.incrementalInterval = decreasedInterval
        } else {
          metronomeEngine.setIncrementalInterval(decreasedInterval)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
        b.buttonOptionsIncrementalIntervalDecrease.icon.start()
      }

      R.id.button_options_incremental_interval_increase -> {
        activity.performHapticClick()
        val increasedInterval = config.incrementalInterval + ticksMaxPerRange
        if (editPart) {
          config.incrementalInterval = increasedInterval
        } else {
          metronomeEngine.setIncrementalInterval(increasedInterval)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
        b.buttonOptionsIncrementalIntervalIncrease.icon.start()
      }

      R.id.button_options_incremental_limit_decrease -> {
        activity.performHapticClick()
        val decreasedLimit = config.incrementalLimit - ticksMaxPerRange
        if (editPart) {
          config.incrementalLimit = decreasedLimit
        } else {
          metronomeEngine.setIncrementalLimit(decreasedLimit)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
        b.buttonOptionsIncrementalLimitDecrease.icon.start()
      }

      R.id.button_options_incremental_limit_increase -> {
        activity.performHapticClick()
        val increasedLimit = config.incrementalLimit + ticksMaxPerRange
        if (editPart) {
          config.incrementalLimit = increasedLimit
        } else {
          metronomeEngine.setIncrementalLimit(increasedLimit)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
        b.buttonOptionsIncrementalLimitIncrease.icon.start()
      }

      R.id.button_options_timer_decrease -> {
        activity.performHapticClick()
        val decreasedDuration = config.timerDuration - ticksMaxPerRange
        if (editPart) {
          config.timerDuration = decreasedDuration
        } else {
          metronomeEngine.setTimerDuration(decreasedDuration)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateTimer()
        if (!editPart) onOptionsListener?.onTimerChanged()
        b.buttonOptionsTimerDecrease.icon.start()
      }

      R.id.button_options_timer_increase -> {
        activity.performHapticClick()
        val increasedDuration = config.timerDuration + ticksMaxPerRange
        if (editPart) {
          config.timerDuration = increasedDuration
        } else {
          metronomeEngine.setTimerDuration(increasedDuration)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateTimer()
        if (!editPart) onOptionsListener?.onTimerChanged()
        b.buttonOptionsTimerIncrease.icon.start()
      }

      R.id.linear_options_mute_random -> b.switchOptionsMuteRandom.toggle()
      R.id.linear_options_polyrhythm -> b.switchOptionsPolyrhythm.toggle()
    }
  }

  override fun onButtonChecked(
    group: MaterialButtonToggleGroup,
    checkedId: Int,
    isChecked: Boolean
  ) {
    val config = configInternal ?: return
    val metronomeEngine = activity.metronomeEngine ?: return
    if (!isChecked) return
    activity.performHapticClick()
    val groupId = group.id
    when (groupId) {
      R.id.toggle_options_incremental_direction -> {
        val incrementalIncrease = checkedId == R.id.button_options_incremental_increase
        if (editPart) {
          config.incrementalIncrease = incrementalIncrease
        } else {
          metronomeEngine.setIncrementalIncrease(incrementalIncrease)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
      }

      R.id.toggle_options_incremental_unit -> {
        val unit = when (checkedId) {
          R.id.button_options_incremental_unit_seconds -> UNIT.SECONDS
          R.id.button_options_incremental_unit_minutes -> UNIT.MINUTES
          else -> UNIT.BARS
        }
        if (editPart) {
          config.incrementalUnit = unit
        } else {
          metronomeEngine.setIncrementalUnit(unit)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateIncremental()
      }

      R.id.toggle_options_timer_unit -> {
        val unit = when (checkedId) {
          R.id.button_options_timer_unit_seconds -> UNIT.SECONDS
          R.id.button_options_timer_unit_minutes -> UNIT.MINUTES
          else -> UNIT.BARS
        }
        if (editPart) {
          config.timerUnit = unit
        } else {
          metronomeEngine.setTimerUnit(unit)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateTimer()
        if (!editPart) onOptionsListener?.onTimerChanged()
      }

      R.id.toggle_options_mute_unit -> {
        val unit = when (checkedId) {
          R.id.button_options_mute_unit_bars -> UNIT.BARS
          R.id.button_options_mute_unit_seconds -> UNIT.SECONDS
          else -> UNIT.BEATS
        }
        if (editPart) {
          config.muteUnit = unit
        } else {
          metronomeEngine.setMuteUnit(unit)
          metronomeEngine.maybeUpdateDefaultSong()
        }
        updateMute()
      }

      R.id.toggle_options_swing -> {
        when (checkedId) {
          R.id.button_options_swing_3 -> {
            if (editPart) {
              config.setSwing3()
            } else {
              metronomeEngine.setSwing3()
              if (config.isTimerActive() && config.timerUnit == UNIT.BARS) {
                metronomeEngine.restartIfPlaying(false)
              }
              metronomeEngine.maybeUpdateDefaultSong()
            }
          }

          R.id.button_options_swing_5 -> {
            if (editPart) {
              config.setSwing5()
            } else {
              metronomeEngine.setSwing5()
              if (config.isTimerActive() && config.timerUnit == UNIT.BARS) {
                metronomeEngine.restartIfPlaying(false)
              }
              metronomeEngine.maybeUpdateDefaultSong()
            }
          }

          R.id.button_options_swing_7 -> {
            if (editPart) {
              config.setSwing7()
            } else {
              metronomeEngine.setSwing7()
              if (config.isTimerActive() && config.timerUnit == UNIT.BARS) {
                metronomeEngine.restartIfPlaying(false)
              }
              metronomeEngine.maybeUpdateDefaultSong()
            }
          }
        }
        updateSwing()
        updateSubdivisions(false)
        if (!editPart) onOptionsListener?.onSubsChanged()
      }
    }
  }

  override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
    val config = configInternal ?: return
    val metronomeEngine = activity.metronomeEngine ?: return
    if (!fromUser) return
    val id = slider.id
    when (id) {
      R.id.slider_options_tempo -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.tempo = value.toInt() else metronomeEngine.setTempo(value.toInt())
        updateTempo()
      }

      R.id.slider_options_count_in -> {
        activity.performHapticSegmentTick(slider, false)
        if (editPart) config.countIn = value.toInt() else metronomeEngine.setCountIn(value.toInt())
        updateCountIn()
      }

      R.id.slider_options_incremental_amount -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.incrementalAmount =
          value.toInt() else metronomeEngine.setIncrementalAmount(value.toInt())
        updateIncremental()
      }

      R.id.slider_options_incremental_interval -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.incrementalInterval =
          value.toInt() else metronomeEngine.setIncrementalInterval(value.toInt())
        updateIncremental()
      }

      R.id.slider_options_incremental_limit -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.incrementalLimit =
          value.toInt() else metronomeEngine.setIncrementalLimit(value.toInt())
        updateIncremental()
      }

      R.id.slider_options_timer_duration -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.timerDuration = value.toInt() else metronomeEngine.setTimerDuration(
          value.toInt()
        )
        updateTimer()
      }

      R.id.slider_options_mute_play -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.mutePlay =
          value.toInt() else metronomeEngine.setMutePlay(value.toInt())
        updateMute()
      }

      R.id.slider_options_mute_mute -> {
        activity.performHapticSegmentTick(slider, true)
        if (editPart) config.muteMute =
          value.toInt() else metronomeEngine.setMuteMute(value.toInt())
        updateMute()
      }
    }
  }

  override fun onStartTrackingTouch(slider: Slider) {
    val stopMetronome = slider.id != R.id.slider_options_mute_mute
    if (activity.metronomeEngine != null && stopMetronome) {
      activity.metronomeEngine?.savePlayingState()
      activity.metronomeEngine?.stop()
    }
  }

  override fun onStopTrackingTouch(slider: Slider) {
    val metronomeEngine = activity.metronomeEngine ?: return
    val stopMetronome = slider.id != R.id.slider_options_mute_mute
    if (stopMetronome) {
      metronomeEngine.restorePlayingState()
    }
    metronomeEngine.maybeUpdateDefaultSong()

    if (slider.id == R.id.slider_options_timer_duration) {
      onOptionsListener?.onTimerChanged()
    }
  }

  private val configInternal: MetronomeConfig?
    get() = if (editPart) config else activity.metronomeEngine?.config

  interface OnPartEditListener {
    fun onPartAdded(part: Part)
    fun onPartUpdated(part: Part)
  }

  interface OnOptionsListener {
    fun onModifiersCountChanged()
    fun onTimerChanged()
    fun onBeatsChanged()
    fun onSubsChanged()
  }

  companion object {
    private val TAG = OptionsUtil::class.java.simpleName
    private const val PART = "part_dialog"
    private const val IS_NEW = "new_part_dialog"
  }
}
