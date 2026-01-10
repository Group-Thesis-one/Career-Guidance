package com.example.careerguidance.ui.impact

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SkillImpactState(
    val loading: Boolean = true,
    val goalRole: String = "",
    val completedSkills: List<String> = emptyList(),
    val baselineScore: Int? = null,
    val latestScore: Int? = null,
    val scoreChange: Int? = null,
    val error: String? = null
)

class SkillImpactViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _ui = MutableStateFlow(SkillImpactState())
    val ui: StateFlow<SkillImpactState> = _ui

    fun load() {
        val user = auth.currentUser
        if (user == null) {
            _ui.value = SkillImpactState(loading = false, error = "No logged-in user")
            return
        }

        _ui.value = _ui.value.copy(loading = true, error = null)

        val userRef = db.collection("users").document(user.uid)
        val plansRef = userRef.collection("plans")

        userRef.get()
            .addOnSuccessListener { doc ->
                val goalRole = doc.getString("goalRole").orEmpty()

                plansRef.get()
                    .addOnSuccessListener { snap ->
                        val progressDoc = snap.documents.firstOrNull { it.id == "goalPlanProgress" }
                        val completed = (progressDoc?.get("completedSkills") as? List<*>)?.filterIsInstance<String>()
                            ?.map { it.trim().lowercase() }
                            ?.distinct()
                            ?.sorted()
                            ?: emptyList()

                        val scores = snap.documents
                            .filter { it.getString("type") == "history" }
                            .mapNotNull { it.getLong("score")?.toInt() }
                            .sorted()

                        val baseline = scores.firstOrNull()
                        val latest = scores.lastOrNull()
                        val change = if (baseline != null && latest != null) latest - baseline else null

                        _ui.value = SkillImpactState(
                            loading = false,
                            goalRole = goalRole,
                            completedSkills = completed,
                            baselineScore = baseline,
                            latestScore = latest,
                            scoreChange = change,
                            error = null
                        )
                    }
                    .addOnFailureListener { e ->
                        _ui.value = SkillImpactState(
                            loading = false,
                            goalRole = goalRole,
                            completedSkills = emptyList(),
                            error = e.message ?: "Failed to read plan data"
                        )
                    }
            }
            .addOnFailureListener { e ->
                _ui.value = SkillImpactState(
                    loading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
    }
}
