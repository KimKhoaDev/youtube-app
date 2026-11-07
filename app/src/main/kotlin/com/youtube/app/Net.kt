package com.youtube.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Box
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

/** HTTP layer for NewPipeExtractor + image loading. */
object Net : Downloader() {
    val client: OkHttpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val body = request.dataToSend() ?: if (method == "POST") ByteArray(0) else null
        val b = okhttp3.Request.Builder().url(request.url())
            .method(method, body?.toRequestBody()).addHeader("User-Agent", UA)
        request.headers().forEach { (k, vs) -> b.removeHeader(k); vs.forEach { b.addHeader(k, it) } }
        client.newCall(b.build()).execute().use { r ->
            if (r.code == 429) throw ReCaptchaException("Rate limited", request.url())
            return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
        }
    }
}

private val imgCache = LruCache<String, ImageBitmap>(80)

@Composable
fun RemoteImage(url: String?, modifier: Modifier, v: Video? = null) {
    val bmp by produceState(url?.let { imgCache.get(it) }, url) {
        if (url != null && value == null) value = withContext(Dispatchers.IO) {
            runCatching {
                Net.client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use {
                    BitmapFactory.decodeStream(it.body!!.byteStream())?.asImageBitmap()
                }
            }.getOrNull()?.also { imgCache.put(url, it) }
        }
    }
    Box(modifier.then(if (v != null) Modifier.background(Brush.linearGradient(listOf(v.c1, v.c2))) else Modifier)) {
        bmp?.let { Image(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
    }
}
