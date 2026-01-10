package com.example.careerguidance.ui.recommendation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.careerguidance.data.recommendation.GoalPlanEngine
import com.example.careerguidance.data.recommendation.GoalPlanResult
import com.example.careerguidance.data.recommendation.RolesRepository
import com.example.careerguidance.data.recommendation.SkillNormalizer
import com.example.careerguidance.data.recommendation.SkillPriorityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GoalPlanState(
    val loading: Boolean = true,
    val goalRole: String = "",
    val experienceYears: Int = 0,
    val skills: List<String> = emptyList(),
    val plan: GoalPlanResult? = null,
    val scoreDelta: Int? = null,
    val error: String? = null
)

class GoalPlanViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val normalizer = SkillNormalizer()

    private val _ui = MutableStateFlow(GoalPlanState())
    val ui: StateFlow<GoalPlanState> = _ui

    private val progressDocId = "goalPlanProgress"

    fun load() {
        val user = auth.currentUser
        if (user == null) {
            _ui.value = GoalPlanState(loading = false, error = "No logged-in user")
            return
        }

        _ui.value = _ui.value.copy(loading = true, error = null)

        val roles = RolesRepository.loadRoles(getApplication())
        val priorities = try { SkillPriorityRepository.load(getApplication()) } catch (_: Exception) { emptyMap() }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->

                val goalRole = doc.getString("goalRole").orEmpty()
                val exp = (doc.getLong("experienceYears") ?: 0L).toInt()

                val skillsNormalizedFromDb =
                    (doc.get("skillsNormalized") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val skillsRawFromDb =
                    (doc.get("skillsRaw") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val fallbackSkillsFromDb =
                    (doc.get("skills") as? List<*>)?.filterIsInstance<String>().orEmpty()

                val bestSkillsSource = when {
                    skillsNormalizedFromDb.isNotEmpty() -> skillsNormalizedFromDb
                    skillsRawFromDb.isNotEmpty() -> skillsRawFromDb
                    else -> fallbackSkillsFromDb
                }

                val normalizedSkills = normalizer.normalizeAll(bestSkillsSource).toList().sorted()
                val skillSet = normalizedSkills.toSet()

                if (goalRole.isBlank()) {
                    _ui.value = GoalPlanState(
                        loading = false,
                        goalRole = "",
                        experienceYears = exp,
                        skills = normalizedSkills,
                        plan = null,
                        scoreDelta = null,
                        error = "No career goal set. Please set a goal from Home screen."
                    )
                    return@addOnSuccessListener
                }

                val role = roles.firstOrNull { it.title.equals(goalRole, ignoreCase = true) }
                if (role == null) {
                    _ui.value = GoalPlanState(
                        loading = false,
                        goalRole = goalRole,
                        experienceYears = exp,
                        skills = normalizedSkills,
                        plan = null,
                        scoreDelta = null,
                        error = "Goal role not found in roles.json: $goalRole"
                    )
                    return@addOnSuccessListener
                }

                // read last history score (client-side filter, no extra indexes)
                val plansRef = db.collection("users").document(user.uid).collection("plans")
                plansRef.get()
                    .addOnSuccessListener { snap ->
                        val previousScore = snap.documents
                            .filter { it.id != progressDocId }
                            .filter { it.getString("type") == "history" }
                            .mapNotNull { it.getLong("score")?.toInt() }
                            .maxOrNull()

                        val plan = GoalPlanEngine.buildPlan(
                            role = role,
                            normalizedUserSkills = skillSet,
                            experienceYears = exp,
                            modelImportance = priorities
                        )

                        val delta = previousScore?.let { plan.readinessScore - it }

                        // log a history snapshot
                        val historyData = mapOf(
                            "type" to "history",
                            "source" to "recommendations",
                            "goalRole" to goalRole,
                            "goalTitle" to plan.goalTitle,
                            "score" to plan.readinessScore,
                            "requiredMatched" to plan.requiredMatched,
                            "requiredTotal" to plan.requiredTotal,
                            "optionalMatched" to plan.optionalMatched,
                            "optionalTotal" to plan.optionalTotal,
                            "missingTop" to plan.missingSkills.take(10).map { it.skill },
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        plansRef.add(historyData)

                        _ui.value = GoalPlanState(
                            loading = false,
                            goalRole = goalRole,
                            experienceYears = exp,
                            skills = normalizedSkills,
                            plan = plan,
                            scoreDelta = delta,
                            error = null
                        )
                    }
                    .addOnFailureListener {
                        // still show plan even if history read fails
                        val plan = GoalPlanEngine.buildPlan(
                            role = role,
                            normalizedUserSkills = skillSet,
                            experienceYears = exp,
                            modelImportance = priorities
                        )

                        _ui.value = GoalPlanState(
                            loading = false,
                            goalRole = goalRole,
                            experienceYears = exp,
                            skills = normalizedSkills,
                            plan = plan,
                            scoreDelta = null,
                            error = null
                        )
                    }
            }
            .addOnFailureListener { e ->
                _ui.value = GoalPlanState(
                    loading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
    }
}
