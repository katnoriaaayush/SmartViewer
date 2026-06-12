package com.smartai.explorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ── Block model ────────────────────────────────────────────────────────────────

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String, val indent: Int) : MdBlock
    data class Ordered(val number: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object BlankLine : MdBlock
}

// ── Public composable ──────────────────────────────────────────────────────────

/**
 * Renders a Markdown string using native Compose primitives.
 * Handles the patterns Gemini typically emits:
 *   - Headings (# / ## / ###)
 *   - Unordered bullets (- / * / +)
 *   - Ordered lists (1. / 2. …)
 *   - Inline bold (**text**), italic (*text*), bold-italic (***text***)
 *   - Inline code (`text`)
 */
@Composable
fun MarkdownText(
    text:     String,
    modifier: Modifier   = Modifier,
    style:    TextStyle  = LocalTextStyle.current,
    color:    Color      = Color.Unspecified,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        parseBlocks(text).forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text  = inline(block.text),
                    style = when (block.level) {
                        1    -> MaterialTheme.typography.titleLarge
                        2    -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    color    = color,
                    modifier = Modifier.padding(top = if (block.level == 1) 6.dp else 3.dp),
                )
                is MdBlock.Bullet -> Row(
                    modifier = Modifier.padding(start = (block.indent * 12).dp),
                ) {
                    Text("•  ", style = style, color = color)
                    Text(inline(block.text), style = style, color = color, modifier = Modifier.weight(1f))
                }
                is MdBlock.Ordered -> Row {
                    Text("${block.number}.  ", style = style, color = color)
                    Text(inline(block.text), style = style, color = color, modifier = Modifier.weight(1f))
                }
                is MdBlock.Paragraph -> Text(
                    text  = inline(block.text),
                    style = style,
                    color = color,
                )
                MdBlock.BlankLine -> Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Block parser ──────────────────────────────────────────────────────────────

private val BULLET_RE   = Regex("""^(\s*)[-*+] (.+)""")
private val ORDERED_RE  = Regex("""^(\s*)(\d+)\. (.+)""")
private val HEADING_RE  = Regex("""^(#{1,6}) (.+)""")
private val HLINE_RE    = Regex("""^[-*_]{3,}\s*$""")

private fun parseBlocks(raw: String): List<MdBlock> {
    val blocks  = mutableListOf<MdBlock>()
    val pending = mutableListOf<String>()   // accumulate paragraph lines

    fun flush() {
        if (pending.isEmpty()) return
        val joined = pending.joinToString("\n").trim()
        if (joined.isNotEmpty()) blocks.add(MdBlock.Paragraph(joined))
        pending.clear()
    }

    for (line in raw.lines()) {
        val trimmed = line.trimEnd()

        when {
            trimmed.isBlank() -> {
                flush()
                if (blocks.lastOrNull() !is MdBlock.BlankLine) blocks.add(MdBlock.BlankLine)
            }
            HLINE_RE.matches(trimmed) -> {
                flush()   // horizontal rule — just use as visual separator (blank)
            }
            HEADING_RE.matches(trimmed) -> {
                flush()
                val m = HEADING_RE.find(trimmed)!!
                blocks.add(MdBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim()))
            }
            BULLET_RE.matches(trimmed) -> {
                flush()
                val m = BULLET_RE.find(trimmed)!!
                val indent = m.groupValues[1].length / 2
                blocks.add(MdBlock.Bullet(m.groupValues[2].trim(), indent))
            }
            ORDERED_RE.matches(trimmed) -> {
                flush()
                val m = ORDERED_RE.find(trimmed)!!
                val num = m.groupValues[2].toIntOrNull() ?: 1
                blocks.add(MdBlock.Ordered(num, m.groupValues[3].trim()))
            }
            trimmed.startsWith("```") -> flush()  // skip fences; content parsed as paragraph
            else -> pending.add(trimmed)
        }
    }
    flush()
    return blocks
}

// ── Inline parser ─────────────────────────────────────────────────────────────

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var pos = 0
    while (pos < text.length) {
        when {
            // Bold-italic  ***text***
            text.startsWith("***", pos) -> {
                val end = text.indexOf("***", pos + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(pos + 3, end))
                    }
                    pos = end + 3; continue
                }
            }
            // Bold  **text**  or  __text__
            (text.startsWith("**", pos) || text.startsWith("__", pos)) -> {
                val marker = text.substring(pos, pos + 2)
                val end = text.indexOf(marker, pos + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(pos + 2, end))
                    }
                    pos = end + 2; continue
                }
            }
            // Italic  *text*  or  _text_  (not preceded/followed by same char)
            (text[pos] == '*' || text[pos] == '_') -> {
                val ch  = text[pos]
                val end = text.indexOf(ch, pos + 1)
                // Make sure we're not matching the start of ** or __
                if (end != -1 &&
                    (pos + 1 >= text.length || text[pos + 1] != ch) &&
                    (end + 1 >= text.length || text[end + 1] != ch)
                ) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(pos + 1, end))
                    }
                    pos = end + 1; continue
                }
            }
            // Inline code  `text`
            text[pos] == '`' -> {
                val end = text.indexOf('`', pos + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(text.substring(pos + 1, end))
                    }
                    pos = end + 1; continue
                }
            }
        }
        append(text[pos])
        pos++
    }
}
