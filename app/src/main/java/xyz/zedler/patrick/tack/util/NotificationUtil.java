package xyz.zedler.patrick.tack.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.DynamicColors;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;

public class NotificationUtil {

  private static final String TAG = NotificationUtil.class.getSimpleName();

  private final static String CHANNEL_ID = "metronome";
  private final static int REQUEST_CODE = 0;

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final NotificationManagerCompat notificationManager;

  public NotificationUtil(Context context) {
    this.context = context;
    sharedPrefs = new PrefsUtil(context).getSharedPrefs();
    notificationManager = NotificationManagerCompat.from(context);
  }

  public boolean hasPermission() {
    return hasPermission(context);
  }

  public static boolean hasPermission(@NonNull Context context) {
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      int status = ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
      );
      return status == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  public void createNotificationChannel() {
    if (VERSION.SDK_INT < VERSION_CODES.O) {
      return;
    }
    notificationManager.createNotificationChannel(
        new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.title_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        )
    );
  }

  public Notification getNotification() {
    int themeResId = -1;
    Context dynamicColorContext = null;
    switch (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
      case THEME.RED:
        themeResId = R.style.Theme_Tack_Red;
        break;
      case THEME.YELLOW:
        themeResId = R.style.Theme_Tack_Yellow;
        break;
      case THEME.LIME:
        themeResId = R.style.Theme_Tack_Lime;
        break;
      case THEME.GREEN:
        themeResId = R.style.Theme_Tack_Green;
        break;
      case THEME.TURQUOISE:
        themeResId = R.style.Theme_Tack_Turquoise;
        break;
      case THEME.TEAL:
        themeResId = R.style.Theme_Tack_Teal;
        break;
      case THEME.BLUE:
        themeResId = R.style.Theme_Tack_Blue;
        break;
      case THEME.PURPLE:
        themeResId = R.style.Theme_Tack_Purple;
        break;
      default:
        if (DynamicColors.isDynamicColorAvailable()) {
          dynamicColorContext = DynamicColors.wrapContextIfAvailable(context);
        } else {
          themeResId = R.style.Theme_Tack_Yellow;
        }
        break;
    }

    Context colorContext;
    if (themeResId != -1) {
      colorContext = new ContextThemeWrapper(context, themeResId);
    } else {
      colorContext = dynamicColorContext;
    }

    String text = context.getString(R.string.msg_metronome_playing_return);
    Intent intentApp = new Intent(context, MainActivity.class);
    intentApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pendingIntentApp = PendingIntent.getActivity(
        context, REQUEST_CODE, intentApp, getPendingIntentFlags()
    );
    PendingIntent intentStop = PendingIntent.getBroadcast(
        context, REQUEST_CODE, new Intent(ACTION.STOP), getPendingIntentFlags()
    );
    Action actionStop = new Action(
        R.drawable.ic_round_stop, context.getString(R.string.action_stop), intentStop
    );
    return new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.msg_metronome_playing))
        .setContentText(text)
        .setContentIntent(pendingIntentApp)
        .addAction(actionStop)
        .setAutoCancel(true)
        .setSilent(true)
        .setOngoing(true)
        .setColor(ResUtil.getColorAttr(colorContext, R.attr.colorPrimary))
        .setSmallIcon(R.drawable.ic_round_tack_notification)
        .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build();
  }

  private int getPendingIntentFlags() {
    return VERSION.SDK_INT >= VERSION_CODES.M
        ? PendingIntent.FLAG_IMMUTABLE
        : PendingIntent.FLAG_UPDATE_CURRENT;
  }
}
