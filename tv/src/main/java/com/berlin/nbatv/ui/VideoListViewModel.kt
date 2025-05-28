package com.berlin.nbatv.ui // 或者您放置ViewModel的包，例如 com.berlin.nbatv.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berlin.nbatv.PornhubParserWithOkHttp
import com.berlin.nbatv.data.AppDatabase
import com.berlin.nbatv.data.VideoDao
import com.berlin.nbatv.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val videoDao: VideoDao = AppDatabase.getDatabase(application).videoDao()

    // 假设我们为 "new" 类别视频缓存
    private val currentCategory = "xvideos_new" // 用于区分不同来源/类别的缓存

    // 私有的、可变的 StateFlow，用于 ViewModel 内部更新数据
    private val _videoList = MutableStateFlow<List<VideoItem>>(emptyList())

    // 公开的、不可变的 StateFlow，供 UI 观察
    val videoList: StateFlow<List<VideoItem>> = _videoList.asStateFlow()

    // (可选) 加载状态和错误状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false) // 用于上拉加载的特定状态
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false // 标记是否已到达最后一页

    // 新的基础 URL (Xvideos new)
    private val baseCategoryUrl = "https://www.xvideos.com/new/"
    private val itemsPerPageForCacheLimit = 100 // 例如，每个分类最多缓存100条


    init {
        observeLocalData() // 开始观察本地数据
        // ViewModel 初始化时加载数据
        fetchVideoData(isRefresh = true)
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            videoDao.getVideosByCategory(currentCategory)
                .catch { e ->
                    Log.e(
                        "ViewModel",
                        "Error observing local data for $currentCategory: ${e.localizedMessage}",
                        e
                    )
                    _errorMessage.value = "无法加载本地缓存: ${e.localizedMessage}"
                }
                .collect { videosFromDb ->
                    Log.d(
                        "ViewModel",
                        "Loaded ${videosFromDb.size} videos from DB for category $currentCategory"
                    )
                    // 如果列表为空或者正在进行刷新，则直接用数据库数据更新UI
                    // 否则，需要更复杂的合并逻辑（这里简化为如果网络加载中，则等待网络结果）
                    if (_videoList.value.isEmpty() || !_isLoading.value) {
                        _videoList.value = videosFromDb
                    }
                    // 如果本地数据非空，可以认为不是“最后一页”，除非网络确认了
                    isLastPage = false // 重置，让网络请求可以进行
                }
        }
    }

    fun fetchVideoData(isRefresh: Boolean = false) {
        if (isLoading.value || isLoadingMore.value || (isLastPage && !isRefresh)) {
            Log.d(
                "ViewModel",
                "Fetch skipped: loading=${isLoading.value}, loadingMore=${isLoadingMore.value}, isLastPage=$isLastPage, isRefresh=$isRefresh"
            )
            return
        }

        viewModelScope.launch {
            val pageToFetch: Int
            if (isRefresh) {
                _isLoading.value = true
                pageToFetch = 1
                // 不立即清除 _videoList.value，让本地数据显示，直到网络数据返回
            } else {
                _isLoadingMore.value = true
                pageToFetch = currentPage // 使用当前的 currentPage 加载更多
            }
            _errorMessage.value = null

            // 构建目标 URL
            // 如果第一页就是 baseCategoryUrl (不带页码)
            // 如果页码大于1，则附加页码
            val targetUrl =
                "${baseCategoryUrl}${pageToFetch}/" // 例如 https://www.xvideos.com/new/2/ (注意末尾的斜杠，根据实际URL格式调整)
            // 如果第一页的URL也是 https://www.xvideos.com/new/1/，则可以直接用下面的
            // val targetUrl = "${baseCategoryUrl}${currentPage}/"

            Log.d("ViewModel", "Fetching page: $currentPage, isRefresh: $isRefresh")

            try {
                // 网络请求在 IO Dispatcher 中执行
                val newVideosFromNetwork = withContext(Dispatchers.IO) {
                    PornhubParserWithOkHttp.fetchAndParseVideos(targetUrl)
                }

                if (newVideosFromNetwork.isNotEmpty()) {
                    // 给从网络获取的数据打上分类和时间戳
                    val processedNetworkVideos = newVideosFromNetwork.map {
                        it.copy(category = currentCategory, timestamp = System.currentTimeMillis())
                    }

                    if (isRefresh) {
                        videoDao.clearCategory(currentCategory) // 刷新时清除旧的分类数据
                        videoDao.insertAll(processedNetworkVideos) // 插入新的
                        currentPage = 2 // 下一次加载更多从第二页开始
                        isLastPage = false
                    } else {
                        // 增量加载时，只添加新的 (避免重复，虽然DB的REPLACE会处理)
                        videoDao.insertAll(processedNetworkVideos)
                        currentPage++
                    }
                    // videoDao.getVideosByCategory(currentCategory).firstOrNull() 会触发上面的 observeLocalData 更新UI
                } else {
                    if (isRefresh) {
                        Log.w(
                            "ViewModel",
                            "No videos found on initial network refresh from $targetUrl."
                        )
                        // 如果网络刷新没有数据，但本地有，则继续显示本地的
                        // 如果本地也为空，可以显示错误或空状态
                        if (_videoList.value.isEmpty()) {
                            _errorMessage.value = "没有获取到数据"
                        }
                    } else {
                        Log.i(
                            "ViewModel",
                            "Reached last page on network or no more new videos from $targetUrl."
                        )
                        isLastPage = true // 网络说没有更多数据了
                    }
                }
                // 清理旧缓存数据
                videoDao.deleteOldVideosByCategory(currentCategory, itemsPerPageForCacheLimit)

            } catch (e: Exception) {
                Log.e(
                    "ViewModel",
                    "Error fetching videos from Network ($targetUrl): ${e.localizedMessage}",
                    e
                )
                _errorMessage.value = "网络加载失败: ${e.localizedMessage}"
                if (!isRefresh) isLastPage = true // 网络错误时，假设是最后一页，避免无限加载
            } finally {
                if (isRefresh) {
                    _isLoading.value = false
                } else {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    // 提供给UI调用的加载更多函数
    fun loadMoreVideos() {
        Log.d(
            "ViewModel",
            "loadMoreVideos called. Current page: $currentPage, isLastPage: $isLastPage"
        )
        if (!isLastPage && !isLoading.value && !isLoadingMore.value) {
            fetchVideoData(isRefresh = false)
        }
    }

    // 提供给UI调用的刷新函数
    fun refreshVideos() {
        Log.d("ViewModel", "refreshVideos called")
        // 重置状态以允许刷新
        isLastPage = false
        currentPage = 1 // 确保刷新从第一页开始
        fetchVideoData(isRefresh = true)
    }

    // 如果需要获取单个视频的真实播放地址并缓存
    suspend fun getActualPlayUrl(videoItem: VideoItem): String? {
        if (!videoItem.actualPlayUrl.isNullOrEmpty()) {
            return videoItem.actualPlayUrl
        }
        // 尝试从数据库获取（可能在其他地方更新过）
        val cachedItem = videoDao.getVideoByUrl(videoItem.videoUrl ?: "")
        if (cachedItem?.actualPlayUrl != null) {
            // 更新当前列表中的项 (如果你的 _videoList 包含这个 VideoItem 的实例)
            _videoList.update { list ->
                list.map { if (it.videoUrl == videoItem.videoUrl) cachedItem else it }
            }
            return cachedItem.actualPlayUrl
        }

        // 从网络获取
        val networkUrl = withContext(Dispatchers.IO) {
            PornhubParserWithOkHttp.fetchActualVideoUrl(videoItem.videoUrl ?: "")
        }
        if (networkUrl != null) {
            val updatedItem =
                videoItem.copy(actualPlayUrl = networkUrl, timestamp = System.currentTimeMillis())
            videoDao.update(updatedItem) // 更新到数据库
            _videoList.update { list -> // 更新UI StateFlow
                list.map { if (it.videoUrl == updatedItem.videoUrl) updatedItem else it }
            }
            return networkUrl
        }
        return null
    }
}