package com.aliveline.app.feature.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.aliveline.app.data.photo.PhotoStore
import com.aliveline.app.ui.theme.AliveColors

/** A note photo at its natural proportions, 150dp wide before the user resizes it on the board. */
@Composable
fun PhotoImage(name: String, store: PhotoStore, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(initialValue = null, name) { value = store.load(name) }
    val bitmap = image
    val shape = RoundedCornerShape(10.dp)

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .width(150.dp)
                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                .clip(shape),
        )
    } else {
        Box(modifier.size(150.dp).clip(shape).background(AliveColors.Surface))
    }
}