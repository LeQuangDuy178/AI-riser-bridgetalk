package com.bridgetalk.app.sender

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * On-device Android Text-to-Speech voice used as the offline fallback when
 * the BridgeTalk backend is unreachable. This is a real TTS engine, not a
 * mock of Cloud TTS: production still calls Cloud TTS through the backend
 * first; this voice exists so the caller's experience never degrades into
 * the legacy "production backend not connected" error.
 */
internal class LocalTtsVoice(
    context: Context,
    private val locale: Locale = Locale("vi", "VN"),
) {
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(locale)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        } else {
            Log.w(TAG, "TextToSpeech init failed with status $status")
        }
    }

    @Volatile
    private var ready: Boolean = false

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            // The engine is still initialising. Queue with FLUSH so it
            // replaces any prior utterance; if the engine never becomes
            // ready, this is a no-op.
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun close() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "BridgeTalk/TTS"
        private const val UTTERANCE_ID = "bridgetalk-offline-send"
    }
}
