package com.cantbebetter.bowly.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.AdminKeysDto
import com.cantbebetter.bowly.data.network.RegisterRequest
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val adminKeys by viewModel.adminKeys.collectAsState()
    val users by viewModel.users.collectAsState()
    val error by viewModel.error.collectAsState()

    // Pokazywanie błędu na ekranie admina, jeśli wystąpił
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAdminData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Panel Administratora") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Powrót")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ApiKeysSection(
                    keys = adminKeys,
                    onSave = { viewModel.saveAdminKeys(it) }
                )
            }

            item {
                UsersSection(
                    users = users,
                    onAddUser = { viewModel.registerUser(it) },
                    onDeleteUser = { viewModel.deleteUser(it) }
                )
            }
        }
    }
}

@Composable
fun ApiKeysSection(
    keys: AdminKeysDto?,
    onSave: (AdminKeysDto) -> Unit
) {
    var spoonacular by remember(keys) { mutableStateOf(keys?.spoonacularKey ?: "") }
    var off by remember(keys) { mutableStateOf(keys?.openFoodFactsKey ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Klucze API", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = spoonacular,
                onValueChange = { spoonacular = it },
                label = { Text("Spoonacular API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = off,
                onValueChange = { off = it },
                label = { Text("Open Food Facts Key (Opcjonalnie)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(AdminKeysDto(spoonacular, off)) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Zapisz klucze")
            }
        }
    }
}

@Composable
fun UsersSection(
    users: List<com.cantbebetter.bowly.data.network.UserDto>,
    onAddUser: (RegisterRequest) -> Unit,
    onDeleteUser: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<com.cantbebetter.bowly.data.network.UserDto?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Domownicy", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Dodaj")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        users.forEach { user ->
            ListItem(
                headlineContent = { Text(user.username) },
                supportingContent = { Text(user.role ?: "USER") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user.role == "ADMIN") {
                            Badge(modifier = Modifier.padding(end = 8.dp)) { Text("Admin") }
                        }
                        IconButton(onClick = { userToDelete = user }) {
                            Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { username, password ->
                onAddUser(RegisterRequest(username, password))
                showAddDialog = false
            }
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Usuń użytkownika") },
            text = { Text("Czy na pewno chcesz usunąć domownika ${user.username}? Tej operacji nie można cofnąć.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        user.id?.let { onDeleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isUsernameValid = username.length in 3..50
    val isPasswordValid = password.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj nowego domownika") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Login") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = username.isNotEmpty() && !isUsernameValid,
                    supportingText = {
                        Text("Od 3 do 50 znaków")
                    }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Hasło") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = password.isNotEmpty() && !isPasswordValid,
                    supportingText = {
                        Text("Minimum 6 znaków")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(username, password) },
                enabled = isUsernameValid && isPasswordValid
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
