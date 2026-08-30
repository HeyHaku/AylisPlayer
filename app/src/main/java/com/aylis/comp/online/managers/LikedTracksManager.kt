package com.aylis.comp.online.managers

import android.content.Context
import android.content.SharedPreferences
import com.aylis.PlayerCore
import com.aylis.comp.online.repository.OnlineTrack
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch

object LikedTracksManager {
    private const val PREFS_NAME = "liked_tracks_prefs"
    private const val KEY_TRACKS = "liked_tracks_array"

    private fun getPrefs(): SharedPreferences? {
        return PlayerCore.s().appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLikedTracks(): List<OnlineTrack> {
        val prefs = getPrefs() ?: return emptyList()
        val jsonStr = prefs.getString(KEY_TRACKS, "[]")
        val list = mutableListOf<OnlineTrack>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val track = OnlineTrack(
                    videoId = obj.optString("videoId"),
                    title = obj.optString("title"),
                    artist = obj.optString("artist"),
                    thumbnail = obj.optString("thumbnail"),
                )
                list.add(track)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun isLiked(videoId: String): Boolean {
        return getLikedTracks().any { it.videoId == videoId }
    }

    fun toggleLike(track: OnlineTrack): Boolean {
        val tracks = getLikedTracks().toMutableList()
        val existing = tracks.find { it.videoId == track.videoId }
        val isNowLiked = if (existing != null) {
            tracks.remove(existing)
            false
        } else {
            tracks.add(0, track) // add to top
            true
        }
        
        saveTracks(tracks)
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (isNowLiked) {
                com.aylis.comp.online.repository.OnlineMusicRepository.likeTrack(track.videoId)
            } else {
                com.aylis.comp.online.repository.OnlineMusicRepository.removeLikeTrack(track.videoId)
            }
        }
        
        return isNowLiked
    }

    private fun saveTracks(tracks: List<OnlineTrack>) {
        val arr = JSONArray()
        for (t in tracks) {
            val obj = JSONObject()
            obj.put("videoId", t.videoId)
            obj.put("title", t.title)
            obj.put("artist", t.artist)
            obj.put("thumbnail", t.thumbnail)
            arr.put(obj)
        }
        getPrefs()?.edit()?.putString(KEY_TRACKS, arr.toString())?.apply()
    }
}
