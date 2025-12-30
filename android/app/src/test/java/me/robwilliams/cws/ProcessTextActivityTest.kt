package me.robwilliams.cws

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProcessTextActivityTest {

    private lateinit var clipboard: ClipboardManager

    @Before
    fun setUp() {
        // Clear any previous toasts
        ShadowToast.reset()
    }

    private fun buildProcessTextIntent(text: String?): Intent {
        return Intent(Intent.ACTION_PROCESS_TEXT).apply {
            if (text != null) {
                putExtra(Intent.EXTRA_PROCESS_TEXT, text as CharSequence)
            }
        }
    }

    private fun buildShareIntent(text: String?): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (text != null) {
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
    }

    private fun launchActivity(intent: Intent): ActivityController<ProcessTextActivity> {
        return Robolectric.buildActivity(ProcessTextActivity::class.java, intent)
            .create()
    }

    private fun getClipboard(activity: ProcessTextActivity): ClipboardManager {
        return activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // ==================== PROCESS_TEXT Intent Tests ====================

    @Test
    fun `PROCESS_TEXT intent with valid Chinese text copies result to clipboard`() {
        val intent = buildProcessTextIntent("一")
        val controller = launchActivity(intent)
        val activity = controller.get()

        val clipboard = getClipboard(activity)
        assertTrue(clipboard.hasPrimaryClip())
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        assertTrue(clipText.contains("一"))
    }

    @Test
    fun `PROCESS_TEXT intent with valid text shows success toast`() {
        val intent = buildProcessTextIntent("一")
        launchActivity(intent)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("Copied to clipboard", latestToast)
    }

    @Test
    fun `PROCESS_TEXT intent with valid text finishes activity`() {
        val intent = buildProcessTextIntent("一")
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `PROCESS_TEXT intent with no match shows no match toast`() {
        val intent = buildProcessTextIntent("XYZ")
        launchActivity(intent)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("No match found", latestToast)
    }

    @Test
    fun `PROCESS_TEXT intent with empty text finishes without toast`() {
        val intent = buildProcessTextIntent("")
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `PROCESS_TEXT intent with null text finishes without toast`() {
        val intent = buildProcessTextIntent(null)
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `PROCESS_TEXT intent with whitespace only finishes without toast`() {
        val intent = buildProcessTextIntent("   ")
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())
    }

    // ==================== SEND (Share) Intent Tests ====================

    @Test
    fun `SEND intent with valid Chinese text copies result to clipboard`() {
        val intent = buildShareIntent("二")
        val controller = launchActivity(intent)
        val activity = controller.get()

        val clipboard = getClipboard(activity)
        assertTrue(clipboard.hasPrimaryClip())
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        assertTrue(clipText.contains("二"))
    }

    @Test
    fun `SEND intent with valid text shows success toast`() {
        val intent = buildShareIntent("二")
        launchActivity(intent)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("Copied to clipboard", latestToast)
    }

    @Test
    fun `SEND intent with valid text finishes activity`() {
        val intent = buildShareIntent("二")
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `SEND intent with no match shows no match toast`() {
        val intent = buildShareIntent("ABC")
        launchActivity(intent)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("No match found", latestToast)
    }

    @Test
    fun `SEND intent with empty text finishes without toast`() {
        val intent = buildShareIntent("")
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `SEND intent with null text finishes without toast`() {
        val intent = buildShareIntent(null)
        val controller = launchActivity(intent)
        val activity = controller.get()

        assertTrue(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())
    }

    // ==================== Priority Tests ====================

    @Test
    fun `PROCESS_TEXT takes priority over EXTRA_TEXT when both present`() {
        // Create an intent with both extras - PROCESS_TEXT should take priority
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "一" as CharSequence)
            putExtra(Intent.EXTRA_TEXT, "二")
        }
        val controller = launchActivity(intent)
        val activity = controller.get()

        val clipboard = getClipboard(activity)
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        // Should contain results for "一", not "二"
        assertTrue(clipText.contains("一个"))
    }

    // ==================== Multiple Characters Tests ====================

    @Test
    fun `PROCESS_TEXT with multiple characters returns multiple lines`() {
        val intent = buildProcessTextIntent("一二三")
        val controller = launchActivity(intent)
        val activity = controller.get()

        val clipboard = getClipboard(activity)
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        val lines = clipText.split("\n")
        assertEquals(3, lines.size)
    }

    @Test
    fun `SEND with multiple characters returns multiple lines`() {
        val intent = buildShareIntent("一二三")
        val controller = launchActivity(intent)
        val activity = controller.get()

        val clipboard = getClipboard(activity)
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        val lines = clipText.split("\n")
        assertEquals(3, lines.size)
    }
}
