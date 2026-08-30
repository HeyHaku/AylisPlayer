package com.aylis.comp.online.repository

sealed class OnlineItem {
    abstract val title: String
    abstract val thumbnail: String
}

data class OnlineTrack(
    val videoId: String,
    override val title: String,
    val artist: String = "YouTube Music",
    override val thumbnail: String = ""
) : OnlineItem()

data class OnlinePlaylist(
    val browseId: String,
    override val title: String,
    val subtitle: String = "YouTube Music",
    override val thumbnail: String = ""
) : OnlineItem()

data class Shelf(
    val title: String,
    val items: List<OnlineItem>,
    val isImmersive: Boolean = false
)

// Чип фильтрации / модификатора
data class FilterChip(
    val title: String,
    val params: String
)

// Результат запроса радио/очереди с доступными модификаторами
data class RadioResult(
    val tracks: List<OnlineTrack>,
    val queueChips: List<FilterChip> // Чипы: "Familiar", "Discover", "Deep Cuts", "Instrumental"
)

// Элемент категории жанра/настроения
data class CategoryItem(
    val title: String,
    val params: String,
    val colorHex: String? = null
)

data class AccountProfileInfo(
    val name: String,
    val handle: String?
)
