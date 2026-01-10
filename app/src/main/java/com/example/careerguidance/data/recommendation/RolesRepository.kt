package com.example.careerguidance.data.recommendation

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RolesRepository {

    fun loadRoles(context: Context): List<RoleDefinition> {
        val json = context.assets.open("roles.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<RoleDefinition>>() {}.type
        return Gson().fromJson(json, type)
    }
}
