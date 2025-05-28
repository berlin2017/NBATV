package com.berlin.nbatv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE) // 如果已存在，则替换
    suspend fun insertAll(videos: List<VideoItem>)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(video: VideoItem)

    @Update
    suspend fun update(video: VideoItem)

    // 根据分类获取视频，并按时间戳降序排列 (最新的在前面)
    @Query("SELECT * FROM videos WHERE category = :category ORDER BY timestamp DESC")
    fun getVideosByCategory(category: String): Flow<List<VideoItem>>

    // 获取所有视频 (如果需要)
    @Query("SELECT * FROM videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<VideoItem>>

    // 根据详情页 URL 获取单个视频
    @Query("SELECT * FROM videos WHERE videoUrl = :videoUrl LIMIT 1")
    suspend fun getVideoByUrl(videoUrl: String): VideoItem?

    // 删除旧数据 (例如，只保留最近 N 条，或超过一定时间的数据)
    @Query("DELETE FROM videos WHERE category = :category AND videoUrl NOT IN (SELECT videoUrl FROM videos WHERE category = :category ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun deleteOldVideosByCategory(category: String, limit: Int)

    @Query("DELETE FROM videos WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Query("DELETE FROM videos")
    suspend fun clearAll()
}