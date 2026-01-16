package me.rjy.android.media.center.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object APKInstaller {
    
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            return
        }
        
        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0及以上需要使用FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }
        
        installApkFromUri(context, apkUri)
    }
    
    fun installApkFromUri(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isApkFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) {
            return false
        }
        val name = file.name.lowercase()
        return name.endsWith(".apk")
    }
    
    fun isApkUri(contentResolver: ContentResolver, uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType == "application/vnd.android.package-archive"
    }
}
