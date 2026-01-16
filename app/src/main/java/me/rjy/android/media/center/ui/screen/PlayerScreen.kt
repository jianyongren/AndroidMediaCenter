package me.rjy.android.media.center.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import me.rjy.android.media.center.player.MediaPlayerController
import me.rjy.android.media.center.player.PlaybackMode
import me.rjy.android.media.center.player.rememberExoPlayer
import me.rjy.android.media.center.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaUri: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    val exoPlayer = rememberExoPlayer(
        uri = mediaUri,
        onError = { error ->
            // 处理播放错误
        },
        onPlaybackStateChanged = { state ->
            // 播放状态变化
        },
        onIsPlayingChanged = { isPlaying ->
            // 播放/暂停状态变化
        }
    )
    
    val playerController = remember { MediaPlayerController(exoPlayer) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var playbackMode by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTimerActive by remember { mutableStateOf(true) }
    
    // 检测横屏/TV自动进入全屏
    LaunchedEffect(isLandscape) {
        if (isLandscape && !isFullscreen) {
            isFullscreen = true
        }
    }
    
    // 控制UI自动隐藏逻辑
    LaunchedEffect(showControls, controlsTimerActive) {
        if (showControls && controlsTimerActive) {
            delay(3000L) // 3秒后隐藏控制UI
            if (controlsTimerActive) {
                showControls = false
            }
        }
    }
    
    // 监听播放状态
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentPosition = exoPlayer.currentPosition.toFloat()
            duration = exoPlayer.duration.toFloat().coerceAtLeast(1f)
            playbackMode = exoPlayer.repeatMode
            kotlinx.coroutines.delay(500)
        }
    }
    
    // 全屏模式系统UI控制
    LaunchedEffect(isFullscreen) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        window?.let {
            val insetsController = WindowCompat.getInsetsController(it, it.decorView)
            if (isFullscreen) {
                // 隐藏系统状态栏和导航栏
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = 
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // 显示系统状态栏和导航栏
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    // 全屏布局
    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 点击屏幕切换控制UI显示
                    showControls = !showControls
                    if (showControls) {
                        controlsTimerActive = true
                    }
                }
        ) {
            // 播放器视图
            AndroidView(
                factory = { context ->
                    StyledPlayerView(context).apply {
                        player = exoPlayer
                        useController = false // 使用自定义控制器
                        setBackgroundColor(Color.Black.hashCode())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 浮动控制UI
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    // 顶部控制栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回按钮
                        IconButton(
                            onClick = {
                                if (isFullscreen) {
                                    isFullscreen = false
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = if (isFullscreen) "退出全屏" else "返回",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 标题
                        Text(
                            text = "正在播放",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // 全屏切换按钮
                        IconButton(
                            onClick = {
                                isFullscreen = !isFullscreen
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = "退出全屏",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 播放进度
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Slider(
                            value = currentPosition,
                            onValueChange = { newValue ->
                                currentPosition = newValue
                                playerController.seekTo(newValue.toLong())
                                showControls = true
                                controlsTimerActive = true
                            },
                            valueRange = 0f..duration,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = FileUtils.formatDuration(currentPosition.toLong()),
                                color = Color.White
                            )
                            Text(
                                text = FileUtils.formatDuration(duration.toLong()),
                                color = Color.White
                            )
                        }
                    }
                    
                    // 播放控制按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 循环模式按钮
                        IconButton(
                            onClick = {
                                val nextMode = when (playbackMode) {
                                    Player.REPEAT_MODE_OFF -> PlaybackMode.REPEAT_ONE
                                    Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ALL
                                    Player.REPEAT_MODE_ALL -> PlaybackMode.SHUFFLE
                                    else -> PlaybackMode.NORMAL
                                }
                                playerController.setPlaybackMode(nextMode)
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = when (playbackMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                    Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
                                    else -> Icons.Filled.Repeat
                                },
                                contentDescription = "循环模式",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 上一首
                        IconButton(
                            onClick = {
                                // 实现上一首逻辑
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "上一首",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 快退10秒
                        IconButton(
                            onClick = {
                                playerController.seekBackward()
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Replay10,
                                contentDescription = "快退10秒",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // 播放/暂停
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    playerController.pause()
                                } else {
                                    playerController.play()
                                }
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // 快进10秒
                        IconButton(
                            onClick = {
                                playerController.seekForward()
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Forward10,
                                contentDescription = "快进10秒",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 下一首
                        IconButton(
                            onClick = {
                                // 实现下一首逻辑
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "下一首",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 随机播放
                        IconButton(
                            onClick = {
                                playerController.setPlaybackMode(PlaybackMode.SHUFFLE)
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = "随机播放",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    } else {
        // 非全屏布局（原布局）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 播放器视图区域（可点击进入全屏）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color.Black)
                    .clickable {
                        isFullscreen = true
                        showControls = true
                        controlsTimerActive = true
                    }
            ) {
                AndroidView(
                    factory = { context ->
                        StyledPlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 全屏按钮
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = {
                            isFullscreen = true
                            showControls = true
                            controlsTimerActive = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = "全屏",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 播放进度
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Slider(
                    value = currentPosition,
                    onValueChange = { newValue ->
                        currentPosition = newValue
                        playerController.seekTo(newValue.toLong())
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FileUtils.formatDuration(currentPosition.toLong()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FileUtils.formatDuration(duration.toLong()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 播放控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 循环模式按钮
                IconButton(
                    onClick = {
                        val nextMode = when (playbackMode) {
                            Player.REPEAT_MODE_OFF -> PlaybackMode.REPEAT_ONE
                            Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ALL
                            Player.REPEAT_MODE_ALL -> PlaybackMode.SHUFFLE
                            else -> PlaybackMode.NORMAL
                        }
                        playerController.setPlaybackMode(nextMode)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (playbackMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "循环模式",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 上一首
                IconButton(
                    onClick = {
                        // 实现上一首逻辑
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 快退10秒
                IconButton(
                    onClick = {
                        playerController.seekBackward()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Replay10,
                        contentDescription = "快退10秒",
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 播放/暂停
                Button(
                    onClick = {
                        if (isPlaying) {
                            playerController.pause()
                        } else {
                            playerController.play()
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 快进10秒
                IconButton(
                    onClick = {
                        playerController.seekForward()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Forward10,
                        contentDescription = "快进10秒",
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 下一首
                IconButton(
                    onClick = {
                        // 实现下一首逻辑
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 随机播放
                IconButton(
                    onClick = {
                        playerController.setPlaybackMode(PlaybackMode.SHUFFLE)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "随机播放",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 媒体信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "媒体信息",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "URI: $mediaUri",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "格式: ${FileUtils.getFileExtension(mediaUri)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 返回按钮（在底部）
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(text = "返回")
            }
        }
    }
}
