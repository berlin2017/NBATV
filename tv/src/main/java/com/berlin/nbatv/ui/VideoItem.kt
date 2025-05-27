package com.berlin.nbatv.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.berlin.nbatv.data.VideoItem

const val TAG: String = "VideoItemPreview"

@Composable
fun VideoItemPreview(
    item: VideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.aspectRatio(16 / 9f),
        onClick = {
            onClick()
            Log.e(TAG, "VideoItemPreview: ")
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "${item.name}",
                contentScale = ContentScale.Crop
            )
            Text(text = item.name ?: "")
        }
    }
}
