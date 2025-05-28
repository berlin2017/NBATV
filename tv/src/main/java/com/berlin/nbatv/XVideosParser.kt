package com.berlin.nbatv // 保持你的包名

import android.util.Log
import com.berlin.nbatv.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException

// 保持 User-Agent，或者根据需要调整
private const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"

// 可以考虑重命名这个对象，例如 XvideosPageParser 或 GeneralVideoPageParser
object PornhubParserWithOkHttp { // 或者叫 XvideosPageParser

    private val client = OkHttpClient.Builder().build()

    // 这个函数现在将针对 Xvideos 的列表页进行解析
    suspend fun fetchAndParseVideos(pageUrl: String): List<VideoItem> {
        // pageUrl 已经是完整的页面URL了，例如 "https://www.xvideos.com/new/" 或 "https://www.xvideos.com/new/2/"
        return withContext(Dispatchers.IO) {
            val videoList = mutableListOf<VideoItem>()
            Log.d("VideoParser", "Fetching URL: $pageUrl")
            try {
                val request = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7") // 可以保留或调整
                    .build()

                val response: Response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(
                        "VideoParser",
                        "HTTP Error fetching URL: $pageUrl - Status: ${response.code}"
                    )
                    throw IOException("Unexpected code ${response.code} from $pageUrl")
                }

                val htmlContent = response.body?.string()
                response.close()

                if (htmlContent == null) {
                    Log.e("VideoParser", "Response body was null for URL: $pageUrl")
                    return@withContext videoList
                }

                val document: Document = Jsoup.parse(htmlContent)
                Log.d("VideoParser", "HTML content fetched and parsed by Jsoup for $pageUrl.")

                // *************************************************************************
                // ** 关键：分析 Xvideos 的 HTML 结构并编写准确的 Jsoup 选择器 **
                // 以下选择器是【高度推测性的】，你需要根据实际的网站HTML结构进行修改。
                // 打开浏览器的开发者工具（通常是F12），检查包含视频列表项的HTML元素。
                // *************************************************************************

                // 尝试找到包含每个视频信息的父级元素。
                // 常见的可能是 <div> 标签，并且有特定的 class，例如 "thumb-block", "video-item", "gallery-item" 等。
                // 你需要找到一个能够选中所有视频条目的共同选择器。
                val videoElements: Elements =
                    document.select("div.thumb-block") // <--- 【极度需要你自行验证和修改】
                // 也可能是 "div.mozaique div.thumb-block"
                // 或者 "div.video-list div.item" 等等。

                if (videoElements.isEmpty()) {
                    Log.w(
                        "VideoParser",
                        "No video elements found with selector 'div.thumb-block'. Check selector and website structure for $pageUrl."
                    )
                    // 你可以在这里打印部分HTML内容以帮助调试
                    // Log.d("VideoParser", "HTML snippet: ${htmlContent.substring(0, minOf(htmlContent.length, 2000))}")
                } else {
                    Log.d(
                        "VideoParser",
                        "Found ${videoElements.size} potential video elements on $pageUrl."
                    )
                }

                for ((index, videoElement) in videoElements.withIndex()) {
                    try {
                        // --- 提取标题和详情页链接 ---
                        // 通常标题在一个 <a> 标签内，该标签的 href 属性是详情页链接。
                        // 标题可能在 <a> 标签的 title 属性，或者在内部的 <p class="title"> 或类似的元素中。
                        val titleAnchor: Element? =
                            videoElement.select("p.title a").first() // <--- 【验证和修改】
                        // 或 videoElement.select("a.thumb-image").first()
                        // 或 videoElement.select("div.thumb a").first()

                        val title = titleAnchor?.attr("title")?.trim()
                            ?: titleAnchor?.text()?.trim()
                            ?: videoElement.select("p.title").first()?.text()
                                ?.trim() // <--- 【验证和修改】
                            ?: "N/A"

                        var videoPageUrl = titleAnchor?.attr("href")?.trim() ?: "N/A"
                        if (videoPageUrl != "N/A" && !videoPageUrl.startsWith("http")) {
                            // Xvideos 的相对链接通常以 / 开头，需要拼接域名
                            if (videoPageUrl.startsWith("/")) {
                                videoPageUrl = "https://www.xvideos.com$videoPageUrl"
                            } else {
                                Log.w(
                                    "VideoParser",
                                    "Unexpected videoPageUrl format: $videoPageUrl"
                                )
                                // 根据实际情况处理，可能需要更完整的域名或不同的拼接方式
                            }
                        }

                        // --- 提取缩略图 URL ---
                        // 缩略图通常在一个 <img> 标签中，URL 可能在 src, data-src, data-thumb_url 等属性中。
                        // 注意懒加载图片，真实的URL可能在 data-src 中。
                        val imgElement: Element? =
                            videoElement.select("div.thumb img").first() // <--- 【验证和修改】
                        // 或 videoElement.select("a.thumb-image img").first()

                        var thumbnailUrl = imgElement?.attr("data-src") // 优先尝试 data-src (懒加载)
                        if (thumbnailUrl.isNullOrEmpty()) thumbnailUrl = imgElement?.attr("src")
                        thumbnailUrl = thumbnailUrl?.trim()

                        // --- 提取时长、观看次数、评分等 (可选) ---
                        // 这些信息通常在特定的 <span> 或 <div> 标签中，带有特定的 class。
                        // 例如: <span class="duration">10:30</span>
                        //         <span class="views">1.2M views</span>
                        //         <span class="rating-good">90%</span>
                        val durationElement: Element? =
                            videoElement.select("span.duration").first() // <--- 【验证和修改】
                        val duration = durationElement?.text()?.trim() ?: ""

                        // Xvideos 上观看次数和评分的显示可能比较复杂，你可能需要更精确的选择器
                        // val views = videoElement.select("span.views").first()?.text()?.trim()
                        // val rating = videoElement.select("span.rating-value").first()?.text()?.trim()

                        if (title != "N/A" && videoPageUrl != "N/A" && !thumbnailUrl.isNullOrEmpty()) {
                            val videoItem = VideoItem(
                                videoUrl = videoPageUrl, // 这是视频详情页的URL
                                name = title,
                                imageUrl = thumbnailUrl,
                                id = index, // 或者使用从URL中提取的唯一ID
                                description = "时长: $duration" // 可以组合更多信息
                                // 你可能还需要其他字段，例如 uniqueIdFromUrl
                            )
                            videoList.add(videoItem)
                            Log.d(
                                "VideoParser",
                                "Added: ${videoItem.name} -> ${videoItem.videoUrl}"
                            )
                        } else {
                            Log.w(
                                "VideoParser",
                                "Skipped item $index on $pageUrl due to missing title, URL, or thumbnail. Title: '$title', URL: '$videoPageUrl', Thumb: '$thumbnailUrl'"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "VideoParser",
                            "Error parsing a single video item $index on $pageUrl: ${e.localizedMessage}",
                            e
                        )
                    }
                }

            } catch (e: IOException) {
                Log.e(
                    "VideoParser",
                    "Network IOException for URL: $pageUrl - ${e.localizedMessage}",
                    e
                )
            } catch (e: Exception) {
                Log.e(
                    "VideoParser",
                    "Generic Error fetching or parsing URL: $pageUrl - ${e.localizedMessage}",
                    e
                )
            }
            Log.d(
                "VideoParser",
                "Parsing finished for $pageUrl. Total videos parsed: ${videoList.size}"
            )
            videoList
        }
    }

