package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.MaterialColors;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import xyz.zedler.patrick.tack.R;

public class ResUtil {

  private final static String TAG = ResUtil.class.getSimpleName();

  @NonNull
  public static String getRawText(Context context, @RawRes int resId) {
    InputStream inputStream = context.getResources().openRawResource(resId);
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder text = new StringBuilder();
    try {
      for (String line; (line = bufferedReader.readLine()) != null; ) {
        text.append(line).append('\n');
      }
      text.deleteCharAt(text.length() - 1);
      inputStream.close();
    } catch (Exception e) {
      Log.e(TAG, "getRawText", e);
    }
    return text.toString();
  }

  public static void share(Context context, @StringRes int resId) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, context.getString(resId));
    intent.setType("text/plain");
    context.startActivity(Intent.createChooser(intent, null));
  }

  public static int getColor(Context context, @AttrRes int resId) {
    return MaterialColors.getColor(context, resId, Color.BLACK);
  }

  public static int getColor(Context context, @AttrRes int resId, float alpha) {
    return ColorUtils.setAlphaComponent(getColor(context, resId), (int) (alpha * 255));
  }

  public static int getSysColor(Context context, @AttrRes int resId) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(resId, typedValue, true);
    return typedValue.data;
  }

  public static int getColorHighlight(Context context) {
    return getColor(context, R.attr.colorSecondary, 0.09f);
  }

  public static void tintMenuIcons(Context context, Menu menu) {
    if (menu != null) {
      for (int i = 0; i < menu.size(); i++) {
        MenuItem item = menu.getItem(i);
        if (item != null) {
          tintIcon(context, item.getIcon());
        }
      }
    }
  }

  public static void tintIcon(Context context, Drawable icon) {
    if (icon != null) {
      icon.setTint(ResUtil.getColor(context, R.attr.colorOnSurfaceVariant));
    }
  }

  public static int getDimension(Context context, @DimenRes int resId) {
    return (int) context.getResources().getDimension(resId);
  }
}
