package com.youtube.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/** App-wide player so playback survives minimising to the mini-player. */
class Playback(private val ctx: Context) {
    val player = ExoPlayer.Builder(ctx).build()
    var current by mutableStateOf<Video?>(null)
    var detail by mutableStateOf<Detail?>(null)
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var minimized by mutableStateOf(false)
    var speed by mutableFloatStateOf(1f)
    var quality by mutableIntStateOf(0)
    var captions by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var onEnded: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlayerError(e: PlaybackException) { error = "Playback error (${e.errorCodeName})" }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) onEnded?.invoke() }
        })
    }

    fun tick() { position = player.currentPosition; duration = player.duration.coerceAtLeast(0) }
    fun seekBy(ms: Long) { player.seekTo((player.currentPosition + ms).coerceIn(0, player.duration.coerceAtLeast(0))) }
    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun release() = player.release()

    suspend fun open(v: Video, s: SettingsStore) {
        current = v; detail = null; error = null; loading = true; minimized = false; position = 0; duration = 0
        speed = s.speed; captions = s.captions
        player.stop()
        val local = v.localPath?.let { File(it) }?.takeIf { it.exists() }
        if (local != null) {
            start(MediaItem.fromUri(Uri.fromFile(local)), 0, s.autoplay); loading = false; return
        }
        runCatching { Repo.open(v.url) }.onSuccess { d ->
            detail = d
            quality = pick(d, s.dataSaver)
            play(d, quality, 0, s.autoplay)
        }.onFailure { error = it.message ?: "Couldn't open video" }
        loading = false
    }

    private fun pick(d: Detail, saver: Boolean): Int {
        val cap = if (saver) 360 else 720
        return d.qualities.indexOfFirst { it.height <= cap }.takeIf { it >= 0 } ?: (d.qualities.size - 1).coerceAtLeast(0)
    }

    private fun play(d: Detail, idx: Int, startMs: Long, auto: Boolean) {
        val url = if (d.live) d.hls else d.qualities.getOrNull(idx)?.url ?: d.hls
        if (url == null) { error = "No playable stream"; return }
        val supported = setOf(MimeTypes.TEXT_VTT, MimeTypes.APPLICATION_TTML, MimeTypes.APPLICATION_SUBRIP)
        val item = MediaItem.Builder().setUri(url).setSubtitleConfigurations(d.subtitles.map {
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(it.url)).setMimeType(if (it.mime in supported) it.mime else MimeTypes.TEXT_VTT)
                .setLanguage(it.lang).setLabel(it.label).build()
        }).build()
        start(item, startMs, auto)
    }

    private fun start(item: MediaItem, startMs: Long, auto: Boolean) {
        player.setMediaItem(item, startMs); player.prepare()
        player.setPlaybackSpeed(speed); setCaptions(captions)
        player.playWhenReady = auto
    }

    fun setQuality(i: Int, s: SettingsStore) { val d = detail ?: return; quality = i; play(d, i, player.currentPosition, true) }
    @JvmName("setSpeedPref")
    fun setSpeed(f: Float) { speed = f; player.setPlaybackSpeed(f) }
    @JvmName("setCaptionsPref")
    fun setCaptions(on: Boolean) {
        captions = on
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !on).setPreferredTextLanguage("en").build()
    }
    fun close() { player.stop(); current = null; detail = null; error = null }
}

object Downloads {
    /** Downloads the chosen quality into the app's files and records it in the library. */
    suspend fun start(ctx: Context, lib: Library, v: Video, saver: Boolean): String {
        val d = Repo.open(v.url)
        val cap = if (saver) 360 else 720
        val q = d.qualities.firstOrNull { it.height <= cap } ?: d.qualities.lastOrNull() ?: return "No downloadable stream"
        val dir = ctx.getExternalFilesDir("downloads") ?: return "No storage available"
        val file = File(dir, "${v.url.hashCode().toUInt()}.mp4")
        val req = DownloadManager.Request(Uri.parse(q.url)).setTitle(v.title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
        (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        lib.addDownload(v.copy(localPath = file.absolutePath))
        return "Downloading ${q.label}…"
    }
}
