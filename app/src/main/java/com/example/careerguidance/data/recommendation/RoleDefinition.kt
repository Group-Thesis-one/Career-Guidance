package com.example.careerguidance.data.recommendation

data class RoleDefinition(
    val id: String = "",
    val title: String = "",
    val requiredSkills: List<String> = emptyList(),
    val optionalSkills: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    val level: String? = null,
    val minExperienceYears: Int? = null,
    val targetExperienceYears: Int? = null,

    val requiredSkillWeights: Map<String, Int> = emptyMap(),
    val optionalSkillWeights: Map<String, Int> = emptyMap(),

    val skillFocusByExperience: Map<String, List<String>> = emptyMap()
)
