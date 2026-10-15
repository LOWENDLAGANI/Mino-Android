package com.minetallest.mino.domain

/**
 * Minimal markdown parser for chat answers (port of the react-markdown render scope):
 * headings, fenced code blocks (streaming-safe), paragraphs, bullets, ordered lists,
 * blockquotes, hr, and inline code/bold/italic/links.
 */
object Markdown {

    sealed class Block {
        data class Heading(val level: Int, val inlines: List<Inline>) : Block()
        data class Paragraph(val inlines: List<Inline>) : Block()
        data class Code(val lang: String?, val code: String, val closed: Boolean) : Block()
        data class Bullet(val items: List<List<Inline>>) : Block()
        data class Ordered(val items: List<List<Inline>>) : Block()
        data class Quote(val inlines: List<Inline>) : Block()
        data object Rule : Block()
    }

    enum class Style { BOLD, ITALIC }

    sealed class Inline {
        data class Text(val text: String, val styles: Set<Style> = emptySet()) : Inline()
        data class Code(val code: String) : Inline()
        data class Link(val text: String, val url: String) : Inline()
    }

    fun parse(markdown: String): List<Block> {
        val blocks = ArrayList<Block>()
        val lines = markdown.replace("\r\n", "\n").split('\n')
        var i = 0
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks.add(Block.Paragraph(parseInline(paragraph.toString().trim('\n'))))
                paragraph.setLength(0)
            }
        }

        while (i < lines.size) {
            val line = lines[i]
            val fence = FENCE.find(line)
            if (fence != null) {
                flushParagraph()
                val lang = fence.groupValues[1].ifBlank { null }
                val body = StringBuilder()
                i++
                var closed = false
                while (i < lines.size) {
                    if (lines[i].trimStart().startsWith("```")) { closed = true; i++; break }
                    if (body.isNotEmpty()) body.append('\n')
                    body.append(lines[i])
                    i++
                }
                blocks.add(Block.Code(lang, body.toString(), closed))
                continue
            }
            if (line.isBlank()) {
                flushParagraph()
                i++
                continue
            }
            val heading = HEADING.find(line)
            if (heading != null) {
                flushParagraph()
                blocks.add(Block.Heading(heading.groupValues[1].length, parseInline(heading.groupValues[2].trim())))
                i++
                continue
            }
            if (HR.containsMatchIn(line)) {
                flushParagraph()
                blocks.add(Block.Rule)
                i++
                continue
            }
            val bullet = BULLET.find(line)
            if (bullet != null) {
                flushParagraph()
                val items = ArrayList<List<Inline>>()
                while (i < lines.size && BULLET.containsMatchIn(lines[i])) {
                    items.add(parseInline(BULLET.find(lines[i])!!.groupValues[2].trim()))
                    i++
                }
                blocks.add(Block.Bullet(items))
                continue
            }
            val ordered = ORDERED.find(line)
            if (ordered != null) {
                flushParagraph()
                val items = ArrayList<List<Inline>>()
                while (i < lines.size && ORDERED.containsMatchIn(lines[i])) {
                    items.add(parseInline(ORDERED.find(lines[i])!!.groupValues[2].trim()))
                    i++
                }
                blocks.add(Block.Ordered(items))
                continue
            }
            val quote = QUOTE.find(line)
            if (quote != null) {
                flushParagraph()
                val body = StringBuilder(quote.groupValues[1])
                i++
                while (i < lines.size && QUOTE.containsMatchIn(lines[i])) {
                    body.append('\n').append(QUOTE.find(lines[i])!!.groupValues[1])
                    i++
                }
                blocks.add(Block.Quote(parseInline(body.toString())))
                continue
            }
            if (paragraph.isNotEmpty()) paragraph.append('\n')
            paragraph.append(line)
            i++
        }
        flushParagraph()
        return blocks
    }

    private val FENCE = Regex("""^\s*```([\w#+.-]*)\s*$""")
    private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
    private val HR = Regex("""^\s*(?:-{3,}|\*{3,}|_{3,})\s*$""")
    private val BULLET = Regex("""^\s*[-*+]\s+(.*)$""").let { re -> Regex("""^\s*([-*+])\s+(.*)$""") }
    private val ORDERED = Regex("""^\s*\d+[.)]\s+(.*)$""").let { Regex("""^\s*(\d+)[.)]\s+(.*)$""") }
    private val QUOTE = Regex("""^\s*>\s?(.*)$""")

    /** Inline: `code`, **bold**, *italic*, [text](url). */
    fun parseInline(text: String): List<Inline> {
        val out = ArrayList<Inline>()
        val pattern = Regex(
            """(`[^`\n]+`)|(\*\*\*[^*\n]+\*\*\*)|(\*\*[^*\n]+\*\*)|(\*[^*\n]+\*)|(_[^_\n]+_)|(\[[^\]\n]+\]\([^)\s]+\))""",
        )
        var last = 0
        for (match in pattern.findAll(text)) {
            if (match.range.first > last) {
                out.add(Inline.Text(text.substring(last, match.range.first)))
            }
            val token = match.value
            when {
                token.startsWith("`") -> out.add(Inline.Code(token.trim('`')))
                token.startsWith("***") -> out.add(Inline.Text(token.trim('*'), setOf(Style.BOLD, Style.ITALIC)))
                token.startsWith("**") -> out.add(Inline.Text(token.trim('*'), setOf(Style.BOLD)))
                token.startsWith("[") -> {
                    val close = token.indexOf(']')
                    val url = token.substringAfter('(').trim(')')
                    out.add(Inline.Link(token.substring(1, close), url))
                }
                else -> out.add(Inline.Text(token.trim('*'), setOf(Style.ITALIC)))
            }
            last = match.range.last + 1
        }
        if (last < text.length) out.add(Inline.Text(text.substring(last)))
        return out
    }

    /** Plain-text speech form: code blocks announced, link targets dropped (spec §10.8). */
    fun toSpeech(markdown: String): String {
        val sb = StringBuilder()
        for (block in parse(markdown)) {
            when (block) {
                is Block.Code ->
                    sb.append(if (block.lang != null) "code block, ${block.lang}, " else "code block, ")
                        .append(block.code.take(120)).append(". ")
                is Block.Heading -> sb.append(inlinesToText(block.inlines)).append(". ")
                is Block.Paragraph -> sb.append(inlinesToText(block.inlines)).append(' ')
                is Block.Quote -> sb.append(inlinesToText(block.inlines)).append(' ')
                is Block.Bullet -> block.items.forEach { sb.append(inlinesToText(it)).append(". ") }
                is Block.Ordered -> block.items.forEach { sb.append(inlinesToText(it)).append(". ") }
                is Block.Rule -> {}
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun inlinesToText(inlines: List<Inline>): String =
        inlines.joinToString("") { inline ->
            when (inline) {
                is Inline.Text -> inline.text
                is Inline.Code -> "code snippet"
                is Inline.Link -> inline.text
            }
        }
}
