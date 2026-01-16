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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val autoScanUSB = remember { mutableStateOf(true) }
    val showHiddenFiles = remember { mutableStateOf(false) }
    val rememberPlaybackPosition = remember { mutableStateOf(true) }
    val enableTVMode = remember { mutableStateOf(false) }
    val autoPlayNext = remember { mutableStateOf(true) }
    val clearCacheOnExit = remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "设置",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "播放设置",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            SettingItemCard(
                title = "记住播放位置",
                description = "下次播放时从上次停止的位置开始",
                icon = Icons.Filled.PlayCircle,
                checked = rememberPlaybackPosition.value,
                onCheckedChange = { rememberPlaybackPosition.value = it }
            )
            
            SettingItemCard(
                title = "自动播放下一集",
                description = "当前媒体播放完成后自动播放下一个",
                icon = Icons.Filled.TheaterComedy,
                checked = autoPlayNext.value,
                onCheckedChange = { autoPlayNext.value = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "文件管理",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            SettingItemCard(
                title = "自动扫描USB设备",
                description = "插入USB设备时自动扫描媒体文件",
                icon = Icons.Filled.Storage,
                checked = autoScanUSB.value,
                onCheckedChange = { autoScanUSB.value = it }
            )
            
            SettingItemCard(
                title = "显示隐藏文件",
                description = "在文件浏览器中显示隐藏的文件和文件夹",
                icon = Icons.Filled.Visibility,
                checked = showHiddenFiles.value,
                onCheckedChange = { showHiddenFiles.value = it }
            )
            
            SettingItemCard(
                title = "退出时清空缓存",
                description = "退出应用时自动清空播放缓存",
                icon = Icons.Filled.AutoDelete,
                checked = clearCacheOnExit.value,
                onCheckedChange = { clearCacheOnExit.value = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "界面设置",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            SettingItemCard(
                title = "TV模式",
                description = "启用电视优化界面和导航",
                icon = Icons.Filled.Notifications,
                checked = enableTVMode.value,
                onCheckedChange = { enableTVMode.value = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "关于",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Android媒体中心",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "一个功能强大的媒体播放和管理应用，支持本地和USB设备播放，适用于手机和电视。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItemCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
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
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
