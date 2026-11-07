package com.youtube.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.youtube.app.liquidglass.LiquidButton
import com.youtube.app.liquidglass.LiquidToggle
import com.youtube.app.IconBtn
import com.youtube.app.Ic
import com.youtube.app.YT
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme

@Composable private fun statusTop() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** Generic video list page: local lists (live) or online (load). */
@Composable
fun ListScreen(title: String, nav: Nav, onBack: () -> Unit, live: List<Video>? = null, load: (suspend () -> List<Video>)? = null, onClear: (() -> Unit)? = null) {
    BackHandler(onBack = onBack)
    val bd = rememberLayerBackdrop()
    val top = statusTop()
    var items by remember { mutableStateOf<List<Video>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(load != null) }
    LaunchedEffect(Unit) { if (load != null) { runCatching { load() }.onSuccess { items = it }.onFailure { err = it.message ?: "Couldn't load" }; busy = false } }
    val list = live ?: items
    Box(Modifier.fillMaxSize().background(YT.bg())) {
        Box(Modifier.fillMaxSize().layerBackdrop(bd)) { VideoList(list, top + 70.dp, nav, bottomPad = 24.dp) }
        if (busy) Txt("Loading…", YT.sub(), 15, modifier = Modifier.align(Alignment.Center))
        else if (list.isEmpty()) Txt(err ?: "Nothing here yet", YT.sub(), 15, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        Row(Modifier.fillMaxWidth().padding(top = top + 6.dp, start = 12.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Ic.Back, bd, onClick = onBack)
            LiquidButton({}, bd, Modifier.weight(1f), isInteractive = false) { Txt(title, YT.text(), 16, bold = true, maxLines = 1) }
            if (onClear != null) IconBtn(Ic.Close, bd, onClick = onClear)
        }
    }
}

@Composable
fun ChannelScreen(ref: ChannelRef, lib: Library, nav: Nav, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val bd = rememberLayerBackdrop()
    val top = statusTop()
    var page by remember { mutableStateOf<ChannelPage?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ref.url) { runCatching { Repo.channel(ref.url) }.onSuccess { page = it }.onFailure { err = it.message ?: "Couldn't load channel" } }
    val r = page?.ref ?: ref
    val subbed = lib.isSubscribed(r.url)
    Box(Modifier.fillMaxSize().background(YT.bg())) {
        Box(Modifier.fillMaxSize().layerBackdrop(bd)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                item {
                    RemoteImage(page?.banner, Modifier.fillMaxWidth().height(top + 110.dp).background(Brush.linearGradient(listOf(YT.Red.copy(0.6f), Color(0xFF5E5CE6)))))
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(r.avatar, Modifier.size(72.dp).clip(CircleShape).background(Color.Gray))
                        Column { Txt(r.name, YT.text(), 22, bold = true); if (r.subs.isNotEmpty()) Txt(r.subs, YT.sub(), 13) }
                    }
                    page?.description?.takeIf { it.isNotBlank() }?.let { Txt(it, YT.sub(), 13, maxLines = 3, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                }
                if (page == null && err == null) item { Txt("Loading…", YT.sub(), 14, modifier = Modifier.padding(16.dp)) }
                err?.let { e -> item { Txt(e, YT.sub(), 14, modifier = Modifier.padding(16.dp)) } }
                items(page?.videos ?: emptyList()) { VideoCard(it, nav) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = top + 6.dp, start = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Ic.Back, bd, onClick = onBack)
            Spacer(Modifier.weight(1f))
            LiquidButton({ lib.toggleSub(r) }, bd, tint = if (subbed) Color.Unspecified else YT.Red) {
                Txt(if (subbed) "Subscribed" else "Subscribe", if (subbed) YT.text() else Color.White, 14, bold = true)
            }
        }
    }
}

@Composable
fun SearchScreen(lib: Library, nav: Nav, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val bd = rememberLayerBackdrop()
    val top = statusTop()
    var q by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableIntStateOf(0) }
    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }
    var sugg by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val filters = listOf("videos" to "Videos", "channels" to "Channels", "playlists" to "Playlists")

    fun run(query: String) {
        if (query.isBlank()) return
        q = query; submitted = query; lib.rememberSearch(query)
        scope.launch {
            busy = true; err = null
            runCatching { Repo.searchHits(query, filters[filter].first) }.onSuccess { hits = it }.onFailure { err = it.message ?: "Search failed"; hits = emptyList() }
            busy = false
        }
    }
    LaunchedEffect(filter) { submitted?.let { run(it) } }
    LaunchedEffect(q) { if (q != submitted && q.isNotBlank()) { delay(200); sugg = runCatching { Repo.suggestions(q) }.getOrDefault(emptyList()) } }

    Box(Modifier.fillMaxSize().background(YT.bg())) {
        Box(Modifier.fillMaxSize().layerBackdrop(bd)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top + if (submitted != null) 130.dp else 76.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                if (submitted == null || q != submitted) {
                    val rows = if (q.isBlank()) lib.searches.toList() else sugg
                    items(rows) { s -> Txt(s, YT.text(), 16, modifier = Modifier.fillMaxWidth().clickable { run(s) }.padding(horizontal = 20.dp, vertical = 12.dp)) }
                } else items(hits) { h ->
                    when (h) {
                        is Hit.V -> VideoCard(h.v, nav)
                        is Hit.C -> Row(Modifier.fillMaxWidth().clickable { nav.openChannel(h.c) }.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RemoteImage(h.c.avatar, Modifier.size(56.dp).clip(CircleShape).background(Color.Gray))
                            Column { Txt(h.c.name, YT.text(), 16, bold = true); Txt(h.c.subs, YT.sub(), 12) }
                        }
                        is Hit.P -> Row(Modifier.fillMaxWidth().clickable { nav.push(Screen.YtPlaylist(h.p)) }.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RemoteImage(h.p.thumb, Modifier.size(width = 112.dp, height = 63.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(Color.Gray))
                            Column { Txt(h.p.name, YT.text(), 15, bold = true, maxLines = 2); Txt(h.p.count, YT.sub(), 12) }
                        }
                    }
                }
            }
        }
        if (busy) Txt("Searching…", YT.sub(), 15, modifier = Modifier.align(Alignment.Center))
        err?.let { Txt(it, YT.sub(), 14, modifier = Modifier.align(Alignment.Center).padding(24.dp)) }
        Column(Modifier.fillMaxWidth().padding(top = top + 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBtn(Ic.Back, bd, onClick = onBack)
                LiquidButton({}, bd, Modifier.weight(1f), isInteractive = false) {
                    Icon(Ic.Search, YT.sub(), 20.dp)
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Txt("Search YouTube", YT.sub(), 16)
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(YT.text(), 16.sp), cursorBrush = SolidColor(YT.Red),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { run(q) }), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (submitted != null) LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters.indices.toList()) { i -> Chip(filters[i].second, filter == i, bd) { filter = i } }
            }
        }
    }
}

