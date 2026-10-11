package com.aliveline.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.ui.TaskItem
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun TaskCard(
    item: TaskItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onComplete: (() -> Unit)? = null,
) {
    val accent = item.bot.palette().base
    val sound = LocalSoundPlayer.current
    val shape = RoundedCornerShape(18.dp)
    val isDone = item.progress >= 100

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = AliveColors.Surface,
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
            Column(Modifier.weight(1f).padding(14.dp)) {
                Text(
                    item.title,
                    color = AliveColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                item.dueLabel?.let {
                    Text(it, color = AliveColors.TextSecondary, fontSize = 13.sp)
                }
                if (item.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        color = accent,
                        trackColor = AliveColors.Background,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                Text(item.bot.displayName, color = accent, fontSize = 12.sp)
            }
            if (onComplete != null) {
                IconButton(onClick = {
                    sound?.play(Sfx.COMPLETE)
                    onComplete()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Mark done", tint = accent)
                }
            } else if (isDone) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Done",
                    tint = accent,
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
        }
    }
}