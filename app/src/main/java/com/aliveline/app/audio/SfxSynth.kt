package com.aliveline.app.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates short "bloop" sounds as raw audio, so the app needs no audio files.
 * Each sound is a list of blips: a sine tone whose pitch glides from one frequency to another
 * and fades out quickly. Replace with recorded files later by changing SoundPlayer only.
 */
internal object SfxSynth {
    const val SAMPLE_RATE = 22_050

    private class Blip(
        val startMs: Int,
        val durMs: Int,
        val fromHz: Float,
        val toHz: Float,
        val volume: Float = 0.6f,
    )

    private fun recipe(sfx: Sfx): List<Blip> = when (sfx) {
        Sfx.TAP -> listOf(Blip(0, 55, 520f, 780f, 0.45f))
        Sfx.NAV -> listOf(Blip(0, 85, 380f, 640f, 0.5f))
        Sfx.PICKUP -> listOf(Blip(0, 50, 480f, 700f, 0.5f))
        Sfx.SNAP -> listOf(Blip(0, 110, 200f, 90f, 0.9f), Blip(30, 90, 700f, 1100f, 0.45f))
        Sfx.WAKE -> listOf(
            Blip(0, 120, 523f, 600f),
            Blip(110, 120, 659f, 740f),
            Blip(220, 220, 784f, 900f),
        )
        Sfx.COMPLETE -> listOf(Blip(0, 100, 660f, 800f), Blip(90, 190, 880f, 1000f))
        Sfx.CELEBRATE -> listOf(
            Blip(0, 110, 523f, 620f),
            Blip(100, 110, 659f, 760f),
            Blip(200, 110, 784f, 900f),
            Blip(300, 280, 1047f, 1200f),
        )
        Sfx.UNDO -> listOf(Blip(0, 90, 700f, 520f), Blip(80, 130, 520f, 380f))
        Sfx.SAVE -> listOf(Blip(0, 80, 600f, 760f), Blip(70, 150, 900f, 1100f))
        Sfx.DELETE -> listOf(Blip(0, 130, 450f, 200f))
        Sfx.PIN -> listOf(Blip(0, 60, 900f, 1300f, 0.45f))
        Sfx.TOGGLE -> listOf(Blip(0, 45, 600f, 850f, 0.45f))
    }

    fun render(sfx: Sfx): ShortArray {
        val blips = recipe(sfx)
        val totalMs = blips.maxOf { it.startMs + it.durMs } + 40
        val mix = FloatArray(SAMPLE_RATE * totalMs / 1000)

        for (blip in blips) {
            val start = SAMPLE_RATE * blip.startMs / 1000
            val count = SAMPLE_RATE * blip.durMs / 1000
            var phase = 0.0
            for (i in 0 until count) {
                val index = start + i
                if (index >= mix.size) break
                val progress = i.toFloat() / count
                val hz = blip.fromHz + (blip.toHz - blip.fromHz) * progress
                phase += 2.0 * PI * hz / SAMPLE_RATE
                val seconds = i.toFloat() / SAMPLE_RATE
                val envelope = if (seconds < 0.004f) seconds / 0.004f else exp(-5f * progress)
                val tone = sin(phase) + 0.25 * sin(2.0 * phase)
                mix[index] += (blip.volume * envelope * tone).toFloat()
            }
        }
        return ShortArray(mix.size) { (mix[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
    }

    /** Wraps the samples in a minimal 16-bit mono WAV file. */
    fun wav(sfx: Sfx): ByteArray {
        val pcm = render(sfx)
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