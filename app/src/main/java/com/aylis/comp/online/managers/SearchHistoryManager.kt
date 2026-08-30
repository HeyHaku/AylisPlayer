package com.aylis.comp.online.managers

import android.content.Context
import android.content.SharedPreferences

object SearchHistoryManager {
    private const val PREFS_NAME = "OnlineSearchHistory"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_HISTORY = 10

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getHistory(context: Context): List<String> {
        val prefs = getPrefs(context)
        val historyStr = prefs.getString(KEY_HISTORY, "") ?: ""
        if (historyStr.isEmpty()) return emptyList()
        return historyStr.split("|*|")
    }

    fun addQuery(context: Context, query: String) {
        val current = getHistory(context).toMutableList()
        current.remove(query) // Remove if exists to put it at top
        current.add(0, query)
        
        if (current.size > MAX_HISTORY) {
            current.removeAt(current.size - 1)
        }
        
        saveHistory(context, current)
    }

    fun removeQuery(context: Context, query: String) {
        val current = getHistory(context).toMutableList()
        current.remove(query)
        saveHistory(context, current)
    }

    private fun saveHistory(context: Context, history: List<String>) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_HISTORY, history.joinToString("|*|")).apply()
    }
}
