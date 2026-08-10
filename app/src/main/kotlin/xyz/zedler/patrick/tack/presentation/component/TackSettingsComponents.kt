package xyz.zedler.patrick.tack.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val SegmentedCornerSize = 20.dp

@Composable
fun TackCategoryHeader(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.secondary,
    fontWeight = FontWeight.Bold,
    modifier = Modifier
      .padding(start = 56.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
  )
}

@Composable
fun TackSettingsGroup(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(SegmentedCornerSize),
    color = MaterialTheme.colorScheme.surfaceBright,
    content = {
      Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
  )
}

@Composable
fun TackSettingsListItem(
  icon: Int?,
  title: String,
  description: String? = null,
  overline: String? = null,
  trailing: @Composable (() -> Unit)? = null,
  onClick: (() -> Unit)? = null
) {
  val interactionSource = remember { MutableInteractionSource() }
  val clickableModifier = if (onClick != null) {
    Modifier.clickable(
      interactionSource = interactionSource,
      indication = ripple(),
      onClick = onClick
    )
  } else Modifier
  
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(clickableModifier)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .defaultMinSize(minHeight = 72.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      Icon(
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
      )
    } else {
      Spacer(modifier = Modifier.size(24.dp))
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      if (overline != null) {
        Text(
          text = overline,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      if (description != null) {
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    if (trailing != null) {
      Box(modifier = Modifier.padding(start = 16.dp)) {
        trailing()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TackButtonGroup(
  options: List<String>,
  labels: List<String>,
  selected: String,
  enabled: Boolean = true,
  onSelect: (String) -> Unit
) {
  ButtonGroup(
    overflowIndicator = { menuState ->
      ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
    },
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    modifier = Modifier
      .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
      .fillMaxWidth()
  ) {
    options.forEachIndexed { index, option ->
      val isSelected = option == selected
      
      customItem(
        buttonGroupContent = {
          ToggleButton(
            checked = isSelected,
            onCheckedChange = { if (enabled) onSelect(option) },
            enabled = enabled,
            shapes = when (index) {
              0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
              options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
              else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            },
            modifier = Modifier.semantics { role = Role.RadioButton },
          ) {
            Text(
              text = labels[index],
              maxLines = 1
            )
          }
        },
        menuContent = {}
      )
    }
  }
}
