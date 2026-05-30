package com.cantbebetter.bowly.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.LoginRequest
import com.cantbebetter.bowly.data.network.RegisterRequest
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

@Composable
fun ServerAddressScreen(
    error: String? = null,
    onSetAddress: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Połącz z serwerem Bowly", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Wprowadź adres IP (np. 192.168.1.10:8080)", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Adres serwera") },
            placeholder = { Text("http://") },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            singleLine = true
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSetAddress(url.trim().trimEnd('/')) }) {
            Text("Połącz")
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    error: String? = null,
    onLogin: (LoginRequest) -> Unit,
    onRegister: (RegisterRequest) -> Unit,
    onChangeServer: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var registrationSecret by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val registrationSuccess by viewModel.registrationSuccess.collectAsState()

    LaunchedEffect(registrationSuccess) {
        if (registrationSuccess) {
            isRegisterMode = false
            viewModel.registrationSuccessHandled()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isRegisterMode) "Rejestracja" else "Logowanie", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Login") },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            singleLine = true
        )

        if (isRegisterMode) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Powtórz hasło") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = registrationSecret,
                onValueChange = { registrationSecret = it },
                label = { Text("Hasło backendu") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                if (isRegisterMode) {
                    onRegister(RegisterRequest(username, password, registrationSecret))
                } else {
                    onLogin(LoginRequest(username, password))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = if (isRegisterMode) {
                username.isNotBlank() && password.isNotBlank() && password == confirmPassword && registrationSecret.isNotBlank()
            } else {
                username.isNotBlank() && password.isNotBlank()
            }
        ) {
            Text(if (isRegisterMode) "Zarejestruj się" else "Zaloguj")
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Masz już konto? Zaloguj się" else "Nie masz konta? Zarejestruj się")
        }

        TextButton(onClick = onChangeServer) {
            Text("Zmień adres serwera")
        }
    }
}