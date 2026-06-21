package com.thiyagaraaj.aicorebase.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiyagaraaj.aicorebase.data.AppScreenState
import com.thiyagaraaj.aicorebase.model.AiRouterViewModel


@Composable
fun BaseView(viewModel: AiRouterViewModel = viewModel()){
    // ShowText("")
// Observe the state from the ViewModel
    val screenState by viewModel.uiState.collectAsState()

    // Compose automatically redraws when screenState changes
    when (screenState) {
        is AppScreenState.CheckingHardware -> {
            LoadingScreen("Checking your device's AI capabilities...")
        }

        is AppScreenState.PremiumNanoReady -> {
            // Path A: Nano is ready. Route them into the main app!
            DashboardScreen()
        }

        is AppScreenState.NeedsNanoDownload -> {
            // Path A (Delayed): Ask the user to download the model
            DownloadPromptScreen(
                title = "Unlock Offline Privacy",
                message = "Your device supports Google's private offline AI. Download the engine (via Wi-Fi) to enable smart health tracking.",
                onDownloadClick = { viewModel.triggerNanoDownloadFlow() }
            )
        }

        is AppScreenState.DownloadingNano -> {
            val currentMessage = (screenState as AppScreenState.DownloadingNano).progressMessage
            LoadingScreen(currentMessage)
        }

        is AppScreenState.FallbackToLiteRT -> {
            // Path B: Fallback to your 260 MB LiteRT-LM model.
            // You would trigger your custom .gguf download logic here
            LoadingScreen("Unsupported Device")
        }
    }
}
