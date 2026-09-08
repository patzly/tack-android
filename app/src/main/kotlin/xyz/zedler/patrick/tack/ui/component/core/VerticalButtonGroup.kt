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

package xyz.zedler.patrick.tack.ui.component.core

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.node.invalidateParentData
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerticalButtonGroup(
  modifier: Modifier = Modifier,
  @FloatRange(from = 0.0) expandedRatio: Float = VerticalButtonGroupDefaults.EXPANDED_RATIO,
  verticalArrangement: Arrangement.Vertical = VerticalButtonGroupDefaults.VerticalArrangement,
  horizontalAlignment: Alignment.Horizontal = VerticalButtonGroupDefaults.HorizontalAlignment,
  overflowIndicator: @Composable (VerticalButtonGroupMenuState) -> Unit = {
    VerticalButtonGroupDefaults.OverflowIndicator(it)
  },
  content: VerticalButtonGroupScope.() -> Unit,
) {
  val defaultAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
  val scope = remember(defaultAnimationSpec, content) {
    VerticalButtonGroupScopeImpl(animationSpec = defaultAnimationSpec).apply(content)
  }

  val menuState = rememberVerticalButtonGroupMenuState()
  var visibleCount by remember { mutableIntStateOf(Int.MAX_VALUE) }

  val measurePolicy = remember(
    verticalArrangement,
    horizontalAlignment,
    expandedRatio,
  ) {
    VerticalButtonGroupMeasurePolicy(
      onVisibleItemCountChanged = { visibleCount = it },
      verticalArrangement = verticalArrangement,
      horizontalAlignment = horizontalAlignment,
      expandedRatio = expandedRatio,
    )
  }

  Layout(
    contents = listOf(
      {
        scope.items.fastForEachIndexed { index, item ->
          key(index) {
            item.ButtonGroupContent()
          }
        }
      },
      {
        Box {
          overflowIndicator(menuState)
          DropdownMenu(
            expanded = menuState.isShowing,
            onDismissRequest = { menuState.dismiss() },
          ) {
            val totalItems = scope.items.size
            val start = visibleCount.coerceIn(0, totalItems)
            for (i in start until totalItems) {
              key(i) {
                scope.items[i].MenuContent(menuState)
              }
            }
          }
        }
      }
    ),
    measurePolicy = measurePolicy,
    modifier = modifier,
  )
}

object VerticalButtonGroupDefaults {
  const val EXPANDED_RATIO: Float = 0.15f
  val VerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
  val HorizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
  val CompressionLimit: Dp = 8.dp

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun OverflowIndicator(
    menuState: VerticalButtonGroupMenuState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
  ) {
    val contentDescription = stringResource(R.string.action_more)

    TooltipBox(
      positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        TooltipAnchorPosition.Start
      ),
      tooltip = { PlainTooltip { Text(contentDescription) } },
      state = rememberTooltipState(),
    ) {
      FilledIconButton(
        onClick = {
          if (menuState.isShowing) menuState.dismiss() else menuState.show()
        },
        modifier = modifier.semantics {
          if (menuState.isShowing) {
            collapse {
              menuState.dismiss()
              true
            }
          } else {
            expand {
              menuState.show()
              true
            }
          }
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_rounded_more_vert),
          contentDescription = contentDescription,
        )
      }
    }
  }
}

@Stable
class VerticalButtonGroupMenuState(initialIsShowing: Boolean = false) {
  var isShowing: Boolean by mutableStateOf(initialIsShowing)
    private set

  fun dismiss() {
    isShowing = false
  }

  fun show() {
    isShowing = true
  }

  companion object {
    val Saver: Saver<VerticalButtonGroupMenuState, *> = Saver(
      save = { it.isShowing },
      restore = { VerticalButtonGroupMenuState(it) },
    )
  }
}

@Composable
fun rememberVerticalButtonGroupMenuState(
  initialIsShowing: Boolean = false
): VerticalButtonGroupMenuState {
  return rememberSaveable(saver = VerticalButtonGroupMenuState.Saver) {
    VerticalButtonGroupMenuState(initialIsShowing)
  }
}

