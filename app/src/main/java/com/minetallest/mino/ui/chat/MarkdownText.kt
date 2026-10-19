package com.minetallest.mino.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.minetallest.mino.ui.chat.highlightCode
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.minetallest.mino.domain.Markdown
import com.minetallest.mino.ui.theme.LocalMinoColors

import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle

/**
 * Markdown renderer for chat answers (spec §6.6): paragraphs, headings, fenced code
 * blocks with copy buttons, bullets, ordered lists, quotes, inline styles and links.
 * Streaming-safe — an unclosed fence renders live.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseColor: Color,
    linkColor: Color,
) {
    val colors = LocalMinoColors.current
    val uriHandler = LocalUriHandler.current
    val blocks = remember(markdown) { Markdown.parse(markdown) }

    fun handleUrlClick(annotated: AnnotatedString, offset: Int) {
        annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
            .firstOrNull()?.let { annotation ->
                try {
                    uriHandler.openUri(annotation.item)
                } catch (e: Throwable) {
                    // Ignore invalid URIs safely
                }
            }
    }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is Markdown.Block.Heading -> {
                    val annotated = inlineToAnnotated(block.inlines, baseColor, linkColor)
                    val fontSize = when (block.level) {
                        1 -> 21.sp
                        2 -> 19.sp
                        else -> 17.sp
                    }
                    ClickableText(
                        text = annotated,
                        style = TextStyle(color = baseColor, fontSize = fontSize, fontWeight = FontWeight.SemiBold),
                        onClick = { offset -> handleUrlClick(annotated, offset) },
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                }
                is Markdown.Block.Paragraph -> {
                    val annotated = inlineToAnnotated(block.inlines, baseColor, linkColor)
                    ClickableText(
                        text = annotated,
                        style = TextStyle(color = baseColor, fontSize = 17.sp, lineHeight = 24.sp),
                        onClick = { offset -> handleUrlClick(annotated, offset) },
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
                is Markdown.Block.Code -> CodeBlock(
                    lang = block.lang,
                    code = block.code,
                    closed = block.closed,
                )
                is Markdown.Block.Bullet -> Column(Modifier.padding(vertical = 3.dp)) {
                    block.items.forEach { item ->
                        val annotated = inlineToAnnotated(item, baseColor, linkColor)
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("•  ", color = baseColor, fontSize = 17.sp)
                            ClickableText(
                                text = annotated,
                                style = TextStyle(color = baseColor, fontSize = 17.sp, lineHeight = 24.sp),
                                onClick = { offset -> handleUrlClick(annotated, offset) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is Markdown.Block.Ordered -> Column(Modifier.padding(vertical = 3.dp)) {
                    block.items.forEachIndexed { index, item ->
                        val annotated = inlineToAnnotated(item, baseColor, linkColor)
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("${index + 1}.  ", color = baseColor, fontSize = 17.sp)
                            ClickableText(
                                text = annotated,
                                style = TextStyle(color = baseColor, fontSize = 17.sp, lineHeight = 24.sp),
                                onClick = { offset -> handleUrlClick(annotated, offset) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is Markdown.Block.Quote -> {
                    val annotated = inlineToAnnotated(block.inlines, colors.textMid, linkColor)
                    ClickableText(
                        text = annotated,
                        style = TextStyle(color = colors.textMid, fontSize = 16.sp, lineHeight = 23.sp),
                        onClick = { offset -> handleUrlClick(annotated, offset) },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.bgRaised)
                            .padding(10.dp),
                    )
                }
                Markdown.Block.Rule -> Text(
                    text = "────────────────",
                    color = colors.textLow,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(lang: String?, code: String, closed: Boolean) {
    val colors = LocalMinoColors.current
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgRaised)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lang ?: "code",
                color = colors.code,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Copy code",
                tint = colors.textMid,
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { clipboard.setText(AnnotatedString(code)) },
            )
        }
        Text(
            text = buildAnnotatedString {
                highlightCode(code, lang).forEach { (text, color) ->
                    withStyle(SpanStyle(color = color)) { append(text) }
                }
                if (!closed) append("▍")
            },
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun inlineToAnnotated(
    inlines: List<Markdown.Inline>,
    baseColor: Color,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    inlines.forEach { inline ->
        when (inline) {
            is Markdown.Inline.Text -> withStyle(
                SpanStyle(
                    color = baseColor,
                    fontWeight = if (Markdown.Style.BOLD in inline.styles) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (Markdown.Style.ITALIC in inline.styles) FontStyle.Italic else FontStyle.Normal,
                ),
            ) { append(inline.text) }
            is Markdown.Inline.Code -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = linkColor,
                ),
            ) { append(inline.code) }
            is Markdown.Inline.Link -> addStringAnnotation(
                tag = "URL",
                annotation = inline.url,
                start = length,
                end = length + inline.text.length,
            ).also {
                withStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ) { append(inline.text) }
            }
        }
    }
}
