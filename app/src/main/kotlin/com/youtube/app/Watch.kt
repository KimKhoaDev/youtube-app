package com.youtube.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.util.Rational
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.youtube.app.liquidglass.LiquidButton
import com.youtube.app.liquidglass.LiquidSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? { var c = this; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }

@Composable
fun WatchScreen(pb: Playback, settings: SettingsStore, lib: Library, nav: Nav) {
    val v = pb.current ?: return
    val d = pb.detail
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val act = remember(ctx) { ctx.findActivity() }
    val playerBd = rememberLayerBackdrop()
    val listBd = rememberLayerBackdrop()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var full by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    BackHandler { pb.minimized = true }
    BackHandler(enabled = full) { full = false }
    DisposableEffect(full) {
        val ctl = act?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (full) {
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ctl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ctl?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(controls, pb.playing) { if (controls && pb.playing) { delay(3500); controls = false } }
    val showControls = controls || !pb.playing || pb.loading

    BoxWithConstraints(Modifier.fillMaxSize().background(YT.bg()).clickable(interactionSource = null, indication = null) {}) {
        if (full) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                PlayerLayer(pb, v, playerBd, { controls = !controls }, Modifier.fillMaxSize())
                if (showControls) PlayerControls(pb, playerBd, true, { full = false }, { full = false }, Modifier.fillMaxSize())
            }
        } else {
            val playerH = maxWidth * 9f / 16f
            LazyColumn(
                Modifier.fillMaxSize().layerBackdrop(listBd),
                contentPadding = PaddingValues(top = top + playerH + 64.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                item {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Txt(d?.title ?: v.title, YT.text(), 18, bold = true, modifier = Modifier.clickable { expanded = !expanded })
                        Txt(listOfNotNull(d?.views?.ifEmpty { null }, d?.date?.ifEmpty { null } ?: v.meta).joinToString(" • "), YT.sub(), 13)
                        val up = d?.uploader
                        val chUrl = up?.url?.ifEmpty { null } ?: v.channelUrl
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.weight(1f).clickable { chUrl?.let { nav.openChannel(ChannelRef(it, up?.name ?: v.channel, up?.avatar ?: v.avatar)) } }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RemoteImage(up?.avatar ?: v.avatar, Modifier.size(40.dp).clip(CircleShape), v)
                                Column {
                                    Txt(up?.name?.ifEmpty { null } ?: v.channel, YT.text(), 15, bold = true, maxLines = 1)
                                    if (!up?.subs.isNullOrEmpty()) Txt(up!!.subs, YT.sub(), 12)
                                }
                            }
                            if (chUrl != null) {
                                val sub = lib.isSubscribed(chUrl)
                                Box(
                                    Modifier.height(36.dp).clip(RoundedCornerShape(50)).background(if (sub) YT.sub().copy(0.25f) else YT.text())
                                        .clickable { lib.toggleSub(ChannelRef(chUrl, up?.name ?: v.channel, up?.avatar ?: v.avatar, up?.subs ?: "")) }.padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) { Txt(if (sub) "Subscribed" else "Subscribe", if (sub) YT.text() else YT.bg(), 14, bold = true) }
                            }
                        }
                        if (expanded && d != null) {
                            if (d.description.isNotBlank()) Txt(d.description, YT.text(), 14)
                            d.chapters.forEach { c ->
                                Txt("${clock(c.startSec * 1000L)}  ${c.title}", YT.sub(), 14, modifier = Modifier.fillMaxWidth().clickable { pb.player.seekTo(c.startSec * 1000L) }.padding(vertical = 4.dp))
                            }
                        } else if (d != null && d.description.isNotBlank()) Txt(d.description, YT.sub(), 13, maxLines = 2, modifier = Modifier.clickable { expanded = true })
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(YT.sub().copy(0.12f)).clickable { nav.sheet(Sheet.Comments(v)) }.padding(14.dp)) {
                            Column { Txt("Comments", YT.text(), 15, bold = true); Txt("Tap to read what people are saying", YT.sub(), 12) }
                        }
                        pb.error?.let { Txt(it, YT.Red, 13) }
                    }
                }
                items(d?.related ?: emptyList()) { VideoCard(it, nav) }
            }

            Column(Modifier.fillMaxWidth().background(YT.bg())) {
                Spacer(Modifier.height(top))
                PlayerLayer(pb, v, playerBd, { controls = !controls }, Modifier.fillMaxWidth().height(playerH))
            }
            if (showControls) PlayerControls(pb, playerBd, false, { full = true }, { pb.minimized = true }, Modifier.fillMaxWidth().padding(top = top).height(playerH))

            LazyRow(Modifier.padding(top = top + playerH + 8.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val liked = lib.isLiked(v)
                    Pill(listBd, tint = if (liked) YT.Red else Color.Unspecified, onClick = { lib.toggleLike(v) }) {
                        Icon(Ic.Like, if (liked) Color.White else YT.text(), 20.dp); Txt(d?.likes?.ifEmpty { null } ?: "Like", if (liked) Color.White else YT.text(), 14, bold = true)
                    }
                }
                item { Pill(listBd, onClick = { nav.sheet(Sheet.AddTo(v)) }) { Icon(Ic.Save, YT.text(), 20.dp); Txt("Save", YT.text(), 14, bold = true) } }
                item { Pill(listBd, onClick = { shareVideo(ctx, v) }) { Icon(Ic.Share, YT.text(), 20.dp); Txt("Share", YT.text(), 14, bold = true) } }
                if (v.localPath == null) item {
                    Pill(listBd, onClick = { scope.launch { toast(ctx, runCatching { Downloads.start(ctx, lib, v, settings.dataSaver) }.getOrElse { "Download failed" }) } }) {
                        Icon(Ic.Download, YT.text(), 20.dp); Txt("Download", YT.text(), 14, bold = true)
                    }
                }
                item { Pill(listBd, onClick = { pb.setSpeed(speeds[(speeds.indexOf(pb.speed) + 1) % speeds.size]) }) { Txt("${pb.speed}x", YT.text(), 14, bold = true) } }
                if (d != null && d.qualities.size > 1 && !d.live) item {
                    Pill(listBd, onClick = { pb.setQuality((pb.quality + 1) % d.qualities.size, settings) }) { Txt(d.qualities.getOrNull(pb.quality)?.label ?: "Auto", YT.text(), 14, bold = true) }
                }
                if (d != null && d.subtitles.isNotEmpty()) item {
                    Pill(listBd, tint = if (pb.captions) YT.Red else Color.Unspecified, onClick = { pb.setCaptions(!pb.captions) }) { Txt("CC", if (pb.captions) Color.White else YT.text(), 14, bold = true) }
                }
                item {
                    Pill(listBd, onClick = { act?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) }) { Txt("PiP", YT.text(), 14, bold = true) }
                }
            }
        }
    }
}

