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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

private val BOLD_REGEX = """<b>(.*?)</b>|\*\*(.*?)\*\*"""
  .toRegex(RegexOption.DOT_MATCHES_ALL)

private val DOMAIN_URL_REGEX = """^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(/.*)?$""".toRegex()

@Composable
fun FormattedText(
  text: String,
  modifier: Modifier = Modifier,
  isDialog: Boolean = false,
  textColor: Color = MaterialTheme.colorScheme.onSurface
) {
  if (text.isBlank()) return

  val uriHandler = LocalUriHandler.current
  val textColorVariant = MaterialTheme.colorScheme.onSurfaceVariant
  val cardShape = MaterialTheme.shapes.medium

  val blocks = remember(text, isDialog) {
    val normalizedText = text.replace("\r\n", "\n")
    val parts = normalizedText.split("\n\n")

    parts.mapIndexed { i, p ->
      val partNext = parts.getOrNull(i + 1).orEmpty()
      val isNextLink = partNext.startsWith("=> ") || partNext.startsWith("> ")
      val isNextSubtitle = partNext.startsWith("__")

      when {
        p.startsWith("__") -> {
          TextBlock.Paragraph(
            annotatedText = parseAnnotatedText(p.substring(2).trimStart()),
            isMedium = true,
            keepDistance = true
          )
        }

        p.startsWith("#") -> {
          val firstSpace = p.indexOf(' ')
          val prefixEnd = if (firstSpace != -1) firstSpace else p.length
          val prefix = p.substring(0, prefixEnd)
          val useTNum = prefix.endsWith("_")
          val h0 = if (useTNum) prefix.dropLast(1) else prefix
          val headlineRaw = if (firstSpace != -1) p.substring(firstSpace + 1) else ""

          TextBlock.Headline(
            annotatedText = parseAnnotatedText(headlineRaw),
            level = h0.length,
            useTNum = useTNum,
            keepDistance = !isNextLink
          )
        }

        p.startsWith("- ") -> {
          val bulletStrings = mutableListOf<String>()
          p.lines().forEach { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("- ")) {
              bulletStrings.add(trimmed.removePrefix("- ").trim())
            } else if (trimmed.isNotEmpty() && bulletStrings.isNotEmpty()) {
              val lastIndex = bulletStrings.lastIndex
              bulletStrings[lastIndex] = "${bulletStrings[lastIndex]} $trimmed"
            }
          }
          val bullets = bulletStrings.map {
            TextBlock.BulletItem(annotatedText = parseAnnotatedText(it))
          }
          TextBlock.BulletList(bullets)
        }

        p.startsWith("> ") || p.startsWith("=> ") -> {
          val raw = p.removePrefix("=> ").removePrefix("> ").trim()
          val lastSpace = raw.lastIndexOf(' ')
          val (label, rawUrl) = if (lastSpace != -1) {
            val potentialUrl = raw.substring(lastSpace + 1).trim()
            val potentialLabel = raw.substring(0, lastSpace).trim()

            val isPotentialUrl = potentialUrl.contains("://") ||
                potentialUrl.startsWith("mailto:") ||
                potentialUrl.startsWith("tel:") ||
                potentialUrl.startsWith("www.") ||
                DOMAIN_URL_REGEX.matches(potentialUrl)

            if (isPotentialUrl) {
              potentialLabel to potentialUrl
            } else {
              raw to raw
            }
          } else {
            raw to raw
          }

          val normalizedUrl = if (
            !rawUrl.contains("://") &&
            !rawUrl.startsWith("mailto:") &&
            !rawUrl.startsWith("tel:")
          ) {
            "https://$rawUrl"
          } else {
            rawUrl
          }

          TextBlock.Link(text = label, url = normalizedUrl)
        }

        p.startsWith("? ") -> {
          TextBlock.MessageCard(
            annotatedText = parseAnnotatedText(p.substring(2)),
            isError = false
          )
        }

        p.startsWith("! ") -> {
          TextBlock.MessageCard(
            annotatedText = parseAnnotatedText(p.substring(2)),
            isError = true
          )
        }

        p.startsWith("---") -> {
          TextBlock.Divider
        }

        else -> {
          val keepDistance = if (isDialog) {
            true
          } else {
            !isNextLink && !isNextSubtitle
          }
          TextBlock.Paragraph(
            annotatedText = parseAnnotatedText(p),
            isMedium = isDialog,
            keepDistance = keepDistance
          )
        }
      }
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    blocks.fastForEach { block ->
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
          } else {
            baseStyle
          }

          Text(
            text = block.annotatedText,
            style = style,
            color = textColor,
            modifier = Modifier
              .fillMaxWidth()
              .semantics { heading() }
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
            text = block.annotatedText,
            style = style,
            color = color,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = if (block.keepDistance) 16.dp else 0.dp)
          )
        }

        is TextBlock.BulletList -> {
          val bulletStyle = if (isDialog) {
            MaterialTheme.typography.bodyMedium
          } else {
            MaterialTheme.typography.bodyLarge
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            block.bullets.fastForEachIndexed { index, bullet ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .semantics(mergeDescendants = true) {}
                  .padding(bottom = if (index == block.bullets.lastIndex) 0.dp else 8.dp)
              ) {
                Text(
                  text = "•",
                  style = bulletStyle,
                  color = textColor,
                  modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                )
                Text(
                  text = bullet.annotatedText,
                  style = bulletStyle,
                  color = textColor,
                  modifier = Modifier.weight(1f)
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
              .padding(bottom = 8.dp)
              .minimumInteractiveComponentSize()
              .clip(MaterialTheme.shapes.small)
              .clickable(role = Role.Button) {
                try {
                  uriHandler.openUri(block.url)
                } catch (_: Exception) {
                }
              }
              .padding(vertical = 8.dp)
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
            colors = CardDefaults.cardColors(
              containerColor = containerColor,
              contentColor = contentColor
            ),
            shape = cardShape,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            Text(
              text = block.annotatedText,
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
    val annotatedText: AnnotatedString,
    val level: Int,
    val useTNum: Boolean,
    val keepDistance: Boolean
  ) : TextBlock

  data class Paragraph(
    val annotatedText: AnnotatedString,
    val isMedium: Boolean,
    val keepDistance: Boolean
  ) : TextBlock

  data class BulletItem(
    val annotatedText: AnnotatedString
  )

  data class BulletList(
    val bullets: List<BulletItem>
  ) : TextBlock

  data class Link(
    val text: String,
    val url: String
  ) : TextBlock

  data class MessageCard(
    val annotatedText: AnnotatedString,
    val isError: Boolean
  ) : TextBlock

  data object Divider : TextBlock
}

private fun parseAnnotatedText(text: String): AnnotatedString {
  if (text.isEmpty()) return AnnotatedString("")
  if (!BOLD_REGEX.containsMatchIn(text)) return AnnotatedString(text)

  return buildAnnotatedString {
    var currentIndex = 0
    for (match in BOLD_REGEX.findAll(text)) {
      append(text.substring(currentIndex, match.range.first))

      val boldContent = match.groups[1]?.value
        ?: match.groups[2]?.value
        ?: ""

      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(boldContent)
      }
      currentIndex = match.range.last + 1
    }
    if (currentIndex < text.length) {
      append(text.substring(currentIndex))
    }
  }
}
