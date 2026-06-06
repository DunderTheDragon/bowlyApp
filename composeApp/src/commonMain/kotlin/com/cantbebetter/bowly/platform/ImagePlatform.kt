package com.cantbebetter.bowly.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class CompressedImagePickerActions(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit
)

@Composable
expect fun rememberCompressedImagePicker(onResult: (String?) -> Unit): CompressedImagePickerActions

@Composable
expect fun Base64Image(
    base64: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
)
