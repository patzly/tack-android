package xyz.zedler.patrick.tack.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content = {
      Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
  )
}

@Composable
fun TackSettingsListItem(
  icon: ImageVector?,
  title: String,
  description: String? = null,
  trailing: @Composable (() -> Unit)? = null,
  onClick: (() -> Unit)? = null
) {
  val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
  
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
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
      )
    } else {
      Spacer(modifier = Modifier.size(24.dp))
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
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

@Composable
fun TackButtonGroup(
  options: List<String>,
  labels: List<String>,
  selected: String,
  enabled: Boolean = true,
  onSelect: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.Start
  ) {
    options.forEachIndexed { index, option ->
      val isSelected = option == selected
      val shape = when {
        options.size == 1 -> RoundedCornerShape(20.dp)
        index == 0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
        index == options.size - 1 -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp)
        else -> RoundedCornerShape(4.dp)
      }
      
      Button(
        onClick = { onSelect(option) },
        enabled = enabled,
        shape = shape,
        elevation = null,
        colors = if (isSelected) {
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        } else {
          ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
          .height(40.dp)
          .padding(end = if (index < options.size - 1) 2.dp else 0.dp)
      ) {
        Text(
          text = labels[index],
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1
        )
      }
    }
  }
}
