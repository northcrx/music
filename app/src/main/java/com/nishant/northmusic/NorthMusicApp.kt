package com.nishant.northmusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nishant.northmusic.data.repository.SettingsRepository
import org.jaudiotagger.tag.TagOptionSingleton

class NorthMusicApp : Application() {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "playback_channel_id"
        const val DEFAULT_CHANNEL_ID = "default_channel_id"
    }

    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)

        // Configure Jaudiotagger for Android environment (pure byte manipulation, no java.awt classes)
        try {
            TagOptionSingleton.getInstance().isAndroid = true
        } catch (_: Throwable) {}

        // Create notification channels for Media3 playback service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channels = listOf(
                NotificationChannel(
                    PLAYBACK_CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Music playback controls and current track notification"
                    setShowBadge(false)
                },
                NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    "Media Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Media player notifications"
                    setShowBadge(false)
                }
            )
            channels.forEach { channel ->
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}
