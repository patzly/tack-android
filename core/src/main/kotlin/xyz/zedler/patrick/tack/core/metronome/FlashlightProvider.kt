package xyz.zedler.patrick.tack.core.metronome

interface FlashlightProvider {
  fun flash(duration: Int, strength: Float)
}
