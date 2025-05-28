package com.berlin.nbatv.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Serializable // 如果你之前用了它并且还想用，可以保留
@Entity(tableName = "videos") // 定义表名
data class VideoItem(
    @PrimaryKey
    val videoUrl: String,
    val id: Int,
    val imageUrl: String?,
    val description: String?,
    val name: String?,

    var actualPlayUrl: String? = null, // 真实播放地址，也可能需要缓存
    var category: String? = null, // 可以增加一个字段来区分不同分类的视频
    var timestamp: Long = System.currentTimeMillis() // 用于缓存策略，记录插入或更新时间
)
