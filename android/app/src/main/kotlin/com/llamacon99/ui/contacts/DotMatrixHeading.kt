package com.llamacon99.ui.contacts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp

private val glyphs = mapOf(
    'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    'C' to listOf("01111", "10000", "10000", "10000", "10000", "10000", "01111"),
    'D' to listOf("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
    'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
    'F' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
    'G' to listOf("01111", "10000", "10000", "10111", "10001", "10001", "01110"),
    'H' to listOf("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    'I' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
    'J' to listOf("00111", "00010", "00010", "00010", "10010", "10010", "01100"),
    'K' to listOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
    'L' to listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
    'M' to listOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    'N' to listOf("10001", "11001", "11001", "10101", "10011", "10011", "10001"),
    'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    'P' to listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    'Q' to listOf("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
    'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    'S' to listOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
    'T' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
    'U' to listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
    'V' to listOf("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
    'W' to listOf("10001", "10001", "10001", "10101", "10101", "11011", "10001"),
    'X' to listOf("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
    'Y' to listOf("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
    'Z' to listOf("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
    '#' to listOf("01010", "01010", "11111", "01010", "11111", "01010", "01010"),
)

/** Uses the same dot glyphs as the title, sized to the side index and its font scale. */
@Composable
internal fun DotMatrixIndexLetter(letter: String, color: Color, active: Boolean) {
    val glyph = glyphs[letter.firstOrNull()]
    if (glyph == null) {
        Text(letter, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        return
    }
    val density = LocalDensity.current
    val width = with(density) { 10.sp.toDp() }
    val height = with(density) { 14.sp.toDp() }
    Canvas(Modifier.size(width, height)) {
        drawDotGlyph(glyph, color, if (active) 0.46f else 0.34f)
    }
}

private fun DrawScope.drawDotGlyph(glyph: List<String>, color: Color, radiusRatio: Float) {
    val cell = minOf(size.width / 5, size.height / 7)
    val startX = (size.width - cell * 5) / 2
    val startY = (size.height - cell * 7) / 2
    glyph.forEachIndexed { row, pixels ->
        pixels.forEachIndexed { column, value ->
            if (value == '1') drawCircle(color, cell * radiusRatio,
                Offset(startX + (column + 0.5f) * cell, startY + (row + 0.5f) * cell))
        }
    }
}

/** Vector dot lettering for the localized Contacts heading, wrapping at large font scales. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DotMatrixHeading(label: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface
    val glyphWidth = with(density) { 22.sp.toDp() }
    val glyphHeight = with(density) { 32.sp.toDp() }
    FlowRow(modifier.clearAndSetSemantics { text = AnnotatedString(label); heading() }) {
        label.uppercase().forEach { character ->
            val dots = glyphs[character] ?: return@forEach
            Canvas(Modifier.size(glyphWidth, glyphHeight)) {
                val cell = size.width / 6
                dots.forEachIndexed { row, pixels ->
                    pixels.forEachIndexed { column, value ->
                        if (value == '1') drawCircle(color, cell * 0.34f,
                            Offset((column + 0.5f) * cell, (row + 0.7f) * cell))
                    }
                }
            }
        }
    }
}
