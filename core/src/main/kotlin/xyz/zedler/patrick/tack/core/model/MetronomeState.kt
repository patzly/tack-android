package xyz.zedler.patrick.tack.core.model

data class MetronomeState(
  val isPlaying: Boolean = false,
  val isCountingIn: Boolean = false,
  val tempo: Int = 120,
  val timerProgress: Float = 0f,
  val timerBarIndex: Int = 0,
  val timerBeatIndex: Int = 0,
  val timerSubIndex: Int = 0,
  val elapsedTime: Long = 0,
  val currentSongId: String? = null,
  val currentPartIndex: Int = 0,
  val isMuted: Boolean = false
)
