package me.rjy.android.media.center.utils

import java.text.DecimalFormat

object FileUtils {
    
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val formatter = DecimalFormat("#,##0.#")
        val formattedSize = formatter.format(size / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formattedSize ${units[digitGroups]}"
    }
    
    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun getFileExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }
    
    fun isVideoFile(fileName: String): Boolean {
        val ext = getFileExtension(fileName)
        return ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v")
    }
    
    fun isAudioFile(fileName: String): Boolean {
        val ext = getFileExtension(fileName)
        return ext in listOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")
    }
    
    fun isImageFile(fileName: String): Boolean {
        val ext = getFileExtension(fileName)
        return ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
    
    fun isApkFile(fileName: String): Boolean {
        val ext = getFileExtension(fileName)
        return ext == "apk"
    }
    
    fun getMediaTypeFromFileName(fileName: String): String {
        return when {
            isVideoFile(fileName) -> "video"
            isAudioFile(fileName) -> "audio"
            isImageFile(fileName) -> "image"
            isApkFile(fileName) -> "apk"
            else -> "other"
        }
    }
}
