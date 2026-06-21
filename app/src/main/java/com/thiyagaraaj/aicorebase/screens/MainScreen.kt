package com.thiyagaraaj.aicorebase.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiyagaraaj.aicorebase.data.AppScreenState
import com.thiyagaraaj.aicorebase.view.BaseView

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MainScreenContainer() {

    Scaffold(topBar = { TopAppBar(title = { Text(text = "Nano App") }) }) { innerPadding ->
        // Use innerPadding to automatically place the body content
        // below the TopBar and above the bottom system navigation bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            BaseView()
        }
    }
}
