package me.rjy.android.media.center.data.repository

import kotlinx.coroutines.flow.Flow
import me.rjy.android.media.center.data.model.MediaItem
import me.rjy.android.media.center.data.model.SourceType

interface MediaRepository {
    suspend fun getMediaItems(sourceType: SourceType): List<MediaItem>
    suspend fun searchMediaItems(query: String): List<MediaItem>
    suspend fun getRecentMediaItems(limit: Int = 20): List<MediaItem>
    suspend fun updateMediaItem(mediaItem: MediaItem)
    suspend fun deleteMediaItem(mediaItem: MediaItem)
    suspend fun scanMediaFromInternalStorage(): List<MediaItem>
    suspend fun scanMediaFromUSB(usbPath: String): List<MediaItem>
    suspend fun getMediaItemByUri(uri: String): MediaItem?
}
