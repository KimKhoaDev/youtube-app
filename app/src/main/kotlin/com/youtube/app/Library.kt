package com.youtube.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/** Everything the user keeps on-device: history, watch later, likes, downloads, playlists, subscriptions. */
class Library(ctx: Context) {
    private val p = ctx.getSharedPreferences("library", Context.MODE_PRIVATE)
    val history = mutableStateListOf<Video>()
    val watchLater = mutableStateListOf<Video>()
    val liked = mutableStateListOf<Video>()
    val downloads = mutableStateListOf<Video>()
    val playlists = mutableStateListOf<Playlist>()
    val subs = mutableStateListOf<ChannelRef>()
    val searches = mutableStateListOf<String>()

    init {
        runCatching {
            readVideos("history", history); readVideos("later", watchLater); readVideos("liked", liked); readVideos("downloads", downloads)
            JSONArray(p.getString("playlists", "[]")).let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i); val l = mutableStateListOf<Video>()
                    val vs = o.getJSONArray("v"); for (j in 0 until vs.length()) l.add(fromJson(vs.getJSONObject(j)))
                    playlists.add(Playlist(o.getString("name"), l))
                }
            }
            JSONArray(p.getString("subs", "[]")).let { a -> for (i in 0 until a.length()) a.getJSONObject(i).let { o -> subs.add(ChannelRef(o.getString("u"), o.getString("n"), o.optString("a").ifEmpty { null }, o.optString("s"))) } }
            JSONArray(p.getString("searches", "[]")).let { a -> for (i in 0 until a.length()) searches.add(a.getString(i)) }
        }
    }

    private fun toJson(v: Video) = JSONObject().put("u", v.url).put("t", v.title).put("c", v.channel).put("m", v.meta).put("d", v.duration)
        .put("th", v.thumb ?: "").put("a", v.avatar ?: "").put("s", v.seconds).put("cu", v.channelUrl ?: "").put("lp", v.localPath ?: "")
    private fun fromJson(o: JSONObject) = Video(o.getString("u"), o.getString("t"), o.getString("c"), o.getString("m"), o.getString("d"),
        o.optString("th").ifEmpty { null }, o.optString("a").ifEmpty { null }, o.optLong("s"), o.optString("cu").ifEmpty { null }, o.optString("lp").ifEmpty { null })
    private fun readVideos(key: String, into: MutableList<Video>) {
        val a = JSONArray(p.getString(key, "[]")); for (i in 0 until a.length()) into.add(fromJson(a.getJSONObject(i)))
    }
    private fun arr(l: List<Video>) = JSONArray().also { a -> l.forEach { a.put(toJson(it)) } }

    private fun save() {
        p.edit()
            .putString("history", arr(history).toString()).putString("later", arr(watchLater).toString())
            .putString("liked", arr(liked).toString()).putString("downloads", arr(downloads).toString())
            .putString("playlists", JSONArray().also { a -> playlists.forEach { a.put(JSONObject().put("name", it.name).put("v", arr(it.videos))) } }.toString())
            .putString("subs", JSONArray().also { a -> subs.forEach { a.put(JSONObject().put("u", it.url).put("n", it.name).put("a", it.avatar ?: "").put("s", it.subs)) } }.toString())
            .putString("searches", JSONArray(searches.toList()).toString())
            .apply()
    }

    private fun toggle(list: MutableList<Video>, v: Video) { val i = list.indexOfFirst { it.url == v.url }; if (i >= 0) list.removeAt(i) else list.add(0, v); save() }

    fun addHistory(v: Video) { history.removeAll { it.url == v.url }; history.add(0, v); while (history.size > 300) history.removeAt(history.lastIndex); save() }
    fun clearHistory() { history.clear(); save() }
    fun isLiked(v: Video) = liked.any { it.url == v.url }
    fun toggleLike(v: Video) = toggle(liked, v)
    fun inWatchLater(v: Video) = watchLater.any { it.url == v.url }
    fun toggleWatchLater(v: Video) = toggle(watchLater, v)
    fun addDownload(v: Video) { downloads.removeAll { it.url == v.url }; downloads.add(0, v); save() }
    fun removeDownload(v: Video) { downloads.removeAll { it.url == v.url }; save() }

    fun createPlaylist(name: String) { if (name.isNotBlank() && playlists.none { it.name == name }) { playlists.add(Playlist(name.trim(), mutableStateListOf())); save() } }
    fun inPlaylist(pl: Playlist, v: Video) = pl.videos.any { it.url == v.url }
    fun togglePlaylist(pl: Playlist, v: Video) { toggle(pl.videos, v) }
    fun deletePlaylist(pl: Playlist) { playlists.remove(pl); save() }

    fun isSubscribed(url: String) = subs.any { it.url == url }
    fun toggleSub(c: ChannelRef) { val i = subs.indexOfFirst { it.url == c.url }; if (i >= 0) subs.removeAt(i) else subs.add(0, c); save() }
    fun mergeSubs(list: List<ChannelRef>) { list.forEach { c -> if (subs.none { it.url == c.url }) subs.add(c) }; save() }

    fun rememberSearch(q: String) { searches.remove(q); searches.add(0, q); while (searches.size > 20) searches.removeAt(searches.lastIndex); save() }
    fun clearSearches() { searches.clear(); save() }
}
