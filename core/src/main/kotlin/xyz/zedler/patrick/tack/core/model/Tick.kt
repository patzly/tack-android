package xyz.zedler.patrick.tack.core.model

data class Tick(
  val index: Long,
  val beat: Int,
  val subdivision: Int,
  val type: TickType,
  val isMuted: Boolean,
  val isPoly: Boolean
)

enum class TickType(val key: String) {
  NORMAL("normal"),
  STRONG("strong"),
  SUB("sub"),
  MUTED("muted"),
  BEAT_SUB("beat_sub"),
  BEAT_SUB_MUTED("beat_sub_muted");

  companion object {
    fun fromKey(key: String): TickType = entries.find { it.key == key } ?: NORMAL
  }
}