package me.rjy.android.media.center.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rjy.android.media.center.data.model.MediaItem
import me.rjy.android.media.center.data.model.MediaType
import me.rjy.android.media.center.data.model.SourceType
import me.rjy.android.media.center.utils.FileUtils
import me.rjy.android.media.center.utils.USBManager
import java.io.File
import java.util.Date

class MediaRepositoryImpl(private val context: Context) : MediaRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun getMediaItems(sourceType: SourceType): List<MediaItem> {
        return when (sourceType) {
            SourceType.INTERNAL_STORAGE -> getInternalStorageRoot()
            SourceType.USB -> getUSBDevices()
            SourceType.NETWORK -> emptyList() // 网络媒体需要特殊处理
            SourceType.RECENT -> getRecentMediaItems()
            SourceType.VIDEO_LIST -> getMediaStoreVideos()
            SourceType.AUDIO_LIST -> getMediaStoreAudios()
            SourceType.IMAGE_LIST -> getMediaStoreImages()
            SourceType.APK_LIST -> getMediaStoreApks()
        }
    }

    override suspend fun searchMediaItems(query: String): List<MediaItem> {
        // 简化实现，搜索所有类型的媒体
        val allMedia = mutableListOf<MediaItem>()
        allMedia.addAll(getMediaStoreVideos())
        allMedia.addAll(getMediaStoreAudios())
        allMedia.addAll(getMediaStoreImages())
        allMedia.addAll(getMediaStoreApks())
        return allMedia.filter { it.title.contains(query, ignoreCase = true) }
    }

    override suspend fun getRecentMediaItems(limit: Int): List<MediaItem> {
        val allMedia = mutableListOf<MediaItem>()
        allMedia.addAll(getMediaStoreVideos())
        allMedia.addAll(getMediaStoreAudios())
        return allMedia.sortedByDescending { it.lastPlayed ?: Date(0) }.take(limit)
    }

    override suspend fun updateMediaItem(mediaItem: MediaItem) {
        // 暂时不实现，需要数据库支持
    }

    override suspend fun deleteMediaItem(mediaItem: MediaItem) {
        // 暂时不实现，需要数据库支持
    }

    override suspend fun scanMediaFromInternalStorage(): List<MediaItem> {
        // 扫描内部存储的媒体文件
        val mediaItems = mutableListOf<MediaItem>()
        mediaItems.addAll(getMediaStoreVideos())
        mediaItems.addAll(getMediaStoreAudios())
        mediaItems.addAll(getMediaStoreImages())
        mediaItems.addAll(getMediaStoreApks())
        return mediaItems
    }

    override suspend fun scanMediaFromUSB(usbPath: String): List<MediaItem> {
        // 扫描USB设备中的媒体文件
        return withContext(Dispatchers.IO) {
            val usbDir = File(usbPath)
            if (usbDir.exists() && usbDir.isDirectory) {
                scanDirectory(usbDir, SourceType.USB)
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getMediaItemByUri(uri: String): MediaItem? {
        // 根据URI查找媒体项
        val allMedia = mutableListOf<MediaItem>()
        allMedia.addAll(getMediaStoreVideos())
        allMedia.addAll(getMediaStoreAudios())
        allMedia.addAll(getMediaStoreImages())
        allMedia.addAll(getMediaStoreApks())
        return allMedia.firstOrNull { it.uri == uri }
    }

    /**
     * 获取内部存储根目录内容
     */
    private suspend fun getInternalStorageRoot(): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            val root = Environment.getExternalStorageDirectory()
            if (root.exists() && root.isDirectory) {
                val items = mutableListOf<MediaItem>()
                // 添加常见目录
                val commonDirs = listOf(
                    "Music" to "音乐",
                    "Movies" to "视频", 
                    "Pictures" to "图片",
                    "Downloads" to "下载",
                    "DCIM" to "相机"
                )
                
                commonDirs.forEach { (dirName, displayName) ->
                    val dir = File(root, dirName)
                    if (dir.exists() && dir.isDirectory) {
                        items.add(MediaItem(
                            title = "$displayName/",
                            path = dir.absolutePath,
                            uri = "file://${dir.absolutePath}",
                            size = 0,
                            mediaType = MediaType.OTHER,
                            sourceType = SourceType.INTERNAL_STORAGE
                        ))
                    }
                }
                
                // 添加其他一级目录
                root.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        val dirName = file.name
                        if (commonDirs.none { it.first == dirName }) {
                            items.add(MediaItem(
                                title = "$dirName/",
                                path = file.absolutePath,
                                uri = "file://${file.absolutePath}",
                                size = 0,
                                mediaType = MediaType.OTHER,
                                sourceType = SourceType.INTERNAL_STORAGE
                            ))
                        }
                    }
                }
                
                items
            } else {
                emptyList()
            }
        }
    }

    /**
     * 获取USB设备列表
     */
    private suspend fun getUSBDevices(): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            val usbPaths = USBManager.getUSBMountPaths(context)
            val items = mutableListOf<MediaItem>()
            
            usbPaths.forEachIndexed { index, usbPath ->
                val usbDir = File(usbPath)
                if (usbDir.exists() && usbDir.isDirectory) {
                    // 使用挂载目录的最后一部分作为显示名称，例如：/storage/usb0 显示为 usb0/
                    val dirName = usbDir.name
                    val displayName = if (dirName.isNotEmpty()) "$dirName/" else "USB存储设备/"
                    items.add(MediaItem(
                        title = displayName,
                        path = usbPath,
                        uri = "file://$usbPath",
                        size = 0,
                        mediaType = MediaType.OTHER,
                        sourceType = SourceType.USB
                    ))
                }
            }
            
            // 如果没有检测到USB设备，显示提示信息
            if (items.isEmpty()) {
                // 可以添加一个提示项，但这里返回空列表，让UI显示空状态
                // 或者添加一个提示项，但点击时无操作
                // 暂时返回空列表
            }
            
            items
        }
    }

    /**
     * 获取目录内容
     */
    suspend fun getDirectoryContents(path: String, sourceType: SourceType): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            val dir = File(path)
            if (!dir.exists()) {
                // 目录不存在
                return@withContext emptyList()
            }
            if (!dir.isDirectory) {
                // 路径不是目录
                return@withContext emptyList()
            }
            // 检查目录是否可读
            if (!dir.canRead()) {
                throw SecurityException("没有权限访问目录: $path")
            }
            // 尝试列出文件，如果失败则抛出异常
            val files = dir.listFiles()
            if (files == null) {
                throw SecurityException("无法读取目录内容，可能没有权限: $path")
            }
            scanDirectory(dir, sourceType)
        }
    }

    /**
     * 扫描目录
     */
    private fun scanDirectory(directory: File, sourceType: SourceType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        
        directory.listFiles()?.forEach { file ->
            val mediaType = when {
                file.isDirectory -> MediaType.OTHER
                FileUtils.isVideoFile(file.name) -> MediaType.VIDEO
                FileUtils.isAudioFile(file.name) -> MediaType.AUDIO
                FileUtils.isImageFile(file.name) -> MediaType.IMAGE
                FileUtils.isApkFile(file.name) -> MediaType.APK
                else -> MediaType.OTHER
            }
            
            val title = if (file.isDirectory) "${file.name}/" else file.name
            
            items.add(MediaItem(
                title = title,
                path = file.absolutePath,
                uri = "file://${file.absolutePath}",
                size = file.length(),
                mediaType = mediaType,
                sourceType = sourceType
            ))
        }
        
        // 排序：目录在前，文件在后，按名称排序
        return items.sortedWith(compareBy(
            { !it.title.endsWith("/") }, // 目录在前
            { it.title.lowercase() }
        ))
    }

    /**
     * 从MediaStore获取视频文件
     */
    private fun getMediaStoreVideos(): List<MediaItem> {
        return queryMediaStore(
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_MODIFIED
            ),
            mediaType = MediaType.VIDEO
        )
    }

    /**
     * 从MediaStore获取音频文件
     */
    private fun getMediaStoreAudios(): List<MediaItem> {
        return queryMediaStore(
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_MODIFIED
            ),
            mediaType = MediaType.AUDIO
        )
    }

    /**
     * 从MediaStore获取图片文件
     */
    private fun getMediaStoreImages(): List<MediaItem> {
        return queryMediaStore(
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED
            ),
            mediaType = MediaType.IMAGE
        )
    }

    /**
     * 扫描APK文件
     */
    private suspend fun getMediaStoreApks(): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            val apkItems = mutableListOf<MediaItem>()
            // 扫描常见目录中的APK文件
            val commonDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStorageDirectory(),
                File(Environment.getExternalStorageDirectory(), "APK"),
                File(Environment.getExternalStorageDirectory(), "Download")
            )
            
            commonDirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    scanApkFiles(dir, apkItems)
                }
            }
            
            apkItems
        }
    }

    /**
     * 递归扫描目录中的APK文件
     */
    private fun scanApkFiles(directory: File, apkItems: MutableList<MediaItem>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // 递归扫描子目录，但限制深度
                if (file.depthFromRoot() < 3) { // 限制递归深度为3
                    scanApkFiles(file, apkItems)
                }
            } else if (FileUtils.isApkFile(file.name)) {
                apkItems.add(MediaItem(
                    title = file.name,
                    path = file.absolutePath,
                    uri = "file://${file.absolutePath}",
                    size = file.length(),
                    mediaType = MediaType.APK,
                    sourceType = SourceType.INTERNAL_STORAGE
                ))
            }
        }
    }

    /**
     * 计算文件从根目录的深度
     */
    private fun File.depthFromRoot(): Int {
        var depth = 0
        var current = this
        while (current.parentFile != null) {
            depth++
            current = current.parentFile!!
        }
        return depth
    }

    /**
     * 查询MediaStore的通用方法
     */
    private fun queryMediaStore(
        contentUri: Uri,
        projection: Array<String>,
        mediaType: MediaType
    ): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        
        val cursor: Cursor? = contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val durationColumn = it.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "未知"
                val path = it.getString(dataColumn) ?: continue
                val size = it.getLong(sizeColumn)
                val duration = if (durationColumn != -1) it.getLong(durationColumn) else 0L
                val dateModified = it.getLong(dateModifiedColumn) * 1000 // 转换为毫秒
                
                mediaItems.add(MediaItem(
                    title = title,
                    path = path,
                    uri = "file://$path",
                    size = size,
                    duration = duration,
                    mediaType = mediaType,
                    lastPlayed = Date(dateModified),
                    sourceType = SourceType.INTERNAL_STORAGE
                ))
            }
        }
        
        return mediaItems
    }
}
