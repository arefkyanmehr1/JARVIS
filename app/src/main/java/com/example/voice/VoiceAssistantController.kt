package com.example.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.data.repository.AiRepository
import com.example.data.repository.AiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAssistantController(
    private val context: Context,
    private val aiRepository: AiRepository,
    private val scope: CoroutineScope
) {
    data class State(
        val listening: Boolean = false,
        val thinking: Boolean = false,
        val status: String = "آماده",
        val userText: String = "",
        val assistantText: String = "",
        val error: String? = null
    )

    var onState: ((State) -> Unit)? = null
    private var state = State()
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            emit(state.copy(error = "تشخیص گفتار روی این دستگاه در دسترس نیست."))
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { emit(state.copy(listening = true, status = "گوش می‌دهم…", error = null)) }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { emit(state.copy(listening = false, status = "در حال تحلیل…", thinking = true)) }
                    override fun onError(error: Int) { emit(state.copy(listening = false, thinking = false, status = "آماده", error = "تشخیص گفتار خطا داد: " + error)) }
                    override fun onResults(results: android.os.Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        if (text.isBlank()) {
                            emit(state.copy(listening = false, thinking = false, status = "آماده", error = "صدایی به متن تبدیل نشد."))
                            return
                        }
                        emit(state.copy(listening = false, thinking = true, status = "در حال پاسخ…", userText = text, error = null))
                        answer(text)
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) emit(state.copy(userText = text))
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }

    private fun answer(text: String) {
        scope.launch(Dispatchers.IO) {
            val result = aiRepository.generateResponse(
                prompt = text,
                conversationContext = emptyList(),
                customSystemInstruction = "تو JARVIS هستی. این یک گفت‌وگوی صوتی با صاحب گوشی است. پاسخ فارسی، طبیعی، کوتاه و دقیق بده. اگر کاربر فقط یک فرمان ساده یا سؤال مشخص دارد، مستقیم پاسخ بده."
            )
            val response = when (result) {
                is AiResult.Success -> result.text.trim()
                is AiResult.Error -> "متأسفم، فعلاً نتوانستم به Gemini وصل شوم: " + result.message
            }
            emit(state.copy(thinking = false, status = "آماده", assistantText = response))
            speak(response)
        }
    }

    private fun speak(text: String) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("fa", "IR")
                    tts?.setSpeechRate(0.95f)
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_voice")
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_voice")
        }
    }

    fun stop() {
        recognizer?.stopListening()
        tts?.stop()
        emit(state.copy(listening = false, thinking = false, status = "آماده"))
    }

    fun close() {
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun emit(next: State) {
        state = next
        onState?.invoke(next)
    }
}