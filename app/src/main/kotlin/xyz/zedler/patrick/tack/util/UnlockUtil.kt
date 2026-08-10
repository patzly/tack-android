package xyz.zedler.patrick.tack.util

import android.content.Context

object UnlockUtil {
  private const val PACKAGE_KEY = "xyz.zedler.patrick.tack.unlock"

  fun isKeyInstalled(context: Context): Boolean {
    return try {
      context.packageManager.getPackageInfo(PACKAGE_KEY, 0)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun isPlayStoreInstalled(context: Context): Boolean {
    return try {
      context.packageManager.getPackageInfo("com.android.vending", 0)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun isUnlocked(context: Context): Boolean {
    return if (isPlayStoreInstalled(context)) {
      isKeyInstalled(context)
    } else {
      true
    }
  }
}
