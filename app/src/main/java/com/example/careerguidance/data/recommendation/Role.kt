package com.example.careerguidance.data.recommendation

data class Role(
    val id: String,
    val title: String,
    val requiredSkills: List<String>,
    val optionalSkills: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)
