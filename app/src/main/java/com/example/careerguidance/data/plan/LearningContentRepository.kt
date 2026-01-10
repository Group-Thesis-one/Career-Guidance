package com.example.careerguidance.data.plan

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LearningContentRepository {

    fun load(context: Context): Map<String, LearningContent> {
        val json = context.assets.open("learning_content.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<LearningContent>>() {}.type
        val list: List<LearningContent> = Gson().fromJson(json, type)

        return list.associateBy { it.skill.trim().lowercase() }
    }

    fun fallback(skill: String): LearningContent {
        val s = skill.trim().lowercase()
        return LearningContent(
            skill = s,
            why = "Recommended for improving your goal readiness.",
            steps = listOf(
                "Search a beginner tutorial for $s and complete it.",
                "Build a small demo project using $s.",
                "Apply $s inside your existing Career Guidance project."
            )
        )
    }
}
