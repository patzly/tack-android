package xyz.zedler.patrick.tack;

import androidx.appcompat.app.AppCompatDelegate;

public final class Constants {

  public final static int ANIM_DURATION_LONG = 400;
  public final static int ANIM_DURATION_SHORT = 250;
  public static final int TEMPO_MIN = 1;
  public static final int TEMPO_MAX = 400;
  public static final int BEATS_MAX = 20;
  public static final int SUBS_MAX = 10;
  public static final int BOOKMARKS_MAX = 6;

  public final static class PREF {
    // General
    public static final String THEME = "app_theme";
    public static final String MODE = "mode";
    public static final String USE_SLIDING = "use_sliding_transition";
    public static final String HAPTIC = "haptic_feedback";
    public static final String LAST_VERSION = "last_version";
    public static final String FEEDBACK_POP_UP_COUNT = "feedback_pop_up_count";

    // Metronome
    public final static String TEMPO = "tempo";
    public final static String BEATS = "beats";
    public final static String SUBDIVISIONS = "subdivisions";
    public final static String BEAT_MODE_VIBRATE = "beat_mode_vibrate";
    public final static String ALWAYS_VIBRATE = "always_vibrate";
    public final static String FLASH_SCREEN = "flash_screen";
    public final static String KEEP_AWAKE = "keep_awake";
    public final static String SOUND = "sound";
    public final static String LATENCY = "latency_offset";
    public final static String GAIN = "gain";
    public final static String BOOKMARKS = "bookmarks";

    @Deprecated
    public final static String EMPHASIS = "emphasis";
  }

  public final static class DEF {
    // General
    public static final String THEME = "";
    public static final int MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final boolean USE_SLIDING = false;

    // Metronome
    public final static int TEMPO = 120;
    public final static String BEATS = String.join(
        ",", TICK_TYPE.STRONG, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL, TICK_TYPE.NORMAL
    );
    public final static String SUBDIVISIONS = TICK_TYPE.MUTED;
    public final static boolean BEAT_MODE_VIBRATE = false;
    public final static boolean ALWAYS_VIBRATE = true;
    public final static boolean FLASH_SCREEN = false;
    public final static boolean KEEP_AWAKE = true;
    public final static String SOUND = Constants.SOUND.WOOD;
    public final static long LATENCY = 100;
    public final static int GAIN = 0;

    @Deprecated
    public final static int EMPHASIS = 50;
  }

  public final static class SOUND {

    public final static String WOOD = "wood";
    public final static String SINE = "sine";
    public final static String CLICK = "click";
    public final static String DING = "ding";
    public final static String BEEP = "beep";
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

  public final static class EXTRA {

    public static final String RUN_AS_SUPER_CLASS = "run_as_super_class";
    public static final String INSTANCE_STATE = "instance_state";
    public static final String SCROLL_POSITION = "scroll_position";
    public static final String TEMPO = "tempo";
  }

  public static final class THEME {

    public static final String DYNAMIC = "dynamic";
    public static final String RED = "red";
    public static final String YELLOW = "yellow";
    public static final String LIME = "lime";
    public static final String GREEN = "green";
    public static final String TURQUOISE = "turquoise";
    public static final String TEAL = "teal";
    public static final String BLUE = "blue";
    public static final String PURPLE = "purple";
  }
}
