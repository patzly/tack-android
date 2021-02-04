package xyz.zedler.patrick.tack;

public final class Constants {

    public final static class PREF {
        public final static String BEAT_MODE_VIBRATE = "beat_mode_vibrate";
        public final static String FIRST_START = "first_start";
        public final static String FIRST_ROTATION = "first_rotation";
        public final static String FIRST_PRESS = "first_button_press";
        public final static String FIRST_SPEAKER_MODE = "first_speaker_mode";
        public final static String INTERVAL = "interval";
        public final static String EMPHASIS = "emphasis";
        public final static String BOOKMARK = "bookmark";
    }

    public final static class DEF {
        public final static String SOUND = Constants.SOUND.WOOD;
        public final static boolean VIBRATE_ALWAYS = false;
        public final static boolean HAPTIC_FEEDBACK = true;
        public final static boolean WRIST_GESTURES = true;
        public final static boolean HIDE_PICKER = false;
        public final static boolean ANIMATIONS = true;
        public final static boolean BEAT_MODE_VIBRATE = true;
        public final static boolean FIRST_START = true;
        public final static boolean FIRST_ROTATION = true;
        public final static boolean FIRST_PRESS = true;
        public final static boolean FIRST_SPEAKER_MODE = true;
        public final static long INTERVAL = 500;
        public final static int EMPHASIS = 0;
        public final static int BOOKMARK = -1;
    }

    public final static class SETTING {
        public final static String SOUND = "sound";
        public final static String VIBRATE_ALWAYS = "vibrate_always";
        public final static String HAPTIC_FEEDBACK = "haptic_feedback";
        public final static String WRIST_GESTURES = "wrist_gestures";
        public final static String HIDE_PICKER = "hide_picker";
        public final static String ANIMATIONS = "animations";
    }

    public final static class SOUND {
        public final static String WOOD = "wood";
        public final static String CLICK = "click";
        public final static String DING = "ding";
        public final static String BEEP = "beep";
    }

    public final static class EXTRA {
        public final static String TYPE = "type";
        public final static String ID = "id";
        public final static String PAGER_POSITION = "position";
        public final static String SELECTED_INDEX = "selected_index";
        public final static String LABELS = "labels";
        public final static String DRAWABLES = "drawables";
        public final static String TITLE = "title";
        public final static String WORD = "word";
        public final static String FILE = "file";
        public final static String LINK = "link";
        public final static String SYMBOLS = "symbols";
        public final static String SUFFIXES = "suffixes";
    }
}
