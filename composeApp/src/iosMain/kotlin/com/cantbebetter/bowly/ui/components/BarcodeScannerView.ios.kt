package com.cantbebetter.bowly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun BarcodeScannerView(
    modifier: Modifier,
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text("Scanner not implemented on iOS yet", color = Color.White)
    }
}
