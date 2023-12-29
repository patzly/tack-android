package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import androidx.annotation.RequiresApi;
import java.util.Collections;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.ShortcutActivity;

public class ShortcutUtil {

  private final Context context;
  private ShortcutManager manager;

  public ShortcutUtil(Context context) {
    this.context = context;
    if (isSupported()) {
      manager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
    }
  }

  public void addShortcut(int tempo) {
    if (isSupported() && !hasShortcut(tempo)) {
      manager.addDynamicShortcuts(Collections.singletonList(getShortcutInfo(tempo)));
    }
  }

  public void removeShortcut(int tempo) {
    if (isSupported() && hasShortcut(tempo)) {
      manager.removeDynamicShortcuts(Collections.singletonList(String.valueOf(tempo)));
    }
  }

  public void removeAllShortcuts() {
    if (isSupported()) {
      manager.removeAllDynamicShortcuts();
    }
  }

  public void reportUsage(int tempo) {
    if (isSupported() && hasShortcut(tempo)) {
      manager.reportShortcutUsed(String.valueOf(tempo));
    }
  }

  private boolean hasShortcut(int tempo) {
    if (isSupported()) {
      for (ShortcutInfo info : manager.getDynamicShortcuts()) {
        if (String.valueOf(tempo).equals(info.getId())) {
          return true;
        }
      }
    }
    return false;
  }

  @RequiresApi(api = VERSION_CODES.N_MR1)
  private ShortcutInfo getShortcutInfo(int tempo) {
    ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, String.valueOf(tempo));
    builder.setShortLabel(context.getString(R.string.label_bpm_value, tempo));
    builder.setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut));
    builder.setIntent(new Intent(context, ShortcutActivity.class)
        .setAction(ACTION.START)
        .putExtra(EXTRA.TEMPO, tempo)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    );
    return builder.build();
  }

  private static boolean isSupported() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
  }
}