private class VerticalButtonGroupMeasurePolicy(
  private val onVisibleItemCountChanged: (Int) -> Unit,
  private val verticalArrangement: Arrangement.Vertical,
  private val horizontalAlignment: Alignment.Horizontal,
  private val expandedRatio: Float,
) : MultiContentMeasurePolicy {

  override fun MeasureScope.measure(
    measurables: List<List<Measurable>>,
    constraints: Constraints,
  ): MeasureResult {
    val (contentMeasurables, overflowMeasurables) = measurables
    val size = contentMeasurables.size

    if (size == 0) {
      onVisibleItemCountChanged(0)
      return layout(constraints.minWidth, constraints.minHeight) {}
    }

    val arrangementSpacingInt = verticalArrangement.spacing.roundToPx()
    val totalSpacings = arrangementSpacingInt * (size - 1)
    val mainAxisMin = constraints.minHeight
    val mainAxisMax = constraints.maxHeight

    var totalWeight = 0f
    var weightChildrenCount = 0
    var fixedSpace = 0
    val heights = IntArray(size)

    for (i in 0 until size) {
      val weight = contentMeasurables[i].verticalButtonGroupParentData?.weight ?: 0f
      if (weight > 0f) {
        totalWeight += weight
        weightChildrenCount++
      } else {
        val desiredHeight = contentMeasurables[i].maxIntrinsicHeight(constraints.maxWidth)
        val clampedHeight = desiredHeight.coerceAtLeast(0)
        heights[i] = clampedHeight
        fixedSpace += clampedHeight
      }
    }

    if (weightChildrenCount > 0) {
      if (mainAxisMax != Constraints.Infinity) {
        val targetSpace = max(mainAxisMax, mainAxisMin)
        val remainingToTarget = (targetSpace - fixedSpace - totalSpacings).coerceAtLeast(0)
        val weightUnitSpace = if (totalWeight > 0f) remainingToTarget / totalWeight else 0f
        var remainder = remainingToTarget

        for (i in 0 until size) {
          val weight = contentMeasurables[i].verticalButtonGroupParentData?.weight ?: 0f
          if (weight > 0f) {
            remainder -= (weightUnitSpace * weight).fastRoundToInt()
          }
        }

        for (i in 0 until size) {
          val weight = contentMeasurables[i].verticalButtonGroupParentData?.weight ?: 0f
          if (weight > 0f) {
            val remainderUnit = remainder.sign
            remainder -= remainderUnit
            val weightedSize = (weightUnitSpace * weight)
            val childHeight = max(0, weightedSize.fastRoundToInt() + remainderUnit)
            heights[i] = childHeight
          }
        }
      } else {
        for (i in 0 until size) {
          val weight = contentMeasurables[i].verticalButtonGroupParentData?.weight ?: 0f
          if (weight > 0f) {
            val desiredHeight = contentMeasurables[i].maxIntrinsicHeight(constraints.maxWidth)
            heights[i] = desiredHeight.coerceAtLeast(0)
          }
        }
      }
    }

    var totalDesiredHeight = totalSpacings
    for (i in 0 until size) {
      totalDesiredHeight += heights[i]
    }

    var lastItem = 0
    val overflowPlaceables: List<Placeable>?
    val overflowHeight: Int

    if (mainAxisMax == Constraints.Infinity || totalDesiredHeight <= mainAxisMax) {
      lastItem = size
      overflowPlaceables = null
      overflowHeight = 0
    } else {
      val rawOverflowHeight = overflowMeasurables.fastMaxOfOrNull {
        it.maxIntrinsicHeight(constraints.maxWidth)
      } ?: 0
      overflowHeight = rawOverflowHeight

      var remainingSpace = mainAxisMax - overflowHeight
      while (lastItem < size) {
        val neededSpace = heights[lastItem] + arrangementSpacingInt
        if (neededSpace > remainingSpace + arrangementSpacingInt) break
        remainingSpace -= neededSpace
        lastItem++
      }

      val safeRemaining = max(0, remainingSpace + overflowHeight)
      val overflowConstraints = constraints.copy(minHeight = 0, maxHeight = safeRemaining)
      overflowPlaceables = overflowMeasurables.map { it.measure(overflowConstraints) }
    }

    onVisibleItemCountChanged(lastItem)

    if (lastItem > 1 && expandedRatio > 0f) {
      var hasAnyActiveAnimation = false
      for (i in 0 until lastItem) {
        val anim = contentMeasurables[i].verticalButtonGroupParentData?.pressedAnimatable
        if (anim != null && anim.value > 0f) {
          hasAnyActiveAnimation = true
          break
        }
      }

      if (hasAnyActiveAnimation) {
        val baseHeights = heights.clone()
        for (index in 0 until lastItem) {
          val anim = contentMeasurables[index].verticalButtonGroupParentData?.pressedAnimatable
          val animValue = anim?.value ?: 0f
          if (animValue == 0f) continue

          val actualGrowth = when (index) {
            0 -> {
              val nextLimit = contentMeasurables[1].verticalButtonGroupParentData
                ?.compressionLimit?.toPx()
                ?: VerticalButtonGroupDefaults.CompressionLimit.toPx()
              val targetGrowth = (animValue * min(
                expandedRatio * baseHeights[0],
                nextLimit,
              )).fastRoundToInt()
              val growthBottom = min(targetGrowth, heights[1])
              heights[1] -= growthBottom
              growthBottom
            }
            lastItem - 1 -> {
              val prevLimit = contentMeasurables[index - 1].verticalButtonGroupParentData
                ?.compressionLimit?.toPx()
                ?: VerticalButtonGroupDefaults.CompressionLimit.toPx()
              val targetGrowth = (animValue * min(
                expandedRatio * baseHeights[index],
                prevLimit,
              )).fastRoundToInt()
              val growthTop = min(targetGrowth, heights[index - 1])
              heights[index - 1] -= growthTop
              growthTop
            }
            else -> {
              val prevLimit = contentMeasurables[index - 1].verticalButtonGroupParentData
                ?.compressionLimit?.toPx()
                ?: VerticalButtonGroupDefaults.CompressionLimit.toPx()
              val nextLimit = contentMeasurables[index + 1].verticalButtonGroupParentData
                ?.compressionLimit?.toPx()
                ?: VerticalButtonGroupDefaults.CompressionLimit.toPx()
              val targetGrowth = (animValue * minOf(
                expandedRatio * baseHeights[index] / 2f,
                prevLimit,
                nextLimit,
              )).fastRoundToInt()
              val growthTop = min(targetGrowth, heights[index - 1])
              val growthBottom = min(targetGrowth, heights[index + 1])
              heights[index - 1] -= growthTop
              heights[index + 1] -= growthBottom
              growthTop + growthBottom
            }
          }
          heights[index] += actualGrowth
        }
      }
    }

    val placeables = ArrayList<Placeable>(lastItem)
    for (index in 0 until lastItem) {
      val itemConstraints = constraints.copy(
        minHeight = heights[index],
        maxHeight = heights[index],
      )
      placeables.add(contentMeasurables[index].measure(itemConstraints))
    }

    val hasOverflow = overflowPlaceables != null
    val totalArrangeCount = if (hasOverflow) lastItem + 1 else lastItem
    val arrangedHeights = IntArray(totalArrangeCount)
    for (i in 0 until lastItem) {
      arrangedHeights[i] = heights[i]
    }
    val actualOverflowHeight = overflowPlaceables?.fastMaxOfOrNull { it.height } ?: overflowHeight
    if (hasOverflow) {
      arrangedHeights[lastItem] = actualOverflowHeight
    }

    var desiredMainSpace = arrangementSpacingInt * max(0, totalArrangeCount - 1)
    for (i in 0 until totalArrangeCount) {
      desiredMainSpace += arrangedHeights[i]
    }
    val mainAxisLayoutSize = max(desiredMainSpace, mainAxisMin).coerceIn(0, mainAxisMax)

    val mainAxisPositions = IntArray(totalArrangeCount)
    with(verticalArrangement) {
      arrange(
        mainAxisLayoutSize,
        arrangedHeights,
        mainAxisPositions,
      )
    }

    val maxContentWidth = placeables.fastMaxBy { it.width }?.width ?: 0
    val maxOverflowWidth = overflowPlaceables?.fastMaxBy { it.width }?.width ?: 0
    val width = maxOf(
      maxContentWidth,
      maxOverflowWidth,
      constraints.minWidth,
    ).coerceAtMost(constraints.maxWidth)

    return layout(width, mainAxisLayoutSize) {
      for (index in placeables.indices) {
        val parentData = contentMeasurables[index].verticalButtonGroupParentData
        val alignment = parentData?.alignment ?: horizontalAlignment
        val xPosition = alignment.align(placeables[index].width, width, layoutDirection)
        placeables[index].place(x = xPosition, y = mainAxisPositions[index])
      }
      overflowPlaceables?.fastForEach {
        val xPosition = horizontalAlignment.align(it.width, width, layoutDirection)
        it.place(x = xPosition, y = mainAxisPositions[lastItem])
      }
    }
  }
}

