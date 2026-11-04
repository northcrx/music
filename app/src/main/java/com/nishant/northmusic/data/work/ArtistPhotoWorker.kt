package com.nishant.northmusic.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nishant.northmusic.data.network.AlbumArtworkDownloader
import com.nishant.northmusic.data.network.ArtistPhotoDownloader
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.data.repository.SettingsRepository
import kotlinx.coroutines.delay

class ArtistPhotoWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository.current
        if (!settings.downloadArtistAndAlbumArt) {
            return Result.success()
        }

        val musicRepo = MusicRepository.getInstance(applicationContext)

        // 1. Download artist photos
        val artists = musicRepo.artists.value
        for (artist in artists) {
            if (isStopped) {
                return Result.retry()
            }
            ArtistPhotoDownloader.fetchAndSaveArtistPhoto(
                context = applicationContext,
                artistName = artist.name,
                forceRefresh = false
            )
            // Rate limit: ~2.5 requests per second
            delay(400)
        }

        // 2. Download missing album artwork (never overwrite embedded artwork)
        val albums = musicRepo.albums.value
        for (album in albums) {
            if (isStopped) {
                return Result.retry()
            }
            // Only search if the album has no artwork
            if (album.albumArtUri == null || album.albumId <= 0L) {
                AlbumArtworkDownloader.fetchAndSaveAlbumArtwork(
                    context = applicationContext,
                    albumTitle = album.title,
                    albumArtist = album.artist,
                    albumKey = album.id,
                    forceRefresh = false
                )
                delay(400)
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "artist_photo_sync_work"

        fun enqueue(context: Context) {
            val settings = SettingsRepository.current
            if (!settings.downloadArtistAndAlbumArt) return

            val networkType = if (settings.downloadOnlyOnWifi) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val request = OneTimeWorkRequestBuilder<ArtistPhotoWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
