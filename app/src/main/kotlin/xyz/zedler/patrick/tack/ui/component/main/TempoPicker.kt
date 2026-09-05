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

package xyz.zedler.patrick.tack.ui.component.main

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.theme.LocalDimens
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.normalize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TempoPicker(
  tempo: Int,
  tempoTerm: String,
  reduceAnimations: Boolean,
  onTempoChangeDelta: (Int) -> Unit,
  onDragStateChange: (Boolean) -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dimens = LocalDimens.current
  val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

  val previousTempoRef = remember { intArrayOf(tempo) }
  val isFaster = remember(tempo) {
    val faster = tempo >= previousTempoRef[0]
    previousTempoRef[0] = tempo
    faster
  }

  var isDragged by remember { mutableStateOf(false) }
  var totalRotation by remember { mutableFloatStateOf(0f) }
  var touchPos by remember { mutableStateOf(Offset.Zero) }

  val spatialSpring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
  val effectsSpring = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

  val morphFactor by animateFloatAsState(
    targetValue = if (isDragged && !reduceAnimations) 1f else 0f,
    animationSpec = spatialSpring,
    label = "morphFactor"
  )

  val colorFraction by animateFloatAsState(
    targetValue = if (isDragged && !reduceAnimations) 0.85f else 0f,
    animationSpec = effectsSpring,
    label = "colorFraction"
  )

  val colorDefault = MaterialTheme.colorScheme.primaryContainer
  val colorDrag1 = MaterialTheme.colorScheme.tertiaryContainer
  val colorDrag2 = MaterialTheme.colorScheme.primaryContainer
  val colorDrag3 = MaterialTheme.colorScheme.secondaryContainer

  val fontWeight = (600f + (colorFraction * 300f)).toInt()
  val fontWidth = 100f + (colorFraction * 5f)

  val dynamicFontFamily = remember(fontWeight, fontWidth) {
    FontFamily(
      Font(
        resId = R.font.google_sans_flex_variable,
        variationSettings = FontVariation.Settings(
          FontVariation.weight(fontWeight),
          FontVariation.width(fontWidth),
          FontVariation.Setting("ROND", 100f)
        )
      )
    )
  }

  val morph = remember {
    Morph(
      normalize(
        MaterialShapes.Cookie12Sided,
        true,
        RectF(-1f, -1f, 1f, 1f)
      ),
      normalize(
        MaterialShapes.SoftBurst,
        true,
        RectF(-1f, -1f, 1f, 1f)
      )
    )
  }

  val androidPath = remember { android.graphics.Path() }
  val composePath = remember { androidPath.asComposePath() }
  val matrix = remember { Matrix() }

  Box(
    modifier = modifier
      .size(dimens.tempoPickerSize)
      .pointerInput(Unit) {
        awaitEachGesture {
          val down = awaitFirstDown()
          val cx = size.width / 2f
          val cy = size.height / 2f
          val radius = minOf(size.width, size.height) / 2f
          val innerRadius = 20.dp.toPx()

          val dist = hypot(down.position.x - cx, down.position.y - cy)

          if (dist <= radius) {
            isDragged = true
            touchPos = down.position
            onDragStateChange(true)

            var isStartedInCenter = dist <= innerRadius
            var currAngle = if (!isStartedInCenter) calculateAngle(touchPos, cx, cy) else 0.0
            var degreeStorage = 0f
            var hasDragged = false

            do {
              val event = awaitPointerEvent()
              val pointer = event.changes.first()
              touchPos = pointer.position

              val currentDist = hypot(touchPos.x - cx, touchPos.y - cy)
              val isOutsideCenter = currentDist > innerRadius

              if (isOutsideCenter) {
                val angle = calculateAngle(touchPos, cx, cy)
                if (isStartedInCenter) {
                  isStartedInCenter = false
                  currAngle = angle
                }

                val prevAngle = currAngle
                currAngle = angle

                var delta = (currAngle - prevAngle).toFloat()
                if (delta > 180f) delta -= 360f
                else if (delta < -180f) delta += 360f

                if (delta != 0f) {
                  hasDragged = true
                  totalRotation += delta
                  degreeStorage += delta

                  if (degreeStorage > 12f) {
                    onTempoChangeDelta(if (isRtl) -1 else 1)
                    degreeStorage = 0f
                  } else if (degreeStorage < -12f) {
                    onTempoChangeDelta(if (isRtl) 1 else -1)
                    degreeStorage = 0f
                  }
                }
              }
              pointer.consume()
            } while (event.changes.any { it.pressed })

            isDragged = false
            onDragStateChange(false)
            if (!hasDragged) {
              onClick()
            }
          }
        }
      }
      .drawWithContent {
        androidPath.rewind()
        morph.toPath(morphFactor, androidPath)

        matrix.reset()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postRotate(totalRotation)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        androidPath.transform(matrix)

        val brush = Brush.radialGradient(
          0.0f to lerp(colorDefault, colorDrag1, colorFraction),
          0.1f to lerp(colorDefault, colorDrag1, colorFraction),
          0.5f to lerp(colorDefault, colorDrag2, colorFraction),
          0.9f to lerp(colorDefault, colorDrag3, colorFraction),
          center = touchPos,
          radius = size.width
        )

        drawPath(
          path = composePath,
          brush = brush
        )

        clipPath(path = composePath) {
          this@drawWithContent.drawContent()
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      val slideSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
      val fadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

      AnimatedContent(
        targetState = tempoTerm,
        transitionSpec = {
          if (reduceAnimations) {
            fadeIn(animationSpec = tween(200)) togetherWith
                fadeOut(animationSpec = tween(200))
          } else {
            val direction = if (isFaster) SlideDirection.Start else SlideDirection.End
            (slideIntoContainer(
              towards = direction,
              animationSpec = slideSpec,
              initialOffset = { offset -> offset / 3 }
            ) + fadeIn(animationSpec = fadeSpec)) togetherWith
                (slideOutOfContainer(
                  towards = direction,
                  animationSpec = slideSpec,
                  targetOffset = { offset -> offset / 3 }
                ) + fadeOut(animationSpec = fadeSpec))
          }
        },
        label = "tempoTermAnim",
        modifier = Modifier.fillMaxWidth()
      ) { term ->
        Text(
          text = term,
          style = dimens.tempoPickerLabelTextStyle,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Text(
        text = tempo.toString(),
        style = dimens.tempoPickerBpmTextStyle.copy(
          fontFamily = dynamicFontFamily,
          fontFeatureSettings = "tnum"
        ),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      Text(
        text = stringResource(R.string.label_bpm),
        style = dimens.tempoPickerLabelTextStyle,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

private fun calculateAngle(pos: Offset, cx: Float, cy: Float): Double {
  val angleRaw = Math.toDegrees(atan2((pos.x - cx).toDouble(), (cy - pos.y).toDouble()))
  return if (angleRaw >= 0) angleRaw else 180 + (180 - abs(angleRaw))
}

@Preview
@Composable
fun TempoPickerPreview() {
  TackTheme {
    TempoPicker(
      tempo = 120,
      tempoTerm = "Allegro",
      reduceAnimations = false,
      onTempoChangeDelta = {},
      onDragStateChange = {},
      onClick = {}
    )
  }
}
