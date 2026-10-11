package com.aliveline.app.feature.celebration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.model.Achievement
import com.aliveline.app.ui.theme.AliveColors

/** Sits on top of the whole app: confetti plus the "achievement unlocked" banner. Never blocks touches. */
@Composable
fun CelebrationLayer(
    modifier: Modifier = Modifier,
    viewModel: CelebrationViewModel = hiltViewModel(),
) {
    val banner by viewModel.banner.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    var lastBanner by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.celebrations.collect {
            celebration = it
            if (it.level == CelebrationLevel.BIG) sound?.play(Sfx.CELEBRATE)
        }
    }
    LaunchedEffect(banner) { banner?.let { lastBanner = it } }

    Box(modifier.fillMaxSize()) {
        celebration?.let { ConfettiBurst(it) }

        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            lastBanner?.let { AchievementBanner(it) }
        }
    }
}

@Composable
private fun AchievementBanner(achievement: Achievement) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AliveColors.Surface,
        border = BorderStroke(1.5.dp, AliveColors.Primary),
        shadowElevation = 8.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(achievement.emoji, fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Achievement unlocked", color = AliveColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(achievement.title, color = AliveColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(achievement.description, color = AliveColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}