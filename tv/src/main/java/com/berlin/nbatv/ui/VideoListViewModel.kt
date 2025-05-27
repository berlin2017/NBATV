package com.berlin.nbatv.ui // 或者您放置ViewModel的包，例如 com.berlin.nbatv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berlin.nbatv.data.VideoItem // 确保路径正确
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoListViewModel : ViewModel() {

    // 私有的、可变的 StateFlow，用于 ViewModel 内部更新数据
    private val _videoList = MutableStateFlow<List<VideoItem>>(emptyList())

    // 公开的、不可变的 StateFlow，供 UI 观察
    val videoList: StateFlow<List<VideoItem>> = _videoList.asStateFlow()

    // (可选) 加载状态和错误状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // ViewModel 初始化时加载数据
        fetchVideoData()
    }

    // 从您的数据源获取数据的方法
    // 这里我们暂时使用之前 MainActivity 中的静态列表作为示例数据源
    // 将来您可以替换为网络请求、数据库查询或网页解析逻辑
    fun fetchVideoData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // 模拟数据加载延迟 (如果需要)
                // kotlinx.coroutines.delay(1000)

                // !!! 关键：将您之前在 MainActivity 中的 list 移到这里
                // 或者在这里实现从 https://jzb123.huajiaedu.com/ 解析的逻辑
                val items = listOf(
                    VideoItem(
                        id = 0, // 建议使用更唯一的 ID，例如视频名称或URL哈希
                        name = "TV1 from ViewModel",
                        description = "Description for TV1", // 添加描述
                        imageUrl = "https://images.unsplash.com/photo-1575936123452-b67c3203c357?fm=jpg&q=60&w=3000&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8aW1hZ2V8ZW58MHx8MHx8fDA%3D",
                        videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" // 替换为真实的视频流 URL
                    ),
                    VideoItem(
                        id = 1,
                        name = "TV2 from ViewModel",
                        description = "Description for TV2",
                        imageUrl = "https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885_1280.jpg", // 换个图片
                        videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                    ),
                    VideoItem(
                        id = 2,
                        name = "TV3 from ViewModel",
                        description = "Description for TV3",
                        imageUrl = "https://images.pexels.com/photos/206359/pexels-photo-206359.jpeg?cs=srgb&dl=pexels-pixabay-206359.jpg&fm=jpg", // 换个图片
                        videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                    ),
                    VideoItem(
                        id = 4,
                        name = "TV3 from ViewModel",
                        description = "Description for TV4",
                        imageUrl = "https://images.pexels.com/photos/206359/pexels-photo-206359.jpeg?cs=srgb&dl=pexels-pixabay-206359.jpg&fm=jpg", // 换个图片
                        videoUrl = "YOUR_M3U8_OR_VIDEO_STREAM_URL_3"
                    ),
                    VideoItem(
                        id = 5,
                        name = "TV3 from ViewModel",
                        description = "Description for TV5",
                        imageUrl = "https://images.pexels.com/photos/206359/pexels-photo-206359.jpeg?cs=srgb&dl=pexels-pixabay-206359.jpg&fm=jpg", // 换个图片
                        videoUrl = "YOUR_M3U8_OR_VIDEO_STREAM_URL_3"
                    ),
                    // ... 更多 VideoItem
                )
                _videoList.value = items

            } catch (e: Exception) {
                // 处理错误
                _errorMessage.value = "Failed to load videos: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 如果您有刷新数据的需求
    fun refreshVideos() {
        fetchVideoData()
    }
}