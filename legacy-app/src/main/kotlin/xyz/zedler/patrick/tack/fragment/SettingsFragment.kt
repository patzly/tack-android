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

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.snackbar.Snackbar
import xyz.zedler.patrick.tack.Constants.BEAT_MODE
import xyz.zedler.patrick.tack.Constants.CONTRAST
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.EXTRA
import xyz.zedler.patrick.tack.Constants.FLASHLIGHT
import xyz.zedler.patrick.tack.Constants.FLASH_SCREEN
import xyz.zedler.patrick.tack.Constants.KEEP_AWAKE
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.SOUND
import xyz.zedler.patrick.tack.Constants.THEME
import xyz.zedler.patrick.tack.Constants.VIBRATION_INTENSITY
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.ScrollBehavior
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.databinding.FragmentSettingsBinding
import xyz.zedler.patrick.tack.util.DialogUtil
import xyz.zedler.patrick.tack.util.FlashlightUtil
import xyz.zedler.patrick.tack.util.HapticUtil
import xyz.zedler.patrick.tack.util.followsSystem
import xyz.zedler.patrick.tack.util.getLocaleName
import xyz.zedler.patrick.tack.util.ShortcutUtil
import xyz.zedler.patrick.tack.util.dialog.BackupDialogUtil
import xyz.zedler.patrick.tack.util.dialog.GainDialogUtil
import xyz.zedler.patrick.tack.util.dialog.LanguagesDialogUtil
import xyz.zedler.patrick.tack.util.dialog.LatencyDialogUtil
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.edit
import xyz.zedler.patrick.tack.util.isLayoutRtl
import xyz.zedler.patrick.tack.util.keepScreenAwake
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showMenu
import xyz.zedler.patrick.tack.util.startIcon
import xyz.zedler.patrick.tack.util.uncheckAllChildren
import xyz.zedler.patrick.tack.view.ThemeSelectionCardView

