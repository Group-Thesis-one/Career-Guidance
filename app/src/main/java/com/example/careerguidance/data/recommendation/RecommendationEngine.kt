package com.example.careerguidance.data.recommendation

object RecommendationEngine {

    fun recommendTopRoles(
        applicantSkillsNormalized: Set<String>,
        applicantInterestsNormalized: Set<String>,
        roles: List<Role>,
        topK: Int = 10
    ): List<Recommendation> {

        return roles.map { role ->
            val req = role.requiredSkills.map { it.trim().lowercase() }
            val opt = role.optionalSkills.map { it.trim().lowercase() }

            val matchedReq = req.filter { it in applicantSkillsNormalized }
            val matchedOpt = opt.filter { it in applicantSkillsNormalized }

            val matchRequired = if (req.isNotEmpty()) matchedReq.size.toDouble() / req.size else 0.0
            val matchOptional = if (opt.isNotEmpty()) matchedOpt.size.toDouble() / opt.size else 0.0

            var score = 0.75 * matchRequired + 0.25 * matchOptional

            val roleTags = role.tags.map { it.trim().lowercase() }.toSet()
            val overlap = roleTags.intersect(applicantInterestsNormalized).size
            if (overlap > 0) score += 0.05 * overlap

            val missingReq = req.filter { it !in applicantSkillsNormalized }
            val missingOpt = opt.filter { it !in applicantSkillsNormalized }

            Recommendation(
                roleId = role.id,
                title = role.title,
                score = score.coerceIn(0.0, 1.0),
                matchedSkills = (matchedReq + matchedOpt).distinct(),
                missingRequired = missingReq,
                missingOptional = missingOpt
            )
        }
            .sortedByDescending { it.score }
            .take(topK)
    }
}