sealed interface VerticalButtonGroupScope {
  fun Modifier.weight(
    @FloatRange(from = 0.0, fromInclusive = false) weight: Float
  ): Modifier

  fun Modifier.animateHeight(interactionSource: InteractionSource): Modifier
  fun Modifier.animateHeight(interactionSource: InteractionSource, compressionLimit: Dp): Modifier
  @Stable
  fun Modifier.align(alignment: Alignment.Horizontal): Modifier

  fun clickableItem(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    weight: Float = Float.NaN,
    enabled: Boolean = true,
  )

  fun toggleableItem(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    weight: Float = Float.NaN,
    enabled: Boolean = true,
  )

  fun customItem(
    buttonGroupContent: @Composable () -> Unit,
    menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
  )
}

private val IntrinsicMeasurable.verticalButtonGroupParentData: VerticalButtonGroupParentData?
  get() = parentData as? VerticalButtonGroupParentData

private data class VerticalButtonGroupParentData(
  val weight: Float = 0f,
  val pressedAnimatable: Animatable<Float, AnimationVector1D>? = null,
  val alignment: Alignment.Horizontal? = null,
  val compressionLimit: Dp = VerticalButtonGroupDefaults.CompressionLimit,
)

private class VerticalButtonGroupElement(val weight: Float = 0f) :
  ModifierNodeElement<VerticalButtonGroupNode>() {
  override fun create(): VerticalButtonGroupNode = VerticalButtonGroupNode(weight)
  override fun update(node: VerticalButtonGroupNode) {
    node.weight = weight
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "weight"
    value = weight
  }

  override fun hashCode(): Int = weight.hashCode()
  override fun equals(other: Any?): Boolean =
    (other as? VerticalButtonGroupElement)?.weight == weight
}

