package com.example.careerguidance.data.recommendation

class SkillNormalizer {

    private val aliases: Map<String, String> = mapOf(
        // Android / Kotlin
        "compose" to "jetpack compose",
        "jetpackcompose" to "jetpack compose",
        "jetpack-compose" to "jetpack compose",
        "android compose" to "jetpack compose",

        "kotlin programming" to "kotlin",
        "android kotlin" to "kotlin",

        // Architecture
        "mvvm architecture" to "mvvm",
        "mvvm pattern" to "mvvm",

        // APIs
        "rest" to "rest api",
        "restful api" to "rest api",
        "api" to "rest api",
        "api integration" to "rest api",

        // Firebase
        "firebase authentication" to "firebase auth",
        "firebase auth" to "firebase auth",
        "firestore" to "firebase firestore",
        "firebase firestore" to "firebase firestore",
        "firebase database" to "firebase firestore",

        // Database
        "postgres" to "postgresql",
        "postgre" to "postgresql",
        "postgre sql" to "postgresql",
        "sql database" to "sql",

        // Tools
        "github" to "git",
        "git hub" to "git",
        "version control" to "git",

        // Testing
        "unit testing" to "testing",
        "ui testing" to "testing"
    )

    fun normalize(skill: String): String {
        val cleaned = skill
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 +#.-]"), "")

        return aliases[cleaned] ?: cleaned
    }

    fun normalizeAll(skills: List<String>): Set<String> {
        return skills
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .toSet()
    }
}