@Composable
private fun PlayerLayer(pb: Playback, v: Video, bd: LayerBackdrop, onTap: () -> Unit, modifier: Modifier) {
    Box(modifier.background(Color.Black).layerBackdrop(bd)) {
        RemoteImage(v.thumb, Modifier.fillMaxSize())
        AndroidView({ c -> (LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView).also { it.player = pb.player } }, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onTap = { onTap() }, onDoubleTap = { o -> pb.seekBy(if (o.x < size.width / 2) -10_000 else 10_000) })
        })
    }
}

@Composable
private fun PlayerControls(pb: Playback, bd: Backdrop, full: Boolean, onFull: () -> Unit, onBack: () -> Unit, modifier: Modifier) {
    val glass = Color.Black.copy(0.2f)
    val progress = if (pb.duration > 0) (pb.position.toFloat() / pb.duration).coerceIn(0f, 1f) else 0f
    Box(modifier) {
        IconBtn(Ic.Back, bd, Modifier.align(Alignment.TopStart).padding(10.dp), tint = glass, onClick = onBack)
        if (pb.loading) Txt("Loading…", Color.White, 14, bold = true, modifier = Modifier.align(Alignment.Center))
        else Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LiquidButton({ pb.seekBy(-10_000) }, bd, tint = glass) { Txt("−10", Color.White, 14, bold = true) }
            IconBtn(if (pb.playing) Ic.Pause else Ic.Play, bd, width = 88.dp, iconSize = 26.dp, tint = glass) { pb.toggle() }
            LiquidButton({ pb.seekBy(10_000) }, bd, tint = glass) { Txt("+10", Color.White, 14, bold = true) }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(if (pb.duration > 0) "${clock(pb.position)} / ${clock(pb.duration)}" else "LIVE", Color.White, 12, bold = true, modifier = Modifier.weight(1f))
                IconBtn(Ic.Full, bd, width = 52.dp, iconSize = 18.dp, tint = glass, onClick = onFull)
            }
            LiquidSlider({ progress }, { if (pb.duration > 0) pb.player.seekTo((pb.duration * it).toLong()) }, 0f..1f, 0.001f, bd, Modifier.fillMaxWidth())
        }
    }
}
