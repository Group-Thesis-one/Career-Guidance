package com.example.careerguidance.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState

    fun updateEmail(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun updatePassword(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun loginEmailPassword() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Email and password required")
            return
        }

        _uiState.value = _uiState.value.copy(loading = true, error = null)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(loading = false, success = true)
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Login failed"
                )
            }
    }
}
