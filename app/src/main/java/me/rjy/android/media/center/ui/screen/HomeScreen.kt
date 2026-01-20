package me.rjy.android.media.center.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rjy.android.media.center.data.model.SourceType

data class HomeMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val sourceType: SourceType
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFileBrowser: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val menuItems = listOf(
        HomeMenuItem(
            title = "内部存储",
            description = "浏览设备内部存储中的媒体文件",
            icon = Icons.Filled.Storage,
            sourceType = SourceType.INTERNAL_STORAGE
        ),
        HomeMenuItem(
            title = "U盘/USB设备",
            description = "浏览连接的USB存储设备中的媒体文件",
            icon = Icons.Filled.Usb,
            sourceType = SourceType.USB
        ),
        HomeMenuItem(
            title = "网络媒体",
            description = "通过URL播放网络视频和音频",
            icon = Icons.Filled.Wifi,
            sourceType = SourceType.NETWORK
        ),
        HomeMenuItem(
            title = "最近播放",
            description = "查看最近播放的媒体文件",
            icon = Icons.Filled.PlayCircle,
            sourceType = SourceType.RECENT
        ),
        HomeMenuItem(
            title = "视频",
            description = "所有视频文件",
            icon = Icons.Filled.VideoLibrary,
            sourceType = SourceType.VIDEO_LIST
        ),
        HomeMenuItem(
            title = "音频",
            description = "所有音频文件",
            icon = Icons.Filled.MusicNote,
            sourceType = SourceType.AUDIO_LIST
        ),
        HomeMenuItem(
            title = "应用安装",
            description = "从U盘安装APK文件",
            icon = Icons.Filled.Computer,
            sourceType = SourceType.APK_LIST
        ),
    )

    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    
    // 检测是否为横屏、大屏或TV模式
    val screenLayout = configuration.screenLayout
    val isLargeScreen = (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= 
                        Configuration.SCREENLAYOUT_SIZE_LARGE
    
    // 增强横屏检测：优先使用方向检测，备用使用宽高比检测
    val isLandscapeByOrientation = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // 备用检测：通过屏幕宽高比判断（横屏时宽度大于高度）
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscapeByAspect = screenWidthDp > screenHeightDp
    val isLandscape = isLandscapeByOrientation || isLandscapeByAspect
    
    val uiMode = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    val isTvMode = uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    
    // 如果是TV模式、大屏设备、或横屏模式，使用两列布局
    // 增加宽度阈值：当屏幕宽度大于等于600dp时也使用两列布局（适应大屏设备）
    val isWideScreen = screenWidthDp >= 600
    val useTwoColumn = isTvMode || isLargeScreen || isLandscape || isWideScreen

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体中心", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(28.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "选择媒体来源",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            if (useTwoColumn) {
                // 两列布局 - 使用固定网格（每行两个项目）
                val chunkedItems = menuItems.chunked(2)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(chunkedItems) { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 第一列
                            HomeMenuItemCard(
                                item = rowItems[0],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onNavigateToFileBrowser(rowItems[0].sourceType.name)
                                }
                            )
                            
                            // 第二列（如果有）
                            if (rowItems.size > 1) {
                                HomeMenuItemCard(
                                    item = rowItems[1],
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onNavigateToFileBrowser(rowItems[1].sourceType.name)
                                    }
                                )
                            } else {
                                // 单数项目时，第二列留空保持对齐
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // 单列布局
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(menuItems) { item ->
                        HomeMenuItemCard(
                            item = item,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onNavigateToFileBrowser(item.sourceType.name)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuItemCard(
    item: HomeMenuItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = "进入",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