private class VerticalButtonGroupNode(weight: Float) : ParentDataModifierNode, Modifier.Node() {
  var weight: Float = weight
    set(value) {
      if (field != value) {
        field = value
        invalidateParentData()
      }
    }

  override fun Density.modifyParentData(parentData: Any?): VerticalButtonGroupParentData {
    return ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData())
      .copy(weight = weight)
  }
}

private class VerticalEnlargeOnPressElement(
  val interactionSource: InteractionSource,
  val animationSpec: AnimationSpec<Float>,
  val compressionLimit: Dp? = null,
) : ModifierNodeElement<VerticalEnlargeOnPressNode>() {
  override fun create() =
    VerticalEnlargeOnPressNode(interactionSource, animationSpec, compressionLimit)

  override fun update(node: VerticalEnlargeOnPressNode) {
    if (node.interactionSource != interactionSource) {
      node.interactionSource = interactionSource
      node.launchCollectionJob()
    }
    node.animationSpec = animationSpec
    node.compressionLimit = compressionLimit
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "animateHeight"
    properties["interactionSource"] = interactionSource
    properties["compressionLimit"] = compressionLimit
  }

  override fun hashCode(): Int =
    interactionSource.hashCode() * 31 + animationSpec.hashCode() + (
        compressionLimit?.hashCode() ?: 0
        )

  override fun equals(other: Any?): Boolean =
    other is VerticalEnlargeOnPressElement &&
        interactionSource == other.interactionSource &&
        animationSpec == other.animationSpec &&
        compressionLimit == other.compressionLimit
}

