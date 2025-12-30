package me.robwilliams.cws

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DictionaryTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun `lookup returns null for empty string`() {
        val result = Dictionary.lookup(context, "")
        assertNull(result)
    }

    @Test
    fun `lookup returns null for whitespace only`() {
        val result = Dictionary.lookup(context, "   ")
        assertNull(result)
    }

    @Test
    fun `lookup returns null for string with only tabs and newlines`() {
        val result = Dictionary.lookup(context, "\t\n  \t")
        assertNull(result)
    }

    @Test
    fun `lookup returns words for single character`() {
        // Using a character that exists in the dictionary
        val result = Dictionary.lookup(context, "一")
        assertNotNull(result)
        assertTrue(result!!.contains("一"))
    }

    @Test
    fun `lookup returns multiple lines for multiple characters`() {
        // Using characters that exist in the dictionary
        val result = Dictionary.lookup(context, "一二")
        assertNotNull(result)
        // Should have two lines, one for each character
        val lines = result!!.split("\n")
        assertEquals(2, lines.size)
    }

    @Test
    fun `lookup trims input before processing`() {
        val resultWithSpaces = Dictionary.lookup(context, "  一  ")
        val resultWithoutSpaces = Dictionary.lookup(context, "一")
        assertEquals(resultWithoutSpaces, resultWithSpaces)
    }

    @Test
    fun `lookup returns null for character not in dictionary`() {
        // Using a character unlikely to be in the Chinese dictionary
        val result = Dictionary.lookup(context, "X")
        assertNull(result)
    }

    @Test
    fun `lookup skips characters not in dictionary but includes found ones`() {
        // Mix of valid Chinese character and ASCII character
        val result = Dictionary.lookup(context, "一X二")
        assertNotNull(result)
        // Should have two lines for the two valid characters
        val lines = result!!.split("\n")
        assertEquals(2, lines.size)
    }

    @Test
    fun `lookup returns comma-separated words for a character`() {
        val result = Dictionary.lookup(context, "一")
        assertNotNull(result)
        // The dictionary has multiple words for this character, joined by ", "
        assertTrue(result!!.contains(", "))
    }

    @Test
    fun `lookup handles mixed content with only invalid characters`() {
        val result = Dictionary.lookup(context, "ABC123")
        assertNull(result)
    }
}
