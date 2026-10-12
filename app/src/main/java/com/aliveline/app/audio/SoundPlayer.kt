package com.aliveline.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.aliveline.app.domain.model.BotType
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
    private data class Key(val sfx: Sfx, val bot: BotType?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val soundIds = ConcurrentHashMap<Key, Int>()

    @Volatile private var enabled = true
    @Volatile private var volume = 0.8f

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
        scope.launch {
            settings.settings.collect {
                enabled = it.soundEnabled
                volume = it.soundVolume
            }
        }
        scope.launch {
            Sfx.entries.forEach { sfx ->
                val variants: List<BotType?> =
                    if (SfxSynth.isBotAware(sfx)) listOf<BotType?>(null) + BotType.entries else listOf(null)
                variants.forEach { bot -> load(sfx, bot) }
            }
        }
    }

    private fun load(sfx: Sfx, bot: BotType?) {
        val name = "sfx2_${sfx.name.lowercase()}_${bot?.name?.lowercase() ?: "any"}.wav"
        val file = File(context.cacheDir, name)
        file.writeBytes(SfxSynth.wav(sfx, bot))
        soundIds[Key(sfx, bot)] = pool.load(file.absolutePath, 1)
    }

    /**
     * @param bot picks that bot's "voice" for wake, complete and celebrate sounds (ignored for other sounds).
     * @param volume overrides the saved volume, used by the Settings slider preview.
     */
    fun play(sfx: Sfx, bot: BotType? = null, volume: Float? = null) {
        if (!enabled) return
        val key = Key(sfx, if (SfxSynth.isBotAware(sfx)) bot else null)
        val id = soundIds[key] ?: return
        val v = (volume ?: this.volume).coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 1f)
    }
}