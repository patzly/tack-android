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

package xyz.zedler.patrick.tack.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.motion.MotionUtils
import com.google.android.material.slider.Slider
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.databinding.ViewTimerBinding
import xyz.zedler.patrick.tack.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.model.MetronomeConfig
import xyz.zedler.patrick.tack.util.configureSafely
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.dpFromPx
import kotlin.math.abs
import kotlin.math.ceil

class TimerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  private val binding = ViewTimerBinding.inflate(
    LayoutInflater.from(context), this, true
  )
  private val sliderHeightExpanded = context.dpToPx(48f)
  private val exclusionRects = mutableListOf<Rect>()
  private val exclusionRect = Rect()

  private var activity: MainActivity? = null
  private var listener: TimerListener? = null
  private var progressAnimator: ValueAnimator? = null
  private var progressTransitionAnimator: ValueAnimator? = null
  private var displayHeightExpanded = 0
  private var timerExpandFraction = 0f
  private var elapsedExpandFraction = 0f
  private var timerExpanded = false
  private var elapsedExpanded = false
  private var changeHeightOfChips = false
  private var springAnimationTimerExpand: SpringAnimation? = null
  private var springAnimationElapsedExpand: SpringAnimation? = null

  init {
    binding.sliderTimer.addOnChangeListener { _, value, fromUser ->
      val engine = getMetronomeEngine()
      if (!fromUser || engine == null) return@addOnChangeListener

      val positions = engine.config.timerDuration
      val timerPositionCurrent = (engine.getTimerProgress() * positions).toInt()
      val fraction = value / binding.sliderTimer.valueTo
      val timerPositionNew = (fraction * positions).toInt()

      if (timerPositionCurrent != timerPositionNew &&
        timerPositionCurrent < positions &&
        timerPositionNew < positions
      ) {
        activity?.performHapticSegmentTick(binding.sliderTimer, false)
      }
      engine.updateTimerHandler(fraction, true)
      updateDisplay()
    }

    binding.sliderTimer.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
        getMetronomeEngine()?.apply {
          savePlayingState()
          stop()
        }
      }

      override fun onStopTrackingTouch(slider: Slider) {
        getMetronomeEngine()?.restorePlayingState()
      }
    })

    binding.chipTimerCurrent.frameChipNumbersContainer.setOnClickListener {
      listener?.onCurrentTimeClick()
    }
    binding.chipTimerElapsed.frameChipNumbersContainer.setOnClickListener {
      listener?.onElapsedTimeClick()
    }
    binding.chipTimerTotal.frameChipNumbersContainer.setOnClickListener {
      listener?.onTotalTimeClick()
    }
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    binding.sliderTimer.getHitRect(exclusionRect)
    exclusionRects.clear()
    exclusionRects.add(exclusionRect)
    ViewCompat.setSystemGestureExclusionRects(this, exclusionRects)
  }

  fun setMainActivity(activity: MainActivity) {
    this.activity = activity
  }

  fun setListener(listener: TimerListener) {
    this.listener = listener
  }

  fun setChangeHeightOfChips(change: Boolean) {
    changeHeightOfChips = change
  }

  fun setBigText(bigText: Boolean) {
    activity?.let { act ->
      if (bigText) {
        val typeface = ResourcesCompat.getFont(act, R.font.google_sans_flex_regular)
        listOf(
          binding.chipTimerCurrent.textChipNumbers,
          binding.chipTimerElapsed.textChipNumbers,
          binding.chipTimerTotal.textChipNumbers
        ).forEach {
          it.textSize = 28f
          it.typeface = typeface
        }
      } else {
        binding.chipTimerCurrent.imageChipNumbers.setImageResource(R.drawable.ic_rounded_timer_anim)
        binding.chipTimerCurrent.imageChipNumbers.visibility = View.VISIBLE
        binding.chipTimerElapsed.imageChipNumbers.setImageResource(
          R.drawable.ic_rounded_schedule_anim
        )
        binding.chipTimerElapsed.imageChipNumbers.visibility = View.VISIBLE
      }
    }
  }

  fun measureControls() {
    binding.linearTimerContainer.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          val width = binding.sliderTimer.width - binding.sliderTimer.trackSidePadding * 2
          val valueFrom = binding.sliderTimer.valueFrom
          val valueTo = valueFrom.coerceAtLeast(width.toFloat())
          if (valueFrom < valueTo) {
            binding.sliderTimer.valueTo = valueTo
          }
          binding.sliderTimer.configureSafely(
            binding.sliderTimer.valueFrom.toInt(),
            valueTo.toInt(),
            0,
            binding.sliderTimer.value.toInt()
          )
          displayHeightExpanded = binding.frameTimerDisplayContainer.height
          val engine = getMetronomeEngine()
          updateControls(
            false,
            engine != null && engine.isPlaying() && engine.config.isTimerActive(),
            true
          )
          if (binding.linearTimerContainer.viewTreeObserver.isAlive) {
            binding.linearTimerContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      }
    )
  }

  fun updateControls(
    animateVisibility: Boolean,
    animateProgress: Boolean,
    withTransition: Boolean
  ) {
    val metronomeEngine = activity?.metronomeEngine
    val metronomeConfig = metronomeEngine?.config ?: MetronomeConfig()
    val isPlaying = metronomeEngine?.isPlaying() == true
    val isTimerActive = metronomeConfig.isTimerActive()
    setTimerExpanded(isTimerActive, animateVisibility)

    var tickCount = metronomeConfig.timerDuration
    var tickSpacingPx = (binding.sliderTimer.valueTo / tickCount).toInt()
    var tickSpacingDp = context.dpFromPx(tickSpacingPx.toFloat())
    if (tickSpacingDp < 16) {
      tickCount = ceil(tickCount / 10f).toInt()
      tickSpacingPx = (binding.sliderTimer.valueTo / tickCount).toInt()
      tickSpacingDp = context.dpFromPx(tickSpacingPx.toFloat())
      if (tickSpacingDp < 16) {
        tickCount = ceil(tickCount / 10f).toInt()
      }
    }
    binding.sliderTimer.setContinuousModeTickCount(tickCount + 1)

    if (metronomeEngine == null) return

    if (isPlaying && isTimerActive && !metronomeEngine.isCountingIn()) {
      if (withTransition) {
        val timerInterval = metronomeEngine.getTimerInterval()
        var fraction = Constants.ANIM_DURATION_LONG.toFloat() / timerInterval
        fraction += metronomeEngine.getTimerProgress()
        startProgressTransition(fraction)
      }
      updateProgress(
        1f,
        metronomeEngine.getTimerIntervalRemaining(),
        animateProgress,
        true
      )
    } else {
      val timerProgress = metronomeEngine.getTimerProgress()
      if (animateProgress) {
        startProgressTransition(timerProgress)
      } else {
        updateProgress(timerProgress, 0, false, false)
      }
    }
    updateDisplay()
  }

  fun updateDisplay() {
    val engine = getMetronomeEngine() ?: return
    val totalTime = engine.getTotalTimeString()
    if (totalTime.isNotEmpty()) {
      binding.chipTimerTotal.textChipNumbers.text = totalTime
    }
    val currentTime = engine.getCurrentTimerString()
    if (currentTime.isNotEmpty()) {
      binding.chipTimerCurrent.textChipNumbers.text = currentTime
    }

    setElapsedExpanded(engine.isElapsedActive(), false)
    binding.chipTimerElapsed.textChipNumbers.text = engine.getElapsedTimeString()
  }

  private fun updateProgress(fraction: Float, duration: Long, animated: Boolean, linear: Boolean) {
    stopProgress()
    val max = binding.sliderTimer.valueTo.toInt()
    if (animated) {
      val engine = getMetronomeEngine() ?: return
      progressAnimator = ValueAnimator.ofFloat(
        engine.getTimerProgress(), fraction
      ).apply {
        addUpdateListener {
          if (progressTransitionAnimator != null) return@addUpdateListener
          binding.sliderTimer.configureSafely(
            binding.sliderTimer.valueFrom.toInt(),
            binding.sliderTimer.valueTo.toInt(),
            0,
            ((it.animatedValue as Float) * max).toInt()
          )
        }
        interpolator = if (linear) LinearInterpolator() else FastOutSlowInInterpolator()
        this.duration = duration
        start()
      }
    } else {
      binding.sliderTimer.configureSafely(
        binding.sliderTimer.valueFrom.toInt(),
        binding.sliderTimer.valueTo.toInt(),
        0,
        (fraction * max).toInt()
      )
    }
  }

  fun stopProgress() {
    progressAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      removeAllListeners()
      cancel()
    }
    progressAnimator = null
  }

  private fun startProgressTransition(fractionTo: Float) {
    val engine = getMetronomeEngine() ?: return
    val current = binding.sliderTimer.value
    val max = binding.sliderTimer.valueTo
    val currentFraction = current / max
    val currentFractionPx = (currentFraction * binding.sliderTimer.valueTo).toInt()
    val currentFractionDp = context.dpFromPx(currentFractionPx.toFloat())
    val currentProgress = engine.getTimerProgress()
    val currentProgressPx = (currentProgress * binding.sliderTimer.valueTo).toInt()
    val currentProgressDp = context.dpFromPx(currentProgressPx.toFloat())
    if (abs(currentFractionDp - currentProgressDp) < 2) return

    progressTransitionAnimator = ValueAnimator.ofFloat(
      currentFraction, fractionTo
    ).apply {
      addUpdateListener {
        val value = ((it.animatedValue as Float) * max).toInt()
        binding.sliderTimer.configureSafely(
          binding.sliderTimer.valueFrom.toInt(),
          binding.sliderTimer.valueTo.toInt(),
          0,
          value
        )
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          stopProgressTransition()
        }
      })
      interpolator = FastOutSlowInInterpolator()
      duration = Constants.ANIM_DURATION_LONG
      start()
    }
  }

  fun stopProgressTransition() {
    progressTransitionAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      removeAllListeners()
      cancel()
    }
    progressTransitionAnimator = null
  }

  @SuppressLint("PrivateResource")
  private fun setTimerExpanded(expanded: Boolean, animated: Boolean) {
    this.timerExpanded = expanded
    springAnimationTimerExpand?.cancel()
    if (animated) {
      binding.sliderTimer.visibility = VISIBLE
      binding.chipTimerCurrent.frameChipNumbersContainer.visibility = VISIBLE
      binding.chipTimerTotal.frameChipNumbersContainer.visibility = VISIBLE
      if (springAnimationTimerExpand == null) {
        springAnimationTimerExpand = SpringAnimation(
          this, TIMER_EXPAND_FRACTION
        ).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringDefaultSpatial,
            R.style.Motion_Material3_Spring_Standard_Default_Spatial
          ).apply {
            if (TEST_ANIMATIONS) {
              stiffness = 20f
              dampingRatio = 0.9f
            }
          }
          minimumVisibleChange = 0.01f
          addEndListener { _, canceled, _, _ ->
            if (!canceled) setTimerExpandAnimationEndState()
          }
        }
      }
      springAnimationTimerExpand?.animateToFinalPosition(if (expanded) 1f else 0f)
    } else {
      setTimerExpandFraction(if (expanded) 1f else 0f)
      setTimerExpandAnimationEndState()
    }
  }

  private fun setTimerExpandAnimationEndState() {
    binding.sliderTimer.alpha = if (timerExpanded) 1f else 0f
    binding.sliderTimer.visibility = if (timerExpanded) VISIBLE else GONE
    binding.chipTimerCurrent.frameChipNumbersContainer.alpha = if (timerExpanded) 1f else 0f
    binding.chipTimerCurrent.frameChipNumbersContainer.visibility =
      if (timerExpanded) VISIBLE else INVISIBLE
    binding.chipTimerCurrent.frameChipNumbersContainer.isClickable = timerExpanded
    binding.chipTimerTotal.frameChipNumbersContainer.alpha = if (timerExpanded) 1f else 0f
    binding.chipTimerTotal.frameChipNumbersContainer.visibility =
      if (timerExpanded) VISIBLE else INVISIBLE
    binding.chipTimerTotal.frameChipNumbersContainer.isClickable = timerExpanded
  }

  fun getTimerExpandFraction(): Float = timerExpandFraction

  fun setTimerExpandFraction(fraction: Float) {
    timerExpandFraction = fraction

    binding.sliderTimer.alpha = fraction
    binding.sliderTimer.pivotY = 0f
    binding.sliderTimer.scaleY = fraction
    binding.frameTimerSliderContainer.layoutParams =
      binding.frameTimerSliderContainer.layoutParams.apply {
        height = (sliderHeightExpanded * fraction).toInt()
      }

    binding.chipTimerCurrent.frameChipNumbersContainer.alpha = fraction
    if (changeHeightOfChips) {
      binding.chipTimerCurrent.frameChipNumbersContainer.pivotY = 0f
      binding.chipTimerCurrent.frameChipNumbersContainer.scaleY = fraction
      binding.chipTimerCurrent.frameChipNumbersContainer.layoutParams =
        binding.chipTimerCurrent.frameChipNumbersContainer.layoutParams.apply {
          height = (displayHeightExpanded * fraction).toInt()
        }
    }

    binding.chipTimerTotal.frameChipNumbersContainer.alpha = fraction
    if (changeHeightOfChips) {
      binding.chipTimerTotal.frameChipNumbersContainer.pivotY = 0f
      binding.chipTimerTotal.frameChipNumbersContainer.scaleY = fraction
      binding.chipTimerTotal.frameChipNumbersContainer.layoutParams =
        binding.chipTimerTotal.frameChipNumbersContainer.layoutParams.apply {
          height = (displayHeightExpanded * fraction).toInt()
        }
    }

    listener?.onHeightChanged()
  }

  fun getSliderHeightExpanded(): Int = sliderHeightExpanded

  fun getMaxHeight(): Int = sliderHeightExpanded + displayHeightExpanded

  fun getTargetHeight(): Int {
    var h = 0
    if (timerExpanded) {
      h += sliderHeightExpanded
      h += displayHeightExpanded
    }
    if (!timerExpanded && elapsedExpanded) {
      h += displayHeightExpanded
    }
    return h
  }

  @SuppressLint("PrivateResource")
  private fun setElapsedExpanded(expanded: Boolean, animated: Boolean) {
    this.elapsedExpanded = expanded
    springAnimationElapsedExpand?.cancel()
    if (animated) {
      binding.chipTimerElapsed.frameChipNumbersContainer.visibility = VISIBLE
      if (springAnimationElapsedExpand == null) {
        springAnimationElapsedExpand = SpringAnimation(
          this, ELAPSED_EXPAND_FRACTION
        ).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringDefaultSpatial,
            R.style.Motion_Material3_Spring_Standard_Default_Spatial
          ).apply {
            if (TEST_ANIMATIONS) {
              stiffness = 30f
              dampingRatio = 0.9f
            }
          }
          minimumVisibleChange = 0.01f
          addEndListener { _, canceled, _, _ ->
            if (!canceled) setElapsedExpandAnimationEndState()
          }
        }
      }
      springAnimationElapsedExpand?.animateToFinalPosition(if (expanded) 1f else 0f)
    } else {
      setElapsedExpandFraction(if (expanded) 1f else 0f)
      setElapsedExpandAnimationEndState()
    }
  }

  private fun setElapsedExpandAnimationEndState() {
    binding.chipTimerElapsed.frameChipNumbersContainer.alpha = if (elapsedExpanded) 1f else 0f
    binding.chipTimerElapsed.frameChipNumbersContainer.visibility =
      if (elapsedExpanded) VISIBLE else INVISIBLE
    binding.chipTimerElapsed.frameChipNumbersContainer.isClickable = elapsedExpanded
  }

  fun getElapsedExpandFraction(): Float = elapsedExpandFraction

  fun setElapsedExpandFraction(fraction: Float) {
    elapsedExpandFraction = fraction

    binding.chipTimerElapsed.frameChipNumbersContainer.alpha = fraction
    if (changeHeightOfChips) {
      binding.chipTimerElapsed.frameChipNumbersContainer.pivotY = 0f
      binding.chipTimerElapsed.frameChipNumbersContainer.scaleY = fraction
      binding.chipTimerElapsed.frameChipNumbersContainer.layoutParams =
        binding.chipTimerElapsed.frameChipNumbersContainer.layoutParams.apply {
          height = (displayHeightExpanded * fraction).toInt()
        }
    }

    listener?.onHeightChanged()
  }

  fun getDisplayHeightExpanded(): Int = displayHeightExpanded

  private fun getMetronomeEngine(): MetronomeEngine? = activity?.metronomeEngine

  interface TimerListener {
    fun onCurrentTimeClick()
    fun onElapsedTimeClick()
    fun onTotalTimeClick()
    fun onHeightChanged()
  }

  companion object {
    private const val TEST_ANIMATIONS = false

    private val TIMER_EXPAND_FRACTION =
      object : FloatPropertyCompat<TimerView>("timerExpandFraction") {
        override fun getValue(delegate: TimerView): Float = delegate.getTimerExpandFraction()
        override fun setValue(delegate: TimerView, value: Float) =
          delegate.setTimerExpandFraction(value)
      }
    private val ELAPSED_EXPAND_FRACTION =
      object : FloatPropertyCompat<TimerView>("elapsedExpandFraction") {
        override fun getValue(delegate: TimerView): Float = delegate.getElapsedExpandFraction()
        override fun setValue(delegate: TimerView, value: Float) =
          delegate.setElapsedExpandFraction(value)
      }
  }
}
