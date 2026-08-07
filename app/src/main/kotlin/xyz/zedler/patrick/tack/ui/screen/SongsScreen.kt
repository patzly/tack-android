package xyz.zedler.patrick.tack.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SongsScreen(
  widthSizeClass: WindowWidthSizeClass
) {
  val isLandscape = widthSizeClass != WindowWidthSizeClass.Compact

  if (isLandscape) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Songs List (Landscape Left)")
      }
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Current Song Details (Landscape Right)")
      }
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text("Songs List (Portrait Full)")
      }
      Box(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Current Song Bar (Portrait Bottom)")
      }
    }
  }
}
