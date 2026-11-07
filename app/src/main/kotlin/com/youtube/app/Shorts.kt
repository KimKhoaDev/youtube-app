package com.youtube.app

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.youtube.app.liquidglass.LiquidButton
import com.youtube.app.liquidglass.LiquidSlider
import kotlinx.coroutines.delay

/** Full-screen vertical Shorts: swipe up/down, autoplay + loop, tap to pause, liquid glass controls. */
@Composable
fun ShortsScreen(settings: SettingsStore, lib: Library, nav: Nav) {
    val ctx = LocalContext.current
    val own = rememberLayerBackdrop()
    var shorts by remember { mutableStateOf<List<Video>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val player = remember { ExoPlayer.Builder(ctx).build().apply { repeatMode = Player.REPEAT_MODE_ONE } }

    DisposableEffect(Unit) {
        val l = object : Player.Listener { override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying } }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(settings.region) {
        runCatching { Repo.shorts() }.onSuccess { shorts = it; if (it.isEmpty()) error = "No Shorts found" }
            .onFailure { error = it.message ?: "Couldn't load Shorts" }
    }
    if (shorts.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Txt(error ?: "Loading Shorts…", Color.White.copy(0.7f), 15)
        }
        return
    }

    val state = rememberPagerState { shorts.size }
    val page = state.settledPage
    val cur = shorts[page]

    LaunchedEffect(page, shorts) {
        player.pause(); progress = 0f
        runCatching { Repo.open(cur.url) }.onSuccess { d ->
            d.hls?.let { url: String -> player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play() }
        }
    }
    LaunchedEffect(page) {
        while (true) { delay(200); val d = player.duration; if (d > 0) progress = (player.currentPosition.toFloat() / d).coerceIn(0f, 1f) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(state, Modifier.fillMaxSize().layerBackdrop(own), beyondViewportPageCount = 0) { i ->
            val v = shorts[i]
            Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { if (player.isPlaying) player.pause() else player.play() }) {
                RemoteImage(v.thumb, Modifier.fillMaxSize(), v)
                if (i == page) AndroidView({ c -> (LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView).also { it.player = player } }, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(0.65f))))
            }
        }

        // Caption + channel
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 96.dp, bottom = 176.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteImage(cur.avatar, Modifier.size(36.dp).clip(CircleShape).clickable { cur.channelUrl?.let { nav.openChannel(ChannelRef(it, cur.channel, cur.avatar)) } }, cur)
                Txt(cur.channel, Color.White, 15, bold = true, maxLines = 1, modifier = Modifier.weight(1f, false))
                val on = cur.channelUrl?.let { lib.isSubscribed(it) } == true
                LiquidButton({ cur.channelUrl?.let { lib.toggleSub(ChannelRef(it, cur.channel, cur.avatar)) } }, own, tint = if (on) Color.Unspecified else YT.Red) {
                    Txt(if (on) "Subscribed" else "Subscribe", Color.White, 13, bold = true, maxLines = 1)
                }
            }
            Txt(cur.title, Color.White, 15, maxLines = 2)
        }

        // Liquid glass action rail
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 176.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBtn(Ic.Like, own, tint = if (lib.isLiked(cur)) YT.Red else Color.Unspecified) { lib.toggleLike(cur) }
            IconBtn(Ic.Share, own) { shareVideo(ctx, cur) }
            IconBtn(Ic.Save, own) { nav.sheet(Sheet.AddTo(cur)) }
            IconBtn(Ic.More, own) { nav.menu(cur) }
        }

        if (!playing) IconBtn(Ic.Play, own, Modifier.align(Alignment.Center), 88.dp, 26.dp) { player.play() }

        // Liquid slider as the progress bar
        LiquidSlider(
            { progress }, { progress = it; if (player.duration > 0) player.seekTo((player.duration * it).toLong()) },
            0f..1f, 0.001f, own,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 108.dp)
        )
    }
}

