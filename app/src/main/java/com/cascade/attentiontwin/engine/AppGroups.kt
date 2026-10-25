package com.cascade.attentiontwin.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Telephony
import org.json.JSONObject

class AppGroups(context: Context) {
    private val ctx = context.applicationContext
    private val ownPkg = ctx.packageName

    // Curated map loaded from assets/app_groups.json
    private val curated: Map<String, String> = try {
        val json = ctx.assets.open("app_groups.json").bufferedReader().readText()
        val obj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key -> map[key] = obj.getString(key) }
        map
    } catch (_: Exception) { emptyMap() }

    // Own-package channel mapping
    private val ownChannelMap = mapOf(
        "messages" to "messaging", "promotions" to "shopping", "likes" to "social",
        "news" to "news", "orders" to "shopping", "security" to "other"
    )

    fun groupOf(pkg: String?, channelId: String?): String {
        if (pkg == null) return "other"
        // 1. Own package: map by channel id
        if (pkg == ownPkg && channelId != null) {
            return ownChannelMap[channelId] ?: "other"
        }
        // 2. Curated map
        curated[pkg]?.let { return it }
        // 3. Fallback using ApplicationInfo.category
        return try {
            val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
            when (ai.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> "social"
                ApplicationInfo.CATEGORY_VIDEO -> "video"
                ApplicationInfo.CATEGORY_NEWS -> "news"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity"
                else -> "other"
            }
        } catch (_: Exception) { "other" }
    }

    fun isDefaultSms(pkg: String): Boolean = try {
        Telephony.Sms.getDefaultSmsPackage(ctx) == pkg
    } catch (_: Exception) { false }

    fun isAuthenticator(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("authenticator") || lower.contains("authy") || lower.contains("otp")
    }
}
