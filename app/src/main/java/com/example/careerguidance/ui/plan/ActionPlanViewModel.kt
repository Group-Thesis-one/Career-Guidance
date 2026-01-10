package com.example.careerguidance.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.careerguidance.data.plan.LearningContent
import com.example.careerguidance.data.plan.LearningContentRepository
import com.example.careerguidance.data.recommendation.GoalPlanEngine
import com.example.careerguidance.data.recommendation.RoleDefinition
import com.example.careerguidance.data.recommendation.RolesRepository
import com.example.careerguidance.data.recommendation.SkillGapItem
import com.example.careerguidance.data.recommendation.SkillNormalizer
import com.example.careerguidance.data.recommendation.SkillPriorityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ActionPlanItem(
    val skill: String,
    val isRequired: Boolean,
    val score: Int,
    val why: String,
    val steps: List<String>,
    val done: Boolean
)

data class ActionPlanState(
    val loading: Boolean = true,
    val goalRole: String = "",
    val experienceYears: Int = 0,
    val readinessScore: Int = 0,
    val items: List<ActionPlanItem> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val error: String? = null
)

class ActionPlanViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val normalizer = SkillNormalizer()

    private val _ui = MutableStateFlow(ActionPlanState())
    val ui: StateFlow<ActionPlanState> = _ui

    private val progressDocId = "goalPlanProgress"

    // cache so checkbox changes don't remove items
    private var cachedRole: RoleDefinition? = null
    private var cachedExpYears: Int = 0
    private var cachedBaseSkills: Set<String> = emptySet()
    private var cachedPriorities: Map<String, Double> = emptyMap()
    private var cachedMissingTop10: List<SkillGapItem> = emptyList()
    private var cachedLearningMap: Map<String, LearningContent> = emptyMap()

    fun load() {
        val user = auth.currentUser
        if (user == null) {
            _ui.value = ActionPlanState(loading = false, error = "No logged-in user")
            return
        }

        _ui.value = _ui.value.copy(loading = true, error = null)

        cachedLearningMap = LearningContentRepository.load(getApplication())
        cachedPriorities = try { SkillPriorityRepository.load(getApplication()) } catch (_: Exception) { emptyMap() }
        val roles = RolesRepository.loadRoles(getApplication())

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->

                val goalRole = userDoc.getString("goalRole").orEmpty()
                val exp = (userDoc.getLong("experienceYears") ?: 0L).toInt()
                cachedExpYears = exp

                val skillsNormalizedFromDb =
                    (userDoc.get("skillsNormalized") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val skillsRawFromDb =
                    (userDoc.get("skillsRaw") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val skillsFallback =
                    (userDoc.get("skills") as? List<*>)?.filterIsInstance<String>().orEmpty()

                val best = when {
                    skillsNormalizedFromDb.isNotEmpty() -> skillsNormalizedFromDb
                    skillsRawFromDb.isNotEmpty() -> skillsRawFromDb
                    else -> skillsFallback
                }

                val normalizedSkills = normalizer.normalizeAll(best).toList().sorted()
                cachedBaseSkills = normalizedSkills.toSet()

                if (goalRole.isBlank()) {
                    _ui.value = ActionPlanState(
                        loading = false,
                        goalRole = "",
                        experienceYears = exp,
                        items = emptyList(),
                        completedCount = 0,
                        totalCount = 0,
                        error = "No career goal set. Please set a goal from Home screen."
                    )
                    return@addOnSuccessListener
                }

                val role = roles.firstOrNull { it.title.equals(goalRole, ignoreCase = true) }
                if (role == null) {
                    _ui.value = ActionPlanState(
                        loading = false,
                        goalRole = goalRole,
                        experienceYears = exp,
                        error = "Goal role not found in roles.json: $goalRole"
                    )
                    return@addOnSuccessListener
                }
                cachedRole = role

                val plansRef = db.collection("users").document(user.uid).collection("plans")
                val progressRef = plansRef.document(progressDocId)

                // compute missing list ONCE from base profile skills (stable list)
                val basePlan = GoalPlanEngine.buildPlan(
                    role = role,
                    normalizedUserSkills = cachedBaseSkills,
                    experienceYears = exp,
                    modelImportance = cachedPriorities
                )
                cachedMissingTop10 = basePlan.missingSkills.take(10)

                // load completed skills, then compute score using base+completed
                progressRef.get()
                    .addOnSuccessListener { progDoc ->
                        val completed = (progDoc.get("completedSkills") as? List<*>)?.filterIsInstance<String>()
                            ?.map { it.trim().lowercase() }
                            ?.toSet()
                            ?: emptySet()

                        setUiFromCaches(goalRole = goalRole, completed = completed)
                    }
                    .addOnFailureListener { e ->
                        setUiFromCaches(goalRole = goalRole, completed = emptySet(), error = e.message)
                    }
            }
            .addOnFailureListener { e ->
                _ui.value = ActionPlanState(
                    loading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
    }

    private fun setUiFromCaches(goalRole: String, completed: Set<String>, error: String? = null) {
        val role = cachedRole
        if (role == null) {
            _ui.value = ActionPlanState(loading = false, error = "Role not loaded")
            return
        }

        val effectiveSkills = cachedBaseSkills + completed

        val scorePlan = GoalPlanEngine.buildPlan(
            role = role,
            normalizedUserSkills = effectiveSkills,
            experienceYears = cachedExpYears,
            modelImportance = cachedPriorities
        )

        val items = cachedMissingTop10.map { gap ->
            val key = gap.skill.trim().lowercase()
            val content = cachedLearningMap[key] ?: LearningContentRepository.fallback(gap.skill)

            ActionPlanItem(
                skill = gap.skill,
                isRequired = gap.isRequired,
                score = gap.score,
                why = gap.reason,
                steps = content.steps,
                done = completed.contains(key)
            )
        }

        _ui.value = ActionPlanState(
            loading = false,
            goalRole = goalRole,
            experienceYears = cachedExpYears,
            readinessScore = scorePlan.readinessScore,
            items = items,
            completedCount = items.count { it.done },
            totalCount = items.size,
            error = error
        )
    }

    fun setDone(skill: String, done: Boolean) {
        val user = auth.currentUser ?: return
        val normalized = skill.trim().lowercase()

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("plans")
            .document(progressDocId)

        val update = if (done) {
            mapOf(
                "type" to "progress",
                "completedSkills" to FieldValue.arrayUnion(normalized),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "type" to "progress",
                "completedSkills" to FieldValue.arrayRemove(normalized),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }

        docRef.set(update, SetOptions.merge())
            .addOnSuccessListener {
                val cur = _ui.value

                // update UI state first
                val newItems = cur.items.map {
                    if (it.skill.trim().lowercase() == normalized) it.copy(done = done) else it
                }
                val completedNow = newItems.filter { it.done }.map { it.skill.trim().lowercase() }.toSet()

                // recompute readiness score but keep the same items list (so nothing disappears)
                setUiFromCaches(goalRole = cur.goalRole, completed = completedNow, error = null)
            }
            .addOnFailureListener { e ->
                _ui.value = _ui.value.copy(error = e.message ?: "Failed to save progress")
            }
    }
}
