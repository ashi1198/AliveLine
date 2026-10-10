package com.aliveline.app.feature.onboarding

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Layout constants shared by the connect and meet screens so they line up when cross-fading. */
internal object StageDims {
    val BotSize = 170.dp
    val BotTop = 8.dp
    val SocketY = 214.dp
    val SocketR = 34.dp
    val PlugW = 52.dp
    val PlugH = 76.dp
    /** Where the plug centre sits (as a fraction of plug height below the socket centre) when seated. */
    const val SeatFraction = 0.35f
}

internal fun DrawScope.drawSocket(center: Offset, radius: Float, accent: Color, glow: Float) {
    drawCircle(Color(0xFF0E1328), radius, center)
    drawCircle(
        accent.copy(alpha = 0.35f + 0.65f * glow),
        radius,
        center,
        style = Stroke(radius * .12f),
    )
    val slotW = radius * .22f
    val slotH = radius * .5f
    listOf(-.31f, .31f).forEach { fx ->
        drawRoundRect(
            Color(0xFF05070F),
            Offset(center.x + radius * fx - slotW / 2f, center.y - slotH / 2f),
            Size(slotW, slotH),
            CornerRadius(slotW / 2f),
        )
    }
}

internal fun DrawScope.drawPlugShape(topLeft: Offset, size: Size, accent: Color, lit: Boolean) {
    val w = size.width
    val h = size.height
    val prongW = w * .13f
    listOf(.30f, .70f).forEach { fx ->
        drawRoundRect(
            Color(0xFFB8BED8),
            Offset(topLeft.x + w * fx - prongW / 2f, topLeft.y),
            Size(prongW, h * .42f),
            CornerRadius(prongW / 3f),
        )
    }
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color(0xFFE6E9F7), Color(0xFFA9AFCB)),
            startY = topLeft.y + h * .34f,
            endY = topLeft.y + h,
        ),
        Offset(topLeft.x, topLeft.y + h * .34f),
        Size(w, h * .66f),
        CornerRadius(w * .22f),
    )
    drawCircle(
        if (lit) accent else Color(0xFF6A7090),
        w * .07f,
        Offset(topLeft.x + w / 2f, topLeft.y + h * .55f),
    )
}
