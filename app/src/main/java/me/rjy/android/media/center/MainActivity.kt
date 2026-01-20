package me.rjy.android.media.center

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.rjy.android.media.center.ui.screen.HomeScreen
import me.rjy.android.media.center.ui.screen.FileBrowserScreen
import me.rjy.android.media.center.ui.screen.PlayerScreen
import me.rjy.android.media.center.ui.screen.SettingsScreen
import me.rjy.android.media.center.ui.theme.AndroidMediaCenterTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private var hasStoragePermission by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasStoragePermission = allGranted
        if (!allGranted) {
            // 权限被拒绝，可以在这里显示说明或再次请求
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 检查并请求存储权限
        checkAndRequestStoragePermission()
        
        setContent {
            AndroidMediaCenterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasStoragePermission) {
                        MediaCenterApp()
                    } else {
                        PermissionRequestScreen {
                            requestStoragePermission()
                        }
                    }
                }
            }
        }
    }
    
    private fun checkAndRequestStoragePermission() {
        val permissions = getRequiredPermissions().toTypedArray()
        
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        hasStoragePermission = allGranted
        
        if (!allGranted) {
            // 延迟请求权限，避免在onCreate中立即请求
            requestPermissionLauncher.launch(permissions)
        }
    }
    
    private fun requestStoragePermission() {
        val permissions = getRequiredPermissions().toTypedArray()
        requestPermissionLauncher.launch(permissions)
    }
    
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要专门的媒体权限
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 12及以下需要外部存储权限
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        return permissions
    }
}

@Composable
fun MediaCenterApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToFileBrowser = { sourceType ->
                    navController.navigate("fileBrowser/$sourceType")
                },
                onNavigateToPlayer = { mediaUri ->
                    val encodedUri = URLEncoder.encode(mediaUri, StandardCharsets.UTF_8.name())
                    navController.navigate("player/$encodedUri")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("fileBrowser/{sourceType}") { backStackEntry ->
            val sourceType = backStackEntry.arguments?.getString("sourceType") ?: "INTERNAL_STORAGE"
            FileBrowserScreen(
                sourceType = sourceType,
                onNavigateBack = { navController.popBackStack() },
                onPlayMedia = { mediaUri ->
                    navController.navigate("player/$mediaUri")
                }
            )
        }
        composable("player/{mediaUri}") { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("mediaUri") ?: ""
            val mediaUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.name())
            PlayerScreen(
                mediaUris = listOf(mediaUri),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要存储权限",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "为了正常浏览和播放您设备上的媒体文件（视频、音频、图片），需要获取存储访问权限。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "请点击下方按钮授予权限，然后应用将重新启动以加载您的媒体文件。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("授予存储权限")
        }
        
        Text(
            text = "注意：如果不授予权限，将无法浏览本地媒体文件。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MediaCenterAppPreview() {
    AndroidMediaCenterTheme {
        MediaCenterApp()
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRequestScreenPreview() {
    AndroidMediaCenterTheme {
        PermissionRequestScreen(onRequestPermission = {})
    }
}
