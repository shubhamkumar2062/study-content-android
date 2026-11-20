package app.studydesk.core.reader.readaloud

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** One sentence to speak, with where it sits in the document so it can be highlighted. */
data class SpeechSegment(val blockIndex: Int, val start: Int, val end: Int, val text: String)

data class ReadAloudState(
    val status: Status = Status.Idle,
    /** Which document is being read (so another document's screen doesn't show the controls). */
    val docKey: String? = null,
    val title: String = "",
    val current: SpeechSegment? = null,
    val rate: Float = 1f,
) {
    enum class Status { Idle, Preparing, Speaking, Paused, Unavailable }

    val active: Boolean get() = status == Status.Speaking || status == Status.Paused || status == Status.Preparing
}

/**
 * Reads notes aloud with the phone's own text-to-speech engine (works offline once the
 * engine's voice is installed). Lives as long as the app, so it keeps going while the
 * reader screen is closed; [ReadAloudService] keeps the app alive with the screen off.
 */
@Singleton
class ReadAloudController @Inject constructor(@ApplicationContext private val context: Context) {
    private val _state = MutableStateFlow(ReadAloudState())
    val state: StateFlow<ReadAloudState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false
    private var segments: List<SpeechSegment> = emptyList()
    private var position = 0
    private var pendingStart: (() -> Unit)? = null

    fun start(docKey: String, title: String, segments: List<SpeechSegment>, from: Int = 0) {
        if (segments.isEmpty()) return
        this.segments = segments
        position = from.coerceIn(0, segments.lastIndex)
        _state.update { it.copy(status = ReadAloudState.Status.Preparing, docKey = docKey, title = title, current = segments[position]) }
        ensureEngine { speakFrom(position) }
        ContextCompat.startForegroundService(context, Intent(context, ReadAloudService::class.java))
    }

    fun pause() {
        if (_state.value.status != ReadAloudState.Status.Speaking) return
        tts?.stop()
        _state.update { it.copy(status = ReadAloudState.Status.Paused) }
        ReadAloudService.refresh(context)
    }

    fun resume() {
        if (_state.value.status != ReadAloudState.Status.Paused) return
        speakFrom(position)
        ReadAloudService.refresh(context)
    }

    fun skip(by: Int) {
        if (segments.isEmpty()) return
        position = (position + by).coerceIn(0, segments.lastIndex)
        if (_state.value.status == ReadAloudState.Status.Speaking) speakFrom(position)
        else _state.update { it.copy(current = segments[position]) }
    }

    fun setRate(rate: Float) {
        val r = rate.coerceIn(0.5f, 2f)
        tts?.setSpeechRate(r)
        _state.update { it.copy(rate = r) }
        if (_state.value.status == ReadAloudState.Status.Speaking) speakFrom(position)
    }

    fun stop() {
        tts?.stop()
        segments = emptyList()
        _state.update { ReadAloudState(rate = it.rate) }
        context.stopService(Intent(context, ReadAloudService::class.java))
    }

    private fun speakFrom(index: Int) {
        val engine = tts ?: return
        engine.stop()
        position = index
        _state.update { it.copy(status = ReadAloudState.Status.Speaking, current = segments[index]) }
        // Queue a few sentences ahead so there's no gap between them.
        for (i in index until minOf(index + 3, segments.size)) {
            engine.speak(segments[i].text, if (i == index) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, i.toString())
        }
    }

    private fun ensureEngine(onReady: () -> Unit) {
        if (ready) {
            onReady()
            return
        }
        pendingStart = onReady
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                _state.update { it.copy(status = ReadAloudState.Status.Unavailable) }
                context.stopService(Intent(context, ReadAloudService::class.java))
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            val lang = engine.setLanguage(Locale("en", "IN")).takeIf { it >= TextToSpeech.LANG_AVAILABLE }
                ?: engine.setLanguage(Locale.ENGLISH)
            if (lang < TextToSpeech.LANG_AVAILABLE) {
                _state.update { it.copy(status = ReadAloudState.Status.Unavailable) }
                return@TextToSpeech
            }
            engine.setSpeechRate(_state.value.rate)
            engine.setOnUtteranceProgressListener(listener)
            ready = true
            pendingStart?.invoke()
            pendingStart = null
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            val i = utteranceId.toIntOrNull() ?: return
            segments.getOrNull(i)?.let { seg ->
                position = i
                _state.update { it.copy(current = seg) }
            }
        }

        override fun onDone(utteranceId: String) {
            val i = utteranceId.toIntOrNull() ?: return
            if (_state.value.status != ReadAloudState.Status.Speaking) return
            val queued = i + 3
            when {
                queued < segments.size -> tts?.speak(segments[queued].text, TextToSpeech.QUEUE_ADD, null, queued.toString())
                i == segments.lastIndex -> stop()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) = Unit

        override fun onError(utteranceId: String?, errorCode: Int) = Unit
    }

    companion object {
        /** Splits a block's text into sentences (keeping offsets) for highlighting while speaking. */
        fun sentences(blockIndex: Int, text: String): List<SpeechSegment> {
            val out = mutableListOf<SpeechSegment>()
            val pattern = Regex("[^.!?\\n]+[.!?]*\\s*")
            for (m in pattern.findAll(text)) {
                val trimmed = m.value.trim()
                if (trimmed.length < 2) continue
                val start = m.range.first + (m.value.length - m.value.trimStart().length)
                out += SpeechSegment(blockIndex, start, start + trimmed.length, trimmed)
            }
            return out
        }

        val isForegroundTypeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
