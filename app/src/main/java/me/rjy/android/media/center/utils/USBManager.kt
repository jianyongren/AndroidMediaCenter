package me.rjy.android.media.center.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.annotation.RequiresApi
import java.io.File

object USBManager {
    
    private var usbMountPaths: MutableList<String> = mutableListOf()
    
    fun registerUSBReceiver(context: Context, onUSBChanged: (List<String>) -> Unit): BroadcastReceiver {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val path = intent?.data?.path
                when (intent?.action) {
                    Intent.ACTION_MEDIA_MOUNTED -> {
                        if (path != null && !usbMountPaths.contains(path)) {
                            usbMountPaths.add(path)
                        }
                    }
                    Intent.ACTION_MEDIA_UNMOUNTED, 
                    Intent.ACTION_MEDIA_EJECT, 
                    Intent.ACTION_MEDIA_REMOVED -> {
                        usbMountPaths.remove(path)
                    }
                }
                onUSBChanged(usbMountPaths.toList())
            }
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }
    
    fun unregisterUSBReceiver(context: Context, receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }
    
    fun getUSBMountPaths(context: Context): List<String> {
        val detectedPaths = mutableListOf<String>()
        
        // 方法1：使用系统API（Android 7.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            detectedPaths.addAll(getStorageVolumes(context))
        }
        
        // 方法2：检查常见USB挂载点
        val commonPaths = listOf(
            "/mnt/usb",
            "/storage/usb",
            "/storage/usb0",
            "/storage/usb1",
            "/storage/usb2",
            "/storage/usb3",
            "/storage/usb4",
            "/mnt/media_rw",
            "/storage/external",
            "/storage/udisk",
            "/storage/sdcard1",
            "/storage/external_sd",
            "/storage/ext_sd",
            "/storage/removable",
            "/mnt/sdcard/external_sd",
            "/mnt/external_sd",
            "/mnt/sd",
            "/mnt/extsd",
            "/mnt/extSdCard",
            "/mnt/sdcard/extSdCard"
        )
        
        commonPaths.forEach { path ->
            if (File(path).exists() && File(path).canRead() && !detectedPaths.contains(path)) {
                detectedPaths.add(path)
            }
        }
        
        // 方法3：递归扫描/storage和/mnt目录寻找可能的挂载点
        val rootDirs = listOf("/storage", "/mnt")
        rootDirs.forEach { rootPath ->
            val rootDir = File(rootPath)
            if (rootDir.exists() && rootDir.isDirectory) {
                rootDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.canRead()) {
                        val path = file.absolutePath
                        // 排除已知的内部存储路径
                        if (!path.contains("emulated") && 
                            !path.contains("self") && 
                            !path.contains("primary") &&
                            !detectedPaths.contains(path)) {
                            // 检查是否看起来像USB挂载点（有文件或目录）
                            if (file.list()?.isNotEmpty() == true) {
                                detectedPaths.add(path)
                            }
                        }
                    }
                }
            }
        }
        
        // 去重并返回
        return detectedPaths.distinct()
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getStorageVolumes(context: Context): List<String> {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumes = storageManager.storageVolumes
        return storageVolumes.filter { volume ->
            volume.isRemovable && volume.state == android.os.Environment.MEDIA_MOUNTED
        }.mapNotNull { volume ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                volume.directory?.absolutePath
            } else {
                // 对于Android 10及以下，使用反射或其他方法获取路径
                getVolumePath(volume)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getVolumePath(volume: StorageVolume): String? {
        return try {
            val getPathMethod = StorageVolume::class.java.getMethod("getPath")
            getPathMethod.invoke(volume) as String
        } catch (e: Exception) {
            null
        }
    }
    
    fun isUSBConnected(context: Context): Boolean {
        val paths = getUSBMountPaths(context)
        // 如果有检测到路径，并且路径存在且可读，则认为是USB已连接
        return paths.any { path ->
            val file = File(path)
            file.exists() && file.isDirectory && file.canRead()
        }
    }
    
    fun scanUSBForMedia(context: Context): List<File> {
        val mediaFiles = mutableListOf<File>()
        val usbPaths = getUSBMountPaths(context)
        val supportedExtensions = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v",
            "mp3", "wav", "ogg", "flac", "m4a", "aac", "wma",
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "apk"
        )
        
        usbPaths.forEach { usbPath ->
            val root = File(usbPath)
            if (root.exists() && root.isDirectory) {
                scanDirectory(root, supportedExtensions, mediaFiles)
            }
        }
        
        return mediaFiles
    }
    
    private fun scanDirectory(directory: File, extensions: Set<String>, result: MutableList<File>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectory(file, extensions, result)
            } else {
                val ext = file.extension.lowercase()
                if (extensions.contains(ext)) {
                    result.add(file)
                }
            }
        }
    }
}
