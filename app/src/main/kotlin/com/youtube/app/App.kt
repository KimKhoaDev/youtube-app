package com.youtube.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(getSharedPreferences("settings", MODE_PRIVATE).getString("region", "US")!!)
    }
}
