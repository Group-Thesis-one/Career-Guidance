package com.example.careerguidance.data.recommendation

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoleCatalogDataSource(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    fun loadRoles(): List<Role> {
        val json = context.assets.open("roles.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Role>>() {}.type
        return gson.fromJson(json, type)
    }
}
