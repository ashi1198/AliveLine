package com.aliveline.app.feature.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.theme.AliveColors

object StickerCatalog {
    val categories: List<Pair<String, List<String>>> = listOf(
        "Bots" to listOf("bot:BRAIN", "bot:HEALTH", "bot:SOCIAL"),
        "Moods" to listOf("😀", "😄", "😍", "🥳", "😎", "🤩", "😴", "🤔", "😅", "🥰", "😭", "😡"),
        "Study" to listOf("📚", "📖", "✏️", "📝", "🧠", "💡", "🎓", "💻", "🔬", "📐", "🗂️", "📌"),
        "Health" to listOf("💪", "🏃", "🧘", "🥗", "💧", "🍎", "🛌", "❤️", "🩺", "🚴", "🏋️", "🌿"),
        "Social" to listOf("👋", "🤝", "💬", "📞", "🎉", "🎂", "🫂", "☕", "🍕", "🎵", "🌍", "💌"),
        "Fun" to listOf("⭐", "✨", "🔥", "🌈", "🌸", "🍀", "🎯", "🏆", "🚀", "🎈", "💎", "🌙"),
    )
}

/** One sticker, either an emoji or a bot face. Used by the picker and by the note board. */
@Composable
fun StickerGlyph(content: String, emojiSize: TextUnit, botSize: Dp) {
    if (content.startsWith("bot:")) {
        val bot = runCatching { BotType.valueOf(content.removePrefix("bot:")) }.getOrDefault(BotType.BRAIN)
        BotAvatar(bot, BotState.HAPPY, Modifier.size(botSize))
    } else {
        Text(content, fontSize = emojiSize)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerSheet(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var category by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AliveColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Stickers",
                color = AliveColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StickerCatalog.categories.forEachIndexed { index, (name, _) ->
                    FilterChip(
                        selected = category == index,
                        onClick = { category = index },
                        label = { Text(name) },
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp),
                modifier = Modifier.heightIn(max = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(StickerCatalog.categories[category].second) { sticker ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(sticker) },
                        contentAlignment = Alignment.Center,
                    ) {
                        StickerGlyph(sticker, emojiSize = 32.sp, botSize = 56.dp)
                    }
                }
            }
        }
    }
}