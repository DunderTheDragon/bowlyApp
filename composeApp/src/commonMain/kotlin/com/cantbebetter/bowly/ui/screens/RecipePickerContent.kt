package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.RecipeDto
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

@Composable
fun RecipePickerContent(
    viewModel: MainViewModel,
    onRecipeSelected: (RecipeDto) -> Unit,
    onPersonalize: (RecipeDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("MINE") }
    val recipes by viewModel.recipeSearchResults.collectAsState()

    LaunchedEffect(query, scope) {
        viewModel.searchRecipes(query, scope = scope)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Szukaj przepisu...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(
                selected = scope == "MINE",
                onClick = { scope = "MINE" },
                label = { Text("Moje") }
            )
            FilterChip(
                selected = scope == "ALL",
                onClick = { scope = "ALL" },
                label = { Text("Wszyscy") }
            )
        }

        if (recipes.isEmpty()) {
            Text(
                "Brak przepisów do wyświetlenia",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recipes) { recipe ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onRecipeSelected(recipe) }
                            ) {
                                Text(recipe.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${recipe.sections.size} sekcji · ${recipe.sections.sumOf { it.ingredients.size }} składników",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                recipe.username?.let {
                                    Text(
                                        "Autor: $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onPersonalize(recipe) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Personalizuj")
                            }
                        }
                    }
                }
            }
        }
    }
}
