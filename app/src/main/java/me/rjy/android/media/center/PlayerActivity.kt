package me.rjy.android.media.center

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import me.rjy.android.media.center.ui.screen.PlayerScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class PlayerActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_MEDIA_URIS = "extra_media_uris"
        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
        
        fun createIntent(
            context: android.content.Context,
            mediaUris: List<String>,
            currentIndex: Int = 0
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_MEDIA_URIS, ArrayList(mediaUris))
                putExtra(EXTRA_CURRENT_INDEX, currentIndex)
            }
        }
        
        fun createIntentForSingleMedia(
            context: android.content.Context,
            mediaUri: String
        ): Intent {
            return createIntent(context, listOf(mediaUri), 0)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mediaUris = intent.getStringArrayListExtra(EXTRA_MEDIA_URIS) ?: listOf()
        val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        
        // 解码URI（如果是从MainActivity传递过来的编码URI）
        val decodedUris = mediaUris.map { uri ->
            try {
                URLDecoder.decode(uri, StandardCharsets.UTF_8.name())
            } catch (e: Exception) {
                uri
            }
        }
        
        setContent {
            val onNavigateBack = remember {
                { finish() }
            }
            
            PlayerScreen(
                mediaUris = decodedUris,
                currentIndex = currentIndex,
                onNavigateBack = onNavigateBack
            )
        }
    }
}