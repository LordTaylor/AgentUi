package com.agentcore.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {
    private val KeywordColor = Color(0xFFC678DD)
    private val FunctionColor = Color(0xFF61AFEF)
    private val StringColor = Color(0xFF98C379)
    private val NumberColor = Color(0xFFD19A66)
    private val CommentColor = Color(0xFF5C6370)
    private val TypeColor = Color(0xFFE5C07B)

    private val Keywords = setOf(
        "fun", "val", "var", "if", "else", "for", "while", "return", "class", "interface", "object",
        "struct", "enum", "impl", "fn", "let", "mut", "pub", "use", "mod", "crate",
        "def", "import", "from", "as", "try", "except", "with", "async", "await", "yield"
    )

    fun highlight(code: String, language: String? = null): AnnotatedString {
        return buildAnnotatedString {
            val lines = code.lines()
            lines.forEachIndexed { index, line ->
                val words = line.split(Regex("(?=[^a-zA-Z0-9])|(?<=[^a-zA-Z0-9])"))
                var inString = false
                var stringChar = ' '

                words.forEach { word ->
                    when {
                        word == "\"" || word == "'" -> {
                            inString = !inString
                            withStyle(SpanStyle(color = StringColor)) { append(word) }
                        }
                        inString -> withStyle(SpanStyle(color = StringColor)) { append(word) }
                        word in Keywords -> withStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold)) { append(word) }
                        word.matches(Regex("\\d+")) -> withStyle(SpanStyle(color = NumberColor)) { append(word) }
                        word.startsWith("//") || word.startsWith("#") -> {
                            withStyle(SpanStyle(color = CommentColor)) { append(word) }
                        }
                        word.matches(Regex("[A-Z][a-zA-Z0-9]*")) -> withStyle(SpanStyle(color = TypeColor)) { append(word) }
                        word.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*\\(")) -> {
                            val name = word.dropLast(1)
                            withStyle(SpanStyle(color = FunctionColor)) { append(name) }
                            append("(")
                        }
                        else -> append(word)
                    }
                }
                if (index < lines.size - 1) append("\n")
            }
        }
    }
}
