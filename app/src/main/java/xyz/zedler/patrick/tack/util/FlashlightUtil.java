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

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

public class FlashlightUtil {

  private static final String TAG = FlashlightUtil.class.getSimpleName();
  private final CameraManager cameraManager;
  private final Handler handler;
  private String cameraId;
  private Runnable turnOffRunnable;

  public FlashlightUtil(@NonNull Context context) {
    this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    this.handler = new Handler(Looper.getMainLooper());
    initCameraId();
  }

  private void initCameraId() {
    try {
      String[] ids = cameraManager.getCameraIdList();
      for (String id : ids) {
        CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
        Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Integer facing = c.get(CameraCharacteristics.LENS_FACING);

        if (hasFlash != null && hasFlash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
          this.cameraId = id;
          break;
        }
      }
    } catch (CameraAccessException e) {
      Log.e(TAG, "Cannot find camera ID", e);
    } catch (Exception e) {
      Log.e(TAG, "Unexpected error during initialization", e);
    }
  }

  public void flash(long durationMs, float strength) {
    if (cameraId == null || strength <= 0f) {
      return;
    }
    final float safeStrength = Math.min(strength, 1.0f);

    if (turnOffRunnable != null) {
      handler.removeCallbacks(turnOffRunnable);
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
          CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
          Integer maxLevel = characteristics.get(
              CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
          );
          if (maxLevel != null && maxLevel > 1) {
            int targetLevel = Math.round(maxLevel * safeStrength);
            if (targetLevel < 1) {
              targetLevel = 1;
            }
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, targetLevel);
          } else {
            // Fallback for devices without strength control
            cameraManager.setTorchMode(cameraId, true);
          }
        } catch (Exception e) {
          // Fallback if strength API fails
          cameraManager.setTorchMode(cameraId, true);
        }
      } else {
        cameraManager.setTorchMode(cameraId, true);
      }

      turnOffRunnable = () -> {
        try {
          cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException | IllegalArgumentException e) {
          Log.w(TAG, "Cannot turn off flashlight: " + e.getMessage());
        }
      };
      handler.postDelayed(turnOffRunnable, durationMs);
    } catch (CameraAccessException e) {
      Log.w(TAG, "Flashlight temporarily unavailable: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      initCameraId();
    }
  }

  public void cleanup() {
    if (turnOffRunnable != null) {
      handler.removeCallbacks(turnOffRunnable);
    }
    try {
      if (cameraId != null) {
        cameraManager.setTorchMode(cameraId, false);
      }
    } catch (Exception ignored) {}
  }

  public static boolean hasFlash(@NonNull Context context) {
    try {
      CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      if (manager == null) return false;

      for (String id : manager.getCameraIdList()) {
        CameraCharacteristics c = manager.getCameraCharacteristics(id);
        Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Integer facing = c.get(CameraCharacteristics.LENS_FACING);
        if (hasFlash != null && hasFlash &&
            facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
          return true;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Error checking for flashlight", e);
    }
    return false;
  }

  public static boolean hasStrengthControl(@NonNull Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return false;
    }
    try {
      CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      if (manager == null) return false;

      for (String id : manager.getCameraIdList()) {
        CameraCharacteristics c = manager.getCameraCharacteristics(id);
        Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Integer facing = c.get(CameraCharacteristics.LENS_FACING);

        if (hasFlash != null && hasFlash &&
            facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
          Integer maxLevel = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
          return maxLevel != null && maxLevel > 1;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Error checking for strength control", e);
    }
    return false;
  }
}