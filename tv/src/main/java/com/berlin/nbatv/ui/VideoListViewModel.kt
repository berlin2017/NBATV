package com.berlin.nbatv.ui // 或者您放置ViewModel的包，例如 com.berlin.nbatv.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berlin.nbatv.PornhubParserWithOkHttp
import com.berlin.nbatv.PornhubParserWithOkHttp.fetchAndParseVideos
import com.berlin.nbatv.data.VideoItem // 确保路径正确
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoListViewModel : ViewModel() {

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

    // 目标 URL (不含 page 参数)
    private val baseCategoryUrl = "https://www.xvideos.com/new/"


    init {
        // ViewModel 初始化时加载数据
        fetchVideoData()
    }


    fun fetchVideoData(isRefresh: Boolean = false) {
        if (isLoading.value || isLoadingMore.value || (isLastPage && !isRefresh)) {
            Log.d("ViewModel", "Fetch skipped: loading=${isLoading.value}, loadingMore=${isLoadingMore.value}, isLastPage=$isLastPage, isRefresh=$isRefresh")
            return
        }

        viewModelScope.launch {
            if (isRefresh) {
                _isLoading.value = true
                currentPage = 1
                isLastPage = false
                _videoList.value = emptyList() // 清空列表进行刷新
            } else {
                _isLoadingMore.value = true
            }
            _errorMessage.value = null

            // 构建目标 URL
            // 如果第一页就是 baseCategoryUrl (不带页码)
            // 如果页码大于1，则附加页码
            val targetUrl = "${baseCategoryUrl}${currentPage}/" // 例如 https://www.xvideos.com/new/2/ (注意末尾的斜杠，根据实际URL格式调整)
            // 如果第一页的URL也是 https://www.xvideos.com/new/1/，则可以直接用下面的
            // val targetUrl = "${baseCategoryUrl}${currentPage}/"

            Log.d("ViewModel", "Fetching page: $currentPage, isRefresh: $isRefresh")

            try {
                // val newVideos = parser.fetchAndParseVideos(baseUrl, currentPage) // 如果 baseUrl 不含查询参数
                val newVideos = fetchAndParseVideos(targetUrl) // 如果 baseCategoryUrl 包含所有固定参数

                if (newVideos.isEmpty() && currentPage > 1) {
                    isLastPage = true
                    Log.i("ViewModel", "Reached last page or no more new videos on page $currentPage.")
                } else if (newVideos.isNotEmpty()) {
                    _videoList.update { currentList ->
                        if (isRefresh) newVideos else currentList + newVideos
                    }
                    if (!isRefresh) { // 只有在加载更多成功时才增加页码
                        currentPage++
                    }
                } else if (isRefresh && newVideos.isEmpty()) {
                    // 刷新时第一页就没有数据
                    Log.w("ViewModel", "No videos found on initial refresh (page 1).")
                }
                if (isRefresh) currentPage++ // 刷新成功后，下一页是第二页

            } catch (e: Exception) {
                Log.e("ViewModel", "Error fetching videos for page $currentPage: ${e.localizedMessage}", e)
                _errorMessage.value = "加载失败: ${e.localizedMessage}"
                if (!isRefresh && currentPage > 1) { // 如果加载更多失败，可以考虑是否回滚页码或标记错误
                    // isLastPage = true; // 或者标记为无法加载更多
                }
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
        if (!isLastPage && !isLoading.value && !isLoadingMore.value) {
            Log.d("ViewModel", "loadMoreVideos called, current page before fetch: ${currentPage-1}")
            fetchVideoData(isRefresh = false)
        } else {
            Log.d("ViewModel", "loadMoreVideos skipped: isLastPage=$isLastPage, isLoading=${isLoading.value}, isLoadingMore=${isLoadingMore.value}")
        }
    }

    // 提供给UI调用的刷新函数
    fun refreshVideos() {
        Log.d("ViewModel", "refreshVideos called")
        fetchVideoData(isRefresh = true)
    }
}