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

package xyz.zedler.patrick.tack

object Constants {

  const val ANIM_DURATION_LONG: Long = 400
  const val ANIM_DURATION_SHORT: Long = 250
  const val BEAT_ANIM_OFFSET: Long = 25
  const val TEMPO_MIN: Int = 1
  const val TEMPO_MAX: Int = 600
  const val BEATS_MAX: Int = 20
  const val SUBS_MAX: Int = 10
  const val COUNT_IN_MAX: Int = 4
  const val TIMER_MAX: Int = 399
  const val INCREMENTAL_AMOUNT_MAX: Int = 99
  const val INCREMENTAL_INTERVAL_MAX: Int = 400
  const val MUTE_PLAY_MAX: Int = 20
  const val MUTE_MUTE_MIN: Int = 1
  const val MUTE_MUTE_MIN_BEATS: Int = 0
  const val MUTE_MUTE_MAX: Int = 20
  const val MUTE_MUTE_MAX_BEATS: Int = 100
  const val MUTE_MUTE_STEP_SIZE: Int = 1
  const val MUTE_MUTE_STEP_SIZE_BEATS: Int = 5
  const val SONG_ID_DEFAULT: String = "default"

  object PREF {
    // General
    const val THEME: String = "app_theme"
    const val UI_MODE: String = "ui_mode"
    const val UI_CONTRAST: String = "ui_contrast"
    const val HAPTIC: String = "haptic_feedback"
    const val VIBRATION_INTENSITY: String = "vibration_intensity"
    const val REDUCE_ANIM: String = "reduce_animations"
    const val LAST_VERSION: String = "last_version"
    const val FEEDBACK_POP_UP_COUNT: String = "feedback_app_start_count"
    const val SONGS_INTRO_SHOWN: String = "songs_intro_shown"
    const val SONGS_VISIT_COUNT: String = "songs_visit_count"
    const val CHECK_UNLOCK_KEY: String = "check_installer"
    const val PERMISSION_DENIED: String = "notification_permission_denied"

    // Metronome
    const val TEMPO: String = "tempo"
    const val BEATS: String = "beats"
    const val SUBDIVISIONS: String = "subdivisions"
    const val USE_POLYRHYTHM: String = "use_polyrhythm"
    const val BEAT_MODE: String = "beat_mode"
    const val ACTIVE_BEAT: String = "highlight_active_beat"
    const val SHOW_ELAPSED: String = "show_elapsed"
    const val RESET_TIMER_ON_STOP: String = "reset_timer"
    const val BIG_TIME_TEXT: String = "big_time_text"
    const val PERM_NOTIFICATION: String = "permanent_notification"
    const val FLASH_SCREEN: String = "flash_screen_strength"
    const val FLASHLIGHT: String = "flashlight_strength"
    const val KEEP_AWAKE: String = "keep_screen_awake"
    const val SOUND: String = "sound"
    const val LATENCY: String = "latency_ms"
    const val IGNORE_FOCUS: String = "ignore_focus"
    const val GAIN: String = "gain"
    const val BIG_LOGO: String = "big_logo"
    const val TEMPO_INPUT_KEYBOARD: String = "tempo_input_keyboard"
    const val TEMPO_TAP_INSTANT: String = "tempo_tap_instant"

    // Options
    const val COUNT_IN: String = "count_in"
    const val INCREMENTAL_AMOUNT: String = "incremental_amount"
    const val INCREMENTAL_INCREASE: String = "incremental_increase"
    const val INCREMENTAL_INTERVAL: String = "incremental_interval"
    const val INCREMENTAL_UNIT: String = "incremental_unit"
    const val INCREMENTAL_LIMIT: String = "incremental_limit"
    const val TIMER_DURATION: String = "timer_duration"
    const val TIMER_UNIT: String = "timer_unit"
    const val MUTE_PLAY: String = "mute_play"
    const val MUTE_MUTE: String = "mute_mute"
    const val MUTE_UNIT: String = "mute_unit"
    const val MUTE_RANDOM: String = "mute_random"

    // Song library
    const val SONGS_ORDER: String = "songs_order"
    const val SONG_CURRENT_ID: String = "current_song_id"
    const val PART_CURRENT_INDEX: String = "current_part_index"
  }

  object DEF {
    // General
    const val THEME: String = ""
    const val UI_MODE: Int = -1
    const val UI_CONTRAST: String = CONTRAST.STANDARD
    const val REDUCE_ANIM: Boolean = false
    const val VIBRATION_INTENSITY: String = Constants.VIBRATION_INTENSITY.AUTO

