package xyz.zedler.patrick.tack.ui.component

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R

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
        Slider(
          value = hue,
          onValueChange = onHueChange,
          valueRange = 0f..360f,
          colors = SliderDefaults.colors(
            thumbColor = Color.hsv(hue, 0.6f, 0.9f),
            activeTrackColor = Color.hsv(hue, 0.6f, 0.9f)
          ),
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
  }
}