@Composable
private fun GlassBackdrop(bd: LayerBackdrop) {
    Canvas(Modifier.fillMaxSize().layerBackdrop(bd)) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1030), Color(0xFF0B1B3A))))
        drawCircle(Color(0xFFFF0033).copy(0.55f), size.width * .5f, Offset(size.width * .1f, size.height * .2f))
        drawCircle(Color(0xFF0A84FF).copy(0.55f), size.width * .55f, Offset(size.width * .95f, size.height * .5f))
        drawCircle(Color(0xFFBF5AF2).copy(0.5f), size.width * .5f, Offset(size.width * .3f, size.height * .9f))
    }
}

@Composable
private fun LibRow(bd: Backdrop, ic: Ic, title: String, count: String = "", onClick: () -> Unit) {
    LiquidButton(onClick, bd, Modifier.fillMaxWidth()) {
        Icon(ic, Color.White, 22.dp)
        Txt(title, Color.White, 16, bold = true, maxLines = 1, modifier = Modifier.weight(1f))
        if (count.isNotEmpty()) Txt(count, Color.White.copy(0.7f), 14)
    }
}

/** "You" tab: account + library. */
@Composable
fun LibraryScreen(settings: SettingsStore, lib: Library, nav: Nav, onSignIn: () -> Unit) {
    val ctx = LocalContext.current
    val bd = rememberLayerBackdrop()
    val top = statusTop()
    val acc = Auth.account
    Box(Modifier.fillMaxSize()) {
        GlassBackdrop(bd)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = top + 70.dp, bottom = 130.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (acc?.photoUrl != null) RemoteImage(acc.photoUrl.toString(), Modifier.size(64.dp).clip(CircleShape).background(Color.Gray))
                else Box(Modifier.size(64.dp).clip(CircleShape).background(YT.Red), contentAlignment = Alignment.Center) { Txt("Y", Color.White, 26, bold = true) }
                Column(Modifier.weight(1f)) {
                    Txt(acc?.displayName ?: "Not signed in", Color.White, 20, bold = true)
                    Txt(acc?.email ?: "Sign in to import your subscriptions", Color.White.copy(0.7f), 13)
                }
            }
            if (acc == null) LiquidButton({ onSignIn() }, bd, Modifier.fillMaxWidth(), tint = YT.Red) { Txt("Sign in with Google", Color.White, 16, bold = true) }
            else LiquidButton({ Auth.signOut(ctx) }, bd, Modifier.fillMaxWidth()) { Txt("Sign out", Color.White, 16, bold = true) }
            Auth.error?.let { Txt(it, Color(0xFFFFB4B4), 12) }

            Txt("Library", Color.White, 18, bold = true)
            LibRow(bd, Ic.Clock, "History", "${lib.history.size}") { nav.push(Screen.Local("History", "history")) }
            LibRow(bd, Ic.Save, "Watch later", "${lib.watchLater.size}") { nav.push(Screen.Local("Watch later", "later")) }
            LibRow(bd, Ic.Like, "Liked videos", "${lib.liked.size}") { nav.push(Screen.Local("Liked videos", "liked")) }
            LibRow(bd, Ic.Download, "Downloads", "${lib.downloads.size}") { nav.push(Screen.Local("Downloads", "downloads")) }

            Txt("Playlists", Color.White, 18, bold = true)
            lib.playlists.forEach { pl -> LibRow(bd, Ic.Save, pl.name, "${pl.videos.size}") { nav.push(Screen.Local(pl.name, "pl:" + pl.name)) } }
            LibRow(bd, Ic.Check, "New playlist") { nav.sheet(Sheet.NewPlaylist) }

            LibRow(bd, Ic.Settings, "Settings") { nav.push(Screen.Settings) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, value: Boolean, bd: Backdrop, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Txt(title, Color.White, 16, bold = true); Txt(sub, Color.White.copy(0.65f), 12) }
        LiquidToggle({ value }, onChange, bd)
    }
}

