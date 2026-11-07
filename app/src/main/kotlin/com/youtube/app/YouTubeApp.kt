package com.youtube.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.youtube.app.liquidglass.LiquidBottomTab
import com.youtube.app.liquidglass.LiquidBottomTabs
import com.youtube.app.liquidglass.LiquidButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Feed {
    var items by mutableStateOf<List<Video>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    suspend fun load(query: String?) {
        loading = true; error = null
        runCatching { if (query == null) Repo.trending() else Repo.search(query) }
            .onSuccess { items = it }.onFailure { error = it.message ?: "Couldn't load videos" }
        loading = false
    }
}

fun toast(ctx: android.content.Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

@Composable
fun YouTubeApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsStore(ctx) }
    val lib = remember { Library(ctx) }
    val pb = remember { Playback(ctx) }
    DisposableEffect(Unit) { onDispose { pb.release() } }
    val feed = remember { Feed() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var chip by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Screen>() }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var subFeed by remember { mutableStateOf<List<Video>>(emptyList()) }
    val bdAll = rememberLayerBackdrop()
    val bd = rememberLayerBackdrop()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val openVideo: (Video) -> Unit = { v -> if (!settings.pauseHistory) lib.addHistory(v); scope.launch { pb.open(v, settings) } }
    val nav = Nav(
        openVideo = openVideo,
        openChannel = { c -> if (c.url.isNotEmpty()) stack.add(Screen.Channel(c)) },
        menu = { sheet = Sheet.Menu(it) },
        sheet = { sheet = it },
        push = { stack.add(it) }
    )
    SideEffect { pb.onEnded = { if (settings.autoplay) pb.detail?.related?.firstOrNull()?.let(openVideo) } }

    LaunchedEffect(Unit) { Auth.restore(ctx) }
    LaunchedEffect(settings.region, chip) { Repo.init(settings.region); feed.load(filterChips[chip].second) }
    LaunchedEffect(Auth.account) { if (Auth.account != null) lib.mergeSubs(Auth.subscriptions(ctx)) }
    LaunchedEffect(tab == 2, lib.subs.size) { if (tab == 2 && lib.subs.isNotEmpty()) subFeed = Repo.subsFeed(lib.subs.toList()) }
    LaunchedEffect(pb.current != null) { while (pb.current != null) { pb.tick(); delay(250) } }
    LaunchedEffect(tab) { if (tab == 1) pb.player.pause() }
    val signIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { Auth.onResult(it.data) }
    val doSignIn = { signIn.launch(Auth.client(ctx).signInIntent) }
    val pop = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }

    Box(Modifier.fillMaxSize().background(YT.bg())) {
        Box(Modifier.fillMaxSize().layerBackdrop(bdAll)) {
            Box(Modifier.fillMaxSize().layerBackdrop(bd)) {
                when (tab) {
                    0 -> VideoList(feed.items, top + 112.dp, nav)
                    1 -> ShortsScreen(settings, lib, nav)
                    2 -> VideoList(if (lib.subs.isEmpty()) emptyList() else subFeed, top + 112.dp + if (lib.subs.isNotEmpty()) 92.dp else 0.dp, nav, header = { if (lib.subs.isNotEmpty()) SubsRow(lib.subs, nav) })
                    else -> LibraryScreen(settings, lib, nav, doSignIn)
                }
            }

            TopBar(bd, tab, chip, { chip = it }, top, onSearch = { stack.add(Screen.Search) }, onAccount = { tab = 3 })

            if (tab == 0) {
                if (feed.loading && feed.items.isEmpty()) Txt("Loading…", YT.sub(), 15, modifier = Modifier.align(Alignment.Center))
                feed.error?.let { e ->
                    Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Txt(e, YT.sub(), 14)
                        LiquidButton({ scope.launch { feed.load(filterChips[chip].second) } }, bd) { Txt("Retry", YT.text(), 15, bold = true) }
                    }
                }
            }
            if (tab == 2 && lib.subs.isEmpty()) {
                Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt("Subscribe to channels to see their latest videos here", YT.text(), 16, bold = true)
                    if (Auth.account == null) LiquidButton({ doSignIn() }, bd, tint = YT.Red) { Txt("Sign in with Google", Color.White, 15, bold = true) }
                }
            }

            LiquidBottomTabs(
                selectedTabIndex = { tab }, onTabSelected = { tab = it }, backdrop = bd, tabsCount = 4,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 28.dp).padding(bottom = 8.dp)
            ) {
                listOf(Ic.Home to "Home", Ic.Shorts to "Shorts", Ic.Subs to "Subscriptions", Ic.You to "You").forEachIndexed { i, (ic, label) ->
                    LiquidBottomTab({ tab = i }) { Icon(ic, YT.text(), 26.dp); Txt(label, YT.text(), 10, bold = true, maxLines = 1) }
                }
            }

            stack.lastOrNull()?.let { s ->
                when (s) {
                    is Screen.Channel -> ChannelScreen(s.ref, lib, nav, pop)
                    is Screen.YtPlaylist -> ListScreen(s.ref.name, nav, pop, load = { Repo.playlist(s.ref.url) })
                    is Screen.Local -> when {
                        s.kind == "history" -> ListScreen(s.title, nav, pop, live = lib.history, onClear = { lib.clearHistory() })
                        s.kind == "later" -> ListScreen(s.title, nav, pop, live = lib.watchLater)
                        s.kind == "liked" -> ListScreen(s.title, nav, pop, live = lib.liked)
                        s.kind == "downloads" -> ListScreen(s.title, nav, pop, live = lib.downloads)
                        else -> lib.playlists.firstOrNull { "pl:" + it.name == s.kind }?.let { pl -> ListScreen(pl.name, nav, pop, live = pl.videos, onClear = { lib.deletePlaylist(pl); pop() }) }
                    }
                    Screen.Search -> SearchScreen(lib, nav, pop)
                    Screen.Settings -> SettingsScreen(settings, lib, pop)
                }
            }

            if (pb.current != null && pb.minimized) MiniPlayer(pb, bd, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 84.dp))
            if (pb.current != null && !pb.minimized) WatchScreen(pb, settings, lib, nav)
        }

        sheet?.let { s -> SheetHost(bdAll, { sheet = null }) { SheetContent(s, bdAll, lib, nav, settings, pb) { sheet = null } } }
    }
}

