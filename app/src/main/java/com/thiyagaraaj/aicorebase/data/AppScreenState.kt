package com.thiyagaraaj.aicorebase.data

sealed class AppScreenState {
    object CheckingHardware : AppScreenState()
    object PremiumNanoReady : AppScreenState()
    object NeedsNanoDownload : AppScreenState()
    data class DownloadingNano(val progressMessage: String) : AppScreenState()
    object FallbackToLiteRT : AppScreenState()
}
