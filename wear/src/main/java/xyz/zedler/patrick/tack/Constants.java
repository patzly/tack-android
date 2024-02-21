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

  public final static int ROTARY_SCROLL_DIVIDER = 4;

  public final static class PREF {

    public final static String BEAT_MODE_VIBRATE = "beat_mode_vibrate";
    public final static String FIRST_START = "first_start";
    public final static String FIRST_PRESS = "first_button_press";
    public final static String FIRST_SPEAKER_MODE = "first_speaker_mode";
    public final static String INTERVAL = "interval";
    public final static String EMPHASIS = "emphasis";
    public final static String BOOKMARK = "bookmark";
  }

  public final static class DEF {

    public final static String SOUND = Constants.SOUND.WOOD;
    public final static boolean HEAVY_VIBRATION = false;
    public final static boolean VIBRATE_ALWAYS = false;
    public final static boolean HAPTIC_FEEDBACK = true;
    public final static boolean WRIST_GESTURES = true;
    public final static boolean ANIMATIONS = true;
    public final static boolean BEAT_MODE_VIBRATE = true;
    public final static boolean FIRST_START = true;
    public final static boolean FIRST_PRESS = true;
    public final static boolean FIRST_SPEAKER_MODE = true;
    public final static long INTERVAL = 500;
    public final static int EMPHASIS = 0;
    public final static int BOOKMARK = -1;
  }

  public final static class SETTINGS {

    public final static String SOUND = "sound";
    public final static String HEAVY_VIBRATION = "heavy_vibration";
    public final static String VIBRATE_ALWAYS = "vibrate_always";
    public final static String HAPTIC_FEEDBACK = "haptic_feedback";
    public final static String WRIST_GESTURES = "wrist_gestures";
    public final static String ANIMATIONS = "animations";
  }

  public final static class SOUND {

    public final static String WOOD = "wood";
    public final static String CLICK = "click";
    public final static String DING = "ding";
    public final static String BEEP = "beep";
  }
}
