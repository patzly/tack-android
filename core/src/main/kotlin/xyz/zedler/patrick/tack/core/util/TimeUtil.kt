package xyz.zedler.patrick.tack.core.util

import java.util.Locale

object TimeUtil {
  fun getTimeStringFromSeconds(seconds: Int, forceHours: Boolean): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0 || forceHours) {
      String.format(
        Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60
      )
    } else {
      String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds % 60)
    }
  }
}
