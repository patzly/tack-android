package xyz.zedler.patrick.tack.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(
  widthSizeClass: WindowWidthSizeClass
) {
  val isLandscape = widthSizeClass != WindowWidthSizeClass.Compact

  if (isLandscape) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Metronome Options (Landscape Left)")
      }
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Metronome (Landscape Right)")
      }
    }
  } else {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Metronome (Portrait - Options as Dialog)")
    }
  }
}
