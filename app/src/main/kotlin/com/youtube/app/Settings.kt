package com.youtube.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Persisted app settings. */
class SettingsStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var autoplay by mutableStateOf(p.getBoolean("autoplay", true))
        private set
    var dataSaver by mutableStateOf(p.getBoolean("dataSaver", false))
        private set
    var captions by mutableStateOf(p.getBoolean("captions", false))
        private set
    var pauseHistory by mutableStateOf(p.getBoolean("pauseHistory", false))
        private set
    var region by mutableStateOf(p.getString("region", "US")!!)
        private set
    var speed by mutableFloatStateOf(p.getFloat("speed", 1f))
        private set

    @JvmName("setAutoplayPref")
    fun setAutoplay(v: Boolean) { autoplay = v; p.edit().putBoolean("autoplay", v).apply() }
    @JvmName("setDataSaverPref")
    fun setDataSaver(v: Boolean) { dataSaver = v; p.edit().putBoolean("dataSaver", v).apply() }
    @JvmName("setCaptionsPref")
    fun setCaptions(v: Boolean) { captions = v; p.edit().putBoolean("captions", v).apply() }
    @JvmName("setPauseHistoryPref")
    fun setPauseHistory(v: Boolean) { pauseHistory = v; p.edit().putBoolean("pauseHistory", v).apply() }
    @JvmName("setRegionPref")
    fun setRegion(v: String) { region = v; p.edit().putString("region", v).apply() }
    @JvmName("setSpeedPref")
    fun setSpeed(v: Float) { speed = v; p.edit().putFloat("speed", v).apply() }
}
