package com.thiyagaraaj.aicorebase.model

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

// ==========================================
// 1. THE VIEWMODEL (LOGIC LAYER)
// ==========================================
class VoiceAiViewModel : ViewModel() {

    private val _statusText = MutableStateFlow("Press 'Start Listening' to speak.")
    val statusText: StateFlow<String> = _statusText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isRecording) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            _statusText.value = "Failed to initialize microphone."
            return
        }

        isRecording = true
        _isListening.value = true
        _statusText.value = "Listening..."
        audioRecord?.startRecording()

        // Capture microphone data on a background IO thread safely
        viewModelScope.launch(Dispatchers.IO) {
            val audioOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)

            while (isRecording) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readBytes > 0) {
                    audioOutputStream.write(buffer, 0, readBytes)
                }
            }

            // Send the raw PCM audio bytes to the local AI engine
            processAudioWithNano(audioOutputStream.toByteArray())
        }
    }

    fun stopListening() {
        isRecording = false
        _isListening.value = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Safely ignore if recorder wasn't fully initialized
        }
        audioRecord = null
    }

    private suspend fun processAudioWithNano(audioBytes: ByteArray) {
        _statusText.value = "Analyzing speech locally via Gemini Nano..."

        try {
            // Configure options for the local speech client
            val options = speechRecognizerOptions {
                locale = Locale.US
                // Explicitly request the on-device Gemini Nano engine
                preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
            }
            // 1. Use SpeechRecognition (the factory) to get the client instance
            val speechRecognizer = SpeechRecognition.getClient(options)

            // 2. Instantiate the builder using the public constructor
            val requestBuilder = SpeechRecognizerRequest.Builder()
            // 3. Add parentheses to Builder() to instantiate it
            // 3. Assign the AudioSource directly using the native fromMic() factory
            // Note: fromMic() requires Android 12 (API 31) or higher
            requestBuilder.audioSource = AudioSource.fromMic()

            val request = requestBuilder.build()

            _statusText.value = "Listening live..."

            // 4. Use 'startRecognition' and handle the sealed class states
            speechRecognizer.startRecognition(request).collect { response ->
                when (response) {
                    is SpeechRecognizerResponse.PartialTextResponse -> {
                        // Updates the UI live as the user speaks
                        _statusText.value = response.text
                    }
                    is SpeechRecognizerResponse.FinalTextResponse -> {
                        // The final completed sentence
                        _statusText.value = response.text
                    }
                    is SpeechRecognizerResponse.ErrorResponse -> {
                        _statusText.value = "Recognition error occurred."
                    }
                    is SpeechRecognizerResponse.CompletedResponse -> {
                        // The audio stream is finished processing
                    }
                }
            }
            speechRecognizer.close()

        } catch (e: Exception) {
            _statusText.value = "Error: ${e.localizedMessage}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}

// ==========================================
// 2. THE COMPOSE UI (PRESENTATION LAYER)
// ==========================================
@Composable
fun SimpleVoiceNanoScreen(viewModel: VoiceAiViewModel = viewModel()) {
    val statusText by viewModel.statusText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Voice-to-Text Gemini Nano",
            style = MaterialTheme.typography.titleLarge
        )

        // Recording Controller Button
        Button(
            onClick = {
                if (isListening) {
                    viewModel.stopListening()
                } else {
                    viewModel.startListening()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isListening) "Stop Listening" else "Start Listening")
        }

        // Output Display Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = statusText,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}