    // Metronome
    const val TEMPO: Int = 120
    val BEATS: String = listOf(
      TICK_TYPE.STRONG, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL
    ).joinToString(",")
    const val SUBDIVISIONS: String = TICK_TYPE.BEAT_SUB
    const val USE_POLYRHYTHM: Boolean = false
    const val BEAT_MODE: String = Constants.BEAT_MODE.ALL
    const val ACTIVE_BEAT: Boolean = false
    const val SHOW_ELAPSED: Boolean = false
    const val RESET_TIMER_ON_STOP: Boolean = false
    const val BIG_TIME_TEXT: Boolean = false
    const val PERM_NOTIFICATION: Boolean = false
    const val FLASH_SCREEN: String = Constants.FLASH_SCREEN.OFF
    const val FLASHLIGHT: String = Constants.FLASHLIGHT.OFF
    const val KEEP_AWAKE: String = Constants.KEEP_AWAKE.WHILE_PLAYING
    const val SOUND: String = Constants.SOUND.SINE
    const val LATENCY: Long = 0
    const val IGNORE_FOCUS: Boolean = false
    const val GAIN: Int = 0
    const val BIG_LOGO: Boolean = false
    const val TEMPO_INPUT_KEYBOARD: Boolean = false
    const val TEMPO_TAP_INSTANT: Boolean = true

    // Options
    const val COUNT_IN: Int = 0
    const val INCREMENTAL_AMOUNT: Int = 0
    const val INCREMENTAL_INCREASE: Boolean = true
    const val INCREMENTAL_INTERVAL: Int = 1
    const val INCREMENTAL_UNIT: String = UNIT.BARS
    const val INCREMENTAL_LIMIT: Int = 0
    const val TIMER_DURATION: Int = 0
    const val TIMER_UNIT: String = UNIT.BARS
    const val MUTE_PLAY: Int = 1
    const val MUTE_MUTE: Int = 0
    const val MUTE_UNIT: String = UNIT.BEATS
    const val MUTE_RANDOM: Boolean = false

    // Song library
    const val SONGS_ORDER: Int = 0
    const val SONG_CURRENT_ID: String = SONG_ID_DEFAULT
    const val PART_CURRENT_INDEX: Int = 0
  }

  object SOUND {
    const val SINE: String = "sine"
    const val WOOD: String = "wood"
    const val MECHANICAL: String = "mechanical"
    const val BEATBOXING_1: String = "beatboxing_1"
    const val BEATBOXING_2: String = "beatboxing_2"
    const val HANDS: String = "hands"
    const val FOLDING: String = "folding"
  }

  object BEAT_MODE {
    const val ALL: String = "all"
    const val SOUND: String = "sound"
    const val VIBRATION: String = "vibration"
  }

  object FLASH_SCREEN {
    const val OFF: String = "off"
    const val SUBTLE: String = "subtle"
    const val STRONG: String = "strong"
  }

  object FLASHLIGHT {
    const val OFF: String = "off"
    const val SUBTLE: String = "subtle"
    const val STRONG: String = "strong"
  }

  object KEEP_AWAKE {
    const val ALWAYS: String = "always"
    const val WHILE_PLAYING: String = "while_playing"
    const val NEVER: String = "never"
  }

  object TICK_TYPE {
    const val NORMAL: String = "normal"
    const val STRONG: String = "strong"
    const val SUB: String = "sub"
    const val MUTED: String = "muted"
    const val BEAT_SUB: String = "beat_sub"
    const val BEAT_SUB_MUTED: String = "beat_sub_muted"
  }

  object UNIT {
    const val BEATS: String = "beats"
    const val BARS: String = "bars"
    const val SECONDS: String = "seconds"
    const val MINUTES: String = "minutes"
  }

  object SONGS_ORDER {
    const val NAME_ASC: Int = 0
    const val LAST_PLAYED_ASC: Int = 2
    const val MOST_PLAYED_ASC: Int = 4
  }

  object ACTION {
    const val START: String = "xyz.zedler.patrick.tack.intent.action.START"
    const val STOP: String = "xyz.zedler.patrick.tack.intent.action.STOP"
    const val DISMISS: String = "xyz.zedler.patrick.tack.intent.action.DISMISS"
    const val APPLY_SONG: String = "xyz.zedler.patrick.tack.intent.action.APPLY_SONG"
    const val START_SONG: String = "xyz.zedler.patrick.tack.intent.action.START_SONG"
    const val SHOW_SONGS: String = "xyz.zedler.patrick.tack.intent.action.SHOW_SONGS"
  }

  object EXTRA {
    const val RUN_AS_SUPER_CLASS: String = "run_as_super_class"
    const val INSTANCE_STATE: String = "instance_state"
    const val SCROLL_POSITION: String = "scroll_position"
    const val SONG_ID: String = "song_id"
  }

  object THEME {
    const val DYNAMIC: String = "dynamic"
    const val RED: String = "red"
    const val YELLOW: String = "yellow"
    const val GREEN: String = "green"
    const val BLUE: String = "blue"
  }

  object CONTRAST {
    const val STANDARD: String = "standard"
    const val MEDIUM: String = "medium"
    const val HIGH: String = "high"
  }

  object VIBRATION_INTENSITY {
    const val AUTO: String = "auto"
    const val SOFT: String = "soft"
    const val STRONG: String = "strong"
  }
}
