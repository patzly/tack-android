package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import xyz.zedler.patrick.tack.R;

public class BulletUtil {

  public static CharSequence makeBulletList(
      Context context,
      float leadingMargin,
      float bulletSize,
      String prefixToReplace,
      @Nullable String text,
      String... highlights
  ) {
    if (text == null) {
      return null;
    }

    int color = ContextCompat.getColor(context, R.color.on_background);
    int margin = UnitUtil.getSp(context, leadingMargin);
    int size = UnitUtil.getSp(context, bulletSize);

    String[] lines = text.split("\n");
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i] + (i < lines.length - 1 ? "\n" : "");
      if (!line.startsWith(prefixToReplace)) {
        builder.append(line);
        continue;
      }
      line = line.substring(prefixToReplace.length());

      BulletSpan bulletSpan;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        bulletSpan = new BulletSpan(margin, color, size);
      } else {
        bulletSpan = new BulletSpan(margin, color);
      }

      for (String highlight : highlights) {
        line = line.replaceAll(highlight, "<b>" + highlight + "</b>");
        line = line.replaceAll("\n", "<br/>");
      }

      Spannable spannable = new SpannableString(Html.fromHtml(line));
      spannable.setSpan(
          bulletSpan,
          0,
          spannable.length(),
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE
      );
      builder.append(spannable);
    }
    return builder;
  }
}
