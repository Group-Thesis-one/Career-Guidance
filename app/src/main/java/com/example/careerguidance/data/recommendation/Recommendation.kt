package com.example.careerguidance.data.recommendation

data class Recommendation(
    val roleId: String,
    val title: String,
    val score: Double,
    val matchedSkills: List<String>,
    val missingRequired: List<String>,
    val missingOptional: List<String>
)
