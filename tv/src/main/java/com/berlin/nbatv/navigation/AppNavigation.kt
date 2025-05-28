package com.berlin.nbatv.navigation // 替换为您的包名

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.berlin.nbatv.ui.PlayerScreen // 确保路径正确
import com.berlin.nbatv.ui.VideoListScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// 定义导航路由名称和参数键
object AppDestinations {
    const val VIDEO_LIST_ROUTE = "video_list"
    const val PLAYER_ROUTE = "player"
    const val PLAYER_ARG_VIDEO_URL = "videoUrl" // 播放器页面需要的视频 URL 参数
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestinations.VIDEO_LIST_ROUTE) {
        // 视频列表页面
        composable(AppDestinations.VIDEO_LIST_ROUTE) {
            VideoListScreen(
                // 当视频被点击时，导航到播放器页面
                onVideoClick = { videoItem -> // 假设 videoItem 有一个 videoUrl 属性
                    // 对 URL进行编码以安全地作为导航参数传递
                    val encodedUrl =
                        URLEncoder.encode(videoItem.videoUrl, StandardCharsets.UTF_8.toString())
                    navController.navigate("${AppDestinations.PLAYER_ROUTE}/$encodedUrl")
                }
            )
        }

        // 视频播放页面
        composable(
            route = "${AppDestinations.PLAYER_ROUTE}/{${AppDestinations.PLAYER_ARG_VIDEO_URL}}",
            arguments = listOf(navArgument(AppDestinations.PLAYER_ARG_VIDEO_URL) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val videoUrlArg =
                backStackEntry.arguments?.getString(AppDestinations.PLAYER_ARG_VIDEO_URL)
            // 对 URL 进行解码
            val decodedUrl =
                videoUrlArg?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }

            if (decodedUrl != null) {
                PlayerScreen(videoPageUrl = decodedUrl)
            } else {
                // 如果 URL 无效或未找到，可以导航回列表或显示错误
                // 这里简单地返回上一页
                navController.popBackStack()
            }
        }
    }
}