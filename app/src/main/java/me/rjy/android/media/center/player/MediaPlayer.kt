package me.rjy.android.media.center.player

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource

@Composable
fun rememberExoPlayer(
    uri: String,
    onError: (PlaybackException?) -> Unit = {},
    onPlaybackStateChanged: (Int) -> Unit = {},
    onIsPlayingChanged: (Boolean) -> Unit = {}
): ExoPlayer {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    onPlaybackStateChanged(playbackState)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onIsPlayingChanged(isPlaying)
                }

                override fun onPlayerError(error: PlaybackException) {
                    onError(error)
                }
            })
        }
    }

    DisposableEffect(uri) {
        val mediaItem = MediaItem.fromUri(Uri.parse(uri))
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.release()
        }
    }

    return exoPlayer
}

enum class PlaybackMode {
    NORMAL, REPEAT_ONE, REPEAT_ALL, SHUFFLE
}

class MediaPlayerController(
    private val exoPlayer: ExoPlayer
) {
    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun stop() {
        exoPlayer.stop()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun seekForward() {
        val currentPosition = exoPlayer.currentPosition
        val newPosition = currentPosition + 10000 // 快进10秒
        exoPlayer.seekTo(newPosition)
    }

    fun seekBackward() {
        val currentPosition = exoPlayer.currentPosition
        val newPosition = maxOf(0, currentPosition - 10000) // 快退10秒
        exoPlayer.seekTo(newPosition)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        when (mode) {
            PlaybackMode.NORMAL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = false
            }
            PlaybackMode.REPEAT_ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.shuffleModeEnabled = false
            }
            PlaybackMode.REPEAT_ALL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.shuffleModeEnabled = false
            }
            PlaybackMode.SHUFFLE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.shuffleModeEnabled = true
            }
        }
    }

    fun getCurrentPosition(): Long = exoPlayer.currentPosition
    fun getDuration(): Long = exoPlayer.duration
    fun isPlaying(): Boolean = exoPlayer.isPlaying
    fun getPlaybackState(): Int = exoPlayer.playbackState
}
