package com.twilight.calleditor

import android.content.Context

data class AppPreferences(
    val theme: String = "system",
    val dynamicColor: Boolean = true,
    val compactList: Boolean = false
)

class PreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun load() = AppPreferences(
        theme = prefs.getString("theme", "system").let { if (it in listOf("light", "dark")) it!! else "system" },
        dynamicColor = prefs.getBoolean("dynamicColor", true),
        compactList = prefs.getBoolean("compactList", false)
    )

    fun save(value: AppPreferences) {
        check(prefs.edit().putString("theme", value.theme)
            .putBoolean("dynamicColor", value.dynamicColor)
            .putBoolean("compactList", value.compactList).commit()) { "设置保存失败，请重试" }
    }
}
