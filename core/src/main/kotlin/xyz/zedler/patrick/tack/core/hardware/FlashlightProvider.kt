package xyz.zedler.patrick.tack.core.hardware

interface FlashlightProvider {
  fun flash(duration: Int, strength: Float)
  fun cleanup()
}
