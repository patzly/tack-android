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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
fun VerticalButtonGroup(
  overflowIndicator: @Composable (VerticalButtonGroupMenuState) -> Unit,
  modifier: Modifier = Modifier,
  @FloatRange(0.0) expandedRatio: Float = VerticalButtonGroupDefaults.ExpandedRatio,
  verticalArrangement: Arrangement.Vertical = VerticalButtonGroupDefaults.VerticalArrangement,
  horizontalAlignment: Alignment.Horizontal = VerticalButtonGroupDefaults.HorizontalAlignment,
  content: VerticalButtonGroupScope.() -> Unit,
) {
  val defaultAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
  val scope: VerticalButtonGroupScopeImpl by rememberVerticalButtonGroupScopeState(
    content = content,
    animationSpec = defaultAnimationSpec
  )
  val menuState = remember { VerticalButtonGroupMenuState() }
  val overflowState = rememberVerticalOverflowState()

  val measurePolicy =
    remember(verticalArrangement, horizontalAlignment, overflowState, expandedRatio) {
      VerticalButtonGroupMeasurePolicy(
        overflowState = overflowState,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        expandedRatio = expandedRatio,
      )
    }

  Layout(
    contents = listOf(
      { scope.items.fastForEach { it.ButtonGroupContent() } },
      {
        Box {
          overflowIndicator(menuState)
          DropdownMenu(
            expanded = menuState.isShowing,
            onDismissRequest = { menuState.dismiss() },
          ) {
            for (i in overflowState.visibleItemCount until overflowState.totalItemCount) {
              scope.items[i].MenuContent(menuState)
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
  val ExpandedRatio: Float = 0.15f
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
        modifier = modifier,
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

class VerticalButtonGroupMenuState(initialIsShowing: Boolean = false) {
  var isShowing: Boolean by mutableStateOf(initialIsShowing)
    private set

  fun dismiss() {
    isShowing = false
  }

  fun show() {
    isShowing = true
  }
}

private class VerticalButtonGroupMeasurePolicy(
  val overflowState: VerticalButtonGroupOverflowState,
  val verticalArrangement: Arrangement.Vertical,
  val horizontalAlignment: Alignment.Horizontal,
  val expandedRatio: Float,
) : MultiContentMeasurePolicy {

  override fun MeasureScope.measure(
    measurables: List<List<Measurable>>,
    constraints: Constraints,
  ): MeasureResult {
    val (contentMeasurables, overflowMeasurables) = measurables
    overflowState.totalItemCount = contentMeasurables.size
    val arrangementSpacingInt = verticalArrangement.spacing.roundToPx()
    val arrangementSpacingPx = arrangementSpacingInt.toLong()
    val size = contentMeasurables.size
    var totalWeight = 0f
    var fixedSpace = 0
    var weightChildrenCount = 0
    val placeables = mutableListOf<Placeable>()
    val childrenMainAxisSize = IntArray(size)
    val childrenConstraints: Array<Constraints?> = arrayOfNulls(size)
    val configs = Array(contentMeasurables.size) {
      contentMeasurables[it].parentData as? VerticalButtonGroupParentData
        ?: VerticalButtonGroupParentData()
    }
    val animatables = Array(contentMeasurables.size) { configs[it].pressedAnimatable }

    val mainAxisMin = constraints.minHeight
    val mainAxisMax = constraints.maxHeight

    // 1. Constraints von Elementen ohne Weight ermitteln
    var spaceAfterLastNoWeight = 0
    for (i in 0 until size) {
      val child = contentMeasurables[i]
      val parentData = child.verticalButtonGroupParentData
      val weight = parentData.weight

      if (weight > 0f) {
        totalWeight += weight
        ++weightChildrenCount
      } else {
        val remaining = mainAxisMax - fixedSpace
        val desiredHeight = child.maxIntrinsicHeight(constraints.maxWidth)
        childrenConstraints[i] = constraints.copy(
          minHeight = 0, maxHeight = desiredHeight.coerceAtLeast(0)
        )
        childrenMainAxisSize[i] = desiredHeight

        spaceAfterLastNoWeight = min(
          arrangementSpacingInt,
          (remaining - desiredHeight).coerceAtLeast(0)
        )
        fixedSpace += desiredHeight + spaceAfterLastNoWeight
      }
    }

    // 2. Constraints von Weighted Children berechnen
    var weightedSpace = 0
    if (weightChildrenCount == 0) {
      fixedSpace -= spaceAfterLastNoWeight
    } else {
      val targetSpace = if (mainAxisMax != Constraints.Infinity) mainAxisMax else mainAxisMin
      val arrangementSpacingTotal = arrangementSpacingPx * (weightChildrenCount - 1)
      val remainingToTarget =
        (targetSpace - fixedSpace - arrangementSpacingTotal).coerceAtLeast(0)
      val weightUnitSpace = remainingToTarget / totalWeight
      var remainder = remainingToTarget
      for (i in 0 until size) {
        val measurable = contentMeasurables[i]
        val itemWeight = measurable.verticalButtonGroupParentData.weight
        val weightedSize = (weightUnitSpace * itemWeight)
        remainder -= weightedSize.fastRoundToInt()
      }

      for (i in 0 until size) {
        if (childrenConstraints[i] == null) {
          val child = contentMeasurables[i]
          val parentData = child.verticalButtonGroupParentData
          val weight = parentData.weight

          val remainderUnit = remainder.sign
          remainder -= remainderUnit
          val weightedSize = (weightUnitSpace * weight)
          val childMainAxisSize = max(0, weightedSize.fastRoundToInt() + remainderUnit)

          childrenConstraints[i] = constraints.copy(
            minHeight = if (childMainAxisSize != Constraints.Infinity) childMainAxisSize else 0,
            maxHeight = childMainAxisSize,
          )
          childrenMainAxisSize[i] = childMainAxisSize
          weightedSpace += childMainAxisSize
        }
        weightedSpace =
          (weightedSpace + arrangementSpacingTotal).toInt().coerceIn(0, mainAxisMax - fixedSpace)
      }
    }

    var remainingSpace = mainAxisMax
    var mainSpace = 0
    var shownItemSpace = 0
    val heights =
      IntArray(contentMeasurables.size) { (childrenConstraints[it] ?: constraints).maxHeight }
    val desiredHeight = heights.sum() + arrangementSpacingInt * (contentMeasurables.size - 1)
    var lastItem = 0

    val overflowPlaceables = if (desiredHeight <= mainAxisMax) {
      lastItem = heights.size
      mainSpace = desiredHeight
      null
    } else {
      val overflowHeight = overflowMeasurables.fastMaxOfOrNull {
        it.maxIntrinsicHeight(constraints.maxWidth)
      } ?: 0
      remainingSpace -= overflowHeight
      mainSpace += overflowHeight

      while (lastItem < heights.size && heights[lastItem] <= remainingSpace) {
        val itemHeight = heights[lastItem]
        mainSpace += itemHeight
        shownItemSpace += itemHeight
        remainingSpace -= itemHeight + arrangementSpacingInt
        lastItem++
      }

      mainSpace += arrangementSpacingInt * lastItem
      shownItemSpace += arrangementSpacingInt * lastItem

      overflowMeasurables.fastMap {
        it.measure(constraints.copy(maxHeight = remainingSpace + overflowHeight))
      }
    }

    overflowState.visibleItemCount = lastItem

    // 3. Höhe animieren (Vergrößern des gedrückten & Stauchen der Nachbarn)
    if (contentMeasurables.size > 1) {
      for (index in 0 until lastItem) {
        if (animatables[index].value == 0f) continue
        var actualGrowth: Int

        if (index in 1 until lastItem - 1) {
          val targetGrowth = (animatables[index].value *
              minOf(
                (expandedRatio * heights[index] / 2f),
                configs[index - 1].compressionLimit.toPx(),
                configs[index + 1].compressionLimit.toPx(),
              )).roundToInt()
          val growthTop = min(targetGrowth, heights[index - 1])
          val growthBottom = min(targetGrowth, heights[index + 1])
          heights[index - 1] -= growthTop
          heights[index + 1] -= growthBottom
          actualGrowth = growthTop + growthBottom
        } else if (index == 0) {
          val targetGrowth = (animatables[index].value *
              min(
                expandedRatio * heights[index],
                configs[index + 1].compressionLimit.toPx(),
              )).roundToInt()
          val growthBottom = min(targetGrowth, heights[index + 1])
          heights[index + 1] -= growthBottom
          actualGrowth = growthBottom
        } else {
          val targetGrowth = (animatables[index].value *
              min(
                expandedRatio * heights[index],
                configs[index - 1].compressionLimit.toPx(),
              )).roundToInt()
          val growthTop = min(targetGrowth, heights[index - 1])
          heights[index - 1] -= growthTop
          actualGrowth = growthTop
        }

        heights[index] += actualGrowth
      }
    }

    for (index in 0 until lastItem) {
      placeables.add(
        contentMeasurables[index].measure(
          (childrenConstraints[index] ?: constraints).copy(
            minHeight = heights[index],
            maxHeight = heights[index],
          )
        )
      )
    }

    // 4. Layout-Größe und Platzierung
    val mainAxisLayoutSize = max(mainSpace.coerceAtLeast(0), mainAxisMin)
    val mainAxisPositions = IntArray(lastItem)
    val measureScope = this
    with(verticalArrangement) {
      measureScope.arrange(
        mainAxisLayoutSize,
        heights.sliceArray(0 until lastItem),
        mainAxisPositions,
      )
    }

    val width = placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth

    return layout(width, mainAxisLayoutSize) {
      for (index in placeables.indices) {
        val parentData = contentMeasurables[index].parentData as? VerticalButtonGroupParentData
        val xPosition = parentData?.alignment?.align(
          placeables[index].width, width, layoutDirection
        ) ?: horizontalAlignment.align(
          placeables[index].width, width, layoutDirection
        )
        placeables[index].place(x = xPosition, y = mainAxisPositions[index])
      }
      overflowPlaceables?.fastForEach {
        val xPosition = horizontalAlignment.align(it.width, width, layoutDirection)
        it.placeRelative(xPosition, shownItemSpace)
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
    icon: (@Composable () -> Unit)? = null,
    weight: Float = Float.NaN,
    enabled: Boolean = true,
  )

  fun toggleableItem(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
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

private val VerticalButtonGroupParentData?.weight: Float
  get() = this?.weight ?: 0f

private data class VerticalButtonGroupParentData(
  var weight: Float = 0f,
  var pressedAnimatable: Animatable<Float, AnimationVector1D> = Animatable(0f),
  var alignment: Alignment.Horizontal? = null,
  var compressionLimit: Dp = VerticalButtonGroupDefaults.CompressionLimit,
)

private class VerticalButtonGroupElement(val weight: Float = 0f) :
  ModifierNodeElement<VerticalButtonGroupNode>() {
  override fun create(): VerticalButtonGroupNode = VerticalButtonGroupNode(weight)
  override fun update(node: VerticalButtonGroupNode) {
    node.weight = weight
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "weight"; value = weight
  }

  override fun hashCode(): Int = weight.hashCode()
  override fun equals(other: Any?): Boolean =
    (other as? VerticalButtonGroupElement)?.weight == weight
}

private class VerticalButtonGroupNode(var weight: Float) : ParentDataModifierNode, Modifier.Node() {
  override fun Density.modifyParentData(parentData: Any?) =
    ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData()).also {
      it.weight = weight
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
  }

  override fun hashCode(): Int = interactionSource.hashCode() * 31 + animationSpec.hashCode()
  override fun equals(other: Any?): Boolean =
    other is VerticalEnlargeOnPressElement &&
        interactionSource == other.interactionSource &&
        animationSpec == other.animationSpec &&
        compressionLimit == other.compressionLimit
}

private class VerticalEnlargeOnPressNode(
  var interactionSource: InteractionSource,
  var animationSpec: AnimationSpec<Float>,
  var compressionLimit: Dp?,
) : ParentDataModifierNode, Modifier.Node(), CompositionLocalConsumerModifierNode {
  private val pressedAnimatable = Animatable(0f)
  private var collectionJob: Job? = null

  override fun onAttach() {
    super.onAttach()
    launchCollectionJob()
  }

  override fun onDetach() {
    super.onDetach()
    collectionJob = null
  }

  internal fun launchCollectionJob() {
    collectionJob?.cancel()
    collectionJob = coroutineScope.launch {
      val pressInteractions = mutableListOf<PressInteraction.Press>()
      interactionSource.interactions
        .map { interaction ->
          when (interaction) {
            is PressInteraction.Press -> pressInteractions.add(interaction)
            is PressInteraction.Release -> pressInteractions.remove(interaction.press)
            is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
          }
          pressInteractions.isNotEmpty()
        }
        .distinctUntilChanged()
        .collectLatest { pressed ->
          if (pressed) {
            launch { pressedAnimatable.animateTo(1f, animationSpec) }
          } else {
            waitUntil { pressedAnimatable.value > 0.75f }
            pressedAnimatable.animateTo(0f, animationSpec)
          }
        }
    }
  }

  override fun Density.modifyParentData(parentData: Any?) =
    ((parentData as? VerticalButtonGroupParentData)
      ?: VerticalButtonGroupParentData()).let { prev ->
      val resolvedLimit = compressionLimit ?: ButtonDefaults.ContentPadding.calculateBottomPadding()
      VerticalButtonGroupParentData(
        prev.weight, pressedAnimatable, prev.alignment, resolvedLimit
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

    val modifier = Modifier
      .then(
        VerticalEnlargeOnPressElement(
          interactionSource,
          animationSpec,
          contentPadding.calculateBottomPadding()
        )
      )
      .then(
        if (!weight.isNaN()) {
          VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
        } else Modifier
      )

    Button(
      onClick = onClick,
      modifier = modifier,
      interactionSource = interactionSource,
      enabled = enabled,
      contentPadding = contentPadding,
    ) {
      icon?.let {
        it.invoke()
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      }
      Text(text = label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
    }
  }

  @Composable
  override fun MenuContent(state: VerticalButtonGroupMenuState) {
    DropdownMenuItem(
      enabled = enabled,
      leadingIcon = icon,
      text = { Text(label) },
      onClick = { onClick(); state.dismiss() },
    )
  }
}

private class ToggleableVerticalButtonGroupItem(
  private val checked: Boolean,
  private val onCheckedChange: (Boolean) -> Unit,
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
    } else ButtonDefaults.ContentPadding

    val modifier = Modifier
      .then(
        VerticalEnlargeOnPressElement(
          interactionSource,
          animationSpec,
          contentPadding.calculateBottomPadding()
        )
      )
      .then(
        if (!weight.isNaN()) {
          VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
        } else Modifier
      )

    ToggleButton(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = modifier,
      interactionSource = interactionSource,
      enabled = enabled,
      contentPadding = contentPadding,
    ) {
      icon?.let {
        it.invoke()
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      }
      Text(text = label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
    }
  }

  @Composable
  override fun MenuContent(state: VerticalButtonGroupMenuState) {
    DropdownMenuItem(
      enabled = enabled,
      leadingIcon = icon,
      text = { Text(label) },
      onClick = { onCheckedChange(!checked); state.dismiss() },
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
    name = "align"; value = alignment
  }

  override fun hashCode() = alignment.hashCode()
  override fun equals(other: Any?) = (other as? HorizontalAlignElement)?.alignment == alignment
}

private class HorizontalAlignNode(var alignment: Alignment.Horizontal) :
  ParentDataModifierNode, Modifier.Node() {
  override fun Density.modifyParentData(parentData: Any?) =
    ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData()).also {
      it.alignment = alignment
    }
}

private interface VerticalButtonGroupOverflowState {
  var totalItemCount: Int
  var visibleItemCount: Int
}

@Composable
private fun rememberVerticalOverflowState(): VerticalButtonGroupOverflowState {
  return rememberSaveable(saver = VerticalOverflowStateImpl.Saver) { VerticalOverflowStateImpl() }
}

private suspend fun waitUntil(condition: () -> Boolean) {
  val initialTimeMillis = withFrameMillis { it }
  while (!condition()) {
    val timeMillis = withFrameMillis { it }
    if (timeMillis - initialTimeMillis > 1_000L) return
  }
}

private class VerticalOverflowStateImpl : VerticalButtonGroupOverflowState {
  override var totalItemCount: Int by mutableIntStateOf(0)
  override var visibleItemCount: Int by mutableIntStateOf(0)

  companion object {
    val Saver: Saver<VerticalOverflowStateImpl, *> = Saver(
      save = { listOf(it.totalItemCount, it.visibleItemCount) },
      restore = {
        VerticalOverflowStateImpl().apply {
          totalItemCount = it[0]
          visibleItemCount = it[1]
        }
      },
    )
  }
}

@Composable
private fun rememberVerticalButtonGroupScopeState(
  content: VerticalButtonGroupScope.() -> Unit,
  animationSpec: AnimationSpec<Float>,
): State<VerticalButtonGroupScopeImpl> {
  val latestContent = rememberUpdatedState(content)
  return remember {
    derivedStateOf {
      VerticalButtonGroupScopeImpl(animationSpec = animationSpec).apply(latestContent.value)
    }
  }
}

private class VerticalButtonGroupScopeImpl(
  val animationSpec: AnimationSpec<Float>
) : VerticalButtonGroupScope {
  val items = mutableListOf<VerticalButtonGroupItem>()

  override fun clickableItem(
    onClick: () -> Unit,
    label: String,
    icon: (@Composable () -> Unit)?,
    weight: Float,
    enabled: Boolean,
  ) {
    items.add(
      ClickableVerticalButtonGroupItem(
        onClick,
        icon,
        weight,
        animationSpec,
        enabled,
        label
      )
    )
  }

  override fun toggleableItem(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)?,
    weight: Float,
    enabled: Boolean,
  ) {
    items.add(
      ToggleableVerticalButtonGroupItem(
        checked,
        onCheckedChange,
        weight,
        animationSpec,
        icon,
        enabled,
        label
      )
    )
  }

  override fun customItem(
    buttonGroupContent: @Composable () -> Unit,
    menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
  ) {
    items.add(CustomVerticalButtonGroupItem(buttonGroupContent, menuContent))
  }

  override fun Modifier.weight(weight: Float): Modifier = this.then(
    VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
  )

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