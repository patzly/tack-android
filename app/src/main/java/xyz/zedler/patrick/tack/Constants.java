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
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack;

import androidx.appcompat.app.AppCompatDelegate;

public final class Constants {

  public final static long ANIM_DURATION_LONG = 400;
  public final static long ANIM_DURATION_SHORT = 250;
  public static final long BEAT_ANIM_OFFSET = 25;
  public static final int TEMPO_MIN = 1;
  public static final int TEMPO_MAX = 600;
  public static final int BEATS_MAX = 20;
  public static final int SUBS_MAX = 10;
  public static final int TIMER_MAX = 399;
  public static final int INCREMENTAL_INTERVAL_MAX = 399;
  public static final String SONG_ID_DEFAULT = "default";

  public final static class PREF {
    // General
    public static final String THEME = "app_theme";
    public static final String UI_MODE = "ui_mode";
    public static final String UI_CONTRAST = "ui_contrast";
    public static final String HAPTIC = "haptic_feedback";
    public static final String REDUCE_ANIM = "reduce_animations";
    public static final String LAST_VERSION = "last_version";
    public static final String FEEDBACK_POP_UP_COUNT = "feedback_pop_up_count";
    public static final String SONGS_INTRO_SHOWN = "songs_intro_shown";
    public static final String SONGS_VISIT_COUNT = "songs_visit_count";
    public static final String CHECK_UNLOCK_KEY = "check_installer";
    public static final String PERMISSION_DENIED = "notification_permission_denied";

    // Metronome
    public final static String TEMPO = "tempo";
    public final static String BEATS = "beats";
    public final static String SUBDIVISIONS = "subdivisions";
    public final static String BEAT_MODE = "beat_mode";
    public final static String ACTIVE_BEAT = "highlight_active_beat";
    public final static String SHOW_ELAPSED = "show_elapsed";
    public final static String RESET_TIMER_ON_STOP = "reset_timer";
    public final static String BIG_TIME_TEXT = "big_time_text";
    public final static String PERM_NOTIFICATION = "permanent_notification";
    public final static String FLASH_SCREEN = "flash_screen_strength";
    public final static String KEEP_AWAKE = "keep_screen_awake";
    public final static String SOUND = "sound";
    public final static String LATENCY = "latency_offset";
    public final static String IGNORE_FOCUS = "ignore_focus";
    public final static String GAIN = "gain";
    public final static String BIG_LOGO = "big_logo";
    public final static String TEMPO_INPUT_KEYBOARD = "tempo_input_keyboard";
    public final static String TEMPO_TAP_INSTANT = "tempo_tap_instant";

    // Options
    public final static String COUNT_IN = "count_in";
    public final static String INCREMENTAL_AMOUNT = "incremental_amount";
    public final static String INCREMENTAL_INCREASE = "incremental_increase";
    public final static String INCREMENTAL_INTERVAL = "incremental_interval";
    public final static String INCREMENTAL_UNIT = "incremental_unit";
    public final static String INCREMENTAL_LIMIT = "incremental_limit";
    public final static String TIMER_DURATION = "timer_duration";
    public final static String TIMER_UNIT = "timer_unit";
    public final static String MUTE_PLAY = "mute_play";
    public final static String MUTE_MUTE = "mute_mute";
    public final static String MUTE_UNIT = "mute_unit";
    public final static String MUTE_RANDOM = "mute_random";

    // Song library
    public static final String SONGS_ORDER = "songs_order";
    public static final String SONG_CURRENT_ID = "current_song_id";
    public static final String PART_CURRENT_INDEX = "current_part_index";
  }

  public final static class DEF {
    // General
    public static final String THEME = "";
    public static final int UI_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final String UI_CONTRAST = CONTRAST.STANDARD;
    public static final boolean REDUCE_ANIM = false;

