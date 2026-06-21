package com.thiyagaraaj.aicorebase.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GeminiNanoViewModel : ViewModel() {

    private val _output = MutableStateFlow("AI Response will appear here...")
    val output: StateFlow<String> = _output

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun generateText(prompt: String) {
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _output.value = "Thinking..."

            try {
                // Get the local client managed by AICore
                val generativeModel = Generation.getClient()

                // Execute the prompt completely offline
                val response = generativeModel.generateContent(prompt)

                _output.value = response.candidates?.firstOrNull()?.text ?: "No text returned."
            } catch (e: Exception) {
                _output.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}