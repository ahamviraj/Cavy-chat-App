package com.virudaya.cavy.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object EmojiUtils {
    val COMMON_EMOJIS = listOf(
        "😀", "😂", "🥹", "😍", "🥰", "😎", "🤔", "😭", "❤️", "👍",
        "👋", "🔥", "🎉", "👏", "💯", "🙌", "✨", "💬", "🤩", "🥳",
        "🙏", "💪", "🚀", "💡", "⚡", "🌟"
    )

    fun insertEmojiAtCursor(currentValue: TextFieldValue, emoji: String): TextFieldValue {
        val text = currentValue.text
        val selection = currentValue.selection
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(0, text.length)

        val newText = text.replaceRange(start, end, emoji)
        val newCursorPos = start + emoji.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }
}
