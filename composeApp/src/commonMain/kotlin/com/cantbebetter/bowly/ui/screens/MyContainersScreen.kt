package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.CreateWeighingContainerRequest
import com.cantbebetter.bowly.data.network.UpdateWeighingContainerRequest
import com.cantbebetter.bowly.data.network.WeighingContainerDto
import com.cantbebetter.bowly.platform.Base64Image
import com.cantbebetter.bowly.platform.rememberCompressedImagePicker
import com.cantbebetter.bowly.ui.components.containerTypeLabel
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

private val containerTypes = listOf("PAN", "PLATE", "POT", "OTHER")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContainersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val containers by viewModel.containers.collectAsState()
    val error by viewModel.error.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingContainer by remember { mutableStateOf<WeighingContainerDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadContainers() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Moje naczynia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingContainer = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj naczynie")
            }
        }
    ) { padding ->
        if (containers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Dodaj patelnie, talerze lub garnki z wagą,\naby łatwo odejmować tarę przy ważeniu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(containers, key = { it.id }) { container ->
                    ContainerCard(
                        container = container,
                        onEdit = {
                            editingContainer = container
                            showDialog = true
                        },
                        onDelete = { viewModel.deleteContainer(container.id) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        ContainerEditorDialog(
            initial = editingContainer,
            onDismiss = { showDialog = false },
            onSave = { name, type, weightG, imageBase64 ->
                if (editingContainer != null) {
                    viewModel.updateContainer(
                        editingContainer!!.id,
                        UpdateWeighingContainerRequest(name, type, weightG, imageBase64)
                    ) { showDialog = false }
                } else {
                    viewModel.createContainer(
                        CreateWeighingContainerRequest(name, type, weightG, imageBase64)
                    ) { showDialog = false }
                }
            }
        )
    }
}

@Composable
private fun ContainerCard(
    container: WeighingContainerDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Base64Image(
                base64 = container.imageBase64,
                modifier = Modifier.size(56.dp),
                contentDescription = container.name
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(container.name, fontWeight = FontWeight.Bold)
                Text(
                    "${containerTypeLabel(container.type)} · ${container.weightG.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edytuj")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ContainerEditorDialog(
    initial: WeighingContainerDto?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, weightG: Double, imageBase64: String?) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "PLATE") }
    var weightText by remember(initial) {
        mutableStateOf(initial?.weightG?.toString()?.removeSuffix(".0") ?: "")
    }
    var imageBase64 by remember(initial) { mutableStateOf(initial?.imageBase64) }

    val imagePicker = rememberCompressedImagePicker { picked ->
        if (picked != null) imageBase64 = picked
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nowe naczynie" else "Edytuj naczynie") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nazwa") },
                    singleLine = true
                )
                Text("Typ", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    containerTypes.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(containerTypeLabel(option)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Waga (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = imagePicker.pickFromGallery) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeria")
                    }
                    OutlinedButton(onClick = imagePicker.takePhoto) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aparat")
                    }
                }
                Base64Image(
                    base64 = imageBase64,
                    modifier = Modifier.size(80.dp),
                    contentDescription = "Podgląd"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.toDoubleOrNull() ?: return@Button
                    if (name.isBlank() || weight <= 0) return@Button
                    onSave(name.trim(), type, weight, imageBase64)
                },
                enabled = name.isNotBlank() && (weightText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
