package com.aliveline.app.feature.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.domain.FocusCalculator
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.FocusPhase
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors
import kotlinx.coroutines.delay

/** Small bar above the bottom tabs while a focus session is running. */
@Composable
fun FocusMiniBar(
    onClick: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val session = (ui as? FocusUiState.Active)?.session ?: return

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val snap = FocusCalculator.snapshot(session, now)
    val accent = session.bot.palette().base
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = AliveColors.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("\u23F1", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(session.title, color = AliveColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    when (snap.phase) {
                        FocusPhase.FOCUS -> "Focusing"
                        FocusPhase.BREAK -> "On a break"
                        FocusPhase.DONE -> "Session complete. Tap to finish"
                    },
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            if (snap.phase != FocusPhase.DONE) {
                Text(TimeFormat.clock(snap.remainingMs), color = accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}