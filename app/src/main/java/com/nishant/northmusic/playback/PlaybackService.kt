package com.nishant.northmusic.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nishant.northmusic.data.repository.SettingsRepository

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val audioDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val settings = SettingsRepository.current
            val p = player ?: return

            when (action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (settings.headphonesPauseOnDisconnect) {
                        p.pause()
                    }
                }
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1 && settings.headphonesResumeOnConnect) {
                        p.play()
                    } else if (state == 0 && settings.headphonesPauseOnDisconnect) {
                        p.pause()
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (settings.bluetoothResumeOnConnect) {
                        p.play()
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (settings.bluetoothPauseOnDisconnect) {
                        p.pause()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer

        mediaSession = MediaSession.Builder(this, exoPlayer).build()

        // Register audio hardware connect/disconnect receivers
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        try {
            registerReceiver(audioDeviceReceiver, filter)
        } catch (_: Exception) {}
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(audioDeviceReceiver)
        } catch (_: Exception) {}

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
