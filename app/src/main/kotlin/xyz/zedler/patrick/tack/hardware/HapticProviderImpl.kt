package xyz.zedler.patrick.tack.hardware

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import xyz.zedler.patrick.tack.core.hardware.HapticConstants
import xyz.zedler.patrick.tack.core.hardware.HapticProvider

class HapticProviderImpl(context: Context) : HapticProvider {

  private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val manager =
      context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    manager.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  }

  override var isEnabled: Boolean = vibrator.hasVibrator()
  override var intensity: String = HapticConstants.Intensity.AUTO

  private val vibrationAttributesMedia: VibrationAttributes? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)
    } else null
  }

  private val audioAttributes: AudioAttributes? by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build()
    } else null
  }

  override fun tick(isPoly: Boolean) {
    val effectId = if (intensity == HapticConstants.Intensity.AUTO &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    ) {
      VibrationEffect.EFFECT_TICK
    } else -1
    val duration = if (intensity == HapticConstants.Intensity.STRONG) 20L else 2L
    vibrate(effectId, duration)
  }

  override fun click(isPoly: Boolean) {
    val effectId = if (intensity == HapticConstants.Intensity.AUTO &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    ) {
      VibrationEffect.EFFECT_CLICK
    } else -1
    val duration = if (intensity == HapticConstants.Intensity.STRONG) 50L else 8L
    vibrate(effectId, duration)
  }

  override fun heavyClick(isPoly: Boolean) {
    val effectId = if (intensity == HapticConstants.Intensity.AUTO &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    ) {
      VibrationEffect.EFFECT_HEAVY_CLICK
    } else -1
    val duration = if (intensity == HapticConstants.Intensity.STRONG) 80L else 40L
    vibrate(effectId, duration)
  }

  private fun vibrate(effectId: Int, fallbackDuration: Long) {
    if (!isEnabled) return

    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != -1) {
      VibrationEffect.createPredefined(effectId)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      VibrationEffect.createOneShot(
        fallbackDuration,
        if (intensity == HapticConstants.Intensity.STRONG) 255
        else VibrationEffect.DEFAULT_AMPLITUDE
      )
    } else null

    if (effect != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(effect, vibrationAttributesMedia!!)
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        @Suppress("DEPRECATION")
        vibrator.vibrate(effect, audioAttributes)
      }
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(fallbackDuration, audioAttributes)
    }
  }
}
