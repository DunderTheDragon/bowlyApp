package com.cantbebetter.bowly.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberCompressedImagePicker(onResult: (String?) -> Unit): CompressedImagePickerActions {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            onResult(bitmap?.let { compressBitmapToBase64(it) })
        } ?: onResult(null)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        onResult(bitmap?.let { compressBitmapToBase64(it) })
    }

    return CompressedImagePickerActions(
        pickFromGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        takePhoto = { cameraLauncher.launch(null) }
    )
}

@Composable
actual fun Base64Image(
    base64: String?,
    modifier: Modifier,
    contentDescription: String?
) {
    val bitmap = remember(base64) {
        base64?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun compressBitmapToBase64(source: Bitmap): String {
    val maxSize = 256
    val ratio = minOf(maxSize.toFloat() / source.width, maxSize.toFloat() / source.height, 1f)
    val width = (source.width * ratio).toInt().coerceAtLeast(1)
    val height = (source.height * ratio).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, width, height, true)
    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 65, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}
