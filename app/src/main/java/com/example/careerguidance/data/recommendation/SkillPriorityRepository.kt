package com.example.careerguidance.data.recommendation

import android.content.Context
import com.google.gson.Gson

data class SkillPriorityMap(
    val version: Int = 1,
    val skills: List<SkillPriorityItem> = emptyList()
)

data class SkillPriorityItem(
    val skill: String = "",
    val importance: Double = 0.0
)

object SkillPriorityRepository {

    fun load(context: Context): Map<String, Double> {
        val json = context.assets.open("skill_priority_map.json")
            .bufferedReader()
            .use { it.readText() }

        val parsed = Gson().fromJson(json, SkillPriorityMap::class.java)

        // normalized map: skill -> importance
        return parsed.skills
            .associate { it.skill.trim().lowercase() to it.importance }
    }
}
