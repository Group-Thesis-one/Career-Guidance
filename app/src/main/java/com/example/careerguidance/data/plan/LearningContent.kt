package com.example.careerguidance.data.plan

data class LearningContent(
    val skill: String = "",
    val why: String = "",
    val steps: List<String> = emptyList()
)
