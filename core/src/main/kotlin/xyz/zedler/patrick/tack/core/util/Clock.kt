package xyz.zedler.patrick.tack.core.util

import android.os.SystemClock

interface Clock {
  fun uptimeMillis(): Long
}

class SystemClockImpl : Clock {
  override fun uptimeMillis(): Long = SystemClock.uptimeMillis()
}
