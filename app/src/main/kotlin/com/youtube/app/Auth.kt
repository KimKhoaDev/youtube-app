package com.youtube.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/** Google sign-in via the GMS API, which microG implements as well. */
object Auth {
    private const val YT_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
    var account by mutableStateOf<GoogleSignInAccount?>(null)
    var error by mutableStateOf<String?>(null)

    private fun options(ctx: Context): GoogleSignInOptions {
        val b = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestProfile().requestScopes(Scope(YT_SCOPE))
        val id = ctx.getString(R.string.google_web_client_id)
        if (id.isNotBlank()) b.requestIdToken(id)
        return b.build()
    }

    fun client(ctx: Context) = GoogleSignIn.getClient(ctx, options(ctx))
    fun restore(ctx: Context) { account = GoogleSignIn.getLastSignedInAccount(ctx) }

    fun onResult(data: Intent?) {
        runCatching { GoogleSignIn.getSignedInAccountFromIntent(data).result }
            .onSuccess { account = it; error = null }
            .onFailure { error = it.message ?: "Sign-in failed" }
    }

    fun signOut(ctx: Context) { client(ctx).signOut(); account = null }

    /** Your real YouTube subscriptions (needs YouTube Data API v3 enabled for your OAuth client). */
    suspend fun subscriptions(ctx: Context): List<ChannelRef> = withContext(Dispatchers.IO) {
        val acc = account?.account ?: return@withContext emptyList()
        runCatching {
            val token = GoogleAuthUtil.getToken(ctx, acc, "oauth2:$YT_SCOPE")
            val req = Request.Builder().header("Authorization", "Bearer $token")
                .url("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&mine=true&maxResults=50").build()
            val json = JSONObject(Net.client.newCall(req).execute().use { it.body!!.string() })
            val items = json.getJSONArray("items")
            (0 until items.length()).map {
                val sn = items.getJSONObject(it).getJSONObject("snippet")
                ChannelRef("https://www.youtube.com/channel/" + sn.getJSONObject("resourceId").getString("channelId"), sn.getString("title"), sn.getJSONObject("thumbnails").getJSONObject("default").getString("url"))
            }
        }.getOrDefault(emptyList())
    }
}
