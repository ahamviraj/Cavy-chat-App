package com.virudaya.cavy

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.virudaya.cavy.util.EmojiUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiUtilsTest {

    @Test
    fun insertEmoji_intoEmptyText() {
        val initial = TextFieldValue(text = "", selection = TextRange(0))
        val result = EmojiUtils.insertEmojiAtCursor(initial, "😀")

        assertEquals("😀", result.text)
        assertEquals(2, result.selection.start)
    }

    @Test
    fun insertEmoji_atMiddleCursorPosition() {
        val initial = TextFieldValue(text = "Hello  world", selection = TextRange(6))
        val result = EmojiUtils.insertEmojiAtCursor(initial, "👋")

        assertEquals("Hello 👋 world", result.text)
        assertEquals(8, result.selection.start)
    }

    @Test
    fun insertEmoji_replacesSelection() {
        val initial = TextFieldValue(text = "Hello replace me world", selection = TextRange(6, 16))
        val result = EmojiUtils.insertEmojiAtCursor(initial, "🔥")

        assertEquals("Hello 🔥 world", result.text)
        assertEquals(8, result.selection.start)
    }

    @Test
    fun insertEmoji_multipleSequentialInsertions() {
        var state = TextFieldValue(text = "Cavy", selection = TextRange(4))
        state = EmojiUtils.insertEmojiAtCursor(state, "🚀")
        state = EmojiUtils.insertEmojiAtCursor(state, "✨")

        assertEquals("Cavy🚀✨", state.text)
        assertEquals(7, state.selection.start)
    }
}
