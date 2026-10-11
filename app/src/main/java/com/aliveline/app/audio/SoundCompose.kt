package com.aliveline.app.audio

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Lets any composable do `LocalSoundPlayer.current?.play(Sfx.X)`. */
val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer?> { null }

/** Only exists so a composable can get the singleton SoundPlayer from Hilt. */
@HiltViewModel
class SoundViewModel @Inject constructor(val player: SoundPlayer) : ViewModel()

/**
 * Plays a soft pop on every tap anywhere in the app. It only watches pointer events
 * (it never consumes them) and ignores drags and scrolls.
 */
fun Modifier.tapSound(player: SoundPlayer): Modifier = pointerInput(player) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var moved = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if ((change.position - down.position).getDistance() > slop) moved = true
            if (change.changedToUp()) {
                if (!moved) player.play(Sfx.TAP)
                break
            }
            if (!change.pressed) break
        }
    }
}