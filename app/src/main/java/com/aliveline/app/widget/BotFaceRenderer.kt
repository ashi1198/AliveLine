package com.aliveline.app.widget

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.palette
import kotlin.math.max

/**
 * Draws a simplified, still bot face into a Bitmap for the home screen widget.
 * Widgets can't run Compose animations, and a widget is just a picture, so we draw it off-screen
 * with the same shapes and colors as the in-app bots.
 */
object BotFaceRenderer {

    private class Face(val lid: Float, val smile: Float, val pupilY: Float)

    private fun BotCondition.face(): Face = when (this) {
        BotCondition.THRIVING -> Face(0f, 1f, 0f)
        BotCondition.HEALTHY -> Face(0f, 0.6f, 0f)
        BotCondition.OKAY -> Face(0f, 0.2f, 0f)
        BotCondition.TIRED -> Face(0.5f, -0.3f, 0.6f)
        BotCondition.STRUGGLING -> Face(0.65f, -0.5f, 0.6f)
        BotCondition.CRITICAL -> Face(1f, 0f, 0f)
    }

    fun render(bot: BotType, condition: BotCondition, sizePx: Int): Bitmap {
        val image = ImageBitmap(sizePx, sizePx)
        val canvas = Canvas(image)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            canvas,
            Size(sizePx.toFloat(), sizePx.toFloat()),
        ) {
            drawFace(bot, condition.face())
        }
        return image.asAndroidBitmap()
    }

    private fun DrawScope.drawFace(type: BotType, face: Face) {
        val s = size.minDimension
        val pal = type.palette()
        val eyeInk = Color(0xFF1B1F3B)
        val outline = pal.dark.copy(alpha = 0.55f)
        val lw = s * .014f

        // The in-app bot is drawn in a 0..1 box; scale it so just the head fills the widget tile.
        withTransform({ scale(1.25f, 1.25f, Offset(s * .5f, s * .05f)) }) {
            // side nubs
            drawCircle(pal.dark, s * .05f, Offset(s * .115f, s * .44f))
            drawCircle(pal.dark, s * .05f, Offset(s * .885f, s * .44f))

            // character detail on top of the head
            when (type) {
                BotType.BRAIN -> {
                    val bumps = listOf(
                        Triple(.38f, .15f, .075f), Triple(.50f, .115f, .085f), Triple(.62f, .15f, .075f),
                        Triple(.44f, .17f, .06f), Triple(.56f, .17f, .06f),
                    )
                    bumps.forEach { (x, y, r) -> drawCircle(pal.dark, s * (r + .012f), Offset(s * x, s * y)) }
                    bumps.forEach { (x, y, r) -> drawCircle(pal.light, s * r, Offset(s * x, s * y)) }
                }
                BotType.HEALTH -> {
                    val hp = Path().apply {
                        moveTo(s * .5f, s * .205f)
                        cubicTo(s * .36f, s * .12f, s * .42f, s * .02f, s * .5f, s * .09f)
                        cubicTo(s * .58f, s * .02f, s * .64f, s * .12f, s * .5f, s * .205f)
                        close()
                    }
                    drawPath(hp, Color(0xFFFF6B8A))
                }
                BotType.SOCIAL -> {
                    val curl = Path().apply {
                        moveTo(s * .5f, s * .21f)
                        cubicTo(s * .5f, s * .12f, s * .59f, s * .13f, s * .58f, s * .07f)
                        cubicTo(s * .57f, s * .01f, s * .47f, s * .03f, s * .49f, s * .09f)
                    }
                    drawPath(curl, pal.dark, style = Stroke(s * .05f, cap = StrokeCap.Round))
                    drawPath(curl, pal.light, style = Stroke(s * .032f, cap = StrokeCap.Round))
                }
            }

            // head
            drawRoundRect(
                Brush.verticalGradient(listOf(pal.light, pal.base), startY = s * .18f, endY = s * .72f),
                Offset(s * .12f, s * .18f), Size(s * .76f, s * .54f), CornerRadius(s * .26f),
            )
            drawRoundRect(
                outline, Offset(s * .12f, s * .18f), Size(s * .76f, s * .54f), CornerRadius(s * .26f),
                style = Stroke(lw),
            )
            drawOval(Color.White.copy(alpha = 0.22f), Offset(s * .22f, s * .23f), Size(s * .18f, s * .07f))

            // eyes
            val closed = max(face.lid, 0f)
            listOf(.36f, .64f).forEach { x ->
                val c = Offset(s * x, s * .42f)
                val r = s * .105f
                drawCircle(Color.White, r, c)
                val pupil = c + Offset(0f, face.pupilY * r * .3f)
                drawCircle(eyeInk, r * .62f, pupil)
                drawCircle(Color.White, r * .17f, pupil + Offset(-r * .18f, -r * .22f))
                val clip = Path().apply { addOval(Rect(c, r)) }
                clipPath(clip) {
                    drawRect(pal.base, c - Offset(r, r), Size(r * 2, r * 2 * closed))
                }
                drawCircle(outline, r, c, style = Stroke(lw * .8f))
            }

            // cheeks
            val cheekAlpha = (0.30f + 0.25f * face.smile).coerceIn(0.15f, 0.6f)
            drawCircle(Color(0xFFFF7A93).copy(alpha = cheekAlpha), s * .045f, Offset(s * .24f, s * .545f))
            drawCircle(Color(0xFFFF7A93).copy(alpha = cheekAlpha), s * .045f, Offset(s * .76f, s * .545f))

            // mouth
            if (face.smile > 0.8f) {
                val open = ((face.smile - 0.8f) / 0.2f).coerceIn(0f, 1f)
                val mouth = Path().apply {
                    moveTo(s * .42f, s * .55f)
                    quadraticBezierTo(s * .5f, s * (.55f + .06f + .12f * open), s * .58f, s * .55f)
                    close()
                }
                drawPath(mouth, eyeInk)
            } else {
                val mouth = Path().apply {
                    moveTo(s * .42f, s * .56f)
                    quadraticBezierTo(s * .5f, s * (.56f + .07f * face.smile), s * .58f, s * .56f)
                }
                drawPath(mouth, eyeInk, style = Stroke(s * .022f, cap = StrokeCap.Round))
            }
        }
    }
}