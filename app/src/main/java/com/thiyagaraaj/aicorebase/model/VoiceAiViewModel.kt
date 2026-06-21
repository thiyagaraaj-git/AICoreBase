package com.thiyagaraaj.aicorebase.model

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAiViewModel(application: Application) : AndroidViewModel(application) {

    private val _statusText = MutableStateFlow("Checking speech recognition availability...")
    val statusText: StateFlow<String> = _statusText

    private val _userSpeech = MutableStateFlow("")
    val userSpeech: StateFlow<String> = _userSpeech

    private val _geminiReply = MutableStateFlow("")
    val geminiReply: StateFlow<String> = _geminiReply

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isRecording = false
    private var accumulatedText = ""

    init {
        createSpeechRecognizer()
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
        viewModelScope.launch { refreshFeatureStatus() }
    }

    private fun createSpeechRecognizer() {
        speechRecognizer?.close()
        speechRecognizer = SpeechRecognition.getClient(
            speechRecognizerOptions {
                locale = Locale.US
                preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
            }
        )
    }

    private suspend fun refreshFeatureStatus() {
        val status = speechRecognizer?.checkStatus() ?: FeatureStatus.UNAVAILABLE
        _statusText.value = featureStatusMessage(status)
    }

    fun startListening() {
        if (isRecording || _isGenerating.value) return

        _userSpeech.value = ""
        _geminiReply.value = ""

        viewModelScope.launch {
            when (speechRecognizer?.checkStatus() ?: FeatureStatus.UNAVAILABLE) {
                FeatureStatus.AVAILABLE -> startSpeechRecognition()
                FeatureStatus.DOWNLOADABLE -> downloadModelAndStart()
                FeatureStatus.DOWNLOADING -> {
                    _statusText.value = "Speech model is downloading. Please wait..."
                }
                else -> {
                    _statusText.value = featureStatusMessage(
                        speechRecognizer?.checkStatus() ?: FeatureStatus.UNAVAILABLE
                    )
                }
            }
        }
    }

    fun stopListening() {
        if (!isRecording) return

        viewModelScope.launch {
            speechRecognizer?.stopRecognition()
        }
    }

    private suspend fun downloadModelAndStart() {
        _statusText.value = "Downloading speech model..."
        speechRecognizer?.download()?.collect { downloadStatus ->
            when (downloadStatus) {
                is DownloadStatus.DownloadStarted -> {
                    _statusText.value = "Download started..."
                }
                is DownloadStatus.DownloadProgress -> {
                    _statusText.value =
                        "Downloading: ${downloadStatus.totalBytesDownloaded} bytes"
                }
                is DownloadStatus.DownloadCompleted -> startSpeechRecognition()
                is DownloadStatus.DownloadFailed -> {
                    _statusText.value = "Download failed: ${downloadStatus.e.message}"
                }
            }
        }
    }

    private suspend fun startSpeechRecognition() {
        isRecording = true
        _isListening.value = true
        accumulatedText = ""
        _statusText.value = "Listening..."

        val request = speechRecognizerRequest {
            audioSource = AudioSource.fromMic()
        }

        try {
            speechRecognizer?.startRecognition(request)?.collect { response ->
                when (response) {
                    is SpeechRecognizerResponse.PartialTextResponse -> {
                        _statusText.value = accumulatedText + response.text
                    }
                    is SpeechRecognizerResponse.FinalTextResponse -> {
                        accumulatedText += response.text
                        _userSpeech.value = accumulatedText
                        _statusText.value = accumulatedText
                    }
                    is SpeechRecognizerResponse.ErrorResponse -> {
                        _statusText.value =
                            "Recognition error: ${response.e.message} (code ${response.e.errorCode})"
                        finishListening()
                    }
                    is SpeechRecognizerResponse.CompletedResponse -> {
                        finishListening()
                        if (accumulatedText.isNotBlank()) {
                            askGeminiNano(accumulatedText)
                        } else {
                            _statusText.value = "No speech detected. Try again."
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _statusText.value = "Error: ${e.localizedMessage}"
            finishListening()
        }
    }

    private fun askGeminiNano(prompt: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            _statusText.value = "Asking Gemini Nano..."
            _geminiReply.value = ""

            try {
                val response = Generation.getClient().generateContent(prompt)
                val reply = response.candidates?.firstOrNull()?.text
                    ?: "No response from Gemini Nano."

                _geminiReply.value = reply
                _statusText.value = "Ready. Press 'Start Listening' to speak."
                speakReply(reply)
            } catch (e: Exception) {
                _geminiReply.value = "Error: ${e.localizedMessage}"
                _statusText.value = "Gemini Nano error: ${e.localizedMessage}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun speakReply(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gemini_reply")
    }

    private fun finishListening() {
        isRecording = false
        _isListening.value = false
    }

    private fun featureStatusMessage(status: Int): String {
        return when (status) {
            FeatureStatus.AVAILABLE -> "Ready. Press 'Start Listening' to speak."
            FeatureStatus.DOWNLOADABLE ->
                "Speech model not installed. Press 'Start Listening' to download and begin."
            FeatureStatus.DOWNLOADING -> "Speech model is downloading. Please wait..."
            FeatureStatus.UNAVAILABLE -> "Speech recognition is unavailable on this device."
            else -> "Speech recognition status unknown ($status)."
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        speechRecognizer?.close()
        speechRecognizer = null
    }
}
