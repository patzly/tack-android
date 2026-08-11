package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.navigation.Route
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun MainScreen(
  viewModel: MainViewModel = viewModel(),
  widthSizeClass: WindowWidthSizeClass
) {
  val isLandscape = widthSizeClass != WindowWidthSizeClass.Compact

  Box(modifier = Modifier.fillMaxSize()) {
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

    IconButton(
      onClick = {
        viewModel.navigateTo(Route.Settings)
      },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
        .padding(top = 32.dp) // Offset for edge-to-edge
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_rounded_more_vert),
        contentDescription = "Settings"
      )
    }
  }
}