private class VerticalEnlargeOnPressNode(
  var interactionSource: InteractionSource,
  var animationSpec: AnimationSpec<Float>,
  compressionLimit: Dp?,
) : ParentDataModifierNode, Modifier.Node() {

  var compressionLimit: Dp? = compressionLimit
    set(value) {
      if (field != value) {
        field = value
        invalidateParentData()
      }
    }

  val pressedAnimatable = Animatable(0f)
  private var collectionJob: Job? = null
  private var animationJob: Job? = null

  override fun onAttach() {
    super.onAttach()
    launchCollectionJob()
  }

  override fun onDetach() {
    super.onDetach()
    collectionJob?.cancel()
    collectionJob = null
    animationJob?.cancel()
    animationJob = null
  }

  override fun onReset() {
    super.onReset()
    animationJob?.cancel()
    coroutineScope.launch {
      pressedAnimatable.snapTo(0f)
    }
  }

  fun launchCollectionJob() {
    collectionJob?.cancel()
    collectionJob = coroutineScope.launch {
      val pressInteractions = mutableListOf<PressInteraction.Press>()
      interactionSource.interactions.collect { interaction ->
        when (interaction) {
          is PressInteraction.Press -> pressInteractions.add(interaction)
          is PressInteraction.Release -> pressInteractions.remove(interaction.press)
          is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
        }
        val targetValue = if (pressInteractions.isNotEmpty()) 1f else 0f
        animationJob?.cancel()
        animationJob = launch {
          pressedAnimatable.animateTo(targetValue, animationSpec)
        }
      }
    }
  }

  override fun Density.modifyParentData(parentData: Any?): VerticalButtonGroupParentData {
    val limit = compressionLimit ?: ButtonDefaults.ContentPadding.calculateBottomPadding()
    return ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData())
      .copy(
        pressedAnimatable = pressedAnimatable,
        compressionLimit = limit,
      )
  }
}

private interface VerticalButtonGroupItem {
  @Composable
  fun ButtonGroupContent()
  @Composable
  fun MenuContent(state: VerticalButtonGroupMenuState)
}

private class CustomVerticalButtonGroupItem(
  private val buttonGroupContent: @Composable () -> Unit,
  private val menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
) : VerticalButtonGroupItem {
  @Composable
  override fun ButtonGroupContent() = buttonGroupContent()
  @Composable
  override fun MenuContent(state: VerticalButtonGroupMenuState) = menuContent(state)
}

