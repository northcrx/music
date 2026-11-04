package com.nishant.northmusic.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nishant.northmusic.MainActivity
import com.nishant.northmusic.NorthMusicApp
import com.nishant.northmusic.R
import com.nishant.northmusic.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Ensure notification channels exist
        createNotificationChannels()

        // 2. Set media notification provider with our playback channel and monochrome icon
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NorthMusicApp.PLAYBACK_CHANNEL_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_launcher_monochrome)
            }
        setMediaNotificationProvider(notificationProvider)

        // 3. Configure audio attributes for ExoPlayer
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer

        // 4. Session Activity PendingIntent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()

        // 5. Connect AudioFxManager (Hardware Equalizer, BassBoost, Virtualizer, LoudnessEnhancer)
        AudioFxManager.init(this)
        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    AudioFxManager.onAudioSessionIdChanged(this@PlaybackService, audioSessionId)
                }
            }
        })
        val initialSessionId = exoPlayer.audioSessionId
        if (initialSessionId != C.AUDIO_SESSION_ID_UNSET && initialSessionId != 0) {
            AudioFxManager.onAudioSessionIdChanged(this, initialSessionId)
        }

        // 6. Connect SleepTimerManager
        SleepTimerManager.attachPlayer(exoPlayer, serviceScope)

        // 7. Setup Audio Device Callback for headset plug/unplug and bluetooth routing
        setupAudioDeviceRouting()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                NorthMusicApp.PLAYBACK_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls and current track notification"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun setupAudioDeviceRouting() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am

        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                val settings = SettingsRepository.current
                val isDisconnectingHeadphones = removedDevices?.any {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                } ?: false

                if (isDisconnectingHeadphones && settings.headphonesPauseOnDisconnect) {
                    player?.pause()
                }
            }

            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                val settings = SettingsRepository.current
                val isConnectingHeadphones = addedDevices?.any {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                } ?: false

                if (isConnectingHeadphones && settings.headphonesResumeOnConnect) {
                    player?.play()
                }
            }
        }
        audioDeviceCallback = cb
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        audioDeviceCallback?.let { cb ->
            audioManager?.unregisterAudioDeviceCallback(cb)
        }
        audioDeviceCallback = null
        audioManager = null

        SleepTimerManager.detachPlayer()
        AudioFxManager.release()
        serviceScope.cancel()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
