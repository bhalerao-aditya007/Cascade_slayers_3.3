package com.cascade.attentiontwin.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Strictness(val tau: Double, val delta: Double) {
    GENTLE(0.25, 0.50), BALANCED(0.35, 0.25), STRICT(0.50, 0.10)
}

enum class Mode { SHADOW, SNOOZE, DND_ONLY }

data class Settings(
    val strictness: Strictness = Strictness.BALANCED,
    val maxHoldMin: Int = 15,
    val digestMin: Int = 15,
    val mode: Mode = Mode.SNOOZE,
    val focusActive: Boolean = false,
    val silenceWhileFocused: Boolean = true,
    val experiments: Boolean = false,
    val epsilon: Double = 0.10,
    val allowHoldChats: Boolean = true,
    val neverHoldPkgs: Set<String> = emptySet(),
    val blockedPkgs: Set<String> = emptySet(),
    val learningPaused: Boolean = false,
    val includeDemo: Boolean = false,
    val simForceLocked: Boolean = false,
    val onboarded: Boolean = false,
    val demoMode: Boolean = false,
    val shieldEnabled: Boolean = false,
    val shieldPkgs: Set<String> = emptySet()
)

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("twin_prefs", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<Settings> = _flow.asStateFlow()
    val current get() = _flow.value

    private fun load(): Settings = Settings(
        strictness = try { Strictness.valueOf(sp.getString("strictness", "BALANCED")!!) } catch (_: Exception) { Strictness.BALANCED },
        maxHoldMin = sp.getInt("maxHoldMin", 15),
        digestMin = sp.getInt("digestMin", 15),
        mode = try { Mode.valueOf(sp.getString("mode", "SNOOZE")!!) } catch (_: Exception) { Mode.SNOOZE },
        focusActive = sp.getBoolean("focusActive", false),
        silenceWhileFocused = sp.getBoolean("silenceWhileFocused", true),
        experiments = sp.getBoolean("experiments", false),
        epsilon = sp.getFloat("epsilon", 0.10f).toDouble(),
        allowHoldChats = sp.getBoolean("allowHoldChats", true),
        neverHoldPkgs = sp.getStringSet("neverHoldPkgs", emptySet())?.toSet() ?: emptySet(),
        blockedPkgs = sp.getStringSet("blockedPkgs", emptySet())?.toSet() ?: emptySet(),
        learningPaused = sp.getBoolean("learningPaused", false),
        includeDemo = sp.getBoolean("includeDemo", false),
        simForceLocked = sp.getBoolean("simForceLocked", false),
        onboarded = sp.getBoolean("onboarded", false),
        demoMode = sp.getBoolean("demoMode", false),
        shieldEnabled = sp.getBoolean("shieldEnabled", false),
        shieldPkgs = sp.getStringSet("shieldPkgs", emptySet())?.toSet() ?: emptySet()
    )

    fun update(fn: Settings.() -> Settings) {
        val s = current.fn()
        sp.edit().apply {
            putString("strictness", s.strictness.name)
            putInt("maxHoldMin", s.maxHoldMin)
            putInt("digestMin", s.digestMin)
            putString("mode", s.mode.name)
            putBoolean("focusActive", s.focusActive)
            putBoolean("silenceWhileFocused", s.silenceWhileFocused)
            putBoolean("experiments", s.experiments)
            putFloat("epsilon", s.epsilon.toFloat())
            putBoolean("allowHoldChats", s.allowHoldChats)
            putStringSet("neverHoldPkgs", s.neverHoldPkgs)
            putStringSet("blockedPkgs", s.blockedPkgs)
            putBoolean("learningPaused", s.learningPaused)
            putBoolean("includeDemo", s.includeDemo)
            putBoolean("simForceLocked", s.simForceLocked)
            putBoolean("onboarded", s.onboarded)
            putBoolean("demoMode", s.demoMode)
            putBoolean("shieldEnabled", s.shieldEnabled)
            putStringSet("shieldPkgs", s.shieldPkgs)
            apply()
        }
        _flow.value = s
    }

    fun reset() {
        sp.edit().clear().apply()
        _flow.value = Settings()
    }
}
