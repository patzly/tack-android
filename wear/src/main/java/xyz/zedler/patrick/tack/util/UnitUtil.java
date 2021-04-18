package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.util.TypedValue;

public class UnitUtil {

  public static int getDp(Context context, float dp) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.getResources().getDisplayMetrics()
    );
  }

  public static int getSp(Context context, float sp) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        context.getResources().getDisplayMetrics()
    );
  }
}
