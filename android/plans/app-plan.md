# Android CWS Dictionary App

CWS = Chinese Word Samples. The goal of the app is to take a given Chinese character (simplified)
and look up the top frequency words for that character in a pre-built dictionary. Those words will
be copied to the user's clipboard so they can paste it wherever they want.

## 1. Overview

This document describes the requirements and full technical implementation for a lightweight Android app that:

* Appears in the **text selection menu** when the user highlights text
* Receives the highlighted text via Android's `ACTION_PROCESS_TEXT` API
* Looks up the text in a **hard-coded dictionary**
* Copies the lookup result to the **system clipboard**
* Exits immediately with minimal UI impact

The app is designed to be fast, reliable, and resilient to OEM menu pruning (e.g. OnePlus, Samsung).

---

## 2. Goals

### Functional Goals

* Accept highlighted text from any compatible Android app
* Perform an offline dictionary lookup
* Copy the result to the clipboard
* Provide brief user feedback (Toast)

### Non‑Goals

* No networking
* No database or persistent storage
* No background services
* No text modification returned to the source app

---

## 3. Target Platform

| Item                    | Value                       |
| ----------------------- | --------------------------- |
| Minimum Android Version | Android 6.0 (API 23)        |
| Target SDK              | Latest stable (recommended) |
| Language                | Kotlin                      |
| UI Framework            | None (Activity only)        |
| Permissions             | None                        |

---

## 4. User Experience Flow

1. User highlights text in any app
2. Android displays the text selection context menu
3. User taps **"CWS: Lookup"**
4. App launches invisibly
5. App copies the dictionary result to clipboard
6. Toast confirms action
7. App finishes immediately

---

## 5. Android System Integration

### 5.1 API Used

* `Intent.ACTION_PROCESS_TEXT`
* `Intent.EXTRA_PROCESS_TEXT`
* `ClipboardManager`

### 5.2 Why PROCESS_TEXT

* Official Android API for text selection extensions
* No permissions required
* Supported by most apps and WebViews
* Designed for exactly this use case

---

## 6. Manifest Configuration

### 6.1 Activity Declaration

```xml
<activity
    android:name=".ProcessTextActivity"
    android:label="CWS: Lookup"
    android:exported="true"
    android:theme="@style/Theme.Transparent">

    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>
```

### 6.2 Design Constraints

* Label must be **short** (2–3 words)
* Only `text/plain` MIME type
* Single PROCESS_TEXT activity

---

## 7. UI & Theme

### 7.1 No Visible UI

The app must not display any UI to avoid flicker and deprioritization by OEMs.

### 7.2 Transparent Theme

```xml
<style name="Theme.Transparent" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
</style>
```

---

## 8. Core Activity Implementation

### 8.1 Activity Lifecycle

* Only `onCreate()` is used
* Activity must complete in under 100 ms
* Activity must always call `finish()`

### 8.2 Kotlin Implementation

```kotlin
class ProcessTextActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent
            .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            ?.trim()

        if (selectedText.isNullOrEmpty()) {
            finish()
            return
        }

        val result = Dictionary.lookup(selectedText)

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
        val clip = ClipData.newPlainText("dictionary result", text)
        clipboard.setPrimaryClip(clip)
    }
}
```

---

## 9. Dictionary Implementation

### 9.1 Requirements

* Hard-coded
* In-memory
* Instant lookup
* No I/O

### 9.2 Example Dictionary

No example needed. The exact dictionary is available at `../../scripts/char_index.json` from here
in the same git repo. Copy this into our Android Studio project as a resource or just hard-code it,
whatever makes the most sense.

### 9.3 Normalization Notes

* Trim whitespace

---

## 10. Clipboard Behavior

* No permissions required
* Replaces current clipboard contents
* Works across all Android versions supported
* Most reliable way to return results to the user

---

## 11. Performance Requirements

| Metric               | Requirement |
| -------------------- | ----------- |
| Cold start           | < 100 ms    |
| Memory usage         | Minimal     |
| ANR risk             | None        |
| Background execution | None        |

Apps that violate these are more likely to be hidden by OEMs.

