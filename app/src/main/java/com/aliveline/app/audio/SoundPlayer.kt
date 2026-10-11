package com.aliveline.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.aliveline.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The one place the app plays sounds. Swap the synthesized sounds for real files here later. */
@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val soundIds = ConcurrentHashMap<Sfx, Int>()

    @Volatile
    private var enabled = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    init {
        scope.launch { settings.soundEnabled.collect { enabled = it } }
        scope.launch {
            Sfx.entries.forEach { sfx ->
                val file = File(context.cacheDir, "sfx_${sfx.name.lowercase()}.wav")
                file.writeBytes(SfxSynth.wav(sfx))
                soundIds[sfx] = pool.load(file.absolutePath, 1)
            }
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val id = soundIds[sfx] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }
}