package com.aylis.comp.online.managers

import com.aylis.Common.Events.WeakEvent5
import com.aylis.Common.Tuple2
import com.aylis.comp.Common.IItemIdentifier
import com.aylis.comp.PlaybackQueue.QueueCore
import com.aylis.comp.playback.Song.PlaylistSong
import com.aylis.comp.online.repository.OnlineMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlaySource {
    ALBUM, PLAYLIST, FOLDER, ONLINE_RADIO, SINGLE_TRACK, ONLINE_SHELF
}

object OnlinePlaybackManager {

    fun init() {
        // Listeners for infinite radio auto-fetch removed to prevent global side effects.
    }

    // Обычное воспроизведение: загружает ВЕСЬ переданный список, ставит нужный индекс.
    // ЗДЕСЬ СТРОГО ЗАПРЕЩЕНО вызывать getSongRadio() или добавлять треки в хвост!
    fun playQueue(
        tracks: List<com.aylis.comp.online.repository.OnlineTrack>,
        startIndex: Int = 0,
        source: PlaySource = PlaySource.SINGLE_TRACK
    ) {
        if (tracks.isEmpty()) return

        val songs = tracks.map { targetTrack ->
            PlaylistSong(
                -1L,
                "ytsearch://${targetTrack.videoId}",
                targetTrack.title,
                targetTrack.artist,
                0,
                targetTrack.thumbnail
            )
        }

        // Standard playback - just start the queue
        com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase.onOpen2.invoke(songs, startIndex, null)

        tracks.getOrNull(startIndex)?.let {
            StatsManager.addPlay(it.videoId, it.title, it.artist)
        }
    }

    // Вызывается ИСКЛЮЧИТЕЛЬНО из блока Endless Recommendations!
    fun startEndlessRadioSession(initialTrack: com.aylis.comp.online.repository.OnlineTrack) {
        val initialSong = PlaylistSong(
            -1L,
            "ytsearch://${initialTrack.videoId}",
            initialTrack.title,
            initialTrack.artist,
            0,
            initialTrack.thumbnail
        )

        // 1. Запускаем выбранный трек (перезаписывая очередь)
        com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase.onOpen2.invoke(listOf(initialSong), 0, null)
        StatsManager.addPlay(initialTrack.videoId, initialTrack.title, initialTrack.artist)

        // 2. В фоне подтягиваем радио и докидываем в хвост
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val radio = OnlineMusicRepository.getSongRadio(initialTrack.videoId)
                val queueTracks = radio.tracks.filter { it.videoId != initialTrack.videoId }

                if (queueTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val newSongs = queueTracks.map {
                            PlaylistSong(-1L, "ytsearch://${it.videoId}", it.title, it.artist, 0, it.thumbnail)
                        }
                        QueueCore.createOrGetInstance()?.enqueue(newSongs, QueueCore.LAST)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playOnlineTrack(targetTrack: com.aylis.comp.online.repository.OnlineTrack, startRadio: Boolean = true) {
        if (startRadio) {
            startEndlessRadioSession(targetTrack)
        } else {
            playQueue(listOf(targetTrack), 0, PlaySource.SINGLE_TRACK)
        }
    }
}