    // Metronome
    public final static int TEMPO = 120;
    public final static String BEATS = String.join(
        ",", TICK_TYPE.STRONG, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL
    );
    public final static String SUBDIVISIONS = TICK_TYPE.MUTED;
    public final static String BEAT_MODE = Constants.BEAT_MODE.ALL;
    public final static boolean ACTIVE_BEAT = false;
    public final static boolean SHOW_ELAPSED = false;
    public final static boolean RESET_TIMER_ON_STOP = false;
    public final static boolean BIG_TIME_TEXT = false;
    public final static boolean PERM_NOTIFICATION = false;
    public final static String FLASH_SCREEN = Constants.FLASH_SCREEN.OFF;
    public final static String KEEP_AWAKE = Constants.KEEP_AWAKE.WHILE_PLAYING;
    public final static String SOUND = Constants.SOUND.SINE;
    public final static long LATENCY = 100;
    public final static boolean IGNORE_FOCUS = false;
    public final static int GAIN = 0;
    public final static boolean BIG_LOGO = false;
    public final static boolean TEMPO_INPUT_KEYBOARD = false;
    public final static boolean TEMPO_TAP_INSTANT = true;

    // Options
    public final static int COUNT_IN = 0;
    public final static int INCREMENTAL_AMOUNT = 0;
    public final static boolean INCREMENTAL_INCREASE = true;
    public final static int INCREMENTAL_INTERVAL = 1;
    public final static String INCREMENTAL_UNIT = UNIT.BARS;
    public final static int INCREMENTAL_LIMIT = 0;
    public final static int TIMER_DURATION = 0;
    public final static String TIMER_UNIT = UNIT.BARS;
    public final static int MUTE_PLAY = 0;
    public final static int MUTE_MUTE = 1;
    public final static String MUTE_UNIT = UNIT.BARS;
    public final static boolean MUTE_RANDOM = false;

    // Song library
    public final static int SONGS_ORDER = 0;
    public final static String SONG_CURRENT_ID = SONG_ID_DEFAULT;
    public final static int PART_CURRENT_INDEX = 0;
  }

  public final static class SOUND {

    public final static String SINE = "sine";
    public final static String WOOD = "wood";
    public final static String MECHANICAL = "mechanical";
    public final static String BEATBOXING_1 = "beatboxing_1";
    public final static String BEATBOXING_2 = "beatboxing_2";
    public final static String HANDS = "hands";
    public final static String FOLDING = "folding";
  }

  public final static class BEAT_MODE {

    public final static String ALL = "all";
    public final static String SOUND = "sound";
    public final static String VIBRATION = "vibration";
  }

  public final static class FLASH_SCREEN {

    public final static String OFF = "off";
    public final static String SUBTLE = "subtle";
    public final static String STRONG = "strong";
  }

  public final static class KEEP_AWAKE {

    public final static String ALWAYS = "always";
    public final static String WHILE_PLAYING = "while_playing";
    public final static String NEVER = "never";
  }

  public final static class TICK_TYPE {

    public final static String NORMAL = "normal";
    public final static String STRONG = "strong";
    public final static String SUB = "sub";
    public final static String MUTED = "muted";
  }

  public final static class UNIT {

    public final static String BEATS = "beats";
    public final static String BARS = "bars";
    public final static String SECONDS = "seconds";
    public final static String MINUTES = "minutes";
  }

  public final static class SONGS_ORDER {

    public final static int NAME_ASC = 0;
    public final static int LAST_PLAYED_ASC = 2;
    public final static int MOST_PLAYED_ASC = 4;
  }

  public final static class ACTION {

    public final static String START = "xyz.zedler.patrick.tack.intent.action.START";
    public final static String STOP = "xyz.zedler.patrick.tack.intent.action.STOP";
    public final static String APPLY_SONG = "xyz.zedler.patrick.tack.intent.action.APPLY_SONG";
    public final static String START_SONG = "xyz.zedler.patrick.tack.intent.action.START_SONG";
    public final static String SHOW_SONGS = "xyz.zedler.patrick.tack.intent.action.SHOW_SONGS";
  }

  public final static class EXTRA {

    public static final String RUN_AS_SUPER_CLASS = "run_as_super_class";
    public static final String INSTANCE_STATE = "instance_state";
    public static final String SCROLL_POSITION = "scroll_position";
    public static final String SONG_ID = "song_id";
  }

  public static final class THEME {

    public static final String DYNAMIC = "dynamic";
    public static final String RED = "red";
    public static final String YELLOW = "yellow";
    public static final String GREEN = "green";
    public static final String BLUE = "blue";
  }

  public static final class CONTRAST {

    public static final String STANDARD = "standard";
    public static final String MEDIUM = "medium";
    public static final String HIGH = "high";
  }
}
