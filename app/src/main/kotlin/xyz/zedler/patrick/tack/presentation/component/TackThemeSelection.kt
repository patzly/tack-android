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

package xyz.zedler.patrick.tack.presentation.component

import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import xyz.zedler.patrick.tack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TackThemeSelection(
  useDynamicColors: Boolean,
  hue: Float,
  onHueChange: (Float) -> Unit,
  onUseDynamicColorsChange: (Boolean) -> Unit
) {
  Column {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Row(
        modifier = Modifier.fillMaxWidth(),
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

    val interactionSource = remember { MutableInteractionSource() }
    val hueColors = remember {
      (0..360 step 2).map {
        Hct.from(it.toDouble(), 70.0, 60.0).toColor()
      }
    }
    val hueBrush = remember(hueColors) {
      Brush.linearGradient(hueColors)
    }

    Box {
      Slider(
        value = hue,
        onValueChange = onHueChange,
        valueRange = 0f..360f,
        steps = 20,
        interactionSource = interactionSource,
        track = { sliderState ->
          SliderDefaults.Track(
            trackCornerSize = 8.dp,
            sliderState = sliderState,
            colors = SliderDefaults.colors(
              activeTrackColor = Color.White,
              inactiveTrackColor = Color.White
            ),
            modifier = Modifier
              .height(24.dp)
              .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
              .drawWithContent {
                drawContent()
                drawRect(brush = hueBrush, blendMode = BlendMode.SrcIn)
              }
          )
        }
      )
      Slider(
        value = hue,
        onValueChange = onHueChange,
        valueRange = 0f..360f,
        steps = 20,
        interactionSource = interactionSource,
        track = { sliderState ->
          SliderDefaults.Track(
            trackCornerSize = 8.dp,
            sliderState = sliderState,
            colors = SliderDefaults.colors(
              activeTrackColor = Color.Transparent,
              inactiveTrackColor = Color.Transparent,
              activeTickColor = Color.White,
              inactiveTickColor = Color.White
            ),
            modifier = Modifier.height(24.dp)
          )
        }
      )
    }
  }
}
