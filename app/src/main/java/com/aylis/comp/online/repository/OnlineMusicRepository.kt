package com.aylis.comp.online.repository

import com.aylis.comp.online.api.YouTubeMusicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.json.JSONObject
import org.json.JSONArray

data class StreamResult(val url: String, val artist: String?, val durationSeconds: Long, val thumbnail: String?)

object OnlineMusicRepository {

    init {
        Thread {
            try {
                NewPipe.init(DownloaderImpl.getInstance(), Localization.DEFAULT)
                android.util.Log.d("OnlineMusicRepo", "NewPipeExtractor успешно инициализирован")
            } catch (e: Exception) {
                android.util.Log.e("OnlineMusicRepo", "Ошибка инициализации NewPipeExtractor", e)
            }
        }.start()
        com.aylis.comp.online.managers.OnlinePlaybackManager.init()
    }

    private val client: YouTubeMusicClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                val cookies = com.aylis.comp.online.managers.AuthManager.getCookies()
                if (cookies.isNotEmpty() && original.url.host.contains("youtube.com")) {
                    requestBuilder.header("Cookie", cookies)
                    val sapisid = com.aylis.comp.online.managers.AuthManager.getSapisid()
                    if (sapisid != null) {
                        val time = System.currentTimeMillis() / 1000
                        val input = "$time $sapisid https://music.youtube.com"
                        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        requestBuilder.header("Authorization", "SAPISIDHASH ${time}_$hash")
                        requestBuilder.header("X-Origin", "https://music.youtube.com")
                    }
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://music.youtube.com/youtubei/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(YouTubeMusicClient::class.java)
    }

