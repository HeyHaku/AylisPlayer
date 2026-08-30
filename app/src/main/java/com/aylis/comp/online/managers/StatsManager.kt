package com.aylis.comp.online.managers

import android.content.Context
import android.content.SharedPreferences
import com.aylis.PlayerCore
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

object StatsManager {
    private const val PREFS_NAME = "stats_manager_prefs"
    private const val KEY_TOTAL_TIME = "total_time_ms"
    private const val KEY_TOTAL_PLAYS = "total_plays"
    private const val KEY_TRACK_COUNTS = "track_counts_json"
    private const val KEY_FIRST_PLAY_TIME = "first_play_time_ms"
    
    private var trackingJob: kotlinx.coroutines.Job? = null

    fun startTracking() {
        if (trackingJob != null) return
        trackingJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                if (com.aylis.Design.PlaybackDesign.isPlaying) {
                    addTime(10000L) // Add 10 seconds
                }
                kotlinx.coroutines.delay(10000L)
            }
        }
    }

    private fun getPrefs(): SharedPreferences? {
        return PlayerCore.s().appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addTime(ms: Long) {
        if (ms <= 0) return
        val prefs = getPrefs() ?: return
        val current = prefs.getLong(KEY_TOTAL_TIME, 0L)
        prefs.edit().putLong(KEY_TOTAL_TIME, current + ms).apply()

        // Track daily time
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val dailyJsonStr = prefs.getString("daily_time_json", "{}")
        try {
            val obj = JSONObject(dailyJsonStr)
            val currentDaily = obj.optLong(todayDate, 0L)
            obj.put(todayDate, currentDaily + ms)
            prefs.edit().putString("daily_time_json", obj.toString()).apply()
        } catch (e: Exception) {}
    }

    fun addPlay(trackId: String, trackTitle: String, trackArtist: String) {
        if (trackId.isEmpty() || trackId.startsWith("http")) return // Only count valid YouTube IDs or simple local names if possible
        
        val prefs = getPrefs() ?: return
        val totalPlays = prefs.getLong(KEY_TOTAL_PLAYS, 0L)
        
        if (totalPlays == 0L) {
            prefs.edit().putLong(KEY_FIRST_PLAY_TIME, System.currentTimeMillis()).apply()
        }
        
        prefs.edit().putLong(KEY_TOTAL_PLAYS, totalPlays + 1).apply()
        
        // Track specific track play count
        val jsonStr = prefs.getString(KEY_TRACK_COUNTS, "{}")
        try {
            val obj = JSONObject(jsonStr)
            val trackKey = "$trackTitle - $trackArtist"
            val count = obj.optInt(trackKey, 0)
            obj.put(trackKey, count + 1)
            prefs.edit().putString(KEY_TRACK_COUNTS, obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTotalTimeMs(): Long = getPrefs()?.getLong(KEY_TOTAL_TIME, 0L) ?: 0L
    fun getTotalPlays(): Long = getPrefs()?.getLong(KEY_TOTAL_PLAYS, 0L) ?: 0L
    
    fun getAvgPlaysPerDay(): Long {
        val prefs = getPrefs() ?: return 0L
        val firstPlay = prefs.getLong(KEY_FIRST_PLAY_TIME, System.currentTimeMillis())
        val diffMs = System.currentTimeMillis() - firstPlay
        val days = Math.max(1, diffMs / (1000 * 60 * 60 * 24))
        return getTotalPlays() / days
    }

    fun getTopTrack(): String {
        val prefs = getPrefs() ?: return "None"
        val jsonStr = prefs.getString(KEY_TRACK_COUNTS, "{}")
        var topTrack = "None"
        var maxCount = 0
        try {
            val obj = JSONObject(jsonStr)
            obj.keys().forEach { key ->
                val count = obj.getInt(key)
                if (count > maxCount) {
                    maxCount = count
                    topTrack = key
                }
            }
        } catch (e: Exception) {}
        
        if (maxCount > 0) {
            return "$topTrack\n• $maxCount plays"
        }
        return "None\n• 0 plays"
    }

    // Returns a list of 7 longs (playback time in ms) for the last 7 days, ending with today.
    fun getLast7DaysTimeMs(): List<Long> {
        val prefs = getPrefs() ?: return List(7) { 0L }
        val dailyJsonStr = prefs.getString("daily_time_json", "{}")
        val result = mutableListOf<Long>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        try {
            val obj = JSONObject(dailyJsonStr)
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
            
            for (i in 0 until 7) {
                val dateStr = sdf.format(cal.time)
                result.add(obj.optLong(dateStr, 0L))
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            return List(7) { 0L }
        }
        
        return result
    }
}
