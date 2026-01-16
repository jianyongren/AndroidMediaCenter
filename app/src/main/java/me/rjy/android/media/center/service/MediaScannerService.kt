package me.rjy.android.media.center.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.rjy.android.media.center.R
import me.rjy.android.media.center.data.model.MediaItem
import me.rjy.android.media.center.data.model.MediaType
import me.rjy.android.media.center.data.model.SourceType
import me.rjy.android.media.center.utils.FileUtils
import java.io.File
import java.util.Date

class MediaScannerService : Service() {

    private val TAG = "MediaScannerService"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isScanning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN_INTERNAL -> {
                startForegroundService()
                scanInternalStorage()
            }
            ACTION_SCAN_USB -> {
                val usbPath = intent.getStringExtra(EXTRA_USB_PATH) ?: ""
                startForegroundService()
                scanUSBStorage(usbPath)
            }
            ACTION_STOP_SCAN -> {
                stopScan()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "media_scanner_channel")
            .setContentTitle("媒体扫描")
            .setContentText("正在扫描媒体文件...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun scanInternalStorage() {
        if (isScanning) return
        isScanning = true

        scope.launch {
            try {
                val mediaItems = mutableListOf<MediaItem>()
                val internalStorage = File("/storage/emulated/0")
                scanDirectory(internalStorage, mediaItems, SourceType.INTERNAL_STORAGE)

                // 发送扫描完成广播
                sendScanCompleteBroadcast(mediaItems, SourceType.INTERNAL_STORAGE)
            } catch (e: Exception) {
                Log.e(TAG, "扫描内部存储失败", e)
                sendScanErrorBroadcast(e.message ?: "未知错误")
            } finally {
                isScanning = false
                stopForeground(true)
            }
        }
    }

    private fun scanUSBStorage(usbPath: String) {
        if (isScanning) return
        isScanning = true

        scope.launch {
            try {
                val mediaItems = mutableListOf<MediaItem>()
                val usbRoot = File(usbPath)
                if (usbRoot.exists() && usbRoot.isDirectory) {
                    scanDirectory(usbRoot, mediaItems, SourceType.USB)
                }

                // 发送扫描完成广播
                sendScanCompleteBroadcast(mediaItems, SourceType.USB)
            } catch (e: Exception) {
                Log.e(TAG, "扫描USB存储失败", e)
                sendScanErrorBroadcast(e.message ?: "未知错误")
            } finally {
                isScanning = false
                stopForeground(true)
            }
        }
    }

    private fun scanDirectory(
        directory: File,
        result: MutableList<MediaItem>,
        sourceType: SourceType
    ) {
        if (!directory.exists() || !directory.isDirectory) return

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // 递归扫描子目录
                scanDirectory(file, result, sourceType)
            } else {
                // 检查文件类型
                val mediaType = when {
                    FileUtils.isVideoFile(file.name) -> MediaType.VIDEO
                    FileUtils.isAudioFile(file.name) -> MediaType.AUDIO
                    FileUtils.isImageFile(file.name) -> MediaType.IMAGE
                    FileUtils.isApkFile(file.name) -> MediaType.APK
                    else -> MediaType.OTHER
                }

                if (mediaType != MediaType.OTHER) {
                    val mediaItem = MediaItem(
                        title = file.name,
                        path = file.absolutePath,
                        uri = file.toURI().toString(),
                        size = file.length(),
                        mediaType = mediaType,
                        addedDate = Date(),
                        sourceType = sourceType
                    )
                    result.add(mediaItem)
                }
            }
        }
    }

    private fun sendScanCompleteBroadcast(mediaItems: List<MediaItem>, sourceType: SourceType) {
        val intent = Intent(ACTION_SCAN_COMPLETE).apply {
            putExtra(EXTRA_SOURCE_TYPE, sourceType.name)
            putExtra(EXTRA_MEDIA_COUNT, mediaItems.size)
        }
        sendBroadcast(intent)
    }

    private fun sendScanErrorBroadcast(errorMessage: String) {
        val intent = Intent(ACTION_SCAN_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        sendBroadcast(intent)
    }

    private fun stopScan() {
        isScanning = false
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isScanning = false
    }

    companion object {
        const val ACTION_SCAN_INTERNAL = "me.rjy.android.media.center.SCAN_INTERNAL"
        const val ACTION_SCAN_USB = "me.rjy.android.media.center.SCAN_USB"
        const val ACTION_STOP_SCAN = "me.rjy.android.media.center.STOP_SCAN"
        const val ACTION_SCAN_COMPLETE = "me.rjy.android.media.center.SCAN_COMPLETE"
        const val ACTION_SCAN_ERROR = "me.rjy.android.media.center.SCAN_ERROR"

        const val EXTRA_USB_PATH = "usb_path"
        const val EXTRA_SOURCE_TYPE = "source_type"
        const val EXTRA_MEDIA_COUNT = "media_count"
        const val EXTRA_ERROR_MESSAGE = "error_message"
    }
}
