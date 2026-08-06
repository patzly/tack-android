package xyz.zedler.patrick.tack.core.audio

interface AudioProvider {
  var isMuted: Boolean
  fun play()
  fun stop()
  fun playTick(tickType: String, muted: Boolean)
  fun scheduleDelayedStop()
  fun warmUp()
}
