package com.example.careerguidance.ui.profile

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val phone: String = "",
    val bio: String = "",
    val nameEditable: Boolean = true,   // can user edit name?
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
