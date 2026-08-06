package xyz.zedler.patrick.tack.core.hardware

interface HapticProvider {
  fun click(isPoly: Boolean = false)
  fun heavyClick(isPoly: Boolean = false)
  fun tick(isPoly: Boolean = false)
}
