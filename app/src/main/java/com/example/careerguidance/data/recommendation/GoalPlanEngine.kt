package com.example.careerguidance.data.recommendation

import kotlin.math.roundToInt

data class SkillGapItem(
    val skill: String,
    val score: Int,
    val isRequired: Boolean,
    val reason: String
)

data class GoalPlanResult(
    val goalTitle: String,
    val matchedSkills: List<String>,
    val missingSkills: List<SkillGapItem>,
    val readinessScore: Int,
    val requiredMatched: Int,
    val requiredTotal: Int,
    val optionalMatched: Int,
    val optionalTotal: Int
)

object GoalPlanEngine {

    fun buildPlan(
        role: RoleDefinition,
        normalizedUserSkills: Set<String>,
        experienceYears: Int,
        modelImportance: Map<String, Double> = emptyMap()
    ): GoalPlanResult {

        val required = role.requiredSkills.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val optional = role.optionalSkills.map { it.trim().lowercase() }.filter { it.isNotBlank() }

        val matchedRequired = required.filter { normalizedUserSkills.contains(it) }
        val matchedOptional = optional.filter { normalizedUserSkills.contains(it) }

        val missingRequired = required.filterNot { normalizedUserSkills.contains(it) }
        val missingOptional = optional.filterNot { normalizedUserSkills.contains(it) }

        val matchedAll = (matchedRequired + matchedOptional).distinct().sorted()

        val maxImp = modelImportance.values.maxOrNull() ?: 0.0
        fun mlBonus(skill: String): Int {
            if (maxImp <= 0.0) return 0
            val imp = modelImportance[skill] ?: 0.0
            val scaled = (imp / maxImp) * 3.0
            return scaled.roundToInt().coerceIn(0, 3)
        }

        val bucket = expBucket(experienceYears)

        fun baseScore(isRequired: Boolean): Int = if (isRequired) 10 else 5
        fun expBonus(skill: String): Int {
            // simple experience logic: early career emphasizes fundamentals
            if (bucket != "0-1") return 0
            return when (skill) {
                "git", "rest api", "sql", "testing" -> 2
                "kotlin", "java", "javascript", "python" -> 1
                else -> 0
            }
        }

        val missingItems = mutableListOf<SkillGapItem>()

        missingRequired.forEach { s ->
            val score = baseScore(true) + 3 + expBonus(s) + mlBonus(s)
            missingItems.add(
                SkillGapItem(
                    skill = s,
                    score = score,
                    isRequired = true,
                    reason = "Required for ${role.title}. Experience bucket $bucket. Model bonus ${mlBonus(s)}."
                )
            )
        }

        missingOptional.forEach { s ->
            val score = baseScore(false) + expBonus(s) + mlBonus(s)
            missingItems.add(
                SkillGapItem(
                    skill = s,
                    score = score,
                    isRequired = false,
                    reason = "Helpful for ${role.title}. Experience bucket $bucket. Model bonus ${mlBonus(s)}."
                )
            )
        }

        val sortedMissing = missingItems.sortedWith(
            compareByDescending<SkillGapItem> { it.score }
                .thenByDescending { it.isRequired }
                .thenBy { it.skill }
        )

        val readiness = computeReadinessScore(
            requiredMatched = matchedRequired.size,
            requiredTotal = required.size,
            optionalMatched = matchedOptional.size,
            optionalTotal = optional.size,
            experienceYears = experienceYears,
            modelImportance = modelImportance,
            matchedSkills = matchedAll
        )

        return GoalPlanResult(
            goalTitle = role.title,
            matchedSkills = matchedAll,
            missingSkills = sortedMissing,
            readinessScore = readiness,
            requiredMatched = matchedRequired.size,
            requiredTotal = required.size,
            optionalMatched = matchedOptional.size,
            optionalTotal = optional.size
        )
    }

    private fun expBucket(years: Int): String {
        return when {
            years <= 1 -> "0-1"
            years <= 3 -> "2-3"
            else -> "4+"
        }
    }

    private fun computeReadinessScore(
        requiredMatched: Int,
        requiredTotal: Int,
        optionalMatched: Int,
        optionalTotal: Int,
        experienceYears: Int,
        modelImportance: Map<String, Double>,
        matchedSkills: List<String>
    ): Int {
        val reqRatio = if (requiredTotal <= 0) 1.0 else requiredMatched.toDouble() / requiredTotal.toDouble()
        val optRatio = if (optionalTotal <= 0) 1.0 else optionalMatched.toDouble() / optionalTotal.toDouble()

        val requiredPart = reqRatio * 70.0
        val optionalPart = optRatio * 20.0

        val expPart = when (expBucket(experienceYears)) {
            "0-1" -> 0.0
            "2-3" -> 2.0
            else -> 4.0
        }

        val maxImp = modelImportance.values.maxOrNull() ?: 0.0
        val avgImp = if (maxImp <= 0.0 || matchedSkills.isEmpty()) 0.0 else {
            matchedSkills.map { s -> modelImportance[s] ?: 0.0 }.average()
        }
        val modelPart = if (maxImp <= 0.0) 0.0 else ((avgImp / maxImp) * 6.0).coerceIn(0.0, 6.0)

        val score = requiredPart + optionalPart + expPart + modelPart
        return score.roundToInt().coerceIn(0, 100)
    }
}
