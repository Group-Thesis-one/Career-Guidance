package com.example.careerguidance.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SignupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(SignupState())
    val uiState: StateFlow<SignupState> = _uiState

    fun updateEmail(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun updatePassword(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun signup() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank() || password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
            return
        }

        _uiState.value = _uiState.value.copy(loading = true, error = null)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(loading = false, success = true)
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Signup failed"
                )
            }
    }
}
