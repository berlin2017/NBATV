package com.berlin.nbatv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme.colorScheme
import com.berlin.nbatv.data.VideoItem
import com.berlin.nbatv.ui.theme.NBATVTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen( // 可能您的函数名不同
    onVideoClick: (VideoItem) -> Unit, // 添加这个回调参数
    modifier: Modifier = Modifier, viewModel: VideoListViewModel = viewModel() // 1. 注入/获取 ViewModel
) {
    // 2. 收集 ViewModel 中的状态
    val videoList by viewModel.videoList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // (可选) 如果你希望在 Composable 进入屏幕时自动调用刷新，
    // 但在 ViewModel 的 init 块中调用通常更好，除非有特定触发条件
    // LaunchedEffect(Unit) {
    //     viewModel.fetchVideoData()
    // }

    NBATVTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = colorScheme.primary,
                        titleContentColor = colorScheme.primary,
                    ), title = {
                        Text("NBA", color = Color.White)
                    })
            },
        ) { innerPadding ->
            // 在这里您需要决定如何提供 VideoItem 列表给 VideoListScreen
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center // 居中加载指示器和错误信息
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(
                        text = "Error: $errorMessage",
                        color = colorScheme.error
                    )
                } else if (videoList.isEmpty()) {
                    Text(text = "No videos found.")
                } else {
                    // 3. 使用从 ViewModel 获取的 videoList
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3), // 或其他你想要的列数
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(videoList) { videoItem ->
                            VideoItemPreview( // 假设这是您显示单个视频项的 Composable
                                item = videoItem, onClick = { onVideoClick(videoItem) })
                        }
                    }
                }
            }
        }
    }


}