    // fetchActualVideoUrl 函数也需要针对 Xvideos 的详情页进行彻底改造。
    // 这通常比列表页解析更复杂，因为视频播放器和真实流媒体地址的获取方式千差万别。
    // 你需要分析详情页的HTML，特别是包含播放器的 <script> 标签或JavaScript变量。
    suspend fun fetchActualVideoUrl(videoPageUrl: String): String? {
        Log.d("VideoParser", "Fetching actual video URL from details page: $videoPageUrl")
        return withContext(Dispatchers.IO) {
            try {
                // val client = OkHttpClient() // 复用上面的 client 实例
                val request = Request.Builder().url(videoPageUrl)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("VideoParser", "Error fetching page $videoPageUrl: ${response.code}")
                    return@withContext null
                }

                val htmlContent = response.body?.string() ?: return@withContext null
                response.close()
                val document = Jsoup.parse(htmlContent)

                // --- 关键的Xvideos详情页解析逻辑 ---
                // Xvideos 可能会将视频流信息（如 .m3u8 URL）嵌入到页面的 <script> 标签中的 JavaScript 变量里。
                // 你需要仔细查找类似 `html5player.setVideoUrlHigh('...')` 或 `flashvars.video_url`
                // 或者包含 "master.m3u8" 或 ".mp4" 的 JavaScript 字符串。

                var actualVideoUrl: String? = null
                var bestQualityUrl: String? = null
                var mediumQualityUrl: String? = null
                var lowQualityUrl: String? = null

                val scripts = document.select("script")
                for (script in scripts) {
                    val scriptData = script.data()

                    // 优先查找包含 "HD", "1080", "720", "high" 的链接
                    // 示例：html5player.setVideoUrlHD('URL_HERE');
                    var regex =
                        """html5player\.setVideoUrlHD\s*\(\s*['"]([^'"]+)['"]\s*\)""".toRegex()
                    var matchResult = regex.find(scriptData)
                    if (matchResult != null && matchResult.groupValues.size > 1) {
                        val url = matchResult.groupValues[1].replace("\\/", "/")
                        if (url.isLikelyVideoStream()) {
                            bestQualityUrl = url
                            Log.i(
                                "VideoParser",
                                "Found HD video URL (from setVideoUrlHD): $bestQualityUrl"
                            )
                            break // 如果找到了明确的HD，可能就不需要再找其他的了
                        }
                    }

                    // 示例：html5player.setVideoUrlHigh('URL_HERE');
                    if (bestQualityUrl == null) { // 只有在没找到HD时才继续
                        regex =
                            """html5player\.setVideoUrlHigh\s*\(\s*['"]([^'"]+)['"]\s*\)""".toRegex()
                        matchResult = regex.find(scriptData)
                        if (matchResult != null && matchResult.groupValues.size > 1) {
                            val url = matchResult.groupValues[1].replace("\\/", "/")
                            if (url.isLikelyVideoStream()) {
                                bestQualityUrl = url // 假设 High 就是我们想要的最高画质
                                Log.i(
                                    "VideoParser",
                                    "Found HIGH video URL (from setVideoUrlHigh): $bestQualityUrl"
                                )
                                // break // 可以考虑是否在此break
                            }
                        }
                    }

                    // 示例：寻找其他可能的清晰度变量，例如一个包含所有清晰度链接的JSON对象
                    // "quality_1080p": "url", "quality_720p": "url"
                    regex = """["']quality_(\d{3,4}p?)["']\s*:\s*["']([^"']+)["']""".toRegex()
                    regex.findAll(scriptData).forEach { match ->
                        val qualityLabel = match.groupValues[1] // e.g., "1080p", "720p"
                        val url = match.groupValues[2].replace("\\/", "/")
                        if (url.isLikelyVideoStream()) {
                            Log.d("VideoParser", "Found quality option: $qualityLabel -> $url")
                            // 在这里你可以根据 qualityLabel 来决定哪个是最高清的，并保存它
                            // 例如，如果这是你第一次找到1080p，就保存它
                            if (qualityLabel.contains("1080") && bestQualityUrl?.contains("1080") != true) {
                                bestQualityUrl = url
                            } else if (qualityLabel.contains("720") && bestQualityUrl?.contains("1080") != true && bestQualityUrl?.contains(
                                    "720"
                                ) != true
                            ) {
                                bestQualityUrl = url // 如果没有1080p，720p也是好的
                            }
                            // ... 可以添加更多逻辑
                        }
                    }


                    // 保留原有的查找逻辑作为备选 (setVideoUrlLow, setVideoUrl)
                    if (bestQualityUrl == null) { // 如果上面都没找到高清的
                        regex =
                            """html5player\.setVideoUrlLow\s*\(\s*['"]([^'"]+)['"]\s*\)""".toRegex()
                        matchResult = regex.find(scriptData)
                        if (matchResult != null && matchResult.groupValues.size > 1) {
                            val url = matchResult.groupValues[1].replace("\\/", "/")
                            if (url.isLikelyVideoStream()) {
                                lowQualityUrl = url
                                Log.i(
                                    "VideoParser",
                                    "Found LOW video URL (from setVideoUrlLow): $lowQualityUrl"
                                )
                            }
                        }
                    }

                    // 如果找到了 bestQualityUrl，就不用再继续循环了
                    if (bestQualityUrl != null) break
                }

                // 决定最终返回哪个 URL
                actualVideoUrl = bestQualityUrl ?: mediumQualityUrl ?: lowQualityUrl // 优先高清

                if (actualVideoUrl == null) {
                    Log.w(
                        "VideoParser",
                        "Could not find any valid video stream URL on page: $videoPageUrl"
                    )
                }

                actualVideoUrl
                // --- 结束关键的解析逻辑 ---
            } catch (e: IOException) {
                Log.e(
                    "VideoParser",
                    "IOException while fetching/parsing $videoPageUrl: ${e.message}",
                    e
                )
                null
            } catch (e: Exception) {
                Log.e(
                    "VideoParser",
                    "Exception while fetching/parsing $videoPageUrl: ${e.message}",
                    e
                )
                null
            }
        }
    }
}

// 辅助函数，判断URL是否可能是视频流
fun String.isLikelyVideoStream(): Boolean {
    return this.endsWith(".m3u8", true) ||
            this.contains(".mp4", true) ||
            this.contains("googlevideo.com", true) // 有些视频可能来自google的CDN
    // 可以根据需要添加更多判断条件
}