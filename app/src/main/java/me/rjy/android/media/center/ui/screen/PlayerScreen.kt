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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import me.rjy.android.media.center.player.MediaPlayerController
import me.rjy.android.media.center.player.PlaybackMode
import me.rjy.android.media.center.player.rememberExoPlayer
import me.rjy.android.media.center.utils.FileUtils

sealed class PlayerState {
    object IDLE : PlayerState()
    object BUFFERING : PlayerState()
    object READY : PlayerState()
    object PLAYING : PlayerState()
    object PAUSED : PlayerState()
    object ENDED : PlayerState()
    data class ERROR(val error: PlaybackException?) : PlayerState()
}

@Composable
fun BufferingIndicator(
    modifier: Modifier = Modifier,
    progress: Int = 0
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (progress > 0) "正在缓冲... $progress%" else "正在缓冲...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ErrorOverlay(
    modifier: Modifier = Modifier,
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = "错误",
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "播放错误",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage ?: "未知错误",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text(text = "返回")
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "重试",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "重试")
                    }
                }
            }
        }
    }
}

@Composable
fun EndOverlay(
    modifier: Modifier = Modifier,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                text = "播放结束",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text(text = "返回")
                }
                Button(
                    onClick = onReplay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "重播",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "重播")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaUris: List<String>,
    currentIndex: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    var playerState by remember { mutableStateOf<PlayerState>(PlayerState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var bufferingProgress by remember { mutableIntStateOf(0) }
    
    var isPlayingRealTime by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTimerActive by remember { mutableStateOf(true) }
    
    var currentPlaybackMode by remember { mutableStateOf(PlaybackMode.NORMAL) }
    var currentMediaIndex by remember { mutableIntStateOf(currentIndex) }
    
    val exoPlayer = rememberExoPlayer(
        uris = mediaUris,
        currentIndex = currentIndex,
        onError = { error ->
            playerState = PlayerState.ERROR(error)
            errorMessage = error?.message ?: "未知播放错误"
            // 自动重置错误状态（例如10秒后）
            // 这里可以根据错误类型提供不同的处理
        },
        onPlaybackStateChanged = { state ->
            when (state) {
                Player.STATE_IDLE -> playerState = PlayerState.IDLE
                Player.STATE_BUFFERING -> playerState = PlayerState.BUFFERING
                Player.STATE_READY -> {
                    if (isPlayingRealTime) {
                        playerState = PlayerState.PLAYING
                    } else {
                        playerState = PlayerState.READY
                    }
                }
                Player.STATE_ENDED -> {
                    // 播放结束，设置结束状态，并显示控制UI
                    playerState = PlayerState.ENDED
                    showControls = true
                    controlsTimerActive = false
                }
            }
        },
        onIsPlayingChanged = { isPlaying ->
            isPlayingRealTime = isPlaying
            if (isPlaying && playerState is PlayerState.READY) {
                playerState = PlayerState.PLAYING
            } else if (!isPlaying && playerState is PlayerState.PLAYING) {
                playerState = PlayerState.PAUSED
            }
        },
        onMediaItemTransition = { index ->
            currentMediaIndex = index
            // 如果当前播放模式是顺序播放（NORMAL）且播放到了最后一个媒体，则播放结束后不循环
            // 对于其他模式，ExoPlayer会根据设置自动处理
        }
    )
    
    val playerController = remember { MediaPlayerController(exoPlayer) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var exoPlaybackMode by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    
    // 初始化MediaPlayerController的媒体数量
    LaunchedEffect(mediaUris.size) {
        playerController.setMediaCount(mediaUris.size)
    }
    
    // 是否为单视频
    val isSingleMedia = mediaUris.size <= 1
    
    // 检测横屏/TV自动进入全屏
    LaunchedEffect(isLandscape) {
        if (isLandscape && !isFullscreen) {
            isFullscreen = true
        }
    }
    
    // 控制UI自动隐藏逻辑
    LaunchedEffect(showControls, controlsTimerActive) {
        if (showControls && controlsTimerActive) {
            delay(5000L) // 5秒后隐藏控制UI
            if (controlsTimerActive) {
                showControls = false
            }
        }
    }
    
    // 监听播放状态
    // 自动开始播放
    LaunchedEffect(exoPlayer) {
        if (!exoPlayer.isPlaying) {
            playerController.play()
        }
        
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentPosition = exoPlayer.currentPosition.toFloat()
            duration = exoPlayer.duration.toFloat().coerceAtLeast(1f)
            exoPlaybackMode = exoPlayer.repeatMode
            delay(500)
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
            
            // 缓冲指示器
            AnimatedVisibility(
                visible = playerState is PlayerState.BUFFERING,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                BufferingIndicator(
                    progress = bufferingProgress,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                )
            }
            
            // 错误覆盖层
            AnimatedVisibility(
                visible = playerState is PlayerState.ERROR,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                ErrorOverlay(
                    errorMessage = errorMessage,
                    onRetry = {
                        // 重试逻辑：重新准备播放器
                        exoPlayer.prepare()
                        playerState = PlayerState.IDLE
                        errorMessage = null
                    },
                    onBack = {
                        if (isFullscreen) {
                            isFullscreen = false
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                )
            }
            
            // 播放结束覆盖层
            AnimatedVisibility(
                visible = playerState is PlayerState.ENDED,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                EndOverlay(
                    onReplay = {
                        // 重播逻辑：定位到开始位置并播放
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                        playerState = PlayerState.PLAYING
                        showControls = true
                        controlsTimerActive = true
                    },
                    onBack = {
                        if (isFullscreen) {
                            isFullscreen = false
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                )
            }
            
            // 浮动控制UI
            AnimatedVisibility(
                visible = showControls || playerState is PlayerState.ENDED,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    // 顶部控制栏 - 减少高度
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
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
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
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
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = "退出全屏",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // 进一步增加视频空间，减少顶部控制栏高度
                    Spacer(modifier = Modifier.weight(1.5f))
                    
                    // 播放进度 - 进一步下移
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
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
                    
                    // 播放控制按钮 - 更靠近底部
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 循环模式按钮 - 整合随机播放
                        IconButton(
                            onClick = {
                                // 计算下一个模式
                                val nextMode = when (currentPlaybackMode) {
                                    PlaybackMode.NORMAL -> {
                                        if (isSingleMedia) PlaybackMode.REPEAT_ONE else PlaybackMode.REPEAT_ALL
                                    }
                                    PlaybackMode.REPEAT_ONE -> {
                                        if (isSingleMedia) PlaybackMode.NORMAL else PlaybackMode.REPEAT_ALL
                                    }
                                    PlaybackMode.REPEAT_ALL -> {
                                        if (isSingleMedia) PlaybackMode.NORMAL else PlaybackMode.SHUFFLE
                                    }
                                    PlaybackMode.SHUFFLE -> PlaybackMode.NORMAL
                                }
                                currentPlaybackMode = nextMode
                                playerController.setPlaybackMode(nextMode)
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = when (currentPlaybackMode) {
                                    PlaybackMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                                    PlaybackMode.REPEAT_ALL -> Icons.Filled.Repeat
                                    PlaybackMode.SHUFFLE -> Icons.Filled.Shuffle
                                    else -> Icons.Filled.Repeat // NORMAL模式显示普通重复图标
                                },
                                contentDescription = when (currentPlaybackMode) {
                                    PlaybackMode.REPEAT_ONE -> "单曲循环"
                                    PlaybackMode.REPEAT_ALL -> "列表循环"
                                    PlaybackMode.SHUFFLE -> "随机播放"
                                    else -> "顺序播放"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 上一首
                        IconButton(
                            onClick = {
                                playerController.previous()
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
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
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Replay10,
                                contentDescription = "快退10秒",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                // 播放/暂停/重播
                IconButton(
                    onClick = {
                        if (playerState is PlayerState.ENDED) {
                            // 重播逻辑：定位到开始位置并播放
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                            playerState = PlayerState.PLAYING
                            showControls = true
                            controlsTimerActive = true
                        } else {
                            if (isPlaying) {
                                playerController.pause()
                            } else {
                                playerController.play()
                            }
                            showControls = true
                            controlsTimerActive = true
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (playerState is PlayerState.ENDED) Icons.Filled.Refresh else if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState is PlayerState.ENDED) "重播" else if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(32.dp)
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
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
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
                                playerController.next()
                                showControls = true
                                controlsTimerActive = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "下一首",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    // 最小化底部空间，让控制按钮紧贴底部
                    Spacer(modifier = Modifier.height(8.dp))
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
            // 添加状态栏高度的顶部padding，防止状态栏遮盖视频和全屏按钮
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
                    .statusBarsPadding()
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
                
                // 缓冲指示器（非全屏）
                if (playerState is PlayerState.BUFFERING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                    ) {
                        BufferingIndicator(progress = bufferingProgress)
                    }
                }
                
                // 错误覆盖层（非全屏）
                if (playerState is PlayerState.ERROR) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f))
                    ) {
                        ErrorOverlay(
                            errorMessage = errorMessage,
                            onRetry = {
                                exoPlayer.prepare()
                                playerState = PlayerState.IDLE
                                errorMessage = null
                            },
                            onBack = onNavigateBack
                        )
                    }
                }
                
                // 播放结束覆盖层（非全屏）
                if (playerState is PlayerState.ENDED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                    ) {
                        EndOverlay(
                            onReplay = {
                                // 重播逻辑：定位到开始位置并播放
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                                playerState = PlayerState.PLAYING
                                showControls = true
                                controlsTimerActive = true
                            },
                            onBack = onNavigateBack
                        )
                    }
                }
                
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
                        // 如果处于播放结束状态，开始播放
                        if (playerState is PlayerState.ENDED) {
                            exoPlayer.play()
                            playerState = PlayerState.PLAYING
                            showControls = true
                            controlsTimerActive = true
                        }
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
                // 循环模式按钮 - 整合随机播放
                IconButton(
                    onClick = {
                        // 计算下一个模式
                        val nextMode = when (currentPlaybackMode) {
                            PlaybackMode.NORMAL -> {
                                if (isSingleMedia) PlaybackMode.REPEAT_ONE else PlaybackMode.REPEAT_ALL
                            }
                            PlaybackMode.REPEAT_ONE -> {
                                if (isSingleMedia) PlaybackMode.NORMAL else PlaybackMode.REPEAT_ALL
                            }
                            PlaybackMode.REPEAT_ALL -> {
                                if (isSingleMedia) PlaybackMode.NORMAL else PlaybackMode.SHUFFLE
                            }
                            PlaybackMode.SHUFFLE -> PlaybackMode.NORMAL
                        }
                        currentPlaybackMode = nextMode
                        playerController.setPlaybackMode(nextMode)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (currentPlaybackMode) {
                            PlaybackMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                            PlaybackMode.REPEAT_ALL -> Icons.Filled.Repeat
                            PlaybackMode.SHUFFLE -> Icons.Filled.Shuffle
                            else -> Icons.Filled.Repeat // NORMAL模式显示普通重复图标
                        },
                        contentDescription = when (currentPlaybackMode) {
                            PlaybackMode.REPEAT_ONE -> "单曲循环"
                            PlaybackMode.REPEAT_ALL -> "列表循环"
                            PlaybackMode.SHUFFLE -> "随机播放"
                            else -> "顺序播放"
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 上一首
                IconButton(
                    onClick = {
                        playerController.previous()
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
                
                // 播放/暂停/重播
                IconButton(
                    onClick = {
                        if (playerState is PlayerState.ENDED) {
                            // 重播逻辑：定位到开始位置并播放
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                            playerState = PlayerState.PLAYING
                            showControls = true
                            controlsTimerActive = true
                        } else {
                            if (isPlaying) {
                                playerController.pause()
                            } else {
                                playerController.play()
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (playerState is PlayerState.ENDED) Icons.Filled.Refresh else if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState is PlayerState.ENDED) "重播" else if (isPlaying) "暂停" else "播放",
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
                        playerController.next()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(32.dp)
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
                        text = "当前: ${currentMediaIndex + 1}/${mediaUris.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mediaUris.isNotEmpty() && currentMediaIndex < mediaUris.size) {
                        val currentUri = mediaUris[currentMediaIndex]
                        Text(
                            text = "URI: $currentUri",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "格式: ${FileUtils.getFileExtension(currentUri)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
