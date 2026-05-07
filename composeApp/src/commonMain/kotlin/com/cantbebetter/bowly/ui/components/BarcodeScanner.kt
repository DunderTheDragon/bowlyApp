package com.cantbebetter.bowly.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun BarcodeScannerView(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
)
