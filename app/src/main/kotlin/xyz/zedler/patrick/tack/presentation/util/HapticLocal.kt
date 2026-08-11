package xyz.zedler.patrick.tack.presentation.util

import androidx.compose.runtime.compositionLocalOf
import xyz.zedler.patrick.tack.core.hardware.HapticProvider

val LocalHaptic = compositionLocalOf<HapticProvider> {
  error("No HapticProvider provided")
}
