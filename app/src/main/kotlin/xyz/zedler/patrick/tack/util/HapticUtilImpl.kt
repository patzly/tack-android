package xyz.zedler.patrick.tack.util

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import xyz.zedler.patrick.tack.core.metronome.HapticProvider

class HapticUtilImpl(private val context: Context) : HapticProvider {

  private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    manager.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  }

  private val vibrationAttributesMedia: VibrationAttributes? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)
    } else null
  }

  override fun tick(isPoly: Boolean) {
    vibrate(VibrationEffect.EFFECT_TICK, 2)
  }

  override fun click(isPoly: Boolean) {
    vibrate(VibrationEffect.EFFECT_CLICK, 8)
  }

  override fun heavyClick(isPoly: Boolean) {
    vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, 40)
  }

  private fun vibrate(effectId: Int, fallbackDuration: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val effect = VibrationEffect.createPredefined(effectId)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(effect, vibrationAttributesMedia!!)
      } else {
        vibrator.vibrate(effect)
      }
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(fallbackDuration)
    }
  }
}
