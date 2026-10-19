package com.minetallest.mino.ui.chat

import androidx.compose.ui.graphics.Color
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.ui.theme.MinoColors
import java.util.regex.Pattern

/** Simple regex-based syntax highlighting for common languages. */
private data class HighlightRule(val pattern: Pattern, val color: Color)

private fun jsRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("//.*$|\"[^\"]*\"", Pattern.MULTILINE), colors.textLow),
    HighlightRule(Pattern.compile("\\b(var|let|const|function|return|if|else|for|while|do|switch|case|break|continue|new|this|typeof|instanceof|try|catch|finally|throw|class|import|export|from|async|await|true|false|null|undefined|void|delete|yield|of|in)\\b"), colors.code),
    HighlightRule(Pattern.compile("\\b(\\d+\\.?\\d*|(0[xX][a-fA-F0-9]+|0[bB][01]+|0[oO][0-7]+))\\b"), colors.azure),
)

private fun tsRules(colors: MinoColors): List<HighlightRule> = jsRules(colors) + listOf(
    HighlightRule(Pattern.compile("\\b(string|number|boolean|void|never|any|unknown|null|undefined|symbol|bigint|enum|type|interface|extends|implements|keyof|readonly|as|is|abstract|private|protected|public|static|Partial|Required|Record|Pick|Omit|Exclude|Extract|NonNullable|ReturnType|InstanceType)\\b"), colors.code),
)

private fun pythonRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("#.*$", Pattern.MULTILINE), colors.textLow),
    HighlightRule(Pattern.compile("\\b(def|class|return|if|elif|else|for|while|import|from|as|try|except|finally|raise|with|yield|lambda|pass|break|continue|and|or|not|in|is|True|False|None|self|async|await|global|nonlocal|del|print|range|len|str|int|float|list|dict|set|tuple|type|super|property|staticmethod|classmethod|open|format|enumerate|zip|map|filter|sorted|reversed|any|all|sum|min|max|abs|round|isinstance|issubclass|hasattr|getattr|setattr|delattr|vars|dir|exec|eval|compile|repr|chr|ord|hex|oct|bin|complex|bytes|bytearray|memoryview|frozenset|slice|object|type|range|list|dict|set|tuple|type|super|property|staticmethod|classmethod|open|format|enumerate|zip|map|filter|sorted|reversed|any|all|sum|min|max|abs|round)\\b"), colors.code),
    HighlightRule(Pattern.compile("\\b(\\d+\\.?\\d*)\\b"), colors.azure),
)

private fun jsonRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("\\b(true|false|null|\\d+\\.?\\d*)\\b"), colors.code),
    HighlightRule(Pattern.compile("\"([^\"\\\\]*)\""), colors.text),
)

private fun kotlinRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("//.*$", Pattern.MULTILINE), colors.textLow),
    HighlightRule(Pattern.compile("\\b(val|var|fun|class|object|interface|enum|data|sealed|override|private|public|internal|protected|import|package|return|if|else|when|for|while|do|try|catch|finally|throw|nullable|init|suspend|companion|constructor|typealias|operator|inline|infix|external|crossinline|noinline|tailrec|reified)\\b"), colors.code),
    HighlightRule(Pattern.compile("\\b(\\d+\\.?\\d*)\\b"), colors.azure),
)

private fun javaRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("//.*$", Pattern.MULTILINE), colors.textLow),
    HighlightRule(Pattern.compile("\\b(public|private|protected|static|final|abstract|synchronized|native|strictfp|transient|volatile|class|interface|enum|extends|implements|new|this|super|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|import|package|instanceof|true|false|null|void)\\b"), colors.code),
    HighlightRule(Pattern.compile("\\b(\\d+\\.?\\d*)\\b"), colors.azure),
)

private fun htmlRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("</?[a-zA-Z][a-zA-Z0-9]*\\b"), colors.code),
    HighlightRule(Pattern.compile("\\b(id|class|style|href|src|alt|title|type|name|value|placeholder|disabled|checked|selected|readonly|required|multiple|size|maxlength|minlength|pattern|autocomplete|autofocus|form|action|method|enctype|target|accept|allow|allowfullscreen|playsinline|loop|muted|controls|preload|poster|width|height|cols|rows|wrap|headers|scope|rowspan|colspan|axis|nowrap|valign|char|charoff|span|summary|abbr|headers|scope|axis|headers|rowspan|colspan|scope|headers|abbr|axis|headers|headers)\\b", Pattern.CASE_INSENSITIVE), colors.code),
)

private fun cssRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("#([0-9a-fA-F]{3,8})\\b"), colors.code),
    HighlightRule(Pattern.compile("rgba?\\([^)]+\\)"), colors.code),
    HighlightRule(Pattern.compile("\\b(div|span|h1|h2|h3|h4|h5|h6|p|a|img|ul|ol|li|table|tr|td|th|thead|tbody|tfoot|form|input|button|select|option|textarea|label|fieldset|legend|nav|header|footer|main|section|article|aside|figure|figcaption|details|summary|dialog|progress|meter|canvas|svg|math|details|summary)\\b", Pattern.CASE_INSENSITIVE), colors.code),
)

