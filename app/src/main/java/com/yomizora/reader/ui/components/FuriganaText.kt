package com.yomizora.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yomizora.reader.data.content.GrammarCatalog
import com.yomizora.reader.domain.model.GrammarPoint
import com.yomizora.reader.domain.model.Paragraph
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.TextSegment

/**
 * Renders a Japanese paragraph with true Ruby-style furigana centered directly above
 * corresponding kanji segments, progressive furigana hiding for familiar words,
 * and subtle grammar underlines for sentences containing JLPT grammar points.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuriganaParagraph(
    paragraphIndex: Int = 0,
    paragraph: Paragraph,
    settings: ReaderSettings,
    familiarWords: Set<Pair<String, String>> = emptySet(),
    underlinedGrammarPositions: Map<Triple<Int, Int, Int>, GrammarPoint> = emptyMap(),
    selectedSegment: TextSegment?,
    onSegmentClick: (TextSegment) -> Unit,
    onGrammarClick: (GrammarPoint) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val showRubyGlobal = settings.showFurigana && settings.displayMode == TextDisplayMode.KANJI_WITH_FURIGANA
    val baseFontSize = settings.fontSizeSp.sp
    val rubyFontSize = (settings.fontSizeSp * 0.48f).coerceAtLeast(9.5f).sp
    val verticalSpacing = ((settings.lineHeightMultiplier - 1.2f) * 14f).coerceAtLeast(4f).dp

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        paragraph.sentences.forEachIndexed { sIdx, sentence ->
            sentence.segments.forEachIndexed { segIdx, segment ->
                val displayText = when (settings.displayMode) {
                    TextDisplayMode.KANA_ONLY -> segment.reading ?: segment.text
                    else -> segment.text
                }
                val isFamiliar = settings.progressiveFurigana &&
                    !segment.reading.isNullOrBlank() &&
                    familiarWords.contains(segment.text to segment.reading)

                val rubyText = if (showRubyGlobal && !segment.reading.isNullOrBlank() && !isFamiliar) {
                    segment.reading
                } else {
                    null
                }

                val isWordInteractive = !segment.dictId.isNullOrBlank() || !segment.reading.isNullOrBlank()
                val matchedGrammar = underlinedGrammarPositions[Triple(paragraphIndex, sIdx, segIdx)]
                val isGrammarSegment = matchedGrammar != null

                val isSelected = selectedSegment == segment
                val bgColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bgColor)
                        .then(
                            when {
                                isWordInteractive -> Modifier.clickable { onSegmentClick(segment) }
                                isGrammarSegment -> Modifier.clickable { onGrammarClick(matchedGrammar!!) }
                                else -> Modifier
                            }
                        )
                        .padding(horizontal = if (rubyText != null) 1.dp else 0.dp)
                ) {
                    if (showRubyGlobal) {
                        Text(
                            text = rubyText ?: " ",
                            fontSize = rubyFontSize,
                            lineHeight = rubyFontSize * 1.1f,
                            color = if (rubyText != null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color.Transparent
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = displayText,
                        fontSize = baseFontSize,
                        lineHeight = baseFontSize * 1.15f,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isGrammarSegment -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onBackground
                        },
                        textAlign = TextAlign.Center
                    )
                    if (isGrammarSegment) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 18.dp)
                                .height(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Ruby-style furigana row for quiz questions and options, respecting
 * progressive furigana hiding for familiar words.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuriganaInlineRow(
    segments: List<TextSegment>,
    settings: ReaderSettings,
    familiarWords: Set<Pair<String, String>>,
    fontSizeSp: Float,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    val showRubyGlobal = settings.showFurigana && settings.displayMode == TextDisplayMode.KANJI_WITH_FURIGANA
    val baseFontSize = fontSizeSp.sp
    val rubyFontSize = (fontSizeSp * 0.50f).coerceAtLeast(9.5f).sp

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (segment in segments) {
            val displayText = when (settings.displayMode) {
                TextDisplayMode.KANA_ONLY -> segment.reading ?: segment.text
                else -> segment.text
            }
            val isFamiliar = settings.progressiveFurigana &&
                !segment.reading.isNullOrBlank() &&
                familiarWords.contains(segment.text to segment.reading)
            val rubyText = if (showRubyGlobal && !segment.reading.isNullOrBlank() && !isFamiliar) {
                segment.reading
            } else {
                null
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(horizontal = if (rubyText != null) 1.dp else 0.dp)
            ) {
                if (showRubyGlobal) {
                    Text(
                        text = rubyText ?: " ",
                        fontSize = rubyFontSize,
                        lineHeight = rubyFontSize * 1.1f,
                        color = if (rubyText != null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.Transparent
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Text(
                    text = displayText,
                    fontSize = baseFontSize,
                    lineHeight = baseFontSize * 1.15f,
                    fontWeight = fontWeight,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
