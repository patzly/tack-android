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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.navigation.fragment.navArgs
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.BEAT_MODE
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.FLASH_SCREEN
import xyz.zedler.patrick.tack.Constants.KEEP_AWAKE
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.ScrollBehavior
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.FragmentMainBinding
import xyz.zedler.patrick.tack.drawable.BeatsBgDrawable
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListener
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick
import xyz.zedler.patrick.tack.model.MetronomeConfig
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.LogoUtil
import xyz.zedler.patrick.tack.util.NotificationUtil
import xyz.zedler.patrick.tack.util.OptionsUtil
import xyz.zedler.patrick.tack.util.centerScrollContentIfNotFullWidth
import xyz.zedler.patrick.tack.util.dpFromPx
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.getDisplayHeight
import xyz.zedler.patrick.tack.util.getDisplayWidth
import xyz.zedler.patrick.tack.util.isKeyInstalled
import xyz.zedler.patrick.tack.util.isLandTablet
import xyz.zedler.patrick.tack.util.isLayoutRtl
import xyz.zedler.patrick.tack.util.isOrientationPortrait
import xyz.zedler.patrick.tack.util.isPlayStoreInstalled
import xyz.zedler.patrick.tack.util.keepScreenAwake
import xyz.zedler.patrick.tack.util.resetAnimatedIcon
import xyz.zedler.patrick.tack.util.scrollToViewMinimal
import xyz.zedler.patrick.tack.util.setOnClickListeners
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.setTooltipTextAndContentDescription
import xyz.zedler.patrick.tack.util.showMenu
import xyz.zedler.patrick.tack.util.start
import xyz.zedler.patrick.tack.util.dialog.BackupDialogUtil
import xyz.zedler.patrick.tack.util.dialog.PartsDialogUtil
import xyz.zedler.patrick.tack.util.dialog.TempoDialogUtil
import xyz.zedler.patrick.tack.view.BeatView
import xyz.zedler.patrick.tack.view.CircleView
import xyz.zedler.patrick.tack.view.SongPickerView
import xyz.zedler.patrick.tack.view.TempoPickerView
import xyz.zedler.patrick.tack.view.TimerView
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds

class MainFragment : BaseFragment(), View.OnClickListener, MetronomeListener {

  private var _binding: FragmentMainBinding? = null
  private val binding get() = _binding!!

  private val args: MainFragmentArgs by navArgs()
  private var savedState: Bundle? = null
  private var flashScreen = false
  private var reduceAnimations = false
  private var isRtl = false
  private var isPortrait = false
  private var isLandTablet = false
  private var bigLogo = false
  private var showPickerNotLogo = false
  private var activeBeat = false

  private var logoUtil: LogoUtil? = null
  private var logoCenterUtil: LogoUtil? = null
  private var playStopButtonAnimator: ValueAnimator? = null
  private var playStopButtonFraction = 0f

  private var colorFlashNormal = 0
  private var colorFlashStrong = 0
  private var colorFlashMuted = 0

  private var songPickerAvailableHeight = 0
  private var topControlsBottomMin = 0

  private var dialogUtilGain: DialogUtil? = null
  private var dialogUtilSplitScreen: DialogUtil? = null
  private var dialogUtilTimer: DialogUtil? = null
  private var dialogUtilElapsed: DialogUtil? = null
  private var dialogUtilPermission: DialogUtil? = null
  private var dialogUtilBeatMode: DialogUtil? = null
  private var dialogUtilIntro: DialogUtil? = null

  private var optionsUtil: OptionsUtil? = null
  private var partsDialogUtil: PartsDialogUtil? = null
  private var tempoDialogUtil: TempoDialogUtil? = null
  private var backupDialogUtil: BackupDialogUtil? = null

  private var beatsBgDrawable: BeatsBgDrawable? = null
  private var beatsCountBadge: BadgeDrawable? = null
  private var subsCountBadge: BadgeDrawable? = null
  private var optionsBadge: BadgeDrawable? = null

  private var beatsCountBadgeAnimator: ValueAnimator? = null
  private var subsCountBadgeAnimator: ValueAnimator? = null
  private var optionsBadgeAnimator: ValueAnimator? = null
  private var pickerLogoAnimator: ValueAnimator? = null

  private var songsWithParts: List<SongWithParts> = ArrayList()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    _binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()

    metronomeEngine?.removeListener(this)

    playStopButtonAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      cancel()
    }

    _binding = null
    dialogUtilGain?.dismiss()
    dialogUtilSplitScreen?.dismiss()
    dialogUtilTimer?.dismiss()
    dialogUtilElapsed?.dismiss()
    dialogUtilPermission?.dismiss()
    dialogUtilBeatMode?.dismiss()
    tempoDialogUtil?.dismiss()
    backupDialogUtil?.dismiss()
    dialogUtilIntro?.dismiss()
    optionsUtil?.dismiss()
    partsDialogUtil?.dismiss()
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    savedState = savedInstanceState

    val receivedSongId = args.songId
    if (receivedSongId != null) {
      metronomeEngine?.setCurrentSong(receivedSongId, 0)
    }

    isPortrait = activity.isOrientationPortrait()
    isLandTablet = activity.isLandTablet()
    isRtl = activity.isLayoutRtl()

    SystemBarBehavior(activity).apply {
      setAppBar(binding.appBarMain)
      if (isPortrait || isLandTablet) {
        setContainer(binding.constraintMainContainer)
      } else {
        binding.scrollMainStart?.let { scroll ->
          setContainer(binding.constraintMainContainer)
          setScroll(scroll, binding.linearMainTop)
          setMultiColumnLayout(true)
          binding.containerMainEnd?.let {
            SystemBarBehavior.applyBottomInset(it)
          }
        }
      }
      setUp()
    }

    if (isPortrait || isLandTablet) {
      val liftMode = if (isLandTablet) ScrollBehavior.ALWAYS_LIFTED else ScrollBehavior.NEVER_LIFTED
      ScrollBehavior().setUpScroll(binding.appBarMain, null, liftMode)
    } else {
      ScrollBehavior().setUpScroll(
        binding.appBarMain,
        binding.scrollMainStart,
        ScrollBehavior.LIFT_ON_SCROLL
      )
    }

    binding.buttonMainSupport.setOnClickListener {
      performHapticClick()
      activity.showUnlockDialog()
    }
    binding.buttonMainMenu.setOnClickListener { v ->
      performHapticClick()
      v.showMenu(R.menu.menu_main, PopupMenu.OnMenuItemClickListener { item ->
        val id = item.itemId
        if (getViewUtil().isClickDisabled(id)) return@OnMenuItemClickListener false
        performHapticClick()
        when (id) {
          R.id.action_settings -> activity.navigate(
            MainFragmentDirections.actionMainToSettings()
          )

          R.id.action_about -> activity.navigate(
            MainFragmentDirections.actionMainToAbout()
          )

          R.id.action_help -> activity.showHelpDialog()
          R.id.action_feedback -> activity.showFeedbackDialog()
        }
        true
      }, Gravity.END)
    }
    binding.buttonMainSupport.setTooltipText(R.string.action_support)
    binding.buttonMainMenu.setTooltipText(R.string.action_more)

    val checkUnlockKey = sharedPrefs.getBoolean(PREF.CHECK_UNLOCK_KEY, true)
    val isSupportVisible = checkUnlockKey &&
        isPlayStoreInstalled(activity) && !isKeyInstalled(activity)
    binding.buttonMainSupport.visibility = if (isSupportVisible) View.VISIBLE else View.GONE

    reduceAnimations = sharedPrefs.getBoolean(PREF.REDUCE_ANIM, DEF.REDUCE_ANIM)
    activeBeat = sharedPrefs.getBoolean(PREF.ACTIVE_BEAT, DEF.ACTIVE_BEAT)
    val bigText = sharedPrefs.getBoolean(PREF.BIG_TIME_TEXT, DEF.BIG_TIME_TEXT)

    beatsCountBadge = BadgeDrawable.create(activity)
    subsCountBadge = BadgeDrawable.create(activity)
    optionsBadge = BadgeDrawable.create(activity).apply {
      verticalOffset = activity.dpToPx(16f)
      horizontalOffset = activity.dpToPx(16f)
    }

    binding.scrollHorizMainBeats.centerScrollContentIfNotFullWidth()
    updateBeats(
      sharedPrefs.getString(
        PREF.BEATS, DEF.BEATS
      )?.split(",")?.toTypedArray() ?: emptyArray(),
      false
    )

    binding.scrollHorizMainSubs.centerScrollContentIfNotFullWidth()
    updateSubs(
      sharedPrefs.getString(
        PREF.SUBDIVISIONS, DEF.SUBDIVISIONS
      )?.split(",")?.toTypedArray()
        ?: emptyArray(),
      false
    )

    binding.timerMain.apply {
      setMainActivity(this@MainFragment.activity)
      setChangeHeightOfChips(!isPortrait && !isLandTablet)
      setBigText(bigText)
      setListener(object : TimerView.TimerListener {
        override fun onCurrentTimeClick() {
          dialogUtilTimer?.show()
          performHapticClick()
        }

        override fun onElapsedTimeClick() {
          dialogUtilElapsed?.show()
          performHapticClick()
        }

        override fun onTotalTimeClick() {
          dialogUtilTimer?.show()
          performHapticClick()
        }

        override fun onHeightChanged() {
          if (isPortrait || isLandTablet) {
            updateTempoPickerTranslationAndScale()
          }
        }
      })
    }

    dialogUtilGain = DialogUtil(activity, "gain").apply {
      createDialogError { builder ->
        builder.setTitle(R.string.msg_gain)
        builder.setMessage(R.string.msg_gain_description)
        builder.setPositiveButton(R.string.action_play) { _, _ ->
          performHapticClick()
          metronomeEngine?.start()
        }
        builder.setNegativeButton(R.string.action_deactivate) { _, _ ->
          performHapticClick()
          metronomeEngine?.setGain(0)
          metronomeEngine?.start()
        }
      }
      showIfWasShown(savedInstanceState)
    }

    dialogUtilPermission = DialogUtil(activity, "notification_permission").apply {
      createDialog { builder ->
        builder.setTitle(R.string.msg_notification_permission)
        builder.setMessage(R.string.msg_notification_permission_description)
        builder.setPositiveButton(R.string.action_next) { _, _ ->
          performHapticClick()
          metronomeEngine?.start()
        }
        builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
      }
      showIfWasShown(savedInstanceState)
    }

    dialogUtilSplitScreen = DialogUtil(activity, "split_screen").apply {
      createDialog { builder ->
        builder.setTitle(R.string.msg_split_screen)
        builder.setMessage(R.string.msg_split_screen_description)
        builder.setPositiveButton(R.string.action_close) { _, _ -> performHapticClick() }
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val isMultiWindow = activity.isInMultiWindowMode
      val screenHeightDp = activity.dpFromPx(activity.getDisplayHeight().toFloat())
      val screenWidthDp = activity.dpFromPx(activity.getDisplayWidth().toFloat())
      val isHeightTooSmall = isPortrait && screenHeightDp < 700
      val isWidthTooSmall = !isPortrait && screenWidthDp < 600
      if (isMultiWindow && (isHeightTooSmall || isWidthTooSmall)) {
        dialogUtilSplitScreen?.show()
      }
    }

    dialogUtilTimer = DialogUtil(activity, "timer_reset").apply {
      createDialog { builder ->
        builder.setTitle(R.string.msg_reset_timer)
        builder.setMessage(R.string.msg_reset_timer_description)
        builder.setPositiveButton(R.string.action_reset) { _, _ ->
          performHapticClick()
          metronomeEngine?.resetTimerNow()
        }
        builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
      }
      showIfWasShown(savedInstanceState)
    }

    dialogUtilElapsed = DialogUtil(activity, "elapsed_reset").apply {
      createDialog { builder ->
        builder.setTitle(R.string.msg_reset_elapsed)
        builder.setMessage(R.string.msg_reset_elapsed_description)
        builder.setPositiveButton(R.string.action_reset) { _, _ ->
          performHapticClick()
          metronomeEngine?.resetElapsed()
        }
        builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
      }
      showIfWasShown(savedInstanceState)
    }

    backupDialogUtil = BackupDialogUtil(activity, this).apply {
      showIfWasShown(savedInstanceState)
    }

    dialogUtilIntro = DialogUtil(activity, "songs_intro").apply {
      createDialog { builder ->
        builder.setTitle(R.string.msg_songs_intro)
        builder.setMessage(R.string.msg_songs_intro_description)
        builder.setPositiveButton(R.string.action_close) { _, _ ->
          performHapticClick()
          sharedPrefs.edit { putBoolean(PREF.SONGS_INTRO_SHOWN, true) }
        }
        builder.setOnCancelListener {
          performHapticClick()
          sharedPrefs.edit { putBoolean(PREF.SONGS_INTRO_SHOWN, true) }
        }
      }
      showIfWasShown(savedInstanceState)
    }

    dialogUtilBeatMode = DialogUtil(activity, "beat_mode")

    tempoDialogUtil = TempoDialogUtil(activity, this) { tempo ->
      metronomeEngine?.let { updateTempoDisplay(it.config.tempo, tempo) }
    }.apply { showIfWasShown(savedInstanceState) }

    optionsUtil = OptionsUtil(
      activity,
      binding,
      object : OptionsUtil.OnOptionsListener {
        override fun onModifiersCountChanged() = updateOptions(true)
        override fun onTimerChanged() = binding.timerMain.updateControls(
          true, true, true
        )

        override fun onBeatsChanged() {
          metronomeEngine?.let { updateBeats(it.config.beats, true) }
        }

        override fun onSubsChanged() {
          metronomeEngine?.let {
            updateSubs(it.config.subdivisions, false)
            updateSubControls(true)
          }
        }
      }).apply {
      maybeInit()
      showIfWasShown(savedInstanceState)
    }
    binding.buttonMainOptions.isEnabled = !isLandTablet

    logoUtil = LogoUtil(binding.imageMainLogo)
    logoCenterUtil = LogoUtil(binding.imageMainLogoCenter)
    bigLogo = sharedPrefs.getBoolean(PREF.BIG_LOGO, DEF.BIG_LOGO)

    partsDialogUtil = PartsDialogUtil(activity).apply { showIfWasShown(savedInstanceState) }

    beatsBgDrawable = BeatsBgDrawable(activity).also {
      binding.linearMainBeatsBg.background = it
    }

    binding.textSwitcherMainTempoTerm.setFactory {
      TextView(activity).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.label_text_size))
        typeface = ResourcesCompat.getFont(activity, R.font.google_sans_flex_medium)
        setTextColor(activity.getAttrColor(R.attr.colorOnPrimaryContainer))
      }
    }

    binding.circleMain.apply {
      setReduceAnimations(this@MainFragment.reduceAnimations)
      setOnDragAnimListener(object : CircleView.OnDragAnimListener {
        override fun onDragAnim(fraction: Float) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.textMainTempo.fontVariationSettings =
              "'wght' ${600 + (fraction * 300)}, 'ROND' 100, 'wdth' ${100 + (fraction * 5)}"
          }
        }
      })
    }

    binding.tempoPickerMain.apply {
      setOnRotationListener(object : TempoPickerView.OnRotationListener {
        override fun onRotate(tempo: Int) {
          if (changeTempo(if (isRtl) -tempo else tempo)) {
            activity.performHapticSegmentTick(this@apply, false)
          }
        }

        override fun onRotate(degrees: Float) {
          binding.circleMain.rotation += degrees
        }
      })
      setOnPickListener(object : TempoPickerView.OnPickListener {
        override fun onPickDown(x: Float, y: Float) {
          binding.circleMain.setDragged(true, x, y)
          if (bigLogo && metronomeEngine?.isPlaying() == true) {
            updateTempoPickerAndLogo(showPicker = true, animated = true)
          }
        }

        override fun onDrag(x: Float, y: Float) = binding.circleMain.onDrag(x, y)

        override fun onPickUpOrCancel() {
          binding.circleMain.setDragged(false, 0f, 0f)
          if (bigLogo && metronomeEngine?.isPlaying() == true) {
            updateTempoPickerAndLogo(showPicker = false, animated = true)
          }
        }
      })
      setOnClickListener {
        tempoDialogUtil?.show()
        performHapticClick()
      }
    }

    measureSongPicker()
    binding.songPickerMain.setListener(object : SongPickerView.SongPickerListener {
      override fun onCurrentSongChanged(currentSongId: String) {
        performHapticClick()
        metronomeEngine?.setCurrentSong(currentSongId, 0)
      }

      override fun onCurrentSongClicked() {
        performHapticClick()
        metronomeEngine?.let {
          val action = MainFragmentDirections.actionMainToSong()
          action.arguments.putString("songId", it.currentSongId)
          activity.navigate(action)
        }
      }

      override fun onCurrentPartClicked() {
        performHapticClick()
        partsDialogUtil?.show()
      }

      override fun onPreviousPartClicked() {
        performHapticClick()
        metronomeEngine?.let { it.setCurrentPartIndex(it.getCurrentPartIndex() - 1) }
      }

      override fun onNextPartClicked() {
        performHapticClick()
        metronomeEngine?.let { it.setCurrentPartIndex(it.getCurrentPartIndex() + 1) }
      }

      override fun onSongLongClicked(songId: String) {
        val action = MainFragmentDirections.actionMainToSong()
        action.arguments.putString("songId", songId)
        activity.navigate(action)
      }

      override fun onExpandCollapseClicked(expand: Boolean) {
        performHapticClick()
        if (expand && !sharedPrefs.getBoolean(PREF.SONGS_INTRO_SHOWN, false)) {
          viewLifecycleOwner.lifecycleScope.launch {
            delay(200.milliseconds)
            dialogUtilIntro?.show()
          }
        }
      }

      override fun onOpenSongsClicked() {
        performHapticClick()
        val visitCount = sharedPrefs.getInt(PREF.SONGS_VISIT_COUNT, 0)
        if (visitCount != -1) {
          sharedPrefs.edit { putInt(PREF.SONGS_VISIT_COUNT, visitCount + 1) }
        }
        activity.navigate(MainFragmentDirections.actionMainToSongs())
      }

      override fun onMenuOrMenuItemClicked() = performHapticClick()

      override fun onBackupClicked() = backupDialogUtil?.show() ?: Unit

      override fun onSortOrderChanged(sortOrder: Int) {
        metronomeEngine?.setSongsOrder(sortOrder)
      }

      override fun onAddSongClicked() {
        performHapticClick()
        if (activity.isUnlocked() || songsWithParts.size < 3) {
          activity.navigate(MainFragmentDirections.actionMainToSong())
        } else {
          activity.showUnlockDialog()
        }
      }

      override fun onHeightChanged() {
        if (isPortrait || isLandTablet) updateTempoPickerTranslationAndScale()
      }

      override fun onExpandChanged(expanded: Boolean) {
        metronomeEngine?.isSongPickerExpanded = expanded
      }
    })

    binding.buttonMainPlayStop.resetAnimatedIcon()
    binding.buttonMainPlayStop.setIconResource(R.drawable.ic_rounded_play_to_stop_fill_anim)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val variableTypeface = ResourcesCompat.getFont(
        activity, R.font.google_sans_flex_variable
      )
      binding.textMainTempo.apply {
        typeface = variableTypeface
        fontVariationSettings = "'wght' 600, 'ROND' 100, 'wdth' 100"
      }
    }
    updateMetronomeControls(true)

    binding.buttonMainAddBeat.setTooltipText(R.string.action_add_beat)
    binding.buttonMainRemoveBeat.setTooltipText(R.string.action_remove_beat)
    binding.buttonMainAddSubdivision.setTooltipText(R.string.action_add_sub)
    binding.buttonMainRemoveSubdivision.setTooltipText(R.string.action_remove_sub)
    binding.buttonMainPlayStop.setTooltipText(R.string.action_play_stop)
    binding.buttonMainOptions.setTooltipText(R.string.title_options)
    binding.buttonMainBeatMode.setTooltipText(R.string.action_beat_mode)

    binding.buttonMainLess1.setTooltipTextAndContentDescription(
      getString(
        R.string.options_incremental_amount_decrease,
        1
      )
    )
    binding.buttonMainLess5.setTooltipTextAndContentDescription(
      getString(
        R.string.options_incremental_amount_decrease,
        5
      )
    )
    binding.buttonMainLess10.setTooltipTextAndContentDescription(
      getString(
        R.string.options_incremental_amount_decrease,
        10
      )
    )
    binding.buttonMainMore1.setTooltipTextAndContentDescription(
      getString(
        R.string.options_incremental_amount_increase,
        1
      )
    )
    binding.buttonMainMore5.setTooltipTextAndContentDescription(
      getString(
        R.string.options_incremental_amount_increase,
        5
      )
    )
    binding.buttonMainMore10.setTooltipTextAndContentDescription(
      getString(
        R.string.options_incremental_amount_increase,
        10
      )
    )

    setOnClickListeners(
      this,
      binding.buttonMainAddBeat,
      binding.buttonMainRemoveBeat,
      binding.buttonMainAddSubdivision,
      binding.buttonMainRemoveSubdivision,
      binding.buttonMainLess1, binding.buttonMainLess5, binding.buttonMainLess10,
      binding.buttonMainMore1, binding.buttonMainMore5, binding.buttonMainMore10,
      binding.buttonMainPlayStop,
      binding.buttonMainBeatMode,
      binding.buttonMainOptions
    )
  }

  override fun onPause() {
    super.onPause()
    binding.timerMain.stopProgress()
    binding.timerMain.stopProgressTransition()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    dialogUtilGain?.saveState(outState)
    dialogUtilPermission?.saveState(outState)
    dialogUtilTimer?.saveState(outState)
    dialogUtilElapsed?.saveState(outState)
    dialogUtilBeatMode?.saveState(outState)
    dialogUtilIntro?.saveState(outState)
    partsDialogUtil?.saveState(outState)
    optionsUtil?.saveState(outState)
    tempoDialogUtil?.saveState(outState)
    backupDialogUtil?.saveState(outState)
  }

  override fun updateMetronomeControls(init: Boolean) {
    if (_binding == null) return
    val config = metronomeEngine?.config ?: MetronomeConfig(sharedPrefs)

    optionsUtil?.maybeInit()
    optionsUtil?.showIfWasShown(savedState)
    tempoDialogUtil?.showIfWasShown(savedState)
    partsDialogUtil?.showIfWasShown(savedState)
    savedState = null

    updateBeats(config.beats, false)
    updateBeatControls(false)
    updateSubs(config.subdivisions, false)
    updateSubControls(false)

    if (init) binding.timerMain.measureControls()
    else binding.timerMain.updateControls(true, true, true)

    updateOptions(false)

    val tempo = config.tempo
    updateTempoDisplay(tempo, tempo)
    binding.textSwitcherMainTempoTerm.setCurrentText(getTempoTerm(tempo))

    val engine = metronomeEngine ?: return
    engine.addListener(this)

    binding.buttonMainBeatMode.setIconResource(
      if (engine.getBeatMode() == BEAT_MODE.VIBRATION)
        R.drawable.ic_rounded_vibration_to_volume_up_anim
      else R.drawable.ic_rounded_volume_up_to_vibration_anim
    )

    val showLogo = bigLogo && engine.isPlaying()
    updateTempoPickerAndLogo(showPicker = !showLogo, animated = false)

    if (engine.isCountingIn()) {
      beatsBgDrawable?.reset()
      if (config.countIn > 0) {
        beatsBgDrawable?.setProgress(engine.getCountInProgress(), 0L)
        beatsBgDrawable?.setProgress(
          1f, engine.getCountInIntervalRemaining()
        )
      }
    }

    activity.songViewModel.allSongsWithPartsLive.removeObservers(viewLifecycleOwner)
    activity.songViewModel.allSongsWithPartsLive.observe(viewLifecycleOwner) { songs ->
      songsWithParts = songs.filter { it.song.id != Constants.SONG_ID_DEFAULT }
      val currentEngine = metronomeEngine
      if (!binding.songPickerMain.isInitialized && currentEngine != null) {
        binding.songPickerMain.init(
          currentEngine.currentSongId,
          currentEngine.getCurrentPartIndex(),
          songsWithParts,
          currentEngine.getSongsOrder(),
          currentEngine.isSongPickerExpanded
        )
      }
      binding.songPickerMain.setSongs(songsWithParts)
    }

    binding.buttonMainPlayStop.resetAnimatedIcon()
    binding.buttonMainPlayStop.setIconResource(
      if (engine.isPlaying()) R.drawable.ic_rounded_stop_to_play_fill_anim
      else R.drawable.ic_rounded_play_to_stop_fill_anim
    )
    updatePlayStopButton(engine.isPlaying(), false)

    setButtonStates(config.tempo)

    val beatModeLabels = linkedMapOf(
      BEAT_MODE.ALL to getString(R.string.label_beat_mode_all),
      BEAT_MODE.SOUND to getString(R.string.label_beat_mode_sound),
      BEAT_MODE.VIBRATION to getString(R.string.label_beat_mode_vibration)
    )
    val beatModes = beatModeLabels.keys.toList()
    val items = beatModeLabels.values.toTypedArray()
    var initItem = beatModes.indexOf(engine.getBeatMode())
    if (initItem == -1) {
      initItem = 0
      sharedPrefs.edit { remove(PREF.BEAT_MODE) }
    }
    val initItemFinal = initItem
    dialogUtilBeatMode?.createDialog { builder ->
      builder.setTitle(R.string.action_beat_mode)
      if (activity.hapticUtil.hasVibrator()) {
        builder.setSingleChoiceItems(items, initItemFinal) { _, which ->
          val currentEngineInner = metronomeEngine ?: return@setSingleChoiceItems
          val beatModePrev = currentEngineInner.getBeatMode()
          val beatMode = beatModes[which]
          if (beatMode == BEAT_MODE.SOUND) performHapticClick()
          currentEngineInner.setBeatMode(beatMode)
          if (beatMode != BEAT_MODE.SOUND) performHapticClick()

          if (beatModePrev == BEAT_MODE.VIBRATION && beatMode != BEAT_MODE.VIBRATION) {
            binding.buttonMainBeatMode.setIconResource(R.drawable.ic_rounded_vibration_to_volume_up_anim)
            binding.buttonMainBeatMode.icon?.start()
          } else if (beatModePrev != BEAT_MODE.VIBRATION && beatMode == BEAT_MODE.VIBRATION) {
            binding.buttonMainBeatMode.setIconResource(R.drawable.ic_rounded_volume_up_to_vibration_anim)
            binding.buttonMainBeatMode.icon?.start()
          }
        }
      } else {
        builder.setMessage(R.string.msg_vibration_unavailable)
      }
      builder.setPositiveButton(R.string.action_close) { _, _ -> performHapticClick() }
    }
    dialogUtilBeatMode?.showIfWasShown(savedState)

    val flashScreenMode = engine.getFlashScreen()
    flashScreen = flashScreenMode != FLASH_SCREEN.OFF

    colorFlashMuted = activity.getAttrColor(R.attr.colorSurface)
    if (flashScreenMode == FLASH_SCREEN.SUBTLE) {
      val mixRatio = 0.7f
      colorFlashNormal = ColorUtils.blendARGB(
        colorFlashMuted,
        activity.getAttrColor(R.attr.colorPrimaryContainer),
        mixRatio
      )
      colorFlashStrong = ColorUtils.blendARGB(
        colorFlashMuted,
        activity.getAttrColor(R.attr.colorErrorContainer),
        mixRatio
      )
    } else {
      colorFlashNormal = activity.getAttrColor(R.attr.colorPrimary)
      colorFlashStrong = activity.getAttrColor(R.attr.colorError)
    }

    val keepAwake = engine.getKeepAwake()
    val keepAwakeNow =
      keepAwake == KEEP_AWAKE.ALWAYS ||
          (keepAwake == KEEP_AWAKE.WHILE_PLAYING && engine.isPlaying())
    activity.keepScreenAwake(keepAwakeNow)
  }

  override fun onMetronomeStart() {
    activity.runOnUiThread {
      if (_binding == null || metronomeEngine == null || beatsBgDrawable == null)
        return@runOnUiThread
      val engine = metronomeEngine!!
      beatsBgDrawable?.reset()
      if (engine.config.countIn > 0) {
        beatsBgDrawable?.setProgress(1f, engine.countInInterval)
      }
      binding.buttonMainPlayStop.icon?.start()
      viewLifecycleOwner.lifecycleScope.launch {
        delay(300.milliseconds)
        _binding?.buttonMainPlayStop?.setIconResource(R.drawable.ic_rounded_stop_to_play_fill_anim)
      }
      updatePlayStopButton(true, !reduceAnimations)
      if (bigLogo) updateTempoPickerAndLogo(showPicker = false, animated = true)
      activity.keepScreenAwake(engine.getKeepAwake() != KEEP_AWAKE.NEVER)
    }
  }

  override fun onMetronomeStop() {
    activity.runOnUiThread {
      if (_binding == null || metronomeEngine == null || beatsBgDrawable == null)
        return@runOnUiThread
      resetActiveBeats()
      beatsBgDrawable?.setProgressVisible(false, true)
      binding.timerMain.updateDisplay()
      binding.buttonMainPlayStop.icon?.start()
      viewLifecycleOwner.lifecycleScope.launch {
        delay(300.milliseconds)
        _binding?.buttonMainPlayStop?.setIconResource(R.drawable.ic_rounded_play_to_stop_fill_anim)
      }
      updatePlayStopButton(false, !reduceAnimations)
      if (bigLogo) updateTempoPickerAndLogo(showPicker = true, animated = true)
      binding.timerMain.stopProgressTransition()
      binding.timerMain.stopProgress()
      activity.keepScreenAwake(metronomeEngine?.getKeepAwake() == KEEP_AWAKE.ALWAYS)
    }
  }

  override fun onMetronomePreTick(tick: Tick) {
    activity.runOnUiThread {
      if (_binding == null || metronomeEngine == null) return@runOnUiThread
      val beat = binding.linearMainBeats.getChildAt(tick.beat - 1)
      if (beat is BeatView && tick.subdivision == 1 && !tick.isPoly) {
        resetActiveBeats()
        binding.scrollHorizMainBeats.scrollToViewMinimal(beat)
        if (activeBeat) beat.setActive(true)
        beat.beat()
      }
      val subdivision = binding.linearMainSubs.getChildAt(tick.subdivision - 1)
      if (subdivision !is BeatView) return@runOnUiThread
      if (metronomeEngine?.config?.usePolyrhythm == true && !tick.isPoly) return@runOnUiThread
      binding.scrollHorizMainSubs.scrollToViewMinimal(subdivision)
      subdivision.beat()
    }
  }

  override fun onMetronomeTick(tick: Tick) {
    activity.runOnUiThread {
      if (_binding == null || metronomeEngine == null) return@runOnUiThread
      if (flashScreen) {
        val color = when {
          tick.isMuted -> colorFlashMuted
          tick.type == TICK_TYPE.STRONG -> colorFlashStrong
          tick.type == TICK_TYPE.SUB || tick.type == TICK_TYPE.MUTED
              || tick.type == TICK_TYPE.BEAT_SUB_MUTED -> colorFlashMuted

          else -> colorFlashNormal
        }
        if (isLandTablet && binding.cardMainContainerEnd != null) {
          binding.cardMainContainerEnd?.setCardBackgroundColor(color)
          viewLifecycleOwner.lifecycleScope.launch {
            delay(100.milliseconds)
            binding.cardMainContainerEnd?.setCardBackgroundColor(colorFlashMuted)
          }
        } else {
          binding.coordinatorContainer.setBackgroundColor(color)
          viewLifecycleOwner.lifecycleScope.launch {
            delay(100.milliseconds)
            binding.coordinatorContainer.setBackgroundColor(colorFlashMuted)
          }
        }
      }
      if (tick.subdivision == 1) {
        val interval = metronomeEngine?.interval ?: 0L
        if (!reduceAnimations) logoUtil?.nextBeat(interval)
        if (bigLogo) logoCenterUtil?.nextBeat(interval)
      }
      if (metronomeEngine?.config?.isTimerActive() == true
        && metronomeEngine?.config?.timerUnit == UNIT.BARS
      ) {
        binding.timerMain.updateDisplay()
      }
    }
  }

  override fun onMetronomeTempoChanged(tempoOld: Int, tempoNew: Int) {
    activity.runOnUiThread { updateTempoDisplay(tempoOld, tempoNew) }
  }

  override fun onMetronomeTimerStarted() {
    activity.runOnUiThread {
      if (_binding == null) return@runOnUiThread
      binding.timerMain.apply {
        stopProgressTransition()
        stopProgress()
        updateControls(true, true, true)
      }
    }
  }

  override fun onMetronomeElapsedTimeSecondsChanged() {
    activity.runOnUiThread { _binding?.timerMain?.updateDisplay() }
  }

  override fun onMetronomeTimerSecondsChanged() {
    activity.runOnUiThread {
      if (_binding != null && metronomeEngine != null) {
        if (metronomeEngine?.config?.timerUnit != UNIT.BARS) {
          binding.timerMain.updateDisplay()
        }
      }
    }
  }

  override fun onMetronomeTimerProgressOneTime(withTransition: Boolean) {
    activity.runOnUiThread {
      binding.timerMain.updateControls(true, true, withTransition)
    }
  }

  override fun onMetronomeTimerActiveStateChanged(active: Boolean) {}

  override fun onMetronomeConfigChanged() {
    activity.runOnUiThread {
      if (_binding == null || metronomeEngine == null) return@runOnUiThread
      val config = metronomeEngine!!.config
      updateBeats(config.beats, false)
      updateBeatControls(true)
      updateSubs(config.subdivisions, false)
      updateSubControls(true)
      updateOptions(true)
    }
  }

  override fun onMetronomeSongOrPartChanged(song: SongWithParts?, partIndex: Int) {
    activity.runOnUiThread {
      if (song != null && _binding != null) {
        partsDialogUtil?.update()
        if (song.song.id != Constants.SONG_ID_DEFAULT) {
          binding.songPickerMain.setPartIndex(partIndex)
        }
      }
    }
  }

  override fun onMetronomePermissionMissing() {
    activity.runOnUiThread { activity.requestNotificationPermission(true) }
  }

  override fun onClick(v: View) {
    val engine = metronomeEngine ?: return
    if (_binding == null) return
    val config = engine.config
    when (v.id) {
      R.id.button_main_add_beat -> {
        binding.buttonMainAddBeat.icon?.start()
        performHapticClick()
        if (engine.addBeat()) {
          engine.restartIfPlaying(false)
          engine.maybeUpdateDefaultSong()
          TransitionManager.beginDelayedTransition(
            binding.linearMainBeats,
            AutoTransition().apply { duration = Constants.ANIM_DURATION_SHORT })
          binding.scrollHorizMainBeats.centerScrollContentIfNotFullWidth(
            activity.dpToPx(48f)
          )
          val beatView = getNewBeatView(false).apply {
            setTickType(
              if (config.isFirstSubdivisionMuted()) TICK_TYPE.MUTED else TICK_TYPE.NORMAL,
              false
            )
            setIndex(binding.linearMainBeats.childCount)
          }
          binding.linearMainBeats.addView(beatView)
          updateBeatControls(true)
          binding.timerMain.updateDisplay()
        }
      }

      R.id.button_main_remove_beat -> {
        binding.buttonMainRemoveBeat.icon?.start()
        performHapticClick()
        if (engine.removeBeat()) {
          engine.restartIfPlaying(false)
          engine.maybeUpdateDefaultSong()
          TransitionManager.beginDelayedTransition(
            binding.linearMainBeats,
            ChangeBounds().apply { duration = Constants.ANIM_DURATION_SHORT })
          binding.scrollHorizMainBeats.centerScrollContentIfNotFullWidth(
            -activity.dpToPx(48f)
          )
          binding.linearMainBeats.removeViewAt(binding.linearMainBeats.childCount - 1)
          updateBeatControls(true)
          binding.timerMain.updateDisplay()
        }
      }

      R.id.button_main_add_subdivision -> {
        binding.buttonMainAddSubdivision.icon?.start()
        performHapticClick()
        if (engine.addSubdivision()) {
          engine.restartIfPlaying(false)
          engine.maybeUpdateDefaultSong()
          TransitionManager.beginDelayedTransition(
            binding.linearMainSubs,
            AutoTransition().apply { duration = Constants.ANIM_DURATION_SHORT })
          binding.scrollHorizMainSubs.centerScrollContentIfNotFullWidth(
            activity.dpToPx(48f)
          )
          val beatView = getNewBeatView(true).apply {
            setIndex(binding.linearMainSubs.childCount)
          }
          binding.linearMainSubs.addView(beatView)
          updateSubControls(true)
          optionsUtil?.updateSwing()
        }
      }

      R.id.button_main_remove_subdivision -> {
        binding.buttonMainRemoveSubdivision.icon?.start()
        performHapticClick()
        if (engine.removeSubdivision()) {
          engine.restartIfPlaying(false)
          engine.maybeUpdateDefaultSong()
          TransitionManager.beginDelayedTransition(
            binding.linearMainSubs,
            ChangeBounds().apply { duration = Constants.ANIM_DURATION_SHORT })
          binding.scrollHorizMainSubs.centerScrollContentIfNotFullWidth(
            -activity.dpToPx(48f)
          )
          binding.linearMainSubs.removeViewAt(binding.linearMainSubs.childCount - 1)
          updateSubControls(true)
          optionsUtil?.updateSwing()
        }
      }

      R.id.button_main_less_1 -> {
        binding.buttonMainLess1.icon?.start()
        changeTempo(-1)
        performHapticClick()
      }

      R.id.button_main_less_5 -> {
        binding.buttonMainLess5.icon?.start()
        changeTempo(-5)
        performHapticClick()
      }

      R.id.button_main_less_10 -> {
        binding.buttonMainLess10.icon?.start()
        changeTempo(-10)
        performHapticClick()
      }

      R.id.button_main_more_1 -> {
        binding.buttonMainMore1.icon?.start()
        changeTempo(1)
        performHapticClick()
      }

      R.id.button_main_more_5 -> {
        binding.buttonMainMore5.icon?.start()
        changeTempo(5)
        performHapticClick()
      }

      R.id.button_main_more_10 -> {
        binding.buttonMainMore10.icon?.start()
        changeTempo(10)
        performHapticClick()
      }

      R.id.button_main_play_stop -> {
        if (engine.isPlaying()) {
          performHapticClick()
          engine.stop()
        } else {
          if (engine.getGain() > 0 && engine.neverStartedWithGainBefore()) {
            dialogUtilGain?.show()
          } else {
            val permissionDenied = sharedPrefs.getBoolean(
              PREF.PERMISSION_DENIED, false
            )
            if (NotificationUtil.hasPermission(activity) || permissionDenied) {
              engine.start()
            } else {
              dialogUtilPermission?.show()
            }
          }
          performHapticClick()
        }
      }

      R.id.button_main_beat_mode -> {
        performHapticClick()
        dialogUtilBeatMode?.show()
        if (engine.getBeatMode() == BEAT_MODE.VIBRATION) {
          binding.buttonMainBeatMode.setIconResource(R.drawable.ic_rounded_vibration_anim)
          binding.buttonMainBeatMode.icon?.start()
        }
      }

      R.id.button_main_options -> {
        performHapticClick()
        binding.buttonMainOptions.icon?.start()
        optionsUtil?.show()
      }
    }
  }

  private fun updateBeats(beats: Array<String>, firstSubChanged: Boolean) {
    if (_binding == null) return
    val config = metronomeEngine?.config
    val isFirstSubMuted = config?.isFirstSubdivisionMuted() == true
    val usePolyrhythm = config?.usePolyrhythm == true

    if (firstSubChanged) {
      for (i in 0 until binding.linearMainBeats.childCount) {
        val beatView = binding.linearMainBeats.getChildAt(i) as BeatView
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
    if (isFirstSubMuted) beatsMaybeMuted.fill(TICK_TYPE.MUTED)

    val currentBeats = Array(binding.linearMainBeats.childCount) { i ->
      (binding.linearMainBeats.getChildAt(i) as BeatView).toString()
    }

    if (beatsMaybeMuted.contentEquals(currentBeats)) return
    else if (beatsMaybeMuted.size == currentBeats.size) {
      for (i in beatsMaybeMuted.indices) {
        (binding.linearMainBeats.getChildAt(i) as BeatView).setTickType(
          beatsMaybeMuted[i], false
        )
      }
    } else {
      binding.linearMainBeats.removeAllViews()
      for (i in beatsMaybeMuted.indices) {
        val beatView = getNewBeatView(false).apply {
          setTickType(beatsMaybeMuted[i], false)
          setIndex(i)
        }
        binding.linearMainBeats.addView(beatView)
      }
    }

    binding.linearMainBeats.post {
      _binding?.scrollHorizMainBeats?.centerScrollContentIfNotFullWidth()
    }
    updateBeatControls(true)
  }

  private fun resetActiveBeats() {
    for (i in 0 until binding.linearMainBeats.childCount) {
      (binding.linearMainBeats.getChildAt(i) as? BeatView)?.setActive(false)
    }
  }

  @OptIn(ExperimentalBadgeUtils::class)
  private fun updateBeatControls(animated: Boolean) {
    beatsCountBadgeAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      removeAllListeners()
      cancel()
    }
    beatsCountBadgeAnimator = null
    if (_binding == null || metronomeEngine == null) return

    val beats = metronomeEngine!!.config.getBeatsCount()
    binding.buttonMainAddBeat.isEnabled = beats < Constants.BEATS_MAX
    binding.buttonMainRemoveBeat.isEnabled = beats > 1
    beatsCountBadge?.number = beats
    val show = beats > 4

    if (animated) {
      beatsCountBadgeAnimator =
        ValueAnimator.ofInt(beatsCountBadge?.alpha ?: 0, if (show) 255 else 0).apply {
          val colorBadgeBg = activity.getAttrColor(R.attr.colorError)
          addUpdateListener { animation ->
            val alpha = animation.animatedValue as Int
            beatsCountBadge?.alpha = alpha
            val fraction = alpha / 255f
            beatsCountBadge?.backgroundColor =
              ColorUtils.blendARGB(Color.TRANSPARENT, colorBadgeBg, fraction)
          }
          addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              if (!show && _binding != null) {
                beatsCountBadge?.let {
                  BadgeUtils.detachBadgeDrawable(
                    it,
                    binding.linearMainBeatsBg
                  )
                }
              }
            }
          })
          interpolator = FastOutSlowInInterpolator()
          duration = Constants.ANIM_DURATION_LONG
          start()
        }
      if (show) beatsCountBadge?.let {
        BadgeUtils.attachBadgeDrawable(
          it,
          binding.linearMainBeatsBg
        )
      }
    } else {
      beatsCountBadge?.alpha = if (show) 255 else 0
      beatsCountBadge?.backgroundColor =
        if (show) activity.getAttrColor(R.attr.colorError) else Color.TRANSPARENT
      viewLifecycleOwner.lifecycleScope.launch {
        if (_binding == null || beatsCountBadge == null) return@launch
        if (show) BadgeUtils.attachBadgeDrawable(beatsCountBadge!!, binding.linearMainBeatsBg)
        else BadgeUtils.detachBadgeDrawable(beatsCountBadge!!, binding.linearMainBeatsBg)
      }
    }
  }

  private fun updateSubs(subdivisions: Array<String>, firstSubChanged: Boolean) {
    if (_binding == null) return
    val isFirstSubMuted = metronomeEngine?.config?.isFirstSubdivisionMuted() == true

    if (firstSubChanged) {
      (binding.linearMainSubs.getChildAt(0) as BeatView).setTickType(
        if (isFirstSubMuted) TICK_TYPE.BEAT_SUB_MUTED else TICK_TYPE.BEAT_SUB, true
      )
      return
    }

    val currentSubs = Array(binding.linearMainSubs.childCount) { i ->
      (binding.linearMainSubs.getChildAt(i) as BeatView).toString()
    }

    if (subdivisions.contentEquals(currentSubs)) return
    else if (subdivisions.size == currentSubs.size) {
      for (i in subdivisions.indices) {
        (binding.linearMainSubs.getChildAt(i) as BeatView).setTickType(subdivisions[i], false)
      }
    } else {
      binding.linearMainSubs.removeAllViews()
      for (i in subdivisions.indices) {
        val beatView = getNewBeatView(true).apply {
          var tickType = subdivisions[i]
          if (i == 0 && tickType == TICK_TYPE.MUTED) tickType = TICK_TYPE.BEAT_SUB
          setTickType(tickType, false)
          setIndex(i)
        }
        binding.linearMainSubs.addView(beatView)
      }
    }

    binding.linearMainSubs.post {
      _binding?.scrollHorizMainSubs?.centerScrollContentIfNotFullWidth()
    }
    updateSubControls(true)
  }

  @OptIn(ExperimentalBadgeUtils::class)
  private fun updateSubControls(animated: Boolean) {
    if (_binding == null || metronomeEngine == null) return
    subsCountBadgeAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      removeAllListeners()
      cancel()
    }
    subsCountBadgeAnimator = null

    val subdivisions = metronomeEngine!!.config.getSubdivisionsCount()
    binding.buttonMainAddSubdivision.isEnabled = subdivisions < Constants.SUBS_MAX
    binding.buttonMainRemoveSubdivision.isEnabled = subdivisions > 1
    subsCountBadge?.number = subdivisions
    val show = subdivisions > 4

    if (animated) {
      subsCountBadgeAnimator =
        ValueAnimator.ofInt(subsCountBadge?.alpha ?: 0, if (show) 255 else 0).apply {
          val colorBadgeBg = activity.getAttrColor(R.attr.colorError)
          addUpdateListener { animation ->
            val alpha = animation.animatedValue as Int
            subsCountBadge?.alpha = alpha
            val fraction = alpha / 255f
            subsCountBadge?.backgroundColor =
              ColorUtils.blendARGB(Color.TRANSPARENT, colorBadgeBg, fraction)
          }
          addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              if (!show && _binding != null) {
                subsCountBadge?.let { BadgeUtils.detachBadgeDrawable(it, binding.linearMainSubsBg) }
              }
            }
          })
          interpolator = FastOutSlowInInterpolator()
          duration = Constants.ANIM_DURATION_LONG
          start()
        }
      if (show) subsCountBadge?.let { BadgeUtils.attachBadgeDrawable(it, binding.linearMainSubsBg) }
    } else {
      subsCountBadge?.alpha = if (show) 255 else 0
      subsCountBadge?.backgroundColor =
        if (show) activity.getAttrColor(R.attr.colorError) else Color.TRANSPARENT
      viewLifecycleOwner.lifecycleScope.launch {
        if (_binding == null || subsCountBadge == null) return@launch
        if (show) BadgeUtils.attachBadgeDrawable(subsCountBadge!!, binding.linearMainSubsBg)
        else BadgeUtils.detachBadgeDrawable(subsCountBadge!!, binding.linearMainSubsBg)
      }
    }
  }

  private fun getNewBeatView(isSubdivision: Boolean): BeatView {
    return BeatView(activity).apply {
      this.setIsSubdivision(isSubdivision)
      setOnClickListener {
        val engine = metronomeEngine ?: return@setOnClickListener
        performHapticClick()

        if (isSubdivision) {
          engine.setSubdivision(getIndex(), nextTickType())
          engine.maybeUpdateDefaultSong()
          if (getIndex() == 0) updateBeats(engine.config.beats, true)
        } else {
          if (engine.config.isFirstSubdivisionMuted()) {
            engine.setSubdivision(0, TICK_TYPE.BEAT_SUB)
            updateBeats(engine.config.beats, true)
            updateSubs(engine.config.subdivisions, true)
          } else {
            engine.setBeat(getIndex(), nextTickType())
            engine.maybeUpdateDefaultSong()
          }
        }
      }
      setReduceAnimations(this@MainFragment.reduceAnimations)
    }
  }

  private fun measureSongPicker() {
    binding.coordinatorContainer.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          if (_binding == null) return
          songPickerAvailableHeight = binding.frameMainBottom.top - binding.frameMainCenter.bottom

          val songPickerParent: ViewGroup? = when {
            isLandTablet -> binding.cardMainContainerEnd
            !isPortrait -> binding.linearMainTop
            else -> binding.constraintMainContainer
          }
          songPickerParent?.let { binding.songPickerMain.setParentWidth(it.width) }

          topControlsBottomMin = binding.linearMainSubsBg.bottom + activity.dpToPx(24f)

          if (binding.coordinatorContainer.viewTreeObserver.isAlive) {
            binding.coordinatorContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      })
  }

  private fun updateTempoPickerTranslationAndScale() {
    if (_binding == null || binding.frameMainBottom.top == 0) return
    val fraction = binding.songPickerMain.getExpandSpatialFraction()
    val songPickerHeightExpanded = binding.songPickerMain.getHeightExpanded()

    if (songPickerHeightExpanded > songPickerAvailableHeight) {
      val songPickerOverlap = songPickerHeightExpanded - songPickerAvailableHeight
      val timerSliderHeight = binding.timerMain.getSliderHeightExpanded()
      val timerSliderHeightCurrent =
        (timerSliderHeight * binding.timerMain.getTimerExpandFraction()).toInt()
      val timerDisplayHeight = binding.timerMain.getDisplayHeightExpanded()
      val timerDisplayHeightCurrent = (Math.max(
        timerDisplayHeight * binding.timerMain.getTimerExpandFraction(),
        timerDisplayHeight * binding.timerMain.getElapsedExpandFraction()
      )).toInt()

      val topControlsBottom =
        topControlsBottomMin + timerSliderHeightCurrent + timerDisplayHeightCurrent
      val currentWidth = binding.frameMainCenter.width
      val currentHeight = binding.frameMainCenter.height
      var targetHeight = binding.frameMainBottom.top - topControlsBottom - songPickerHeightExpanded
      if (targetHeight > currentHeight) targetHeight = currentHeight

      var scale = 1 + (((targetHeight.toFloat() / currentHeight) - 1) * fraction)
      if (scale.isNaN()) scale = 1f

      val scaleCompensation = (currentHeight - targetHeight) / 2
      val translationY = (-songPickerOverlap + scaleCompensation) * fraction

      binding.frameMainCenter.apply {
        scaleX = scale
        scaleY = scale
        this.translationY = translationY
      }
      binding.imageMainLogoCenter.apply {
        scaleX = scale
        scaleY = scale
        this.translationY = translationY
      }

      binding.buttonGroupMainLess.apply {
        scaleX = scale
        scaleY = scale
        this.translationY = translationY
        val translationX = ((currentWidth - (currentWidth * scale)) / 4)
        this.translationX = if (isRtl) -translationX else translationX
      }

      binding.buttonGroupMainMore.apply {
        scaleX = scale
        scaleY = scale
        this.translationY = translationY
        val translationX = ((currentWidth - (currentWidth * scale)) / 4)
        this.translationX = if (isRtl) translationX else -translationX
      }

      binding.songPickerMain.translationY = -songPickerOverlap * 0.5f * fraction
    }
  }

  @OptIn(ExperimentalBadgeUtils::class)
  private fun updateOptions(animated: Boolean) {
    optionsBadgeAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      removeAllListeners()
      cancel()
    }
    optionsBadgeAnimator = null
    val modifierCount = getModifierCount()
    val show = modifierCount > 0
    optionsBadge?.number = modifierCount

    if (animated) {
      optionsBadgeAnimator =
        ValueAnimator.ofInt(optionsBadge?.alpha ?: 0, if (show) 255 else 0).apply {
          val colorBadgeBg = activity.getAttrColor(R.attr.colorError)
          addUpdateListener { animation ->
            val alpha = animation.animatedValue as Int
            optionsBadge?.alpha = alpha
            val fraction = alpha / 255f
            optionsBadge?.backgroundColor =
              ColorUtils.blendARGB(Color.TRANSPARENT, colorBadgeBg, fraction)
          }
          addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              if (!show && _binding != null) {
                optionsBadge?.let { BadgeUtils.detachBadgeDrawable(it, binding.buttonMainOptions) }
              }
            }
          })
          interpolator = FastOutSlowInInterpolator()
          duration = Constants.ANIM_DURATION_LONG
          start()
        }
      if (show) optionsBadge?.let { BadgeUtils.attachBadgeDrawable(it, binding.buttonMainOptions) }
    } else {
      optionsBadge?.alpha = if (show) 255 else 0
      optionsBadge?.backgroundColor =
        if (show) activity.getAttrColor(R.attr.colorError) else Color.TRANSPARENT
      viewLifecycleOwner.lifecycleScope.launch {
        if (_binding == null || optionsBadge == null) return@launch
        if (show) BadgeUtils.attachBadgeDrawable(optionsBadge!!, binding.buttonMainOptions)
        else BadgeUtils.detachBadgeDrawable(optionsBadge!!, binding.buttonMainOptions)
      }
    }
  }

  private fun getModifierCount(): Int {
    val config = metronomeEngine?.config ?: return 0
    return (if (config.isCountInActive()) 1 else 0) +
        (if (config.isIncrementalActive()) 1 else 0) +
        (if (config.isTimerActive()) 1 else 0) +
        (if (config.isMuteActive()) 1 else 0) +
        (if (config.usePolyrhythm) 1 else 0)
  }

  private fun changeTempo(difference: Int): Boolean {
    val engine = metronomeEngine ?: return false
    val tempoNew = engine.config.tempo + difference
    if (tempoNew in Constants.TEMPO_MIN..Constants.TEMPO_MAX) {
      updateTempoDisplay(engine.config.tempo, tempoNew)
      engine.setTempo(tempoNew)
      engine.maybeUpdateDefaultSong()
      return true
    }
    return false
  }

  private fun updateTempoDisplay(tempoOld: Int, tempoNew: Int) {
    val tempoClamped = tempoNew.coerceIn(Constants.TEMPO_MIN, Constants.TEMPO_MAX)
    if (_binding == null) return
    binding.textMainTempo.text = tempoClamped.toString()
    val termNew = getTempoTerm(tempoClamped)
    if (termNew != getTempoTerm(tempoOld)) {
      val isFaster = tempoClamped > tempoOld
      binding.textSwitcherMainTempoTerm.apply {
        setInAnimation(
          activity,
          if (isFaster) R.anim.tempo_term_open_enter else R.anim.tempo_term_close_enter
        )
        setOutAnimation(
          activity,
          if (isFaster) R.anim.tempo_term_open_exit else R.anim.tempo_term_close_exit
        )
        setText(termNew)
      }
    }
    setButtonStates(tempoClamped)
  }

  private fun setButtonStates(tempo: Int) {
    binding.buttonMainLess1.isEnabled = tempo > 1
    binding.buttonMainLess5.isEnabled = tempo > 5
    binding.buttonMainLess10.isEnabled = tempo > 10
    binding.buttonMainMore1.isEnabled = tempo < Constants.TEMPO_MAX
    binding.buttonMainMore5.isEnabled = tempo <= Constants.TEMPO_MAX - 5
    binding.buttonMainMore10.isEnabled = tempo <= Constants.TEMPO_MAX - 10
  }

  private fun updatePlayStopButton(playing: Boolean, animated: Boolean) {
    binding.buttonMainPlayStop.isChecked = playing
    playStopButtonAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      cancel()
    }
    playStopButtonAnimator = null
    val colorBgPlaying = activity.getAttrColor(R.attr.colorTertiary)
    val colorBgStopped = activity.getAttrColor(R.attr.colorPrimary)
    val colorFgPlaying = activity.getAttrColor(R.attr.colorOnTertiary)
    val colorFgStopped = activity.getAttrColor(R.attr.colorOnPrimary)
    val targetFraction = if (playing) 1f else 0f

    if (animated) {
      playStopButtonAnimator = ValueAnimator.ofFloat(
        playStopButtonFraction, targetFraction
      ).apply {
        addUpdateListener { animation ->
          if (_binding == null) return@addUpdateListener
          playStopButtonFraction = animation.animatedValue as Float
          binding.buttonMainPlayStop.setBackgroundColor(
            ColorUtils.blendARGB(
              colorBgStopped,
              colorBgPlaying,
              playStopButtonFraction
            )
          )
          binding.buttonMainPlayStop.setIconTint(
            ColorStateList.valueOf(
              ColorUtils.blendARGB(
                colorFgStopped,
                colorFgPlaying,
                playStopButtonFraction
              )
            )
          )
        }
        interpolator = FastOutSlowInInterpolator()
        duration = 300
        start()
      }
    } else {
      playStopButtonFraction = targetFraction
      binding.buttonMainPlayStop.setBackgroundColor(
        ColorUtils.blendARGB(
          colorBgStopped,
          colorBgPlaying,
          playStopButtonFraction
        )
      )
      binding.buttonMainPlayStop.setIconTint(
        ColorStateList.valueOf(
          ColorUtils.blendARGB(
            colorFgStopped,
            colorFgPlaying,
            playStopButtonFraction
          )
        )
      )
    }
  }

  private fun updateTempoPickerAndLogo(showPicker: Boolean, animated: Boolean) {
    showPickerNotLogo = showPicker
    pickerLogoAnimator?.apply {
      pause()
      removeAllUpdateListeners()
      removeAllListeners()
      cancel()
    }
    pickerLogoAnimator = null
    val pickerAlpha = if (showPickerNotLogo) 1f else 0f

    if (animated) {
      binding.imageMainLogoCenter.visibility = View.VISIBLE
      binding.imageMainLogo.visibility = View.VISIBLE
      pickerLogoAnimator = ValueAnimator.ofFloat(
        binding.frameMainCenter.alpha, pickerAlpha
      ).apply {
        addUpdateListener { animation ->
          if (_binding == null) return@addUpdateListener
          val fraction = animation.animatedValue as Float
          binding.frameMainCenter.alpha = fraction
          binding.imageMainLogoCenter.alpha = 1 - fraction
          binding.imageMainLogo.scaleX = fraction
          binding.imageMainLogo.scaleY = fraction
          binding.imageMainLogoPlaceholder.alpha = 1 - fraction
          binding.imageMainLogoPlaceholder.scaleX = 1 - fraction
          binding.imageMainLogoPlaceholder.scaleY = 1 - fraction
        }
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            if (_binding != null) {
              binding.imageMainLogoCenter.visibility =
                if (showPickerNotLogo) View.GONE else View.VISIBLE
              binding.imageMainLogo.visibility = if (showPickerNotLogo) View.VISIBLE else View.GONE
            }
          }
        })
        interpolator = FastOutSlowInInterpolator()
        duration = if (reduceAnimations) 150 else 300
        start()
      }
    } else {
      binding.frameMainCenter.alpha = if (showPickerNotLogo) 1f else 0f
      binding.imageMainLogoCenter.visibility = if (showPickerNotLogo) View.GONE else View.VISIBLE
      binding.imageMainLogo.visibility = if (showPickerNotLogo) View.VISIBLE else View.GONE
      binding.imageMainLogo.scaleX = if (showPickerNotLogo) 1f else 0f
      binding.imageMainLogo.scaleY = if (showPickerNotLogo) 1f else 0f
      binding.imageMainLogoPlaceholder.alpha = if (showPickerNotLogo) 0f else 1f
      binding.imageMainLogoPlaceholder.scaleX = if (showPickerNotLogo) 0f else 1f
      binding.imageMainLogoPlaceholder.scaleY = if (showPickerNotLogo) 0f else 1f
    }
  }

  fun showSnackbar(snackbar: Snackbar) {
    snackbar.anchorView = binding.buttonMainPlayStop
    snackbar.show()
  }

  fun getTempoTerm(tempo: Int): String {
    val resId = when {
      tempo < 60 -> R.string.label_tempo_largo
      tempo < 66 -> R.string.label_tempo_larghetto
      tempo < 76 -> R.string.label_tempo_adagio
      tempo < 108 -> R.string.label_tempo_andante
      tempo < 120 -> R.string.label_tempo_moderato
      tempo < 168 -> R.string.label_tempo_allegro
      tempo < 200 -> R.string.label_tempo_presto
      else -> R.string.label_tempo_prestissimo
    }
    return getString(resId)
  }

  fun isReduceAnimations(): Boolean = reduceAnimations
}
