package com.youtube.app

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.youtube.app.liquidglass.LiquidButton
import kotlinx.coroutines.launch

@Composable
fun SheetHost(bd: Backdrop, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f)).clickable(interactionSource = null, indication = null, onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp)
                .panel(bd).clickable(interactionSource = null, indication = null) {}.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SheetRow(ic: Ic, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(ic, YT.text(), 22.dp); Txt(label, YT.text(), 16, bold = true)
    }
}

@Composable
fun SheetContent(s: Sheet, bd: Backdrop, lib: Library, nav: Nav, settings: SettingsStore, pb: Playback, close: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    when (s) {
        is Sheet.Menu -> {
            val v = s.v
            SheetRow(Ic.Save, if (lib.inWatchLater(v)) "Remove from Watch later" else "Save to Watch later") { lib.toggleWatchLater(v); close() }
            SheetRow(Ic.Save, "Add to playlist") { nav.sheet(Sheet.AddTo(v)) }
            if (v.localPath == null) SheetRow(Ic.Download, "Download") { close(); scope.launch { toast(ctx, runCatching { Downloads.start(ctx, lib, v, settings.dataSaver) }.getOrElse { "Download failed" }) } }
            SheetRow(Ic.Share, "Share") { shareVideo(ctx, v); close() }
            v.channelUrl?.let { u -> SheetRow(Ic.You, "Go to ${v.channel}") { close(); nav.openChannel(ChannelRef(u, v.channel, v.avatar)) } }
            if (v.localPath != null) SheetRow(Ic.Close, "Remove download") { lib.removeDownload(v); close() }
        }
        is Sheet.AddTo -> {
            val v = s.v
            Txt("Save to…", YT.text(), 18, bold = true, modifier = Modifier.padding(bottom = 6.dp))
            SheetRow(if (lib.inWatchLater(v)) Ic.Check else Ic.Save, "Watch later") { lib.toggleWatchLater(v) }
            lib.playlists.forEach { pl -> SheetRow(if (lib.inPlaylist(pl, v)) Ic.Check else Ic.Save, pl.name) { lib.togglePlaylist(pl, v) } }
            SheetRow(Ic.Check, "New playlist") { nav.sheet(Sheet.NewPlaylist) }
        }
        Sheet.NewPlaylist -> {
            var name by remember { mutableStateOf("") }
            Txt("New playlist", YT.text(), 18, bold = true, modifier = Modifier.padding(bottom = 10.dp))
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (name.isEmpty()) Txt("Playlist name", YT.sub(), 16)
                BasicTextField(name, { name = it }, singleLine = true, textStyle = TextStyle(YT.text(), 16.sp), cursorBrush = SolidColor(YT.Red), modifier = Modifier.fillMaxWidth())
            }
            LiquidButton({ lib.createPlaylist(name); close() }, bd, Modifier.fillMaxWidth(), tint = YT.Red) { Txt("Create", Color.White, 15, bold = true) }
        }
        is Sheet.Comments -> {
            var list by remember { mutableStateOf<List<Comment>?>(null) }
            var err by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(s.v.url) { runCatching { Repo.comments(s.v.url) }.onSuccess { list = it }.onFailure { err = "Comments are unavailable for this video"; list = emptyList() } }
            Txt("Comments", YT.text(), 18, bold = true, modifier = Modifier.padding(bottom = 8.dp))
            Box(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                if (list == null) Txt("Loading…", YT.sub(), 14)
                else if (list!!.isEmpty()) Txt(err ?: "No comments yet", YT.sub(), 14)
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(list!!) { c ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RemoteImage(c.avatar, Modifier.size(34.dp).clip(CircleShape).background(Color.Gray))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Txt("${c.author} • ${c.date}", YT.sub(), 12, maxLines = 1)
                                Txt(c.text, YT.text(), 14)
                                if (c.likes > 0) Txt("♥ ${c.likes}", YT.sub(), 12)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareVideo(ctx: android.content.Context, v: Video) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${v.title}\n${v.url}")
    ctx.startActivity(Intent.createChooser(i, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