@Composable
fun SettingsScreen(s: SettingsStore, lib: Library, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current
    val bd = rememberLayerBackdrop()
    val top = statusTop()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        GlassBackdrop(bd)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = top + 70.dp, bottom = 40.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Txt("Playback", Color.White, 18, bold = true)
            ToggleRow("Autoplay", "Play the next video automatically", s.autoplay, bd) { s.setAutoplay(it) }
            ToggleRow("Data saver", "Play and download at 360p", s.dataSaver, bd) { s.setDataSaver(it) }
            ToggleRow("Captions", "Show subtitles when available", s.captions, bd) { s.setCaptions(it) }
            Txt("Default speed", Color.White, 15, bold = true)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(speeds) { sp -> LiquidButton({ s.setSpeed(sp) }, bd, tint = if (s.speed == sp) YT.Red else Color.Unspecified) { Txt("${sp}x", Color.White, 14, bold = true) } }
            }
            Txt("Content region", Color.White, 15, bold = true)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(regions) { r -> LiquidButton({ s.setRegion(r) }, bd, tint = if (s.region == r) YT.Red else Color.Unspecified) { Txt(r, Color.White, 14, bold = true) } }
            }
            Txt("Privacy", Color.White, 18, bold = true)
            ToggleRow("Pause watch history", "Videos you open won't be saved", s.pauseHistory, bd) { s.setPauseHistory(it) }
            LiquidButton({ lib.clearHistory(); toast(ctx, "Watch history cleared") }, bd, Modifier.fillMaxWidth()) { Txt("Clear watch history", Color.White, 15, bold = true) }
            LiquidButton({ lib.clearSearches(); toast(ctx, "Search history cleared") }, bd, Modifier.fillMaxWidth()) { Txt("Clear search history", Color.White, 15, bold = true) }
            Txt("YouTube 1.0 • Liquid glass by Kyant0", Color.White.copy(0.5f), 12)
        }
        Row(Modifier.fillMaxWidth().padding(top = top + 6.dp, start = 12.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBtn(Ic.Back, bd, onClick = onBack)
            LiquidButton({}, bd, isInteractive = false) { Txt("Settings", Color.White, 16, bold = true) }
        }
    }
}
