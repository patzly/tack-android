package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SongScreen(
  songId: String,
  widthSizeClass: WindowWidthSizeClass
) {
  val isLandscape = widthSizeClass != WindowWidthSizeClass.Compact

  if (isLandscape) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Song Info ($songId) (Landscape Left)")
      }
      Box(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Text("Song Parts (Landscape Right)")
      }
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Collapsed Song Info ($songId) (Portrait Top)")
      }
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text("Song Parts (Portrait Full)")
      }
    }
  }
}
