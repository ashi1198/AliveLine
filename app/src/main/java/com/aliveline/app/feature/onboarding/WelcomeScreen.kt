package com.aliveline.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("AliveLine", color = AliveColors.TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Text("Small steps. Big life.", color = AliveColors.TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(48.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BotType.entries.forEach {
                BotAvatar(it, BotState.SLEEPY, Modifier.weight(1f).aspectRatio(1f).alpha(.5f))
            }
        }

        Spacer(Modifier.weight(1f))
        Text("Your life is offline.", color = AliveColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Plug in your bots to get started.",
            color = AliveColors.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AliveColors.Primary),
        ) { Text("Get Started \u2192", fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}
