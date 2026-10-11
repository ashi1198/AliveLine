package com.aliveline.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun SpeechBubble(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    speaker: String? = null,
    fillWidth: Boolean = false,
    tailAtBottom: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val fill = accent.copy(alpha = 0.16f).compositeOver(AliveColors.Surface)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            color = fill,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        ) {
            Crossfade(targetState = speaker to text, label = "speech") { (who, line) ->
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (who != null) {
                        Text(who, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(line, color = AliveColors.TextPrimary, fontSize = 14.sp)
                }
            }
        }
        if (tailAtBottom) {
            Canvas(Modifier.size(width = 18.dp, height = 9.dp)) {
                val tail = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(tail, fill)
            }
        }
    }
}