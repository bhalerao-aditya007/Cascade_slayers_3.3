package com.cascade.attentiontwin.engine

import android.content.res.AssetManager
import org.json.JSONObject

class Logit(val coef: DoubleArray, val intercept: Double, val mean: DoubleArray, val std: DoubleArray) {
    fun z(x: DoubleArray): Double {
        var z = intercept
        for (i in x.indices) {
            val v = if (i < mean.size) (x[i] - mean[i]) / std[i] else x[i]
            z += coef[i] * v
        }
        return z
    }

    fun p(x: DoubleArray, offset: Double = 0.0): Double {
        return 1.0 / (1.0 + Math.exp(-(z(x) + offset)))
    }
}

val GROUPS = listOf("messaging", "social", "video", "shopping", "finance", "email", "news", "other")

data class Ctx(
    val nowMs: Long,
    val hour: Int,
    val weekend: Boolean,
    val notifs30: Int,
    val unlocks30: Int,
    val minsSinceUnlock: Double,
    val screenOn: Boolean,
    val state: PhoneState,
    val fgPkg: String?,
    val fgGroup: String?,
    val usageKnown: Boolean,
    val waitingForCode: Boolean
)

enum class PhoneState { LOCKED, IN_SOURCE_APP, IN_TASK_APP, IN_OTHER_APP }

fun featureVector(c: Ctx, impHigh: Boolean, group: String?, control: Boolean): DoubleArray {
    val x = DoubleArray(16)
    x[0] = Math.sin(2 * Math.PI * c.hour / 24.0)
    x[1] = Math.cos(2 * Math.PI * c.hour / 24.0)
    x[2] = if (c.weekend) 1.0 else 0.0
    x[3] = Math.log(1.0 + c.notifs30)
    x[4] = Math.log(1.0 + c.unlocks30)
    x[5] = Math.log(1.0 + minOf(c.minsSinceUnlock, 600.0))
    x[6] = if (c.screenOn) 1.0 else 0.0
    if (!control) {
        x[7] = if (impHigh) 1.0 else 0.0
        val gi = GROUPS.indexOf(group ?: "other").let { if (it < 0) 7 else it }
        x[8 + gi] = 1.0
    }
    return x
}

class ModelStore(val show: Logit, val ctrl: Logit, val isPlaceholder: Boolean) {
    companion object {
        fun createForTest(show: Logit, ctrl: Logit, isPlaceholder: Boolean = true) = ModelStore(show, ctrl, isPlaceholder)

        fun load(assets: AssetManager): ModelStore {
            val showModel = loadModel(assets, "models/engage_show.json")
            val ctrlModel = loadModel(assets, "models/engage_ctrl.json")
            val showJson = loadJson(assets, "models/engage_show.json")
            val isPlaceholder = showJson.optString("source", "placeholder") == "placeholder"
            return ModelStore(showModel, ctrlModel, isPlaceholder)
        }

        private fun loadJson(assets: AssetManager, path: String): JSONObject {
            val text = assets.open(path).bufferedReader().readText()
            return JSONObject(text)
        }

        private fun loadModel(assets: AssetManager, path: String): Logit {
            val json = loadJson(assets, path)
            val coefArr = json.getJSONArray("coef")
            val coef = DoubleArray(coefArr.length()) { coefArr.getDouble(it) }
            val intercept = json.getDouble("intercept")
            val std = json.optJSONObject("standardize")
            val meanArr = std?.optJSONArray("mean")
            val stdArr = std?.optJSONArray("std")
            val mean = if (meanArr != null) DoubleArray(meanArr.length()) { meanArr.getDouble(it) } else DoubleArray(0)
            val stdDev = if (stdArr != null) DoubleArray(stdArr.length()) { stdArr.getDouble(it) } else DoubleArray(0)
            return Logit(coef, intercept, mean, stdDev)
        }
    }
}
