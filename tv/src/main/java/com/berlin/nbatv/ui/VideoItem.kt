package com.berlin.nbatv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.berlin.nbatv.data.VideoItem

@Composable
fun VideoItemPreview(item: VideoItem) {
        Card(
            modifier = Modifier.aspectRatio(16/9f),
            onClick = {
                onItemClick(item)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "${item.name}",
                    contentScale = ContentScale.Crop
                )
                Text(text = item.name?:"")
            }
        }
}

private fun onItemClick(item: VideoItem) {

}