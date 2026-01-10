package com.example.careerguidance.ui.profile

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val phone: String = "",
    val bio: String = "",

    // new fields for recommendations
    // user types comma-separated values like: "Kotlin, Compose, MVVM"
    val skillsText: String = "",
    val interestsText: String = "",

    val nameEditable: Boolean = true,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
