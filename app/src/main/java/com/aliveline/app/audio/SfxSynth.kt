package com.aliveline.app.audio

import com.aliveline.app.domain.model.BotType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates every sound as raw audio, so the app needs no audio files.
 * UI sounds are short glide "bloops". The wake, complete and celebrate sounds also have a
 * different VOICE per bot:
 *   Brain  -> bell / glockenspiel (bright, quick, inharmonic overtones)
 *   Health -> warm round tone with a small pitch scoop
 *   Social -> bouncy pop with a soft echo
 */
internal object SfxSynth {
    const val SAMPLE_RATE = 22_050

    private enum class Voice { BLOOP, BELL, WARM, POP }

    private class Note(
        val startMs: Int,
        val durMs: Int,
        val fromHz: Float,
        val toHz: Float = fromHz,
        val volume: Float = 0.6f,
        val voice: Voice = Voice.BLOOP,
    )

    private class Motif(val voice: Voice, val notes: List<Float>, val stepMs: Int)

    /** Sounds that exist in one version per bot. */
    fun isBotAware(sfx: Sfx): Boolean =
        sfx == Sfx.WAKE || sfx == Sfx.COMPLETE || sfx == Sfx.CELEBRATE

    private fun genericRecipe(sfx: Sfx): List<Note> = when (sfx) {
        Sfx.TAP -> listOf(Note(0, 55, 520f, 780f, 0.45f))
        Sfx.NAV -> listOf(Note(0, 85, 380f, 640f, 0.5f))
        Sfx.PICKUP -> listOf(Note(0, 50, 480f, 700f, 0.5f))
        Sfx.SNAP -> listOf(Note(0, 110, 200f, 90f, 0.9f), Note(30, 90, 700f, 1100f, 0.45f))
        Sfx.WAKE -> listOf(
            Note(0, 120, 523f, 600f),
            Note(110, 120, 659f, 740f),
            Note(220, 220, 784f, 900f),
        )
        Sfx.COMPLETE -> listOf(Note(0, 100, 660f, 800f), Note(90, 190, 880f, 1000f))
        Sfx.CELEBRATE -> listOf(
            Note(0, 110, 523f, 620f),
            Note(100, 110, 659f, 760f),
            Note(200, 110, 784f, 900f),
            Note(300, 280, 1047f, 1200f),
        )
        Sfx.UNDO -> listOf(Note(0, 90, 700f, 520f), Note(80, 130, 520f, 380f))
        Sfx.SAVE -> listOf(Note(0, 80, 600f, 760f), Note(70, 150, 900f, 1100f))
        Sfx.DELETE -> listOf(Note(0, 130, 450f, 200f))
        Sfx.PIN -> listOf(Note(0, 60, 900f, 1300f, 0.45f))
        Sfx.TOGGLE -> listOf(Note(0, 45, 600f, 850f, 0.45f))
    }

    private fun motif(bot: BotType): Motif = when (bot) {
        BotType.BRAIN -> Motif(Voice.BELL, listOf(659f, 831f, 988f, 1319f), 90)
        BotType.HEALTH -> Motif(Voice.WARM, listOf(523f, 659f, 784f, 1047f), 125)
        BotType.SOCIAL -> Motif(Voice.POP, listOf(587f, 740f, 880f, 1175f), 85)
    }

    private fun botRecipe(sfx: Sfx, bot: BotType): List<Note> {
        val m = motif(bot)
        val step = m.stepMs
        fun note(index: Int, startMs: Int, durMs: Int, volume: Float = 0.6f) =
            Note(startMs, durMs, m.notes[index], volume = volume, voice = m.voice)

        return when (sfx) {
            Sfx.WAKE -> listOf(
                note(0, 0, step + 40),
                note(1, step, step + 40),
                note(2, step * 2, step * 3),
            )
            Sfx.COMPLETE -> listOf(
                note(1, 0, step + 30),
                note(3, step, step * 3),
            )
            Sfx.CELEBRATE -> listOf(
                note(0, 0, step + 30),
                note(1, step, step + 30),
                note(2, step * 2, step + 30),
                note(3, step * 3, step * 4),
                note(1, step * 3, step * 4, 0.3f),
                note(2, step * 3, step * 4, 0.3f),
            )
            else -> genericRecipe(sfx)
        }
    }

    fun render(sfx: Sfx, bot: BotType? = null): ShortArray {
        val perBot = bot != null && isBotAware(sfx)
        val notes = if (bot != null && perBot) botRecipe(sfx, bot) else genericRecipe(sfx)
        val echo = perBot && bot == BotType.SOCIAL

        val tailMs = if (echo) 320 else 40
        val totalMs = notes.maxOf { it.startMs + it.durMs } + tailMs
        val mix = FloatArray(SAMPLE_RATE * totalMs / 1000)
        val noise = Random(7)

        for (note in notes) {
            val start = SAMPLE_RATE * note.startMs / 1000
            val count = SAMPLE_RATE * note.durMs / 1000
            var phase = 0.0
            for (i in 0 until count) {
                val index = start + i
                if (index >= mix.size) break

                val progress = i.toFloat() / count
                val seconds = i.toFloat() / SAMPLE_RATE
                var hz = note.fromHz + (note.toHz - note.fromHz) * progress
                if (note.voice == Voice.WARM) {
                    hz *= 0.94f + 0.06f * (seconds / 0.04f).coerceAtMost(1f)
                }
                phase += 2.0 * PI * hz / SAMPLE_RATE

                val decay = when (note.voice) {
                    Voice.BLOOP -> 5f
                    Voice.BELL -> 6.5f
                    Voice.WARM -> 3.5f
                    Voice.POP -> 7f
                }
                val envelope = if (seconds < 0.004f) seconds / 0.004f else exp(-decay * progress)

                val tone: Double = when (note.voice) {
                    Voice.BLOOP -> sin(phase) + 0.25 * sin(2.0 * phase)
                    Voice.BELL -> sin(phase) + 0.4 * sin(2.76 * phase) + 0.18 * sin(5.4 * phase)
                    Voice.WARM -> sin(phase) + 0.5 * sin(2.0 * phase) + 0.2 * sin(3.0 * phase)
                    Voice.POP -> {
                        val burst = if (seconds < 0.012f) {
                            (noise.nextFloat() * 2f - 1f) * (1f - seconds / 0.012f) * 0.5f
                        } else {
                            0f
                        }
                        sin(phase) + 0.3 * sin(3.0 * phase) + burst
                    }
                }
                mix[index] += (note.volume * envelope * tone).toFloat()
            }
        }

        if (echo) {
            val delay = SAMPLE_RATE * 85 / 1000
            for (i in delay until mix.size) mix[i] += 0.32f * mix[i - delay]
        }

        // Soft limiter: scale down instead of clipping.
        val peak = mix.maxOf { abs(it) }
        val gain = if (peak > 0.9f) 0.9f / peak else 1f
        return ShortArray(mix.size) {
            (mix[it] * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    /** Wraps the samples in a minimal 16-bit mono WAV file. */
    fun wav(sfx: Sfx, bot: BotType? = null): ByteArray {
        val pcm = render(sfx, bot)
        val dataLength = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataLength).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataLength)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1.toShort())            // PCM
        buffer.putShort(1.toShort())            // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)          // byte rate
        buffer.putShort(2.toShort())            // block align
        buffer.putShort(16.toShort())           // bits per sample
        buffer.put("data".toByteArray())
        buffer.putInt(dataLength)
        pcm.forEach { buffer.putShort(it) }
        return buffer.array()
    }
}