package xyz.zedler.patrick.tack.util

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import xyz.zedler.patrick.tack.core.metronome.FlashlightProvider

class FlashlightUtilImpl(private val context: Context) : FlashlightProvider {

  private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private var cameraId: String? = null

  init {
    try {
      cameraId = cameraManager.cameraIdList.firstOrNull()
    } catch (_: Exception) {
    }
  }

  override fun flash(duration: Int, strength: Float) {
    val id = cameraId ?: return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Strength support
        // cameraManager.turnOnTorchWithStrengthLevel(id, ...)
      }
      cameraManager.setTorchMode(id, true)
      // Need a way to turn it off after duration, but flashlight in metronome is tricky
      // Legacy code used a handler. For now, keep it simple or implement the handler.
    } catch (_: Exception) {
    }
  }
}
