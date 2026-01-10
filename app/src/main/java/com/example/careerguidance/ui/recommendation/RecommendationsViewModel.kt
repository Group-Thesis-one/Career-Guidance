package com.example.careerguidance.ui.recommendation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.careerguidance.data.recommendation.RecommendationEngine
import com.example.careerguidance.data.recommendation.RoleCatalogDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecommendationsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(RecommendationState())
    val uiState: StateFlow<RecommendationState> = _uiState

    init {
        loadRecommendations()
    }

    private fun asStringSet(anyList: Any?): Set<String> {
        val list = (anyList as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return list.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    fun loadRecommendations() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = RecommendationState(
                loading = false,
                error = "No logged-in user"
            )
            return
        }

        _uiState.value = RecommendationState(loading = true)

        val roles = try {
            RoleCatalogDataSource(getApplication()).loadRoles()
        } catch (e: Exception) {
            _uiState.value = RecommendationState(
                loading = false,
                error = "Failed to load roles.json: ${e.message}"
            )
            return
        }

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val skillsNormalized = asStringSet(doc.get("skillsNormalized"))
                val interestsNormalized = asStringSet(doc.get("interestsNormalized"))

                val recs = RecommendationEngine.recommendTopRoles(
                    applicantSkillsNormalized = skillsNormalized,
                    applicantInterestsNormalized = interestsNormalized,
                    roles = roles,
                    topK = 10
                )

                _uiState.value = RecommendationState(
                    loading = false,
                    error = null,
                    recommendations = recs
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = RecommendationState(
                    loading = false,
                    error = e.message ?: "Failed to read profile"
                )
            }
    }
}
