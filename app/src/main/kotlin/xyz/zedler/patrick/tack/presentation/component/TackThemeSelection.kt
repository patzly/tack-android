package xyz.zedler.patrick.tack.presentation.component

import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TackThemeSelection(
  useDynamicColors: Boolean,
  hue: Float,
  onHueChange: (Float) -> Unit,
  onUseDynamicColorsChange: (Boolean) -> Unit
) {
  Column(modifier = Modifier.padding(bottom = 12.dp)) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Row(
        modifier = Modifier
          .padding(start = 56.dp, end = 16.dp, bottom = 8.dp)
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          stringResource(R.string.settings_theme_dynamic),
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
          checked = useDynamicColors,
          onCheckedChange = onUseDynamicColorsChange
        )
      }
    }

    if (!useDynamicColors || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      Column(modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)) {
        Text(
          stringResource(R.string.settings_theme_hue),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.secondary
        )
        
        val hueColors = remember {
          (0..360 step 2).map { Color.hsv(it.toFloat(), 1f, 1f) }
        }
        val hueBrush = remember(hueColors) {
          Brush.linearGradient(hueColors)
        }

        Slider(
          value = hue,
          onValueChange = onHueChange,
          valueRange = 0f..360f,
          thumb = {
            SliderDefaults.Thumb(
              interactionSource = remember { MutableInteractionSource() },
              colors = SliderDefaults.colors(
                thumbColor = Color.hsv(hue, 1f, 1f)
              )
            )
          },
          track = { sliderState ->
            SliderDefaults.Track(
              sliderState = sliderState,
              colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White
              ),
              modifier = Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                  drawContent()
                  drawRect(brush = hueBrush, blendMode = BlendMode.SrcIn)
                }
            )
          },
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
  }
}
