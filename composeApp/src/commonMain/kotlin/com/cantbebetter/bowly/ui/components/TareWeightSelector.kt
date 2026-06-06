package com.cantbebetter.bowly.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.WeighingContainerDto
import com.cantbebetter.bowly.platform.Base64Image
import kotlin.math.max

fun containerTypeLabel(type: String): String = when (type.uppercase()) {
    "PAN" -> "Patelnia"
    "PLATE" -> "Talerz"
    "POT" -> "Garnek"
    else -> "Inne"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareWeightSelector(
    containers: List<WeighingContainerDto>,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedContainerId: Long?,
    onContainerSelected: (Long?) -> Unit,
    grossWeightText: String,
    onGrossWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    weightLabel: String = "Waga brutto (g)",
    showGrossInput: Boolean = true
) {
    val netWeight = remember(enabled, grossWeightText, selectedContainerId, containers) {
        if (!enabled) return@remember grossWeightText.toDoubleOrNull()
        val gross = grossWeightText.toDoubleOrNull() ?: return@remember null
        val tare = containers.find { it.id == selectedContainerId }?.weightG ?: 0.0
        max(gross - tare, 0.0)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Odejmij wagę naczynia", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }

        if (enabled) {
            if (containers.isEmpty()) {
                Text(
                    "Brak zdefiniowanych naczyń. Dodaj je w profilu → Moje naczynia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                var expanded by remember { mutableStateOf(false) }
                val selected = containers.find { it.id == selectedContainerId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selected?.let { "${it.name} (${it.weightG.toInt()}g)" } ?: "Wybierz naczynie",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        containers.forEach { container ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Base64Image(
                                            base64 = container.imageBase64,
                                            modifier = Modifier.size(32.dp),
                                            contentDescription = container.name
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(container.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${containerTypeLabel(container.type)} · ${container.weightG.toInt()}g",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onContainerSelected(container.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (showGrossInput) {
                OutlinedTextField(
                    value = grossWeightText,
                    onValueChange = onGrossWeightChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(weightLabel) },
                    singleLine = true
                )
            }

            if (showGrossInput || grossWeightText.isNotBlank()) {
                netWeight?.let { net ->
                    Text(
                        "Waga netto (jedzenie): ${net.toInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

fun resolveNetWeight(
    enabled: Boolean,
    grossWeightText: String,
    selectedContainerId: Long?,
    containers: List<WeighingContainerDto>
): Double? {
    val gross = grossWeightText.toDoubleOrNull() ?: return null
    if (!enabled) return gross
    val tare = containers.find { it.id == selectedContainerId }?.weightG ?: return null
    return max(gross - tare, 0.0)
}
