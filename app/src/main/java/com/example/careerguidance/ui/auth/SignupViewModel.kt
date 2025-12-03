package com.example.careerguidance.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SignupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(SignupState())
    val uiState: StateFlow<SignupState> = _uiState

    fun updateEmail(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun updatePassword(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun updateRole(newRole: UserRole) {
        _uiState.value = _uiState.value.copy(role = newRole, error = null)
    }

    fun signup() {
        val state = _uiState.value
        val email = state.email
        val password = state.password
        val role = state.role

        if (email.isBlank() || password.length < 6) {
            _uiState.value = state.copy(error = "Password must be at least 6 characters")
            return
        }

        if (role == null) {
            _uiState.value = state.copy(error = "Please choose account type: Applicant or Company")
            return
        }

        _uiState.value = state.copy(loading = true, error = null)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser

                if (user != null) {
                    val data = mapOf(
                        "email" to (user.email ?: email),
                        "role" to when (role) {
                            UserRole.APPLICANT -> "applicant"
                            UserRole.COMPANY -> "company"
                        },
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("users")
                        .document(user.uid)
                        .set(data, SetOptions.merge())
                }

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
