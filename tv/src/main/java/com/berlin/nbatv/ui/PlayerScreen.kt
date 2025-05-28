package com.berlin.nbatv.ui // 替换为您的包名

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.berlin.nbatv.PornhubParserWithOkHttp.fetchActualVideoUrl

@Composable
fun PlayerScreen(
    videoPageUrl: String, // 视频流的 URL
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var actualVideoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // 我们将在获取到 actualVideoUrl 后设置 MediaItem
            playWhenReady = true // 准备好后自动播放
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Log.e("PlayerScreen", "ExoPlayer Error: ${error.message}", error)
                    errorMessage = "无法播放视频: ${error.message}"
                    isLoading = false // 停止加载状态，显示错误
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isLoading = false // 视频已准备好，隐藏加载指示器
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        isLoading = true // 视频缓冲中
                    }
                }
            })
        }
    }

    // 当 videoPageUrl 变化时，重新获取真实的视频 URL
    LaunchedEffect(videoPageUrl) {
        isLoading = true
        actualVideoUrl = null // 重置
        errorMessage = null
        Log.d("PlayerScreen", "Attempting to fetch actual video URL for: $videoPageUrl")
        val fetchedUrl = fetchActualVideoUrl(videoPageUrl) // 调用解析函数
        if (fetchedUrl != null) {
            Log.i("PlayerScreen", "Successfully fetched actual video URL: $fetchedUrl")
            actualVideoUrl = fetchedUrl
            exoPlayer.setMediaItem(MediaItem.fromUri(fetchedUrl))
            exoPlayer.prepare()
            // playWhenReady 已经是 true，所以会自动播放
        } else {
            Log.e("PlayerScreen", "Failed to fetch actual video URL for: $videoPageUrl")
            errorMessage = "无法获取视频流地址"
            isLoading = false
        }
    }

    // 当 Composable 离开屏幕时释放 ExoPlayer 资源
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            Log.d("PlayerScreen", "ExoPlayer released")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center // 用于居中加载指示器和错误信息
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, color = Color.Red)
        } else if (actualVideoUrl != null) {
            // 只有在获取到真实 URL 且没有错误时才显示 AndroidView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        // 如果 actualVideoUrl 为 null 且 isLoading 为 false 且 errorMessage 为 null，
        // 意味着解析成功但URL为空（理论上不应发生，因为 fetchActualVideoUrl 会返回 null）
        // 或者解析失败但没有设置 errorMessage 的情况，这里可以再加个通用错误提示
        else if (actualVideoUrl == null && !isLoading && errorMessage == null) {
            Text(text = "无法加载视频信息", color = Color.Yellow)
        }
    }
}