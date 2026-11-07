package com.youtube.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

class ChannelPage(val ref: ChannelRef, val banner: String?, val description: String, val videos: List<Video>)

object Repo {
    private val yt get() = ServiceList.YouTube

    fun init(region: String) = NewPipe.init(Net, Localization("en", region), ContentCountry(region))

    private fun fmt(s: Long) = if (s <= 0) "LIVE" else
        (s / 3600).let { h -> if (h > 0) "%d:%02d:%02d".format(h, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60) }

    fun compact(n: Long) = when {
        n < 0 -> ""; n >= 1_000_000_000 -> "%.1fB".format(n / 1e9); n >= 1_000_000 -> "%.1fM".format(n / 1e6)
        n >= 1_000 -> "%.1fK".format(n / 1e3); else -> "$n"
    }.replace(".0", "")

    private fun StreamInfoItem.toVideo() = Video(
        url, name, uploaderName ?: "",
        listOfNotNull(compact(viewCount).ifEmpty { null }?.let { "$it views" }, textualUploadDate).joinToString(" • "),
        fmt(duration), thumbnails.firstOrNull()?.url, uploaderAvatars.firstOrNull()?.url, duration, uploaderUrl
    )
    private fun ChannelInfoItem.toRef() = ChannelRef(url, name, thumbnails.firstOrNull()?.url, compact(subscriberCount).let { if (it.isEmpty()) "" else "$it subscribers" })
    private fun PlaylistInfoItem.toRef() = PlaylistRef(url, name, thumbnails.firstOrNull()?.url, if (streamCount >= 0) "$streamCount videos" else "")

    suspend fun trending(): List<Video> = withContext(Dispatchers.IO) {
        val k = yt.kioskList.getDefaultKioskExtractor(null)
        k.fetchPage()
        k.initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toVideo() }
    }

    suspend fun searchHits(q: String, filter: String): List<Hit> = withContext(Dispatchers.IO) {
        SearchInfo.getInfo(yt, yt.searchQHFactory.fromQuery(q, listOf(filter), "")).relatedItems.mapNotNull {
            when (it) {
                is StreamInfoItem -> Hit.V(it.toVideo())
                is ChannelInfoItem -> Hit.C(it.toRef())
                is PlaylistInfoItem -> Hit.P(it.toRef())
                else -> null
            }
        }
    }
    suspend fun search(q: String): List<Video> = searchHits(q, "videos").filterIsInstance<Hit.V>().map { it.v }

    suspend fun suggestions(q: String): List<String> = withContext(Dispatchers.IO) { yt.suggestionExtractor.suggestionList(q) }

    suspend fun shorts(): List<Video> = withContext(Dispatchers.IO) {
        listOf("#shorts", "shorts funny", "shorts satisfying", "shorts music").flatMap { q ->
            runCatching { search(q) }.getOrDefault(emptyList())
        }.filter { it.seconds in 1..70 }.distinctBy { it.url }.shuffled()
    }

    suspend fun channel(url: String): ChannelPage = withContext(Dispatchers.IO) {
        val info = ChannelInfo.getInfo(yt, url)
        val ref = ChannelRef(info.url, info.name, info.avatars.firstOrNull()?.url, compact(info.subscriberCount).let { if (it.isEmpty()) "" else "$it subscribers" })
        val tab = info.tabs.firstOrNull { it.contentFilters.contains(ChannelTabs.VIDEOS) }
        val vids = tab?.let { t ->
            ChannelTabInfo.getInfo(yt, t).relatedItems.filterIsInstance<StreamInfoItem>()
                .map { it.toVideo().copy(channel = ref.name, avatar = ref.avatar, channelUrl = ref.url) }
        } ?: emptyList()
        ChannelPage(ref, info.banners.firstOrNull()?.url, info.description ?: "", vids)
    }

    /** Latest uploads from subscribed channels, interleaved. */
    suspend fun subsFeed(subs: List<ChannelRef>): List<Video> = coroutineScope {
        val lists = subs.take(25).map { s -> async { runCatching { channel(s.url).videos.take(4) }.getOrDefault(emptyList()) } }.awaitAll()
        (0 until 4).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }
    }

    suspend fun playlist(url: String): List<Video> = withContext(Dispatchers.IO) {
        PlaylistInfo.getInfo(yt, url).relatedItems.filterIsInstance<StreamInfoItem>().map { it.toVideo() }
    }

    suspend fun comments(url: String): List<Comment> = withContext(Dispatchers.IO) {
        CommentsInfo.getInfo(yt, url).relatedItems.map {
            Comment(it.uploaderName ?: "", it.uploaderAvatars.firstOrNull()?.url, it.commentText?.content ?: "", it.likeCount, it.textualUploadDate ?: "")
        }
    }

    suspend fun open(url: String): Detail = withContext(Dispatchers.IO) {
        val i = StreamInfo.getInfo(yt, url)
        val qs = i.videoStreams.filter { !it.isVideoOnly && it.content.isNotEmpty() && it.height > 0 }
            .distinctBy { it.height }.sortedByDescending { it.height }.map { Q("${it.height}p", it.height, it.content) }
        Detail(
            i.name, i.description?.content ?: "", compact(i.viewCount).let { if (it.isEmpty()) "" else "$it views" },
            compact(i.likeCount), i.textualUploadDate ?: "",
            ChannelRef(i.uploaderUrl ?: "", i.uploaderName ?: "", i.uploaderAvatars.firstOrNull()?.url,
                compact(i.uploaderSubscriberCount).let { if (it.isEmpty()) "" else "$it subscribers" }),
            qs, i.hlsUrl.takeIf { it.isNotEmpty() },
            i.subtitles.take(10).map { SubTrack(it.languageTag, it.displayLanguageName, it.content, it.format?.mimeType ?: "text/vtt") },
            i.streamSegments.map { Chapter(it.title ?: "", it.startTimeSeconds) },
            i.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toVideo() },
            i.streamType == StreamType.LIVE_STREAM
        )
    }
}
