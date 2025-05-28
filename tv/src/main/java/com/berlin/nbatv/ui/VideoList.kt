package com.berlin.nbatv.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val isLoadingMore by viewModel.isLoadingMore.collectAsState() // 新增
    val errorMessage by viewModel.errorMessage.collectAsState()

    val lazyGridState = rememberLazyGridState() // 记住 LazyGrid 的状态

    // (可选) 如果你希望在 Composable 进入屏幕时自动调用刷新，
    // 但在 ViewModel 的 init 块中调用通常更好，除非有特定触发条件
    // LaunchedEffect(Unit) {
    //     viewModel.fetchVideoData()
    // }


    // 当列表滚动到接近底部时触发加载更多
    // 这个 LaunchedEffect 会在 lazyGridState.layoutInfo 变化时重新执行
    LaunchedEffect(lazyGridState.layoutInfo.visibleItemsInfo, videoList.size) {
        // 获取可见的最后一个项目的索引
        val lastVisibleItemIndex = lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        if (lastVisibleItemIndex != null) {
            // 当最后一个可见项接近列表末尾时 (例如，在最后 5 个项内)，并且没有在加载中，并且列表不为空
            val threshold = 5 // 可以调整这个阈值
            if (videoList.isNotEmpty() && lastVisibleItemIndex >= videoList.size - 1 - threshold && !isLoading && !isLoadingMore) {
                Log.d(
                    "VideoListScreen",
                    "Approaching end of list (lastVisible: $lastVisibleItemIndex, total: ${videoList.size}), triggering load more."
                )
                viewModel.loadMoreVideos()
            }
        }
    }

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
                if (isLoading && videoList.isEmpty()) { // 初始加载时显示主加载指示器
                    CircularProgressIndicator()
                } else if (errorMessage != null && videoList.isEmpty()) { // 初始加载失败
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: $errorMessage",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.refreshVideos() }) {
                            Text("Retry")
                        }
                    }
                } else if (videoList.isEmpty() && !isLoading) { // 没有数据且不在加载中
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No videos found.")
                        Button(onClick = { viewModel.refreshVideos() }) {
                            Text("Refresh")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3), // 或其他你想要的列数
                        state = lazyGridState, // 将状态传递给 LazyGrid
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(
                            videoList,
                            key = { _, item ->
                                item.videoUrl ?: "" /* 使用唯一键 */
                            }) { index, videoItem ->
                            VideoItemPreview(
                                item = videoItem,
                                onClick = { onVideoClick(videoItem) }
                            )
                        }

                        // 在列表底部添加加载更多的指示器或错误提示
                        if (isLoadingMore) {
                            item(
                                span = { GridItemSpan(maxLineSpan) } // maxLineSpan 相当于当前行的最大可用跨度
                            ) { // LazyGridScope.item
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (errorMessage != null && videoList.isNotEmpty()) { // 加载更多失败
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Error loading more: $errorMessage",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Button(onClick = { viewModel.loadMoreVideos() }) { // 点击重试加载更多
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}