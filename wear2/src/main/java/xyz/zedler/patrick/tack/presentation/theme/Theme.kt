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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

@Composable
fun TackTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        content = content,
        colors = TackColors
    )
}

private val TackColors = Colors(
    primary = Color(0xFFDAC66F),
    primaryVariant = Color(0xFF8AB4F8),
    secondary = Color(0xFFA9D0B3),
    secondaryVariant = Color(0xFF594F33),
    surface = Color(0xFF303133),
    error = Color(0xFFEE675C),
    onPrimary = Color(0xFF393000),
    onSecondary = Color(0xFF143723),
    onSurfaceVariant = Color(0xFFDADCE0),
    onError = Color(0xFF000000)
)