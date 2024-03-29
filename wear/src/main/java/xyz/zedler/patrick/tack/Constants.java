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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack;

public final class Constants {

  public final static long ANIM_DURATION_LONG = 400;
  public final static long ANIM_DURATION_SHORT = 250;
  public static final long BEAT_ANIM_OFFSET = 25;
  public static final long FLASH_SCREEN_DURATION = 100;
  public static final int TEMPO_MIN = 1;
  public static final int TEMPO_MAX = 400;
  public static final int BEATS_MAX = 20;
  public static final int SUBS_MAX = 10;

  public final static class PREF {
    public final static String TEMPO = "tempo";
    public final static String BEATS = "beats";
    public final static String SUBDIVISIONS = "subdivisions";
    public final static String BEAT_MODE_VIBRATE = "beat_mode_vibrate";
    public final static String USE_SUBS = "use_subdivisions";
    public final static String ALWAYS_VIBRATE = "always_vibrate";
    public final static String FLASH_SCREEN = "flash_screen";
    public final static String KEEP_AWAKE = "keep_awake";
    public final static String SOUND = "sound";
    public final static String LATENCY = "latency_offset";
    public final static String IGNORE_FOCUS = "ignore_focus";
    public final static String GAIN = "gain";
    public final static String BOOKMARK = "bookmark";
    public final static String WRIST_GESTURES = "wrist_gestures";
  }

  public final static class DEF {
    public final static int TEMPO = 120;
    public final static String BEATS = String.join(
        ",", TICK_TYPE.STRONG, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL
    );
    public final static String SUBDIVISIONS = TICK_TYPE.MUTED;
    public final static boolean BEAT_MODE_VIBRATE = false;
    public final static boolean USE_SUBS = true;
    public final static boolean ALWAYS_VIBRATE = true;
    public final static boolean FLASH_SCREEN = false;
    public final static boolean KEEP_AWAKE = true;
    public final static String SOUND = Constants.SOUND.SINE;
    public final static long LATENCY = 100;
    public final static boolean IGNORE_FOCUS = false;
    public final static int GAIN = 0;
    public final static boolean WRIST_GESTURES = true;
  }

  public final static class SOUND {

    public final static String WOOD = "wood";
    public final static String SINE = "sine";
    public final static String MECHANICAL = "mechanical";
    public final static String FOLDING = "folding";
  }

  public final static class TICK_TYPE {

    public final static String NORMAL = "normal";
    public final static String STRONG = "strong";
    public final static String SUB = "sub";
    public final static String MUTED = "muted";
  }

  public final static class ACTION {

    public final static String START = "xyz.zedler.patrick.tack.intent.action.START";
    public final static String STOP = "xyz.zedler.patrick.tack.intent.action.STOP";
  }
}
