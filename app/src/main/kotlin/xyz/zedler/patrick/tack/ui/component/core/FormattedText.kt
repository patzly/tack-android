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

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@Composable
fun FormattedText(
  text: String,
  modifier: Modifier = Modifier,
  highlights: List<String> = emptyList(),
  isDialog: Boolean = false,
  textColor: Color = MaterialTheme.colorScheme.onSurface
) {
  val context = LocalContext.current
  val textColorVariant = MaterialTheme.colorScheme.onSurfaceVariant

  val blocks = remember(text, highlights, isDialog) {
    val parts = text.split("\n\n")
    parts.mapIndexed { i, p ->
      val partNext = parts.getOrNull(i + 1) ?: ""

      when {
        p.startsWith("__") -> {
          TextBlock.Paragraph(
            text = p.substring(2).trimStart(),
            isMedium = true,
            keepDistance = true
          )
        }

        p.startsWith("#") -> {
          val keepDistance = !partNext.startsWith("=> ")
          val firstSpace = p.indexOf(' ')
          val prefixEnd = if (firstSpace != -1) firstSpace else p.length
          val prefix = p.substring(0, prefixEnd)
          val useTNum = prefix.endsWith("_")
          val h0 = if (useTNum) prefix.dropLast(1) else prefix
          val headlineText = if (firstSpace != -1) p.substring(firstSpace + 1) else ""

          TextBlock.Headline(
            text = headlineText,
            level = h0.length,
            useTNum = useTNum,
            keepDistance = keepDistance
          )
        }

        p.startsWith("- ") -> {
          val bulletStrings = p.trim().split("- ").drop(1)
          val bullets = bulletStrings.mapIndexed { index, bulletStr ->
            TextBlock.BulletItem(
              text = bulletStr.trim(),
              isLast = index == bulletStrings.size - 1
            )
          }
          TextBlock.BulletList(bullets)
        }

        p.startsWith("> ") || p.startsWith("=> ") -> {
          val isArrow = p.startsWith("=> ")
          val linkTokens =
            p.substring(if (isArrow) 3 else 2).trim().split(" ", limit = 2)
          TextBlock.Link(
            text = linkTokens[0],
            url = if (linkTokens.size > 1) linkTokens[1] else linkTokens[0]
          )
        }

        p.startsWith("? ") -> {
          TextBlock.MessageCard(text = p.substring(2), isError = false)
        }

        p.startsWith("! ") -> {
          TextBlock.MessageCard(text = p.substring(2), isError = true)
        }

        p.startsWith("---") -> {
          TextBlock.Divider
        }

        else -> {
          if (isDialog) {
            TextBlock.Paragraph(text = p, isMedium = true, keepDistance = true)
          } else {
            val keepDistance =
              !partNext.startsWith("=> ") && !partNext.startsWith("__ ")
            TextBlock.Paragraph(text = p, isMedium = false, keepDistance = keepDistance)
          }
        }
      }
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    blocks.forEach { block ->
      when (block) {
        is TextBlock.Headline -> {
          val baseStyle = when (block.level) {
            1 -> MaterialTheme.typography.headlineLarge
            2 -> MaterialTheme.typography.headlineMedium
            3 -> MaterialTheme.typography.headlineSmall
            4 -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.titleMedium
          }
          val style = if (block.useTNum) {
            baseStyle.copy(fontFeatureSettings = "tnum")
          } else baseStyle

          Text(
            text = parseBoldText(block.text, highlights),
            style = style,
            color = textColor,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = if (block.keepDistance) 16.dp else 0.dp)
          )
        }

        is TextBlock.Paragraph -> {
          val style = if (block.isMedium) {
            MaterialTheme.typography.bodyMedium
          } else {
            MaterialTheme.typography.bodyLarge
          }
          val color = if (block.isMedium) textColorVariant else textColor

          Text(
            text = parseBoldText(block.text, highlights),
            style = style,
            color = color,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = if (block.keepDistance) 16.dp else 0.dp)
          )
        }

        is TextBlock.BulletList -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            block.bullets.forEach { bullet ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = if (bullet.isLast) 0.dp else 8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .padding(start = 6.dp, end = 6.dp, top = 8.dp)
                    .size(4.dp)
                    .background(color = textColor, shape = CircleShape)
                )
                Text(
                  text = parseBoldText(bullet.text, highlights),
                  style = if (isDialog) {
                    MaterialTheme.typography.bodyMedium
                  } else {
                    MaterialTheme.typography.bodyLarge
                  },
                  color = textColor
                )
              }
            }
          }
        }

        is TextBlock.Link -> {
          Text(
            text = block.text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
              .clip(MaterialTheme.shapes.small)
              .clickable {
                try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, block.url.toUri()))
                } catch (_: Exception) {
                }
              }
              .padding(vertical = 4.dp)
          )
        }

        is TextBlock.MessageCard -> {
          val containerColor = if (block.isError) {
            MaterialTheme.colorScheme.errorContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
          }
          val contentColor = if (block.isError) {
            MaterialTheme.colorScheme.onErrorContainer
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }

          Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            Text(
              text = parseBoldText(block.text, highlights),
              style = MaterialTheme.typography.bodyLarge,
              color = contentColor,
              modifier = Modifier.padding(16.dp)
            )
          }
        }

        is TextBlock.Divider -> {
          HorizontalDivider(
            modifier = Modifier
              .padding(top = 8.dp, bottom = 24.dp)
              .width(56.dp)
              .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.outlineVariant
          )
        }
      }
    }
  }
}

private sealed interface TextBlock {
  data class Headline(
    val text: String, val level: Int, val useTNum: Boolean, val keepDistance: Boolean
  ) : TextBlock
  data class Paragraph(
    val text: String, val isMedium: Boolean, val keepDistance: Boolean
  ) : TextBlock
  data class BulletItem(val text: String, val isLast: Boolean)
  data class BulletList(val bullets: List<BulletItem>) : TextBlock
  data class Link(val text: String, val url: String) : TextBlock
  data class MessageCard(val text: String, val isError: Boolean) : TextBlock
  data object Divider : TextBlock
}

private fun parseBoldText(text: String, highlights: List<String>): AnnotatedString {
  var currentText = text
  highlights.forEach { h ->
    currentText = currentText.replace(h, "<b>$h</b>")
  }

  return buildAnnotatedString {
    var currentIndex = 0
    val regex = "<b>(.*?)</b>".toRegex(RegexOption.DOT_MATCHES_ALL)
    val matches = regex.findAll(currentText)

    for (match in matches) {
      append(currentText.substring(currentIndex, match.range.first))
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(match.groupValues[1])
      }
      currentIndex = match.range.last + 1
    }
    append(currentText.substring(currentIndex))
  }
}