private fun sqlRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|ALTER|DROP|INDEX|VIEW|JOIN|LEFT|RIGHT|INNER|OUTER|ON|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|ORDER|BY|GROUP|HAVING|LIMIT|OFFSET|AS|DISTINCT|COUNT|SUM|AVG|MIN|MAX|UNION|ALL|EXISTS|CASE|WHEN|THEN|ELSE|END|CAST|COALESCE|NULLIF|CURRENT|CURRENT_TIMESTAMP|CURRENT_DATE|CURRENT_TIME|EXTRACT|DATE|TIME|TIMESTAMP|INTERVAL|YEAR|MONTH|DAY|HOUR|MINUTE|SECOND|OVER|PARTITION|RANK|DENSE_RANK|ROW_NUMBER|LEAD|LAG|FIRST_VALUE|LAST_VALUE|NTH_VALUE|ROW|ROWS|UNBOUNDED|PRECEDING|FOLLOWING|RANGE|BOOLEAN|SMALLINT|INTEGER|BIGINT|DECIMAL|NUMERIC|FLOAT|REAL|DOUBLE|PRECISION|CHARACTER|VARYING|CHAR|STRING|TEXT|BLOB|BINARY|VARBINARY|DATE|TIME|TIMESTAMP|WITH|RECURSIVE|MATERIALIZED|TEMPORARY|TEMP|UNLOGGED|ON|CONFLICT|DO|NOTHING|UNIQUE|PRIMARY|KEY|FOREIGN|REFERENCES|CONSTRAINT|CHECK|EXCLUDE|DEFAULT|GENERATED|ALWAYS|COLUMN|STORAGE|ENABLE|DISABLE|VALID|NOVALID|ANALYZE|VERBOSE|SECURITY|DEFINER|INVOKER|COST|ROWS|PARALLEL|UNSAFE|RETURNS|RETURNS_TABLE|SCHEMATA|CATALOGS|PARAMETERS|FUNCTION|ROUTINE|PROCEDURE|TRIGGER|EVENT|LOOP|ITERATE|CASE|CONTINUE|HANDLER|DECLARE|OPEN|CLOSE|FETCH|DEALLOCATE|PREPARE|EXECUTE|DEALLOCATE|PREPARE|EXECUTE)\\b", Pattern.CASE_INSENSITIVE), colors.code),
)

private fun bashRules(colors: MinoColors): List<HighlightRule> = listOf(
    HighlightRule(Pattern.compile("#.*$", Pattern.MULTILINE), colors.textLow),
    HighlightRule(Pattern.compile("\\$[a-zA-Z_][a-zA-Z0-9_]*\\b"), colors.code),
    HighlightRule(Pattern.compile("\\b(if|then|else|elif|fi|for|while|do|done|case|esac|in|function|return|exit|break|continue|shift|source|alias|unalias|export|readonly|local|typeset|declare|builtin|command|enable|disown|jobs|bg|fg|kill|wait|disown|suspend|lastpipe|auto_resume|histchars|histcontrol|histfiles|histfile|histfiles|histfiles|histexp|histfcnt|histignore|histverify|histreedit|histshow|histtimeformat|histappend|histbackup|histfiles|histfiles|hishcontrol|histrevex|histfiles|histfiles|histfiles|histfiles|hishfiles)\\b"), colors.code),
)

/** Returns a list of (text, color) pairs for syntax-highlighted code. */
fun highlightCode(code: String, lang: String?): List<Pair<String, Color>> {
    val colors = LocalMinoColors.current
    val rules = when {
        lang == "js" || lang == "javascript" -> jsRules(colors)
        lang == "ts" || lang == "tsx" || lang == "typescript" -> tsRules(colors)
        lang == "py" || lang == "python" -> pythonRules(colors)
        lang == "json" -> jsonRules(colors)
        lang == "kt" || lang == "kts" || lang == "kotlin" -> kotlinRules(colors)
        lang == "java" -> javaRules(colors)
        lang == "html" || lang == "htm" -> htmlRules(colors)
        lang == "css" -> cssRules(colors)
        lang == "sql" -> sqlRules(colors)
        lang == "sh" || lang == "bash" || lang == "zsh" -> bashRules(colors)
        else -> emptyList()
    }
    if (rules.isEmpty()) return listOf(code to colors.text)

    val result = mutableListOf<Pair<String, Color>>()
    var pos = 0
    while (pos < code.length) {
        var bestMatch: java.util.regex.Matcher? = null
        var bestRule: HighlightRule? = null
        var bestStart = Int.MAX_VALUE

        for (rule in rules) {
            val m = rule.pattern.matcher(code)
            m.find(pos)
            if (m.start() == pos && m.start() < bestStart) {
                bestStart = m.start()
                bestMatch = m
                bestRule = rule
            }
        }

        if (bestMatch != null && bestStart == pos) {
            result.add(code.substring(pos, bestMatch.end()) to bestRule!!.color)
            pos = bestMatch.end()
        } else {
            var nextStart = code.length
            for (rule in rules) {
                val m = rule.pattern.matcher(code)
                m.find(pos)
                if (m.start() < nextStart) nextStart = m.start()
            }
            if (nextStart > pos) {
                result.add(code.substring(pos, nextStart) to colors.text)
            }
            pos = nextStart
        }
    }
    return result
}
