package com.example.careerguidance.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoogleLogin: () -> Unit,
    onSignupClick: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    // Navigate when Firebase login sets success = true
    if (state.success) {
        onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // --- IMPORTANT: calls the ViewModel function that actually exists ---
        Button(
            onClick = { viewModel.loginEmailPassword() },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("Login with Email")
        }

        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with Google")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = onSignupClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create an account")
        }
    }
}
