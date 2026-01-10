package com.example.careerguidance.ui.recommendation

import com.example.careerguidance.data.recommendation.Recommendation

data class RecommendationState(
    val loading: Boolean = true,
    val error: String? = null,
    val recommendations: List<Recommendation> = emptyList()
)
