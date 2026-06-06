package com.cantbebetter.bowly.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun rememberCompressedImagePicker(onResult: (String?) -> Unit): CompressedImagePickerActions {
    return CompressedImagePickerActions(
        pickFromGallery = { onResult(null) },
        takePhoto = { onResult(null) }
    )
}

@Composable
actual fun Base64Image(
    base64: String?,
    modifier: Modifier,
    contentDescription: String?
) {
    // Brak dekodera base64 na iOS w tej wersji
}
