package com.youtube.app

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

data class Video(
    val url: String,
    val title: String,
    val channel: String,
    val meta: String,
    val duration: String,
    val thumb: String?,
    val avatar: String? = null,
    val seconds: Long = 0,
    val channelUrl: String? = null,
    val localPath: String? = null
) {
    private val h get() = (url.hashCode() and 0x7fffffff)
    val c1: Color get() = palette[h % palette.size]
    val c2: Color get() = palette[(h / 7) % palette.size]
    companion object {
        val palette = listOf(0xFF0A84FF, 0xFF5E5CE6, 0xFFFF9F0A, 0xFFFF375F, 0xFF30D158, 0xFF64D2FF, 0xFFBF5AF2, 0xFFFF453A).map { Color(it) }
    }
}

data class ChannelRef(val url: String, val name: String, val avatar: String?, val subs: String = "")
data class PlaylistRef(val url: String, val name: String, val thumb: String?, val count: String)
data class Comment(val author: String, val avatar: String?, val text: String, val likes: Int, val date: String)
data class Q(val label: String, val height: Int, val url: String)
data class Chapter(val title: String, val startSec: Int)
data class SubTrack(val lang: String, val label: String, val url: String, val mime: String)

sealed interface Hit {
    data class V(val v: Video) : Hit
    data class C(val c: ChannelRef) : Hit
    data class P(val p: PlaylistRef) : Hit
}

class Detail(
    val title: String, val description: String, val views: String, val likes: String, val date: String,
    val uploader: ChannelRef, val qualities: List<Q>, val hls: String?, val subtitles: List<SubTrack>,
    val chapters: List<Chapter>, val related: List<Video>, val live: Boolean
)

class Playlist(val name: String, val videos: SnapshotStateList<Video>)

sealed interface Screen {
    data class Channel(val ref: ChannelRef) : Screen
    data class YtPlaylist(val ref: PlaylistRef) : Screen
    data class Local(val title: String, val kind: String) : Screen
    data object Search : Screen
    data object Settings : Screen
}

sealed interface Sheet {
    data class Menu(val v: Video) : Sheet
    data class Comments(val v: Video) : Sheet
    data class AddTo(val v: Video) : Sheet
    data object NewPlaylist : Sheet
}

class Nav(
    val openVideo: (Video) -> Unit,
    val openChannel: (ChannelRef) -> Unit,
    val menu: (Video) -> Unit,
    val sheet: (Sheet) -> Unit,
    val push: (Screen) -> Unit
)

val filterChips = listOf("All" to null, "Music" to "music", "Gaming" to "gaming", "Live" to "live", "News" to "news",
    "Podcasts" to "podcast", "Cooking" to "cooking", "Travel" to "travel", "Science" to "science", "Sports" to "sports")
val regions = listOf("US", "GB", "DE", "FR", "JP", "KR", "VN", "IN", "BR")
val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
