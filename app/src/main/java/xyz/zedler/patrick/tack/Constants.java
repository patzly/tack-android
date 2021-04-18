package xyz.zedler.patrick.tack;

public final class Constants {

  public final static class PREF {

    public final static String BEAT_MODE_VIBRATE = "beat_mode_vibrate";
    public final static String INTERVAL = "interval";
    public final static String EMPHASIS = "emphasis";
    public final static String BOOKMARKS = "bookmarks";
    public final static String FEEDBACK_POP_UP = "feedback_pop_up";
  }

  public final static class DEF {

    public final static String SOUND = Constants.SOUND.WOOD;
    public final static boolean VIBRATE_ALWAYS = false;
    public final static boolean HAPTIC_FEEDBACK = true;
    public final static boolean EMPHASIS_SLIDER = true;
    public final static boolean DARK_MODE = false;
    public final static boolean KEEP_AWAKE = true;
    public final static boolean BEAT_MODE_VIBRATE = true;
    public final static long INTERVAL = 500;
    public final static int EMPHASIS = 0;
  }

  public final static class SETTING {

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

    public final static String TITLE = "title";
    public final static String FILE = "file";
    public final static String LINK = "link";
  }
}
