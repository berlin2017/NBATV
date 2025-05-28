package com.berlin.nbatv.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
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
        onClick = {
            onClick()
            Log.e(TAG, "VideoItemPreview: ")
        },
        colors = CardDefaults.colors(
            containerColor = Color.Transparent
        )
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "${item.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.aspectRatio(16 / 9f)
            )
            Text(text = item.name ?: "", maxLines = 2)
        }
    }
}
