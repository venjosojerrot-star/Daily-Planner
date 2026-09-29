package com.example.util

import android.content.Context
import android.content.SharedPreferences

object CategoryGoalsManager {
    private const val PREFS_NAME = "category_daily_goals"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getDailyGoalHours(context: Context, category: String): Double {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return 0.0
        val prefs = getPrefs(context)
        return prefs.getFloat(trimmed, 0f).toDouble()
    }

    fun setDailyGoalHours(context: Context, category: String, hours: Double) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return
        val prefs = getPrefs(context)
        if (hours <= 0.0) {
            prefs.edit().remove(trimmed).apply()
        } else {
            val rounded = (Math.round(hours * 100.0) / 100.0).toFloat()
            prefs.edit().putFloat(trimmed, rounded).apply()
        }
    }

    fun getAllDailyGoals(context: Context): Map<String, Double> {
        val prefs = getPrefs(context)
        val result = mutableMapOf<String, Double>()
        for ((key, value) in prefs.all) {
            val dVal = (value as? Number)?.toDouble() ?: 0.0
            if (dVal > 0.0) {
                result[key] = dVal
            }
        }
        return result
    }

    fun removeDailyGoal(context: Context, category: String) {
        getPrefs(context).edit().remove(category.trim()).apply()
    }
}