private class ClickableVerticalButtonGroupItem(
  private val onClick: () -> Unit,
  private val userModifier: Modifier,
  private val icon: (@Composable () -> Unit)?,
  private val weight: Float,
  private val animationSpec: AnimationSpec<Float>,
  private val enabled: Boolean,
  private val label: String,
) : VerticalButtonGroupItem {
  @Composable
  override fun ButtonGroupContent() {
    val interactionSource = remember { MutableInteractionSource() }
    val contentPadding = if (icon != null) {
      ButtonDefaults.ButtonWithIconContentPadding
    } else {
      ButtonDefaults.ContentPadding
    }

    val itemModifier = Modifier
      .then(
        VerticalEnlargeOnPressElement(
          interactionSource = interactionSource,
          animationSpec = animationSpec,
          compressionLimit = contentPadding.calculateBottomPadding(),
        )
      )
      .then(
        if (!weight.isNaN() && weight > 0f) {
          VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
        } else {
          Modifier
        }
      )
      .then(userModifier)

    Button(
      onClick = onClick,
      modifier = itemModifier,
      interactionSource = interactionSource,
      enabled = enabled,
      contentPadding = contentPadding,
    ) {
      icon?.let {
        it.invoke()
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      }
      Text(
        text = label,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  @Composable
  override fun MenuContent(state: VerticalButtonGroupMenuState) {
    DropdownMenuItem(
      enabled = enabled,
      leadingIcon = icon,
      text = { Text(label, overflow = TextOverflow.Ellipsis) },
      onClick = {
        onClick()
        state.dismiss()
      },
    )
  }
}

private class ToggleableVerticalButtonGroupItem(
  private val checked: Boolean,
  private val onCheckedChange: (Boolean) -> Unit,
  private val userModifier: Modifier,
  private val weight: Float,
  private val animationSpec: AnimationSpec<Float>,
  private val icon: (@Composable () -> Unit)?,
  private val enabled: Boolean,
  private val label: String,
) : VerticalButtonGroupItem {
  @Composable
  override fun ButtonGroupContent() {
    val interactionSource = remember { MutableInteractionSource() }
    val contentPadding = if (icon != null) {
      ButtonDefaults.ButtonWithIconContentPadding
    } else {
      ButtonDefaults.ContentPadding
    }

    val itemModifier = Modifier
      .then(
        VerticalEnlargeOnPressElement(
          interactionSource = interactionSource,
          animationSpec = animationSpec,
          compressionLimit = contentPadding.calculateBottomPadding(),
        )
      )
      .then(
        if (!weight.isNaN() && weight > 0f) {
          VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
        } else {
          Modifier
        }
      )
      .then(userModifier)

    ToggleButton(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = itemModifier,
      interactionSource = interactionSource,
      enabled = enabled,
      contentPadding = contentPadding,
    ) {
      icon?.let {
        it.invoke()
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      }
      Text(
        text = label,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  @Composable
  override fun MenuContent(state: VerticalButtonGroupMenuState) {
    SelectableDropdownMenuItem(
      selected = checked,
      onClick = {
        onCheckedChange(!checked)
        state.dismiss()
      },
      shapes = MenuDefaults.itemShapes(),
      enabled = enabled,
      leadingIcon = icon,
      text = { Text(label, overflow = TextOverflow.Ellipsis) },
    )
  }
}

private class HorizontalAlignElement(val alignment: Alignment.Horizontal) :
  ModifierNodeElement<HorizontalAlignNode>() {
  override fun create() = HorizontalAlignNode(alignment)
  override fun update(node: HorizontalAlignNode) {
    node.alignment = alignment
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "align"
    value = alignment
  }

  override fun hashCode() = alignment.hashCode()
  override fun equals(other: Any?) = (other as? HorizontalAlignElement)?.alignment == alignment
}

private class HorizontalAlignNode(alignment: Alignment.Horizontal) :
  ParentDataModifierNode, Modifier.Node() {
  var alignment: Alignment.Horizontal = alignment
    set(value) {
      if (field != value) {
        field = value
        invalidateParentData()
      }
    }

  override fun Density.modifyParentData(parentData: Any?): VerticalButtonGroupParentData {
    return ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData())
      .copy(alignment = alignment)
  }
}

private class VerticalButtonGroupScopeImpl(
  val animationSpec: AnimationSpec<Float>
) : VerticalButtonGroupScope {
  val items = mutableListOf<VerticalButtonGroupItem>()

  override fun clickableItem(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier,
    icon: (@Composable () -> Unit)?,
    weight: Float,
    enabled: Boolean,
  ) {
    require(weight.isNaN() || weight > 0f) {
      "invalid weight $weight; must be greater than zero or Float.NaN"
    }
    items.add(
      ClickableVerticalButtonGroupItem(
        onClick = onClick,
        userModifier = modifier,
        icon = icon,
        weight = weight,
        animationSpec = animationSpec,
        enabled = enabled,
        label = label,
      )
    )
  }

  override fun toggleableItem(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    icon: (@Composable () -> Unit)?,
    weight: Float,
    enabled: Boolean,
  ) {
    require(weight.isNaN() || weight > 0f) {
      "invalid weight $weight; must be greater than zero or Float.NaN"
    }
    items.add(
      ToggleableVerticalButtonGroupItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        userModifier = modifier,
        weight = weight,
        animationSpec = animationSpec,
        icon = icon,
        enabled = enabled,
        label = label,
      )
    )
  }

  override fun customItem(
    buttonGroupContent: @Composable () -> Unit,
    menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
  ) {
    items.add(CustomVerticalButtonGroupItem(buttonGroupContent, menuContent))
  }

  override fun Modifier.weight(weight: Float): Modifier {
    require(weight > 0f) { "invalid weight $weight; must be greater than zero" }
    return this.then(
      VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
    )
  }

  override fun Modifier.animateHeight(interactionSource: InteractionSource): Modifier = this.then(
    VerticalEnlargeOnPressElement(interactionSource, animationSpec, null)
  )

  override fun Modifier.animateHeight(
    interactionSource: InteractionSource,
    compressionLimit: Dp
  ): Modifier = this.then(
    VerticalEnlargeOnPressElement(interactionSource, animationSpec, compressionLimit)
  )

  override fun Modifier.align(alignment: Alignment.Horizontal): Modifier = this.then(
    HorizontalAlignElement(alignment)
  )
}
