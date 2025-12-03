package com.example.careerguidance.ui.auth

data class SignupState(
    val email: String = "",
    val password: String = "",
    val role: UserRole? = null,   // user must choose; starts as null
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