@Composable
fun MiniPlayer(pb: Playback, bd: Backdrop, modifier: Modifier) {
    val v = pb.current ?: return
    LiquidButton({ pb.minimized = false }, bd, modifier.fillMaxWidth()) {
        RemoteImage(v.thumb, Modifier.size(width = 52.dp, height = 30.dp).clip(RoundedCornerShape(6.dp)), v)
        Txt(v.title, YT.text(), 13, bold = true, maxLines = 1, modifier = Modifier.weight(1f))
        Box(Modifier.size(32.dp).clickable { pb.toggle() }, contentAlignment = Alignment.Center) { Icon(if (pb.playing) Ic.Pause else Ic.Play, YT.text(), 20.dp) }
        Box(Modifier.size(32.dp).clickable { pb.close() }, contentAlignment = Alignment.Center) { Icon(Ic.Close, YT.text(), 18.dp) }
    }
}

@Composable
fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(width = 30.dp, height = 21.dp)) {
            drawRoundRect(YT.Red, cornerRadius = CornerRadius(6.dp.toPx()), size = size)
            val w = size.width; val h = size.height
            drawPath(Path().apply { moveTo(w * .4f, h * .3f); lineTo(w * .4f, h * .7f); lineTo(w * .66f, h * .5f); close() }, Color.White)
        }
        Txt("YouTube", YT.text(), 20, bold = true)
    }
}

@Composable
fun TopBar(backdrop: Backdrop, tab: Int, chip: Int, onChip: (Int) -> Unit, topInset: Dp, onSearch: () -> Unit, onAccount: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = topInset + 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LiquidButton({}, backdrop, isInteractive = false) { Logo() }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBtn(Ic.Search, backdrop, onClick = onSearch)
                IconBtn(Ic.You, backdrop, onClick = onAccount)
            }
        }
        if (tab == 0) {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filterChips) { i, c -> Chip(c.first, i == chip, backdrop) { onChip(i) } }
            }
        }
    }
}

@Composable
fun VideoCard(v: Video, nav: Nav) {
    Column(Modifier.fillMaxWidth().clickable(interactionSource = null, indication = null) { nav.openVideo(v) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(16 / 9f)) {
            RemoteImage(v.thumb, Modifier.fillMaxSize(), v)
            Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(if (v.duration == "LIVE") YT.Red else Color.Black.copy(0.8f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Txt(v.duration, Color.White, 12, bold = true)
            }
        }
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteImage(v.avatar, Modifier.size(38.dp).clip(CircleShape).clickable { v.channelUrl?.let { nav.openChannel(ChannelRef(it, v.channel, v.avatar)) } }, v)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt(v.title, YT.text(), 15, bold = true, maxLines = 2)
                Txt("${v.channel} • ${v.meta}", YT.sub(), 12, maxLines = 2)
            }
            Box(Modifier.size(32.dp).clickable { nav.menu(v) }, contentAlignment = Alignment.Center) { Icon(Ic.More, YT.text(), 20.dp) }
        }
    }
}

@Composable
fun VideoList(videos: List<Video>, topPad: Dp, nav: Nav, bottomPad: Dp = 110.dp, header: (@Composable () -> Unit)? = null) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = topPad, bottom = bottomPad + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
        if (header != null) item { header() }
        items(videos) { VideoCard(it, nav) }
    }
}

@Composable
fun SubsRow(subs: List<ChannelRef>, nav: Nav) {
    LazyRow(Modifier.padding(bottom = 12.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(subs) { c ->
            Column(Modifier.width(60.dp).clickable { nav.openChannel(c) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RemoteImage(c.avatar, Modifier.size(56.dp).clip(CircleShape).background(Color.Gray))
                Txt(c.name, YT.text(), 11, maxLines = 1)
            }
        }
    }
}
