package xyz.zedler.patrick.tack.core.model

data class Tick(
  val index: Long,
  val beat: Int,
  val subdivision: Int,
  val type: String,
  val isMuted: Boolean,
  val isPoly: Boolean
)
