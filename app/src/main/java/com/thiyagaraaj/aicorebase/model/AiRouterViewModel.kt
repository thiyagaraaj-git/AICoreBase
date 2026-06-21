package com.thiyagaraaj.aicorebase.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.thiyagaraaj.aicorebase.data.AppScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AiRouterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AppScreenState>(AppScreenState.CheckingHardware)
    val uiState: StateFlow<AppScreenState> = _uiState

    init {
        // Run the check automatically when the ViewModel is created
        checkHardwareCapabilities()
    }

    private fun checkHardwareCapabilities() {
        viewModelScope.launch {
            val generativeModel = Generation.getClient()
            val status = generativeModel.checkStatus()

            _uiState.value = when (status) {
                FeatureStatus.AVAILABLE -> AppScreenState.PremiumNanoReady
                FeatureStatus.DOWNLOADABLE -> AppScreenState.NeedsNanoDownload
                FeatureStatus.DOWNLOADING -> AppScreenState.DownloadingNano("Download in progress...")
                FeatureStatus.UNAVAILABLE -> AppScreenState.FallbackToLiteRT
                else -> AppScreenState.FallbackToLiteRT
            }
        }
    }

    // The user clicked "Download" on the UI
    fun triggerNanoDownloadFlow() {
        viewModelScope.launch {
            val generativeModel = Generation.getClient()

            // Collect the download progress stream from the ML Kit API
            generativeModel.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        _uiState.value = AppScreenState.DownloadingNano("Starting download...")
                    }
                    is DownloadStatus.DownloadProgress -> {
                        // status.totalBytesDownloaded provides byte count
                        _uiState.value = AppScreenState.DownloadingNano("Downloading: ${status.totalBytesDownloaded} bytes")
                    }
                    is DownloadStatus.DownloadCompleted -> {
                        _uiState.value = AppScreenState.PremiumNanoReady
                    }
                    is DownloadStatus.DownloadFailed -> {
                        // If the Nano download fails (e.g., no Wi-Fi), fallback to LiteRT
                        _uiState.value = AppScreenState.FallbackToLiteRT
                    }
                }
            }
        }
    }
}
