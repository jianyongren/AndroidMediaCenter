package me.rjy.android.media.center.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val path: String,
    val uri: String,
    val size: Long,
    val duration: Long = 0,
    val mediaType: MediaType,
    val thumbnailUri: String? = null,
    val lastPlayed: Date? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val addedDate: Date = Date(),
    val sourceType: SourceType
)

enum class MediaType {
    VIDEO, AUDIO, IMAGE, APK, OTHER
}

enum class SourceType {
    INTERNAL_STORAGE, USB, NETWORK, RECENT,
    VIDEO_LIST, AUDIO_LIST, IMAGE_LIST, APK_LIST
}
