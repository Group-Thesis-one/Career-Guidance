package com.example.careerguidance.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val viewModel: SignupViewModel = viewModel()
    val state = viewModel.uiState.collectAsState().value

    if (state.success) onSignupSuccess()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(20.dp))

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
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Register as (required)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.role == UserRole.APPLICANT,
                    onClick = { viewModel.updateRole(UserRole.APPLICANT) }
                )
                Text("Applicant")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.role == UserRole.COMPANY,
                    onClick = { viewModel.updateRole(UserRole.COMPANY) }
                )
                Text("Company")
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.signup() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading && state.role != null  // disabled until role chosen
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("Sign Up")
        }

        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }
    }
}
