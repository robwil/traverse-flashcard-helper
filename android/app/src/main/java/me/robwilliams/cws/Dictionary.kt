package me.robwilliams.cws

import android.content.Context
import org.json.JSONObject

object Dictionary {
    private var data: Map<String, List<String>>? = null

    private fun ensureLoaded(context: Context) {
        if (data == null) {
            val json = context.assets.open("char_index.json")
                .bufferedReader()
                .use { it.readText() }
            val jsonObject = JSONObject(json)
            val map = mutableMapOf<String, List<String>>()
            for (key in jsonObject.keys()) {
                val array = jsonObject.getJSONArray(key)
                val words = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    words.add(array.getString(i))
                }
                map[key] = words
            }
            data = map
        }
    }

    fun lookup(context: Context, text: String): String? {
        ensureLoaded(context)
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val results = mutableListOf<String>()
        for (char in trimmed) {
            val words = data?.get(char.toString())
            if (words != null && words.isNotEmpty()) {
                results.add(words.joinToString(", "))
            }
        }

        return if (results.isNotEmpty()) results.joinToString("\n") else null
    }
}
