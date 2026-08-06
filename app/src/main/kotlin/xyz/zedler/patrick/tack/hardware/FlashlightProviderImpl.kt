package xyz.zedler.patrick.tack.hardware

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import xyz.zedler.patrick.tack.core.hardware.FlashlightProvider

class FlashlightProviderImpl(context: Context) : FlashlightProvider {

  private val cameraManager =
    context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private val handler = Handler(Looper.getMainLooper())
  private var cameraId: String? = null
  private var turnOffRunnable: Runnable? = null

  init {
    try {
      val ids = cameraManager.cameraIdList
      for (id in ids) {
        val c = cameraManager.getCameraCharacteristics(id)
        val hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val facing = c.get(CameraCharacteristics.LENS_FACING)
        if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
          this.cameraId = id
          break
        }
      }
    } catch (_: Exception) {
    }
  }

  override fun flash(duration: Int, strength: Float) {
    val id = cameraId ?: return
    if (strength <= 0f) return

    val safeStrength = strength.coerceAtMost(1.0f)
    turnOffRunnable?.let { handler.removeCallbacks(it) }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
          val characteristics = cameraManager.getCameraCharacteristics(id)
          val maxLevel = characteristics.get(
            CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
          )
          if (maxLevel != null && maxLevel > 1) {
            val targetLevel = (maxLevel * safeStrength).toInt().coerceAtLeast(1)
            cameraManager.turnOnTorchWithStrengthLevel(id, targetLevel)
          } else {
            cameraManager.setTorchMode(id, true)
          }
        } catch (_: Exception) {
          cameraManager.setTorchMode(id, true)
        }
      } else {
        cameraManager.setTorchMode(id, true)
      }

      turnOffRunnable = Runnable {
        try {
          cameraManager.setTorchMode(id, false)
        } catch (_: Exception) {
        }
      }.also {
        handler.postDelayed(it, duration.toLong())
      }
    } catch (_: CameraAccessException) {
    }
  }

  override fun cleanup() {
    turnOffRunnable?.let { handler.removeCallbacks(it) }
    try {
      cameraId?.let { cameraManager.setTorchMode(it, false) }
    } catch (_: Exception) {
    }
  }
}
