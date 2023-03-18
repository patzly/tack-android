package xyz.zedler.patrick.tack;

import androidx.appcompat.app.AppCompatDelegate;

public final class Constants {

  public final static class PREF {

    public static final String THEME = "app_theme";
    public static final String MODE = "mode";
    public static final String USE_SLIDING = "use_sliding_transition";

    public final static String BEAT_MODE_VIBRATE = "beat_mode_vibrate";
    public final static String INTERVAL = "interval";
    public final static String EMPHASIS = "emphasis";
    public final static String BOOKMARKS = "bookmarks";

    public static final String LAST_VERSION = "last_version";
    public static final String FEEDBACK_POP_UP_COUNT = "feedback_pop_up_count";
  }

  public final static class DEF {

    public static final String THEME = "";
    public static final int MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final boolean USE_SLIDING = false;

    public final static String SOUND = Constants.SOUND.WOOD;
    public final static boolean VIBRATE_ALWAYS = false;
    public final static boolean HAPTIC_FEEDBACK = true;
    public final static boolean EMPHASIS_SLIDER = false;
    public final static boolean DARK_MODE = false;
    public final static boolean KEEP_AWAKE = true;
    public final static boolean BEAT_MODE_VIBRATE = false;
    public final static long INTERVAL = 500;
    public final static int EMPHASIS = 0;
  }

  public final static class SETTINGS {

    public final static String SOUND = "sound";
    public final static String VIBRATE_ALWAYS = "vibrate_always";
    public final static String HAPTIC_FEEDBACK = "haptic_feedback";
    public final static String EMPHASIS_SLIDER = "emphasis_slider";
    public final static String DARK_MODE = "force_dark_mode";
    public final static String KEEP_AWAKE = "keep_awake";
  }

  public final static class SOUND {

    public final static String WOOD = "wood";
    public final static String CLICK = "click";
    public final static String DING = "ding";
    public final static String BEEP = "beep";
  }

  public final static class EXTRA {

    public static final String RUN_AS_SUPER_CLASS = "run_as_super_class";
    public static final String INSTANCE_STATE = "instance_state";
    public static final String SCROLL_POSITION = "scroll_position";

    public final static String TITLE = "title";
    public final static String FILE = "file";
    public final static String LINK = "link";
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
