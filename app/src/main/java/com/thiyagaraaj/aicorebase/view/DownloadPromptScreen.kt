package com.thiyagaraaj.aicorebase.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun DownloadPromptScreen(title: String, message: String, onDownloadClick: () -> Unit) {
    Column() {
        Text(
            text = title,
            fontSize = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = message,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onDownloadClick()
            }
        ) {
            Text(
                text = "Download",
            )
        }
    }
}
