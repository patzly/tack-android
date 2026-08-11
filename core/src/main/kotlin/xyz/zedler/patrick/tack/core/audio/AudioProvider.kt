package xyz.zedler.patrick.tack.core.audio

import xyz.zedler.patrick.tack.core.model.TickType

interface AudioProvider {
  var isMuted: Boolean
  fun play()
  fun stop()
  fun playTick(tickType: TickType, muted: Boolean)
  fun scheduleDelayedStop()
  fun warmUp()
}
