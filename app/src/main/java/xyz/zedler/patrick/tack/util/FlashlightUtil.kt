/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class FlashlightUtil(private val context: Context) {

  private val cameraManager: CameraManager by lazy {
    context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  }
  private val handler: Handler by lazy {
    Handler(Looper.getMainLooper())
  }
  private var cameraId: String? = null
  private var turnOffRunnable: Runnable? = null

  init {
    initCameraId()
  }

  private fun initCameraId() {
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
    } catch (e: CameraAccessException) {
      Log.e(TAG, "Cannot find camera ID", e)
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during initialization", e)
    }
  }

  fun flash(durationMs: Long, strength: Float) {
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
        } catch (e: Exception) {
          Log.w(TAG, "Cannot turn off flashlight: ${e.message}")
        }
      }.also {
        handler.postDelayed(it, durationMs)
      }
    } catch (e: CameraAccessException) {
      Log.w(TAG, "Flashlight temporarily unavailable: ${e.message}")
    } catch (_: IllegalArgumentException) {
      initCameraId()
    }
  }

  fun cleanup() {
    turnOffRunnable?.let { handler.removeCallbacks(it) }
    try {
      cameraId?.let { cameraManager.setTorchMode(it, false) }
    } catch (_: Exception) {
    }
  }

  companion object {
    private val TAG = FlashlightUtil::class.java.simpleName

    @JvmStatic
    fun hasFlash(context: Context): Boolean {
      return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
          ?: return false

        manager.cameraIdList.any { id ->
          val c = manager.getCameraCharacteristics(id)
          val hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
          val facing = c.get(CameraCharacteristics.LENS_FACING)
          hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error checking for flashlight", e)
        false
      }
    }

    @JvmStatic
    fun hasStrengthControl(context: Context): Boolean {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
      return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
          ?: return false

        manager.cameraIdList.any { id ->
          val c = manager.getCameraCharacteristics(id)
          val hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
          val facing = c.get(CameraCharacteristics.LENS_FACING)

          if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
            val maxLevel = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
            maxLevel != null && maxLevel > 1
          } else {
            false
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error checking for strength control", e)
        false
      }
    }
  }
}