    private fun getBasePayload(
        countryCode: String = java.util.Locale.getDefault().country.let { if (it.isNullOrEmpty()) "US" else it },
        languageCode: String = "en"
    ): Map<String, Any> {
        return mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to "WEB_REMIX",
                    "clientVersion" to "1.20240401.01.00",
                    "gl" to countryCode,
                    "hl" to languageCode
                )
            )
        )
    }

    private fun parseItem(renderer: JSONObject): OnlineItem? {
        var videoId: String? = null
        var browseId: String? = null
        var title: String? = null
        var artist = "YouTube Music"
        var thumbnail = ""

        try {
            fun findThumbnails(obj: JSONObject?): JSONArray? {
                if (obj == null) return null
                if (obj.has("thumbnails")) return obj.optJSONArray("thumbnails")
                obj.keys().forEach { key ->
                    val child = obj.opt(key)
                    if (child is JSONObject) {
                        val res = findThumbnails(child)
                        if (res != null) return res
                    } else if (child is JSONArray) {
                        for (i in 0 until child.length()) {
                            val item = child.opt(i)
                            if (item is JSONObject) {
                                val res = findThumbnails(item)
                                if (res != null) return res
                            }
                        }
                    }
                }
                return null
            }
            
            val thumbArray = findThumbnails(renderer)
            val rawThumb = thumbArray?.optJSONObject(0)?.optString("url") ?: ""
            thumbnail = rawThumb.replace(Regex("=w\\d+-h\\d+.*"), "=w512-h512-l90-rj")
        } catch (e: Exception) {}

        // Now look for videoId
        try {
            val overlay = renderer.optJSONObject("overlay")
            val playBtn = overlay?.optJSONObject("musicItemThumbnailOverlayRenderer")?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
            videoId = playBtn?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
        } catch (e: Exception) {}
        
        if (videoId == null) {
            try {
                val watchEndpoint = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                videoId = watchEndpoint?.optString("videoId")
            } catch (e: Exception) {}
        }

        // If it has a videoId, it's a track! Otherwise check for playlist/folder
        if (videoId == null) {
            try {
                val navEndpoint = renderer.optJSONObject("navigationEndpoint")
                val bId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
                val pId = navEndpoint?.optJSONObject("watchEndpoint")?.optString("playlistId")
                
                if (bId != null && bId.startsWith("VL")) {
                    browseId = bId.substring(2)
                } else if (!bId.isNullOrEmpty()) {
                    browseId = bId
                } else if (!pId.isNullOrEmpty()) {
                    browseId = pId
                }
            } catch (e: Exception) {}
        }

        try {
            val titleRuns = renderer.optJSONObject("title")?.optJSONArray("runs") ?:
                renderer.optJSONArray("flexColumns")?.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
            title = titleRuns?.optJSONObject(0)?.optString("text")

            val subtitleRuns = renderer.optJSONObject("subtitle")?.optJSONArray("runs") ?:
                renderer.optJSONArray("flexColumns")?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
            
            if (subtitleRuns != null && subtitleRuns.length() > 0) {
                for (i in 0 until subtitleRuns.length()) {
                    val tTrim = subtitleRuns.optJSONObject(i)?.optString("text")?.trim() ?: ""
                    if (tTrim != "•" && tTrim.isNotEmpty() && !tTrim.matches(Regex("\\d+:\\d+.*"))) {
                        val lower = tTrim.lowercase()
                        if (lower != "song" && lower != "video" && lower != "album" && lower != "playlist") {
                            artist = tTrim
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        if (!title.isNullOrEmpty()) {
            if (!videoId.isNullOrEmpty()) {
                return OnlineTrack(videoId, title, artist, thumbnail)
            } else if (!browseId.isNullOrEmpty()) {
                return OnlinePlaylist(browseId, title, artist, thumbnail)
            }
        }
        return null
    }

    private fun parseTrackItem(renderer: JSONObject): OnlineTrack? {
        var videoId: String? = null
        var title: String? = null
        var artist = "YouTube Music"
        var thumbnail = ""

        try {
            fun findThumbnails(obj: JSONObject?): JSONArray? {
                if (obj == null) return null
                if (obj.has("thumbnails")) return obj.optJSONArray("thumbnails")
                obj.keys().forEach { key ->
                    val child = obj.opt(key)
                    if (child is JSONObject) {
                        val res = findThumbnails(child)
                        if (res != null) return res
                    } else if (child is JSONArray) {
                        for (i in 0 until child.length()) {
                            val item = child.opt(i)
                            if (item is JSONObject) {
                                val res = findThumbnails(item)
                                if (res != null) return res
                            }
                        }
                    }
                }
                return null
            }
            
            val thumbArray = findThumbnails(renderer)
            val rawThumb = thumbArray?.optJSONObject(0)?.optString("url") ?: ""
            thumbnail = rawThumb.replace(Regex("=w\\d+-h\\d+.*"), "=w512-h512-l90-rj")
        } catch (e: Exception) {}

        try {
            val navEndpoint = renderer.optJSONObject("navigationEndpoint")
            val overlay = renderer.optJSONObject("overlay")
            val playBtn = overlay?.optJSONObject("musicItemThumbnailOverlayRenderer")?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
            val playWatch = playBtn?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
            val navWatch = navEndpoint?.optJSONObject("watchEndpoint")
            
            videoId = playWatch?.optString("videoId") ?: navWatch?.optString("videoId")
        } catch (e: Exception) {}

        try {
            val titleRuns = renderer.optJSONObject("title")?.optJSONArray("runs") ?:
                renderer.optJSONArray("flexColumns")?.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
            title = titleRuns?.optJSONObject(0)?.optString("text")

            val subtitleRuns = renderer.optJSONObject("subtitle")?.optJSONArray("runs") ?:
                renderer.optJSONArray("flexColumns")?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
            
            if (subtitleRuns != null && subtitleRuns.length() > 0) {
                for (i in 0 until subtitleRuns.length()) {
                    val tTrim = subtitleRuns.optJSONObject(i)?.optString("text")?.trim() ?: ""
                    if (tTrim != "•" && tTrim.isNotEmpty() && !tTrim.matches(Regex("\\d+:\\d+.*"))) {
                        val lower = tTrim.lowercase()
                        if (lower != "song" && lower != "video" && lower != "album" && lower != "playlist") {
                            artist = tTrim
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        if (!title.isNullOrEmpty() && !videoId.isNullOrEmpty()) {
            return OnlineTrack(videoId, title, artist, thumbnail)
        }
        return null
    }

    private fun parseShelves(jsonString: String): List<Shelf> {
        val shelves = mutableListOf<Shelf>()
        val root = JSONObject(jsonString)

        fun searchShelves(obj: Any?) {
            if (obj is JSONObject) {
                val isImmersive = obj.has("musicImmersiveCarouselShelfRenderer")
                val rendererKey = if (isImmersive) "musicImmersiveCarouselShelfRenderer" else "musicCarouselShelfRenderer"
                if (obj.has(rendererKey)) {
                    val renderer = obj.getJSONObject(rendererKey)
                    val headerRuns = renderer.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")?.optJSONObject("title")?.optJSONArray("runs")
                    val shelfTitle = headerRuns?.optJSONObject(0)?.optString("text") ?: "Mixes"
                    
                    val isActuallyImmersive = isImmersive || Regex("(?i)(?<!re)mix|микс").containsMatchIn(shelfTitle)
                    
                    val items = mutableListOf<OnlineItem>()
                    val contents = renderer.optJSONArray("contents")
                    if (contents != null) {
                        for (i in 0 until contents.length()) {
                            val contentItem = contents.getJSONObject(i)
                            val itemRenderer = contentItem.optJSONObject("musicTwoRowItemRenderer") ?: contentItem.optJSONObject("musicResponsiveListItemRenderer")
                            if (itemRenderer != null) {
                                parseItem(itemRenderer)?.let { items.add(it) }
                            }
                        }
                    }
                    if (items.isNotEmpty()) {
                        shelves.add(Shelf(shelfTitle, items))
                    }
                }
                
                obj.keys().forEach { key -> searchShelves(obj.opt(key)) }
            } else if (obj is JSONArray) {
                for (i in 0 until obj.length()) { searchShelves(obj.opt(i)) }
            }
        }

        searchShelves(root)
        return shelves
    }

    private fun parseTracksFromJson(jsonString: String): List<OnlineTrack> {
        val tracks = mutableListOf<OnlineTrack>()
        val root = JSONObject(jsonString)

        fun searchJson(obj: Any?) {
            if (obj is JSONObject) {
                val renderer = obj.optJSONObject("musicResponsiveListItemRenderer") ?: obj.optJSONObject("musicTwoRowItemRenderer")
                if (renderer != null) {
                    val item = parseTrackItem(renderer)
                    if (item != null) {
                        tracks.add(item)
                    }
                }
                
                val playlistPanelVideo = obj.optJSONObject("playlistPanelVideoRenderer")
                if (playlistPanelVideo != null) {
                    val vId = playlistPanelVideo.optString("videoId")
                    val title = playlistPanelVideo.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    val artist = playlistPanelVideo.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "YouTube Music"
                    val thumbObj = playlistPanelVideo.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val rawThumb = thumbObj?.optJSONObject(thumbObj.length() - 1)?.optString("url") ?: ""
                    val thumb = rawThumb.replace(Regex("=w\\d+-h\\d+.*"), "=w512-h512-l90-rj")
                    if (!vId.isNullOrEmpty() && !title.isNullOrEmpty()) {
                        tracks.add(OnlineTrack(vId, title, artist, thumb))
                    }
                }
                
                obj.keys().forEach { key -> searchJson(obj.opt(key)) }
            } else if (obj is JSONArray) {
                for (i in 0 until obj.length()) { searchJson(obj.opt(i)) }
            }
        }

        searchJson(root)
        return tracks.distinctBy { it.videoId }
    }

    suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap()
            payload["query"] = query
            
            val response = client.searchTracks(apiKey = null, requestBody = payload)
            val jsonString = response.string()
            
            return@withContext parseTracksFromJson(jsonString).take(30)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getHomeRecommendations(): List<Shelf> = withContext(Dispatchers.IO) {
        val allShelves = mutableListOf<Shelf>()
        try {
            val payload = getBasePayload().toMutableMap()
            payload["browseId"] = "FEmusic_home"
            
            val response = client.browse(apiKey = null, requestBody = payload)
            val jsonString = response.string()
            
            val shelves = parseShelves(jsonString)
            allShelves.addAll(shelves)

            try {
                var currentJson = jsonString
                var continuationCount = 0
                while (continuationCount < 3) {
                    val match = Regex("\"continuation\":\"([^\"]*)\"").find(currentJson)
                    val contToken = match?.groupValues?.get(1)
                    if (contToken != null) {
                        val contPayload = getBasePayload().toMutableMap()
                        val contResponse = client.browse(apiKey = null, continuation = contToken, requestBody = contPayload)
                        currentJson = contResponse.string()
                        val contShelves = parseShelves(currentJson)
                        if (contShelves.isEmpty()) break
                        allShelves.addAll(contShelves)
                        continuationCount++
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Custom Sorting Strategy requested by User
            val quickPicks = allShelves.filter { it.title.contains("Quick picks", ignoreCase = true) || it.title.contains("Быстрые", ignoreCase = true) || it.title.contains("Быстрый", ignoreCase = true) }
            val listenAgain = allShelves.filter { it.title.contains("Listen again", ignoreCase = true) || it.title.contains("Послушать снова", ignoreCase = true) || it.title.contains("Снова", ignoreCase = true) }
            val mixes = allShelves.filter { it.isImmersive || Regex("(?i)(?<!re)mix|микс|supermix|супермикс").containsMatchIn(it.title) }
            
            val others = allShelves.filter { 
                it !in quickPicks && it !in listenAgain && it !in mixes 
            }.shuffled()

            val sortedShelves = mutableListOf<Shelf>()
            sortedShelves.addAll(quickPicks)
            sortedShelves.addAll(mixes)
            sortedShelves.addAll(listenAgain)
            sortedShelves.addAll(others)
            
            return@withContext sortedShelves
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext allShelves
        }
    }

    suspend fun getLikedTracks(): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap()
            payload["browseId"] = "FEmusic_liked_videos"
            
            val response = client.browse(apiKey = null, requestBody = payload)
            val jsonString = response.string()
            
            return@withContext parseTracksFromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun parseItemsFromJson(jsonString: String): List<OnlineItem> {
        val items = mutableListOf<OnlineItem>()
        val root = JSONObject(jsonString)

        fun searchJson(obj: Any?) {
            if (obj is JSONObject) {
                val renderer = obj.optJSONObject("musicResponsiveListItemRenderer") ?: obj.optJSONObject("musicTwoRowItemRenderer")
                if (renderer != null) {
                    val item = parseItem(renderer)
                    if (item != null) items.add(item)
                }
                obj.keys().forEach { key -> searchJson(obj.opt(key)) }
            } else if (obj is JSONArray) {
                for (i in 0 until obj.length()) { searchJson(obj.opt(i)) }
            }
        }

        searchJson(root)
        return items.distinctBy { if (it is OnlineTrack) it.videoId else if (it is OnlinePlaylist) it.browseId else it.title }
    }

    suspend fun getLikedPlaylists(): List<OnlinePlaylist> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap()
            payload["browseId"] = "FEmusic_liked_playlists"
            
            val response = client.browse(apiKey = null, requestBody = payload)
            val jsonString = response.string()
            
            return@withContext parseItemsFromJson(jsonString).filterIsInstance<OnlinePlaylist>()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    suspend fun getPlaylistTracks(browseId: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap()
            
            val jsonString = if (browseId.startsWith("RD")) {
                payload["playlistId"] = browseId
                val response = client.next(apiKey = null, requestBody = payload)
                response.string()
            } else {
                val finalBrowseId = when {
                    browseId == "LM" -> "FEmusic_liked_videos"
                    browseId.startsWith("PL") -> "VL$browseId"
                    else -> browseId
                }
                payload["browseId"] = finalBrowseId
                val response = client.browse(apiKey = null, requestBody = payload)
                response.string()
            }

            return@withContext parseTracksFromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getStreamUrl(videoId: String): StreamResult? = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, "https://youtube.com/watch?v=$videoId")
            val audioStreams = streamInfo.audioStreams
            if (audioStreams.isNotEmpty()) {
                val bestAudio = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                val url = bestAudio.content
                var artist = streamInfo.uploaderName
                var duration = streamInfo.duration
                var thumbnail = streamInfo.thumbnails.firstOrNull()?.url
                return@withContext StreamResult(url, artist, duration, thumbnail)
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun getDownloadStreamUrl(videoId: String): StreamResult? = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, "https://youtube.com/watch?v=$videoId")
            val audioStreams = streamInfo.audioStreams
            if (audioStreams.isNotEmpty()) {
                // For downloads and taggers (like jaudiotagger), M4A (mp4a) is much better supported than WebM/Opus.
                val m4aStreams = audioStreams.filter { it.format?.name?.contains("M4A", ignoreCase = true) == true || it.format?.suffix?.contains("m4a", ignoreCase = true) == true }
                val bestAudio = if (m4aStreams.isNotEmpty()) {
                    m4aStreams.maxByOrNull { it.averageBitrate } ?: m4aStreams.first()
                } else {
                    audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                }
                
                val url = bestAudio.content
                var artist = streamInfo.uploaderName
                var duration = streamInfo.duration
                var thumbnail = streamInfo.thumbnails.firstOrNull()?.url
                return@withContext StreamResult(url, artist, duration, thumbnail)
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }
    suspend fun likeTrack(videoId: String): Boolean = withContext(Dispatchers.IO) {
        if (!com.aylis.comp.online.managers.AuthManager.isLoggedIn()) {
            return@withContext false
        }
        try {
            val payload = getBasePayload().toMutableMap()
            payload["target"] = mapOf("videoId" to videoId)
            val response = client.like(payload)
            val responseBody = response.string()
            return@withContext responseBody.contains("STATUS_SUCCEEDED") || responseBody.contains("\"status\":\"SUCCESS\"") || responseBody.contains("LIKE_SUCCESS")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun removeLikeTrack(videoId: String): Boolean = withContext(Dispatchers.IO) {
        if (!com.aylis.comp.online.managers.AuthManager.isLoggedIn()) {
            return@withContext false
        }
        try {
            val payload = getBasePayload().toMutableMap()
            payload["target"] = mapOf("videoId" to videoId)
            val response = client.removeLike(payload)
            val responseBody = response.string()
            return@withContext responseBody.contains("STATUS_SUCCEEDED") || responseBody.contains("\"status\":\"SUCCESS\"") || responseBody.contains("LIKE_SUCCESS")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun createPlaylist(title: String): String? = withContext(Dispatchers.IO) {
        if (!com.aylis.comp.online.managers.AuthManager.isLoggedIn()) {
            return@withContext null
        }
        try {
            val payload = getBasePayload().toMutableMap()
            payload["title"] = title
            val response = client.createPlaylist(payload)
            val jsonString = response.string()
            val match = Regex("\"playlistId\":\"([^\"]*)\"").find(jsonString)
            return@withContext match?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, videoId: String): Boolean = withContext(Dispatchers.IO) {
        if (!com.aylis.comp.online.managers.AuthManager.isLoggedIn()) {
            return@withContext false
        }
        try {
            val payload = getBasePayload().toMutableMap()
            payload["playlistId"] = playlistId.replace("VL", "")
            payload["actions"] = listOf(
                mapOf(
                    "action" to "ACTION_ADD_VIDEO",
                    "addedVideoId" to videoId
                )
            )
            val response = client.editPlaylist(payload)
            val responseBody = response.string()
            return@withContext responseBody.contains("STATUS_SUCCEEDED") || responseBody.contains("\"status\":\"SUCCESS\"") || responseBody.contains("success")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun getSongRadio(videoId: String, queueParams: String? = null): RadioResult = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap().apply {
                put("enablePersistentPlaylistPanel", true)
                put("isAudioOnly", true)
                put("videoId", videoId)
                put("playlistId", "RDAMVM$videoId")
                if (!queueParams.isNullOrEmpty()) {
                    put("params", queueParams)
                }
            }

            val response = client.next(apiKey = null, requestBody = payload)
            val jsonString = response.string()
            val root = JSONObject(jsonString)

            val tracks = parseTracksFromJson(jsonString)
            val chips = mutableListOf<FilterChip>()

            // Парсинг чипов модификации очереди (Queue Tuner Chips)
            fun findQueueChips(obj: Any?) {
                if (obj is JSONObject) {
                    if (obj.has("chipCloudChipRenderer")) {
                        val chipRenderer = obj.getJSONObject("chipCloudChipRenderer")
                        val title = chipRenderer.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        val params = chipRenderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("params")
                        if (!title.isNullOrEmpty() && !params.isNullOrEmpty()) {
                            chips.add(FilterChip(title, params))
                        }
                    }
                    obj.keys().forEach { key -> findQueueChips(obj.opt(key)) }
                } else if (obj is JSONArray) {
                    for (i in 0 until obj.length()) findQueueChips(obj.opt(i))
                }
            }

            findQueueChips(root)
            return@withContext RadioResult(tracks, chips)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext RadioResult(emptyList(), emptyList())
        }
    }

    suspend fun getRelatedContent(videoId: String): List<Shelf> = withContext(Dispatchers.IO) {
        try {
            val nextPayload = getBasePayload().toMutableMap().apply {
                put("videoId", videoId)
                put("isAudioOnly", true)
            }
            val nextResponse = client.next(apiKey = null, requestBody = nextPayload)
            val nextJson = JSONObject(nextResponse.string())

            var relatedBrowseId: String? = null
            fun findRelatedBrowseId(obj: Any?) {
                if (obj is JSONObject) {
                    val endpoint = obj.optJSONObject("browseEndpoint")
                    val bId = endpoint?.optString("browseId")
                    if (bId != null && bId.contains("related", ignoreCase = true)) {
                        relatedBrowseId = bId
                        return
                    }
                    obj.keys().forEach { key -> findRelatedBrowseId(obj.opt(key)) }
                } else if (obj is JSONArray) {
                    for (i in 0 until obj.length()) findRelatedBrowseId(obj.opt(i))
                }
            }
            findRelatedBrowseId(nextJson)

            if (!relatedBrowseId.isNullOrEmpty()) {
                val browsePayload = getBasePayload().toMutableMap().apply {
                    put("browseId", relatedBrowseId!!)
                }
                val browseResponse = client.browse(apiKey = null, requestBody = browsePayload)
                return@withContext parseShelves(browseResponse.string())
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getArtistRadio(artistOrChannelId: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val cleanId = artistOrChannelId.removePrefix("UC")
            val payload = getBasePayload().toMutableMap().apply {
                put("playlistId", "RDAMEA$cleanId")
                put("isAudioOnly", true)
            }
            val response = client.next(apiKey = null, requestBody = payload)
            return@withContext parseTracksFromJson(response.string())
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getHomeMoodChips(): List<FilterChip> = withContext(Dispatchers.IO) {
        val chips = mutableListOf<FilterChip>()
        try {
            val payload = getBasePayload().toMutableMap().apply {
                put("browseId", "FEmusic_home")
            }
            val response = client.browse(apiKey = null, requestBody = payload)
            val root = JSONObject(response.string())

            fun searchChips(obj: Any?) {
                if (obj is JSONObject) {
                    if (obj.has("chipCloudChipRenderer")) {
                        val renderer = obj.getJSONObject("chipCloudChipRenderer")
                        val title = renderer.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        val params = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("params")
                        if (!title.isNullOrEmpty() && !params.isNullOrEmpty()) {
                            chips.add(FilterChip(title, params))
                        }
                    }
                    obj.keys().forEach { key -> searchChips(obj.opt(key)) }
                } else if (obj is JSONArray) {
                    for (i in 0 until obj.length()) searchChips(obj.opt(i))
                }
            }
            searchChips(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext chips
    }

    suspend fun getFilteredHome(params: String): List<Shelf> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap().apply {
                put("browseId", "FEmusic_home")
                put("params", params)
            }
            val response = client.browse(apiKey = null, requestBody = payload)
            return@withContext parseShelves(response.string())
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getMoodsAndGenresCategories(): List<Shelf> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap().apply {
                put("browseId", "FEmusic_moods_and_genres")
            }
            val response = client.browse(apiKey = null, requestBody = payload)
            return@withContext parseShelves(response.string())
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getExplore(): List<Shelf> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload().toMutableMap().apply {
                put("browseId", "FEmusic_explore")
            }
            val response = client.browse(apiKey = null, requestBody = payload)
            return@withContext parseShelves(response.string())
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getTopCharts(countryCode: String = "US"): List<Shelf> = withContext(Dispatchers.IO) {
        try {
            val payload = getBasePayload(countryCode = countryCode).toMutableMap().apply {
                put("browseId", "FEmusic_charts")
            }
            val response = client.browse(apiKey = null, requestBody = payload)
            return@withContext parseShelves(response.string())
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun dislikeTrack(videoId: String): Boolean = withContext(Dispatchers.IO) {
        if (!com.aylis.comp.online.managers.AuthManager.isLoggedIn()) return@withContext false
        try {
            val payload = getBasePayload().toMutableMap().apply {
                put("target", mapOf("videoId" to videoId))
            }
            val response = client.dislike(payload)
            val body = response.string()
            return@withContext body.contains("STATUS_SUCCEEDED") || body.contains("\"status\":\"SUCCESS\"")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun fetchAccountProfile(): AccountProfileInfo? = withContext(Dispatchers.IO) {
        if (!com.aylis.comp.online.managers.AuthManager.isLoggedIn()) return@withContext null
        try {
            val payload = getBasePayload()
            val response = client.getAccountMenu(payload)
            val root = org.json.JSONObject(response.string())

            // Search for activeAccountHeaderRenderer
            var header: org.json.JSONObject? = null
            fun findHeader(obj: Any?) {
                if (obj is org.json.JSONObject) {
                    if (obj.has("activeAccountHeaderRenderer")) {
                        header = obj.getJSONObject("activeAccountHeaderRenderer")
                        return
                    }
                    obj.keys().forEach { key -> findHeader(obj.opt(key)) }
                } else if (obj is org.json.JSONArray) {
                    for (i in 0 until obj.length()) findHeader(obj.opt(i))
                }
            }
            findHeader(root)

            if (header != null) {
                val name = header?.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "YouTube User"
                val handle = header?.optJSONObject("channelHandle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                return@withContext AccountProfileInfo(name, handle)
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