class SettingsFragment : BaseFragment(), View.OnClickListener,
  CompoundButton.OnCheckedChangeListener, MaterialButtonToggleGroup.OnButtonCheckedListener {

  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!

  private var savedState: Bundle? = null
  private lateinit var dialogUtilReset: DialogUtil
  private lateinit var dialogUtilSound: DialogUtil
  private lateinit var languagesDialogUtil: LanguagesDialogUtil
  private lateinit var gainDialogUtil: GainDialogUtil
  private lateinit var latencyDialogUtil: LatencyDialogUtil
  private lateinit var backupDialogUtil: BackupDialogUtil

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    dialogUtilReset.dismiss()
    dialogUtilSound.dismiss()
    languagesDialogUtil.dismiss()
    gainDialogUtil.dismiss()
    latencyDialogUtil.dismiss()
    backupDialogUtil.dismiss()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    savedState = savedInstanceState
    SystemBarBehavior(activity).apply {
      setAppBar(binding.appBarSettings)
      setScroll(binding.scrollSettings, binding.linearSettingsContainer)
      setUp()
    }

    ScrollBehavior().setUpScroll(
      binding.appBarSettings,
      binding.scrollSettings,
      ScrollBehavior.LIFT_ON_SCROLL
    )

    binding.buttonSettingsBack.setOnClickListener(getNavigationOnClickListener())
    binding.buttonSettingsMenu.setOnClickListener { v ->
      performHapticClick()
      v.showMenu(R.menu.menu_settings, { item ->
        val id = item.itemId
        if (getViewUtil().isClickDisabled(id)) return@showMenu false
        performHapticClick()
        when (id) {
          R.id.action_feedback -> activity.showFeedbackDialog()
          R.id.action_about -> activity.navigate(
            SettingsFragmentDirections.actionSettingsToAbout()
          )

          R.id.action_help -> activity.showHelpDialog()
          R.id.action_log -> activity.navigate(
            SettingsFragmentDirections.actionSettingsToLog()
          )
        }
        true
      })
    }
    binding.buttonSettingsBack.setTooltipText(R.string.action_back)
    binding.buttonSettingsMenu.setTooltipText(R.string.action_more)

    binding.textSettingsLanguage.text = if (followsSystem()) {
      getString(R.string.settings_language_system)
    } else {
      getLocaleName()
    }

    setUpThemeSelection()

    val idMode = when (sharedPrefs.getInt(PREF.UI_MODE, DEF.UI_MODE)) {
      AppCompatDelegate.MODE_NIGHT_NO -> R.id.button_settings_theme_light
      AppCompatDelegate.MODE_NIGHT_YES -> R.id.button_settings_theme_dark
      else -> R.id.button_settings_theme_auto
    }
    binding.toggleSettingsTheme.check(idMode)
    binding.toggleSettingsTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      val pref = when (checkedId) {
        R.id.button_settings_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
        R.id.button_settings_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      }
      sharedPrefs.edit { putInt(PREF.UI_MODE, pref) }
      performHapticClick()
      binding.imageSettingsTheme.startIcon()
      activity.restartToApply(
        200, getInstanceState(), restoreState = true, stopService = false
      )
    }

    val idContrast = when (
      sharedPrefs.getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)
    ) {
      CONTRAST.MEDIUM -> R.id.button_settings_contrast_medium
      CONTRAST.HIGH -> R.id.button_settings_contrast_high
      else -> R.id.button_settings_contrast_standard
    }
    binding.toggleSettingsContrast.check(idContrast)
    binding.toggleSettingsContrast.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      val pref = when (checkedId) {
        R.id.button_settings_contrast_medium -> CONTRAST.MEDIUM
        R.id.button_settings_contrast_high -> CONTRAST.HIGH
        else -> CONTRAST.STANDARD
      }
      sharedPrefs.edit { putString(PREF.UI_CONTRAST, pref) }
      performHapticClick()
      binding.imageSettingsContrast.startIcon()
      activity.restartToApply(
        0, getInstanceState(), true, stopService = false
      )
    }

    val currentTheme = sharedPrefs.getString(PREF.THEME, DEF.THEME) ?: DEF.THEME
    val hasDynamic = DynamicColors.isDynamicColorAvailable()
    val isDynamic = if (currentTheme.isEmpty()) hasDynamic else currentTheme == THEME.DYNAMIC
    binding.toggleSettingsContrast.isEnabled = !isDynamic
    binding.textSettingsContrastDynamic.visibility = if (isDynamic) View.VISIBLE else View.GONE
    binding.textSettingsContrastDynamic.setText(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        R.string.settings_contrast_dynamic
      } else {
        R.string.settings_contrast_dynamic_unsupported
      }
    )

    binding.switchSettingsHaptic.isChecked = sharedPrefs.getBoolean(
      PREF.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(activity)
    )
    binding.switchSettingsHaptic.jumpDrawablesToCurrentState()

    binding.buttonSettingsVibrationIntensityAuto.visibility =
      if (activity.hapticUtil.supportsMainEffects()) View.VISIBLE else View.GONE
    binding.toggleSettingsVibrationIntensity.removeOnButtonCheckedListener(this)
    val idVibrationIntensity = when (activity.hapticUtil.intensity) {
      VIBRATION_INTENSITY.SOFT -> R.id.button_settings_vibration_intensity_soft
      VIBRATION_INTENSITY.STRONG -> R.id.button_settings_vibration_intensity_strong
      else -> R.id.button_settings_vibration_intensity_auto
    }
    binding.toggleSettingsVibrationIntensity.check(idVibrationIntensity)
    binding.toggleSettingsVibrationIntensity.jumpDrawablesToCurrentState()
    binding.toggleSettingsVibrationIntensity.addOnButtonCheckedListener(this)

    val hasVibrator = activity.hapticUtil.hasVibrator()
    binding.linearSettingsHaptic.visibility = if (hasVibrator) View.VISIBLE else View.GONE
    binding.linearSettingsVibrationIntensity.visibility =
      if (hasVibrator) View.VISIBLE else View.GONE
    binding.linearSettingsReduceAnimations.setBackgroundResource(
      if (hasVibrator) R.drawable.ripple_list_item_bg_segmented_last
      else R.drawable.ripple_list_item_bg_segmented_single
    )

    binding.switchSettingsReduceAnimations.isChecked = sharedPrefs.getBoolean(
      PREF.REDUCE_ANIM, DEF.REDUCE_ANIM
    )
    binding.switchSettingsReduceAnimations.jumpDrawablesToCurrentState()

    binding.switchSettingsActiveBeat.isChecked = sharedPrefs.getBoolean(
      PREF.ACTIVE_BEAT, DEF.ACTIVE_BEAT
    )
    binding.switchSettingsActiveBeat.jumpDrawablesToCurrentState()

    binding.switchSettingsBigTimeText.isChecked = sharedPrefs.getBoolean(
      PREF.BIG_TIME_TEXT, DEF.BIG_TIME_TEXT
    )
    binding.switchSettingsBigTimeText.jumpDrawablesToCurrentState()

    binding.switchSettingsBigLogo.isChecked = sharedPrefs.getBoolean(
      PREF.BIG_LOGO, DEF.BIG_LOGO
    )
    binding.switchSettingsBigLogo.jumpDrawablesToCurrentState()

    dialogUtilReset = DialogUtil(activity, "reset")
    dialogUtilReset.createDialogError { builder ->
      builder.setTitle(R.string.msg_reset)
      builder.setMessage(R.string.msg_reset_description)
      builder.setPositiveButton(R.string.action_reset) { _, _ ->
        performHapticClick()
        metronomeEngine?.stop()
        sharedPrefs.edit { clear() }
        activity.songViewModel.deleteAll()
        ShortcutUtil(activity).removeAllShortcuts()
        activity.restartToApply(
          100, getInstanceState(), restoreState = false, stopService = true
        )
      }
      builder.setNegativeButton(R.string.action_cancel) { _, _ -> performHapticClick() }
    }
    dialogUtilReset.showIfWasShown(savedInstanceState)

    dialogUtilSound = DialogUtil(activity, "sound")
    languagesDialogUtil = LanguagesDialogUtil(activity)
    languagesDialogUtil.showIfWasShown(savedInstanceState)
    gainDialogUtil = GainDialogUtil(activity, this)
    gainDialogUtil.showIfWasShown(savedInstanceState)
    latencyDialogUtil = LatencyDialogUtil(activity, this)
    latencyDialogUtil.showIfWasShown(savedInstanceState)
    backupDialogUtil = BackupDialogUtil(activity, this)
    backupDialogUtil.showIfWasShown(savedInstanceState)

    updateMetronomeControls(true)

    val clickListeners = arrayOf(
      binding.linearSettingsLanguage, binding.linearSettingsHaptic,
      binding.linearSettingsReduceAnimations, binding.linearSettingsBackup,
      binding.linearSettingsReset, binding.linearSettingsSound,
      binding.linearSettingsLatency, binding.linearSettingsIgnoreFocus,
      binding.linearSettingsGain, binding.linearSettingsActiveBeat,
      binding.linearSettingsPermNotification, binding.linearSettingsElapsed,
      binding.linearSettingsResetTimer, binding.linearSettingsBigTimeText,
      binding.linearSettingsBigLogo
    )
    clickListeners.forEach { it.setOnClickListener(this) }

    val checkListeners = arrayOf(
      binding.switchSettingsHaptic, binding.switchSettingsReduceAnimations,
      binding.switchSettingsIgnoreFocus, binding.switchSettingsActiveBeat,
      binding.switchSettingsPermNotification, binding.switchSettingsElapsed,
      binding.switchSettingsResetTimer, binding.switchSettingsBigTimeText,
      binding.switchSettingsBigLogo
    )
    checkListeners.forEach { it.setOnCheckedChangeListener(this) }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    dialogUtilReset.saveState(outState)
    dialogUtilSound.saveState(outState)
    languagesDialogUtil.saveState(outState)
    gainDialogUtil.saveState(outState)
    latencyDialogUtil.saveState(outState)
    backupDialogUtil.saveState(outState)
  }

  override fun updateMetronomeControls(init: Boolean) {
    val engine = metronomeEngine ?: return
    val labels = linkedMapOf(
      SOUND.SINE to getString(R.string.settings_sound_sine),
      SOUND.WOOD to getString(R.string.settings_sound_wood),
      SOUND.MECHANICAL to getString(R.string.settings_sound_mechanical),
      SOUND.BEATBOXING_1 to getString(R.string.settings_sound_beatboxing_1),
      SOUND.BEATBOXING_2 to getString(R.string.settings_sound_beatboxing_2),
      SOUND.HANDS to getString(R.string.settings_sound_hands),
      SOUND.FOLDING to getString(R.string.settings_sound_folding)
    )
    val sounds = ArrayList(labels.keys)
    val items = labels.values.toTypedArray()
    var initItem = sounds.indexOf(engine.getSound())
    if (initItem == -1) {
      initItem = 0
      sharedPrefs.edit { remove(PREF.SOUND) }
    }
    binding.textSettingsSound.text = items[initItem]
    dialogUtilSound.createDialog { builder ->
      builder.setTitle(R.string.settings_sound)
      builder.setSingleChoiceItems(items, initItem) { _, which ->
        performHapticClick()
        metronomeEngine?.setSound(sounds[which])
        binding.textSettingsSound.text = items[which]
      }
      builder.setPositiveButton(R.string.action_close) { _, _ -> performHapticClick() }
    }
    dialogUtilSound.showIfWasShown(savedState)

    gainDialogUtil.showIfWasShown(savedState)
    latencyDialogUtil.showIfWasShown(savedState)

    binding.switchSettingsIgnoreFocus.setOnCheckedChangeListener(null)
    binding.switchSettingsIgnoreFocus.isChecked = engine.getIgnoreAudioFocus()
    binding.switchSettingsIgnoreFocus.jumpDrawablesToCurrentState()
    binding.switchSettingsIgnoreFocus.setOnCheckedChangeListener(this)

    updateGainDescription(engine.getGain())
    updateLatencyDescription(engine.getLatency())

    binding.switchSettingsElapsed.setOnCheckedChangeListener(null)
    binding.switchSettingsElapsed.isChecked = engine.getShowElapsed()
    binding.switchSettingsElapsed.jumpDrawablesToCurrentState()
    binding.switchSettingsElapsed.setOnCheckedChangeListener(this)

    binding.switchSettingsResetTimer.setOnCheckedChangeListener(null)
    binding.switchSettingsResetTimer.isChecked = engine.getResetTimerOnStop()
    binding.switchSettingsResetTimer.jumpDrawablesToCurrentState()
    binding.switchSettingsResetTimer.setOnCheckedChangeListener(this)

    binding.toggleSettingsFlashScreen.removeOnButtonCheckedListener(this)
    val idFlashScreen = when (sharedPrefs.getString(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN)) {
      FLASH_SCREEN.SUBTLE -> R.id.button_settings_flash_screen_subtle
      FLASH_SCREEN.STRONG -> R.id.button_settings_flash_screen_strong
      else -> R.id.button_settings_flash_screen_off
    }
    binding.toggleSettingsFlashScreen.check(idFlashScreen)
    binding.toggleSettingsFlashScreen.jumpDrawablesToCurrentState()
    binding.toggleSettingsFlashScreen.addOnButtonCheckedListener(this)

    binding.linearSettingsFlashlight.visibility =
      if (FlashlightUtil.hasFlash(activity)) View.VISIBLE else View.GONE
    binding.toggleSettingsFlashlight.removeOnButtonCheckedListener(this)
    binding.buttonSettingsFlashlightSubtle.visibility =
      if (FlashlightUtil.hasStrengthControl(activity)) View.VISIBLE else View.GONE
    val idFlashlight = when (sharedPrefs.getString(PREF.FLASHLIGHT, DEF.FLASHLIGHT)) {
      FLASHLIGHT.SUBTLE -> R.id.button_settings_flashlight_subtle
      FLASHLIGHT.STRONG -> R.id.button_settings_flashlight_strong
      else -> R.id.button_settings_flashlight_off
    }
    binding.toggleSettingsFlashlight.check(idFlashlight)
    binding.toggleSettingsFlashlight.jumpDrawablesToCurrentState()
    binding.toggleSettingsFlashlight.addOnButtonCheckedListener(this)

    binding.toggleSettingsKeepAwake.removeOnButtonCheckedListener(this)
    val idKeepAwake = when (sharedPrefs.getString(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE)) {
      KEEP_AWAKE.WHILE_PLAYING -> R.id.button_settings_keep_awake_while_playing
      KEEP_AWAKE.NEVER -> R.id.button_settings_keep_awake_never
      else -> R.id.button_settings_keep_awake_always
    }
    binding.toggleSettingsKeepAwake.check(idKeepAwake)
    binding.toggleSettingsKeepAwake.jumpDrawablesToCurrentState()
    binding.toggleSettingsKeepAwake.addOnButtonCheckedListener(this)

    val service = activity.getMetronomeService()
    val permNotification = service?.usePermNotification() ?: false
    binding.switchSettingsPermNotification.setOnCheckedChangeListener(null)
    binding.switchSettingsPermNotification.isChecked = permNotification
    binding.switchSettingsPermNotification.jumpDrawablesToCurrentState()
    binding.switchSettingsPermNotification.setOnCheckedChangeListener(this)
  }

  override fun onClick(v: View) {
    when (val id = v.id) {
      R.id.linear_settings_language if getViewUtil().isClickEnabled(id) -> {
        performHapticClick()
        binding.imageSettingsLanguage.startIcon()
        languagesDialogUtil.show()
      }
      R.id.linear_settings_haptic -> {
        binding.switchSettingsHaptic.toggle()
      }
      R.id.linear_settings_reduce_animations -> {
        binding.switchSettingsReduceAnimations.toggle()
      }
      R.id.linear_settings_backup if getViewUtil().isClickEnabled(id) -> {
        performHapticClick()
        backupDialogUtil.show()
      }
      R.id.linear_settings_reset if getViewUtil().isClickEnabled(id) -> {
        performHapticClick()
        dialogUtilReset.show()
      }
      R.id.linear_settings_sound if getViewUtil().isClickEnabled(id) -> {
        binding.imageSettingsSound.startIcon()
        performHapticClick()
        dialogUtilSound.show()
      }
      R.id.linear_settings_latency if getViewUtil().isClickEnabled(id) -> {
        performHapticClick()
        latencyDialogUtil.show()
      }
      R.id.linear_settings_ignore_focus -> {
        binding.switchSettingsIgnoreFocus.toggle()
      }
      R.id.linear_settings_gain if getViewUtil().isClickEnabled(id) -> {
        performHapticClick()
        gainDialogUtil.show()
      }
      R.id.linear_settings_active_beat -> {
        binding.switchSettingsActiveBeat.toggle()
      }
      R.id.linear_settings_perm_notification -> {
        binding.switchSettingsPermNotification.toggle()
      }
      R.id.linear_settings_elapsed -> {
        binding.switchSettingsElapsed.toggle()
      }
      R.id.linear_settings_reset_timer -> {
        binding.switchSettingsResetTimer.toggle()
      }
      R.id.linear_settings_big_time_text -> {
        binding.switchSettingsBigTimeText.toggle()
      }
      R.id.linear_settings_big_logo -> {
        binding.switchSettingsBigLogo.toggle()
      }
    }
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    val engine = metronomeEngine ?: return
    when (buttonView.id) {
      R.id.switch_settings_haptic -> {
        performHapticClick()
        binding.imageSettingsHaptic.startIcon()
        sharedPrefs.edit { putBoolean(PREF.HAPTIC, isChecked) }
        activity.hapticUtil.setEnabled(isChecked)
        if (!isChecked) {
          val beatMode = engine.getBeatMode()
          if (beatMode == BEAT_MODE.ALL || beatMode == BEAT_MODE.VIBRATION) {
            val snackbar = activity.getSnackbar(
              R.string.msg_beat_mode_warning, Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.action_apply) {
              activity.performHapticClick()
              metronomeEngine?.setBeatMode(BEAT_MODE.SOUND)
            }
            activity.showSnackbar(snackbar)
          }
        }
      }

      R.id.switch_settings_reduce_animations -> {
        performHapticClick()
        binding.imageSettingsReduceAnimations.startIcon()
        sharedPrefs.edit { putBoolean(PREF.REDUCE_ANIM, isChecked) }
      }

      R.id.switch_settings_ignore_focus -> {
        performHapticClick()
        binding.imageSettingsIgnoreFocus.startIcon()
        engine.setIgnoreFocus(isChecked)
      }

      R.id.switch_settings_active_beat -> {
        performHapticClick()
        sharedPrefs.edit { putBoolean(PREF.ACTIVE_BEAT, isChecked) }
      }

      R.id.switch_settings_perm_notification -> {
        performHapticClick()
        val service = activity.getMetronomeService()
        if (service != null) {
          try {
            val permNotification = service.setPermNotification(isChecked)
            binding.switchSettingsPermNotification.isChecked = permNotification
          } catch (_: IllegalStateException) {
            binding.switchSettingsPermNotification.isChecked = false
            activity.requestNotificationPermission(false)
          }
        }
      }

      R.id.switch_settings_elapsed -> {
        binding.imageSettingsElapsed.startIcon()
        performHapticClick()
        engine.setShowElapsed(isChecked)
      }

      R.id.switch_settings_reset_timer -> {
        binding.imageSettingsResetTimer.startIcon()
        performHapticClick()
        engine.setResetTimerOnStop(isChecked)
      }

      R.id.switch_settings_big_time_text -> {
        performHapticClick()
        sharedPrefs.edit { putBoolean(PREF.BIG_TIME_TEXT, isChecked) }
      }

      R.id.switch_settings_big_logo -> {
        performHapticClick()
        binding.imageSettingsBigLogo.startIcon()
        sharedPrefs.edit { putBoolean(PREF.BIG_LOGO, isChecked) }
      }
    }
  }

  override fun onButtonChecked(
    group: MaterialButtonToggleGroup,
    checkedId: Int,
    isChecked: Boolean
  ) {
    val engine = metronomeEngine
    if (!isChecked || engine == null) return
    when (group.id) {
      R.id.toggle_settings_vibration_intensity -> {
        val vibrationIntensity = when (checkedId) {
          R.id.button_settings_vibration_intensity_soft -> VIBRATION_INTENSITY.SOFT
          R.id.button_settings_vibration_intensity_strong -> VIBRATION_INTENSITY.STRONG
          else -> VIBRATION_INTENSITY.AUTO
        }
        engine.setVibrationIntensity(vibrationIntensity)
        activity.hapticUtil.intensity = vibrationIntensity
        performHapticClick()
      }

      R.id.toggle_settings_flash_screen -> {
        val flashScreen = when (checkedId) {
          R.id.button_settings_flash_screen_subtle -> FLASH_SCREEN.SUBTLE
          R.id.button_settings_flash_screen_strong -> FLASH_SCREEN.STRONG
          else -> FLASH_SCREEN.OFF
        }
        engine.setFlashScreen(flashScreen)
        performHapticClick()
        binding.imageSettingsFlashScreen.startIcon()
      }

      R.id.toggle_settings_flashlight -> {
        val flashlight = when (checkedId) {
          R.id.button_settings_flashlight_subtle -> FLASHLIGHT.SUBTLE
          R.id.button_settings_flashlight_strong -> FLASHLIGHT.STRONG
          else -> FLASHLIGHT.OFF
        }
        engine.setFlashlight(flashlight)
        performHapticClick()
        binding.imageSettingsFlashlight.startIcon()
      }

      R.id.toggle_settings_keep_awake -> {
        val keepAwake = when (checkedId) {
          R.id.button_settings_keep_awake_while_playing -> KEEP_AWAKE.WHILE_PLAYING
          R.id.button_settings_keep_awake_never -> KEEP_AWAKE.NEVER
          else -> KEEP_AWAKE.ALWAYS
        }
        engine.setKeepAwake(keepAwake)
        performHapticClick()
        binding.imageSettingsKeepAwake.startIcon()
        val keepAwakeNow = keepAwake == KEEP_AWAKE.ALWAYS ||
            (keepAwake == KEEP_AWAKE.WHILE_PLAYING && engine.isPlaying())
        activity.keepScreenAwake(keepAwakeNow)
      }
    }
  }

  fun updateGainDescription(gain: Int) {
    if (_binding != null) {
      binding.textSettingsGain.text = getString(
        R.string.label_db_signed, if (gain > 0) "+$gain" else gain.toString()
      )
    }
  }

  fun updateLatencyDescription(latency: Long) {
    if (_binding != null) {
      binding.textSettingsLatency.text = getString(R.string.label_ms, latency.toString())
    }
  }

  private fun setUpThemeSelection() {
    val hasDynamic = DynamicColors.isDynamicColorAvailable()
    val container = binding.linearSettingsThemeContainer
    for (i in (if (hasDynamic) -1 else 0)..3) {
      val name: String
      val resId: Int
      when (i) {
        -1 -> {
          name = THEME.DYNAMIC
          resId = -1
        }

        0 -> {
          name = THEME.RED
          resId = getContrastThemeResId(
            R.style.Theme_Tack_Red,
            R.style.ThemeOverlay_Tack_Red_MediumContrast,
            R.style.ThemeOverlay_Tack_Red_HighContrast
          )
        }

        2 -> {
          name = THEME.GREEN
          resId = getContrastThemeResId(
            R.style.Theme_Tack_Green,
            R.style.ThemeOverlay_Tack_Green_MediumContrast,
            R.style.ThemeOverlay_Tack_Green_HighContrast
          )
        }

        3 -> {
          name = THEME.BLUE
          resId = getContrastThemeResId(
            R.style.Theme_Tack_Blue,
            R.style.ThemeOverlay_Tack_Blue_MediumContrast,
            R.style.ThemeOverlay_Tack_Blue_HighContrast
          )
        }

        else -> {
          name = THEME.YELLOW
          resId = getContrastThemeResId(
            R.style.Theme_Tack_Yellow,
            R.style.ThemeOverlay_Tack_Yellow_MediumContrast,
            R.style.ThemeOverlay_Tack_Yellow_HighContrast
          )
        }
      }

      val card = ThemeSelectionCardView(activity).apply {
        setNestedContext(
          if (i == -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.wrapContextIfAvailable(activity)
          } else {
            ContextThemeWrapper(activity, resId)
          }
        )
        setOnClickListener {
          if (!isChecked && getViewUtil().isClickEnabled(i)) {
            startCheckedIcon()
            binding.imageSettingsTheme.startIcon()
            performHapticClick()
            uncheckAllChildren(container)
            isChecked = true
            sharedPrefs.edit { putString(PREF.THEME, name) }
            activity.restartToApply(
              100, getInstanceState(), true, stopService = false
            )
          }
        }
      }

      val selected = sharedPrefs.getString(PREF.THEME, DEF.THEME)
      card.isChecked = if (selected.isNullOrEmpty()) {
        if (hasDynamic) name == THEME.DYNAMIC else name == THEME.YELLOW
      } else {
        selected == name
      }
      container.addView(card)

      if (hasDynamic && i == -1) {
        val divider = MaterialDivider(activity).apply {
          layoutParams = LinearLayout.LayoutParams(
            activity.dpToPx(1f), activity.dpToPx(40f)
          ).apply {
            val isRtl = activity.isLayoutRtl()
            val marginLeft = activity.dpToPx(if (isRtl) 8f else 4f)
            val marginRight = activity.dpToPx(if (isRtl) 4f else 8f)
            setMargins(marginLeft, 0, marginRight, 0)
            gravity = Gravity.CENTER_VERTICAL
          }
        }
        container.addView(divider)
      }
    }

    val bundleInstanceState = activity.intent.getBundleExtra(EXTRA.INSTANCE_STATE)
    bundleInstanceState?.let {
      binding.scrollHorizSettingsTheme.scrollTo(it.getInt(EXTRA.SCROLL_POSITION, 0), 0)
    }
  }

  private fun getContrastThemeResId(
    resIdStandard: Int,
    resIdMedium: Int,
    resIdHigh: Int
  ): Int {
    return when (sharedPrefs.getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)) {
      CONTRAST.MEDIUM -> resIdMedium
      CONTRAST.HIGH -> resIdHigh
      else -> resIdStandard
    }
  }

  private fun getInstanceState(): Bundle {
    return Bundle().apply {
      _binding?.let { putInt(EXTRA.SCROLL_POSITION, it.scrollHorizSettingsTheme.scrollX) }
    }
  }

  companion object {
    private val TAG = SettingsFragment::class.java.simpleName
  }
}
