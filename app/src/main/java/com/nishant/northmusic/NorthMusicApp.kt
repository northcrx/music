package com.nishant.northmusic

import android.app.Application
import com.nishant.northmusic.data.repository.SettingsRepository

class NorthMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
    }
}
