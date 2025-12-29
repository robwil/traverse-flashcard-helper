package me.robwilliams.cws

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProcessTextActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle both PROCESS_TEXT and SEND intents
        val selectedText = (intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_TEXT))
            ?.toString()
            ?.trim()

        if (selectedText.isNullOrEmpty()) {
            finish()
            return
        }

        val result = Dictionary.lookup(this, selectedText)

        if (result != null) {
            copyToClipboard(result)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No match found", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("CWS result", text)
        clipboard.setPrimaryClip(clip)
    }
}
