package com.nishant.northmusic.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nishant.northmusic.data.model.CategoryRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository managing editable category mapping rules (tag -> folder name/category)
 * backed by SharedPreferences with JSON serialization.
 */
object CategoryRulesRepository {

    private const val PREFS_NAME = "north_music_category_rules_prefs"
    private const val KEY_RULES = "category_rules_json"

    val DEFAULT_RULES: List<CategoryRule> = listOf(
        // Hip-Hop
        CategoryRule("hiphop", "Hip-Hop"),
        CategoryRule("hip-hop", "Hip-Hop"),
        CategoryRule("rap", "Hip-Hop"),
        CategoryRule("trap", "Hip-Hop"),
        CategoryRule("drill", "Hip-Hop"),

        // K-Pop
        CategoryRule("k-pop", "K-Pop"),
        CategoryRule("kpop", "K-Pop"),
        CategoryRule("korean", "K-Pop"),

        // Bollywood
        CategoryRule("bollywood", "Bollywood"),
        CategoryRule("hindi", "Bollywood"),
        CategoryRule("indian", "Bollywood"),
        CategoryRule("filmi", "Bollywood"),
        CategoryRule("punjabi", "Bollywood"),

        // English
        CategoryRule("hollywood", "English"),
        CategoryRule("english", "English"),

        // Pop
        CategoryRule("pop", "Pop"),

        // Rock
        CategoryRule("rock", "Rock"),
        CategoryRule("metal", "Rock"),
        CategoryRule("indie", "Rock"),

        // Electronic
        CategoryRule("electronic", "Electronic"),
        CategoryRule("edm", "Electronic"),
        CategoryRule("house", "Electronic"),
        CategoryRule("techno", "Electronic"),

        // Classical
        CategoryRule("classical", "Classical"),
        CategoryRule("instrumental", "Classical"),

        // Jazz
        CategoryRule("jazz", "Jazz"),
        CategoryRule("blues", "Jazz"),

        // Chill
        CategoryRule("lofi", "Chill"),
        CategoryRule("ambient", "Chill"),
        CategoryRule("chill", "Chill")
    )

    val PRESET_CATEGORIES: List<String> = listOf(
        "Hip-Hop",
        "K-Pop",
        "Bollywood",
        "English",
        "Pop",
        "Rock",
        "Electronic",
        "Classical",
        "Jazz",
        "Chill",
        "Other"
    )

    private var prefs: SharedPreferences? = null
    private val _rulesFlow = MutableStateFlow<List<CategoryRule>>(DEFAULT_RULES)
    val rulesFlow: StateFlow<List<CategoryRule>> = _rulesFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val savedJson = sp.getString(KEY_RULES, null)
        if (savedJson != null) {
            val parsed = parseJson(savedJson)
            if (parsed.isNotEmpty()) {
                _rulesFlow.value = parsed
                return
            }
        }
        _rulesFlow.value = DEFAULT_RULES
        saveRules(DEFAULT_RULES)
    }

    fun getRules(): List<CategoryRule> = _rulesFlow.value

    fun addRule(tag: String, category: String): Boolean {
        val cleanTag = tag.trim().lowercase()
        val cleanCategory = category.trim()
        if (cleanTag.isBlank() || cleanCategory.isBlank()) return false

        val current = _rulesFlow.value.toMutableList()
        // If tag already exists, update its category
        val existingIndex = current.indexOfFirst { it.tag.equals(cleanTag, ignoreCase = true) }
        if (existingIndex != -1) {
            current[existingIndex] = CategoryRule(cleanTag, cleanCategory)
        } else {
            current.add(CategoryRule(cleanTag, cleanCategory))
        }

        saveRules(current)
        return true
    }

    fun updateRule(oldTag: String, newTag: String, newCategory: String): Boolean {
        val cleanOld = oldTag.trim().lowercase()
        val cleanNewTag = newTag.trim().lowercase()
        val cleanCategory = newCategory.trim()
        if (cleanNewTag.isBlank() || cleanCategory.isBlank()) return false

        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.tag.equals(cleanOld, ignoreCase = true) }
        if (index != -1) {
            current[index] = CategoryRule(cleanNewTag, cleanCategory)
            saveRules(current)
            return true
        }
        return false
    }

    fun deleteRule(tag: String) {
        val current = _rulesFlow.value.filterNot { it.tag.equals(tag.trim(), ignoreCase = true) }
        saveRules(current)
    }

    fun resetToDefaults() {
        saveRules(DEFAULT_RULES)
    }

    /**
     * Resolves category from a list of genre/tags.
     * Matching is case-insensitive and checks every tag token; first match wins.
     */
    fun resolveCategory(tags: List<String>): String? {
        val currentRules = _rulesFlow.value
        if (tags.isEmpty() || currentRules.isEmpty()) return null

        // Tokenize tags (e.g. "Pop, Hip-Hop / Rap" -> ["pop", "hip-hop", "rap"])
        val tokens = tags.flatMap { tagStr ->
            tagStr.split(',', '/', ';', '|', '&')
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
        }

        for (rule in currentRules) {
            val ruleTag = rule.tag.trim().lowercase()
            for (token in tokens) {
                if (token == ruleTag || token.contains(ruleTag)) {
                    return rule.category
                }
            }
        }
        return null
    }

    private fun saveRules(rules: List<CategoryRule>) {
        _rulesFlow.value = rules
        val sp = prefs ?: return
        try {
            val jsonArray = JSONArray()
            rules.forEach {
                val obj = JSONObject()
                obj.put("tag", it.tag)
                obj.put("category", it.category)
                jsonArray.put(obj)
            }
            sp.edit().putString(KEY_RULES, jsonArray.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun parseJson(jsonStr: String): List<CategoryRule> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CategoryRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(CategoryRule(obj.getString("tag"), obj.getString("category")))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
