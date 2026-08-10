package xyz.zedler.patrick.tack.core.model

enum class AppTheme(val key: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromKey(key: String): AppTheme = entries.find { it.key == key } ?: SYSTEM
  }
}

enum class AppContrast(val key: String) {
  STANDARD("standard"),
  MEDIUM("medium"),
  HIGH("high");

  companion object {
    fun fromKey(key: String): AppContrast = entries.find { it.key == key } ?: STANDARD
  }
}
