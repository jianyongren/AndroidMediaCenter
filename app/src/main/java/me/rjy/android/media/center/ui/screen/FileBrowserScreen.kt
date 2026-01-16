package me.rjy.android.media.center.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.rjy.android.media.center.data.model.MediaItem
import me.rjy.android.media.center.data.model.MediaType
import me.rjy.android.media.center.data.model.SourceType
import me.rjy.android.media.center.data.repository.MediaRepositoryImpl
import me.rjy.android.media.center.utils.FileUtils
import me.rjy.android.media.center.utils.APKInstaller
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    sourceType: String,
    onNavigateBack: () -> Unit,
    onPlayMedia: (String) -> Unit
) {
    val currentPath = remember { mutableStateOf("") }
    val mediaItems = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val retryTrigger = remember { mutableStateOf(0) }
    val context = LocalContext.current
    // 主文件选择器，使用通用MIME类型以提高兼容性
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                // 验证文件类型是否为APK
                val isApk = APKInstaller.isApkUri(context.contentResolver, uri)
                if (isApk) {
                    APKInstaller.installApkFromUri(context, uri)
                } else {
                    // 提示用户选择正确的文件类型
                    // 这里可以显示一个Snackbar或Toast，但需要更复杂的状态管理
                    // 为了简化，暂时只调用installApkFromUri，系统会处理错误
                    APKInstaller.installApkFromUri(context, uri)
                }
            }
        }
    )
    // 备用文件选择器，使用OPEN_DOCUMENT，适用于某些设备
    val backupApkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                APKInstaller.installApkFromUri(context, uri)
            }
        }
    )
    
    // 处理APK安装按钮点击，包含异常处理
    fun onInstallApkClicked() {
        try {
            apkPickerLauncher.launch("*/*")
        } catch (e: android.content.ActivityNotFoundException) {
            // 如果主选择器不可用，尝试备用选择器
            android.widget.Toast.makeText(
                context,
                "文件选择器不可用，尝试备用方式",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            try {
                backupApkPickerLauncher.launch(arrayOf("*/*"))
            } catch (e2: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(
                    context,
                    "无法打开文件选择器，请安装文件管理器应用",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // 处理备用APK安装按钮点击
    fun onInstallApkBackupClicked() {
        try {
            backupApkPickerLauncher.launch(arrayOf("*/*"))
        } catch (e: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(
                context,
                "备用文件选择器不可用，请安装文件管理器应用",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    val repository = remember { MediaRepositoryImpl(context) }
    
    val parsedSourceType = try {
        SourceType.valueOf(sourceType)
    } catch (e: IllegalArgumentException) {
        SourceType.INTERNAL_STORAGE
    }
    
    LaunchedEffect(sourceType, currentPath.value, retryTrigger.value) {
        isLoading.value = true
        errorMessage.value = null
        try {
            // 根据sourceType获取真实数据
            mediaItems.value = when (parsedSourceType) {
                SourceType.INTERNAL_STORAGE, SourceType.USB -> {
                    if (currentPath.value.isEmpty()) {
                        // 如果是根目录，获取根目录内容
                        repository.getMediaItems(parsedSourceType)
                    } else {
                        // 如果是子目录，获取目录内容
                        repository.getDirectoryContents(currentPath.value, parsedSourceType)
                    }
                }
                SourceType.VIDEO_LIST, SourceType.AUDIO_LIST, 
                SourceType.IMAGE_LIST, SourceType.APK_LIST,
                SourceType.RECENT -> {
                    repository.getMediaItems(parsedSourceType)
                }
                SourceType.NETWORK -> {
                    // 网络媒体在NetworkMediaScreen中处理
                    emptyList()
                }
            }
        } catch (e: SecurityException) {
            errorMessage.value = "权限错误: ${e.message}"
            mediaItems.value = emptyList()
        } catch (e: Exception) {
            errorMessage.value = "错误: ${e.message}"
            mediaItems.value = emptyList()
        }
        isLoading.value = false
    }
    
    // 处理重试
    fun handleRetry() {
        retryTrigger.value++
    }
    
    // 处理返回按钮点击
    fun handleBackPress() {
        if (currentPath.value.isNotEmpty()) {
            // 如果当前路径不为空，返回到上一级目录
            val parentPath = File(currentPath.value).parent ?: ""
            currentPath.value = parentPath
        } else {
            // 如果当前路径为空，退出到首页
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = when (parsedSourceType) {
                                SourceType.INTERNAL_STORAGE -> "内部存储"
                                SourceType.USB -> "USB设备"
                                SourceType.NETWORK -> "网络媒体"
                                SourceType.RECENT -> "最近播放"
                                else -> "文件浏览器"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentPath.value.isNotEmpty()) {
                            Text(
                                text = currentPath.value,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBackPress() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        val coroutineScope = rememberCoroutineScope()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // 显示错误信息（如果有）
                errorMessage.value?.let { error ->
                    PermissionErrorScreen(
                        errorMessage = error,
                        onRetry = {
                            // 重新加载数据
                            coroutineScope.launch {
                                isLoading.value = true
                                errorMessage.value = null
                                try {
                                    mediaItems.value = if (currentPath.value.isEmpty()) {
                                        repository.getMediaItems(parsedSourceType)
                                    } else {
                                        repository.getDirectoryContents(currentPath.value, parsedSourceType)
                                    }
                                } catch (e: Exception) {
                                    errorMessage.value = "错误: ${e.message}"
                                }
                                isLoading.value = false
                            }
                        },
                        onNavigateBack = { handleBackPress() }
                    )
                } ?: run {
                    when (parsedSourceType) {
                        SourceType.NETWORK -> {
                            NetworkMediaScreen(onPlayMedia = onPlayMedia)
                        }
                        SourceType.APK_LIST -> {
                            APKInstallScreen(
                                onInstallApk = ::onInstallApkClicked,
                                onInstallApkBackup = ::onInstallApkBackupClicked
                            )
                        }
                        else -> {
                            if (parsedSourceType == SourceType.USB && mediaItems.value.isEmpty()) {
                                USBEmptyScreen {
                                    // 重新加载数据，在协程中调用挂起函数
                                    coroutineScope.launch {
                                        isLoading.value = true
                                        errorMessage.value = null
                                        try {
                                            mediaItems.value = repository.getMediaItems(parsedSourceType)
                                        } catch (e: Exception) {
                                            errorMessage.value = "错误: ${e.message}"
                                        }
                                        isLoading.value = false
                                    }
                                }
                            } else {
                                FileListScreen(
                                    mediaItems = mediaItems.value,
                                    currentPath = currentPath.value,
                                    onNavigateToFolder = { path ->
                                        currentPath.value = path
                                    },
                                    onPlayMedia = onPlayMedia,
                                    sourceType = parsedSourceType
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListScreen(
    mediaItems: List<MediaItem>,
    currentPath: String,
    onNavigateToFolder: (String) -> Unit,
    onPlayMedia: (String) -> Unit,
    sourceType: SourceType
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 路径导航
        if (currentPath.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前路径: $currentPath",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // 文件列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mediaItems) { item ->
                MediaItemCard(
                    mediaItem = item,
                    onClick = {
                        when (item.mediaType) {
                            MediaType.VIDEO, MediaType.AUDIO -> {
                                val encodedUri = URLEncoder.encode(item.uri, StandardCharsets.UTF_8.name())
                                onPlayMedia(encodedUri)
                            }
                            else -> {
                                // 如果是文件夹，进入文件夹
                                if (item.mediaType == MediaType.OTHER && item.title.endsWith("/")) {
                                    onNavigateToFolder(item.path)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NetworkMediaScreen(
    onPlayMedia: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "网络媒体播放",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "请输入视频或音频的URL地址：",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 这里可以添加URL输入框，为了简化先使用示例URL
        Card(
            onClick = { 
                val encodedUri = URLEncoder.encode("https://example.com/sample.mp4", StandardCharsets.UTF_8.name())
                onPlayMedia(encodedUri)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.VideoFile,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "示例网络视频",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "https://example.com/sample.mp4",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Card(
            onClick = { 
                val encodedUri = URLEncoder.encode("https://example.com/sample.mp3", StandardCharsets.UTF_8.name())
                onPlayMedia(encodedUri)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "示例网络音频",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "https://example.com/sample.mp3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaItemCard(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForMediaType(mediaItem.mediaType),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = formatFileInfo(mediaItem),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (mediaItem.mediaType == MediaType.VIDEO || mediaItem.mediaType == MediaType.AUDIO) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun getIconForMediaType(mediaType: MediaType): ImageVector {
    return when (mediaType) {
        MediaType.VIDEO -> Icons.Filled.VideoFile
        MediaType.AUDIO -> Icons.Filled.AudioFile
        MediaType.IMAGE -> Icons.Filled.Image
        MediaType.APK -> Icons.Filled.Android
        MediaType.OTHER -> Icons.Filled.InsertDriveFile
    }
}

@Composable
fun USBEmptyScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.padding(16.dp))
        
        Text(
            text = "未检测到USB设备",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "请确保：\n" +
                   "1. U盘已正确插入设备\n" +
                   "2. U盘已正确格式化\n" +
                   "3. 已授予应用存储访问权限\n" +
                   "4. U盘已挂载到系统",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("重新检测USB设备")
        }
        
        Text(
            text = "如果问题持续，请尝试重新插拔U盘或重启设备。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun PermissionErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.padding(16.dp))
        
        Text(
            text = "访问权限被拒绝",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "可能的原因：\n" +
                   "1. 目录权限不足\n" +
                   "2. 文件系统加密或受保护\n" +
                   "3. U盘文件系统格式不被支持\n" +
                   "4. Android系统限制访问某些目录",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("重试")
            }
            
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("返回上一级")
            }
        }
        
        Text(
            text = "如果问题持续，请检查系统权限设置或尝试重启设备。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

private fun formatFileInfo(mediaItem: MediaItem): String {
    return when (mediaItem.mediaType) {
        MediaType.VIDEO, MediaType.AUDIO -> {
            val size = FileUtils.formatFileSize(mediaItem.size)
            val duration = if (mediaItem.duration > 0) {
                " · ${FileUtils.formatDuration(mediaItem.duration)}"
            } else ""
            "$size$duration"
        }
        else -> FileUtils.formatFileSize(mediaItem.size)
    }
}

@Composable
fun APKInstallScreen(
    onInstallApk: () -> Unit,
    onInstallApkBackup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Android,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.padding(24.dp))
        
        Text(
            text = "安装APK应用",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "从U盘或其他位置选择APK文件进行安装",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "使用方法：\n" +
                   "1. 点击下方按钮打开文件选择器\n" +
                   "2. 浏览到U盘或本地存储中的APK文件\n" +
                   "3. 选择文件后系统将提示安装\n" +
                   "4. 按照系统指引完成安装",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        Button(
            onClick = onInstallApk,
            modifier = Modifier.fillMaxWidth(0.8f),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Android,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "选择APK文件",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.padding(16.dp))
        
        Button(
            onClick = onInstallApkBackup,
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "备用文件选择",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.padding(24.dp))
        
        Text(
            text = "注意：安装应用需要用户手动确认，请确保已授予安装未知来源应用的权限。如果主按钮无法选择文件，请尝试备用文件选择按钮。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
