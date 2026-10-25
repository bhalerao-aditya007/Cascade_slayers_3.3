# Attention Twin: Complete Build Plan for the Coding Agent
CASCADE '26, Theme 3, Problem 3. Final deliverable: an installable release APK.

---

## A. Read this first (rules of engagement)

You are building a native Android app from scratch. Work autonomously. Do not ask the human questions. If something here is ambiguous, pick the simplest option that keeps the Hard Rules and note the choice in `DECISIONS.md`.

**Decisions already made (do not revisit):**
1. **Native Kotlin + Jetpack Compose (Material 3). No Flutter, no MethodChannel.** One process, one language. The notification listener and the UI share one in-process singleton (`Graph`).
2. **No training data is required.** The app ships with *hand-set prior models* (clearly labelled "PRIOR, not trained") and learns on the device. The decision engine loads model JSON from assets, so a trained model can be dropped in later without code changes.
3. **The control (counterfactual) model is learned on the device** from the user's own "locked phone, no notification" moments. No external dataset is needed for it.
4. minSdk 26, targetSdk 34, compileSdk 34, Java/Kotlin 17.
5. Room for storage, `SharedPreferences` for settings, custom Compose `Canvas` for all charts (no chart library).

**Hard rules (a violation fails the build):**
1. Never read notification text. The constants `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_MESSAGES`, `EXTRA_SUB_TEXT`, `EXTRA_INFO_TEXT`, `EXTRA_SUMMARY_TEXT`, `EXTRA_TEXT_LINES`, `tickerText` must not appear in `src/main`. A Gradle task (step 2.5) enforces this.
2. The release APK has no `INTERNET` permission. Enforced with `tools:node="remove"` plus a verification command (step 14).
3. Unknown means SHOW. Any exception, missing data, disconnect, or low confidence leaves the notification alone.
4. Protected classes (Gate 1) are never held. This is a rule, never a model output.
5. Every decision writes a receipt.
6. Simulated rows carry `source='sim'`, are badged in the UI, and are excluded from Evidence unless the user toggles "include demo data".
7. Never claim: "blocked before arrival", "knows what is important", "measures focus or fatigue". These are proxies.

**Working method:** after each step run `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest`. Fix errors before moving on. Code in this document is reference code: it was not compiled. Fix API names and signatures the compiler rejects, but keep the logic. Commit after each step.

**Reference projects (skim for patterns only; check their licenses; do not paste code):**
- `github.com/eret9616/HideOngoing`: Kotlin + Compose app that uses `snoozeNotification`. Look at how it calls snooze and handles the returning notification.
- `github.com/PinetheApple/notif-filter`: listener resilience, battery-optimization exemption flow, history store.
- Android docs: `NotificationListenerService`, `UsageStatsManager`, `NotificationManager.setInterruptionFilter`.

---

## B. What the app does (one paragraph for your own orientation)

During a Focus Session, for every posted notification, the app decides SHOW or HOLD using only metadata. HOLD means `snoozeNotification(key, ms)` until the next 15-minute digest slot (max 30 min), so the original notification returns intact. The decision uses (1) hard protection rules, (2) a per-channel Beta importance estimate with an optimistic bound, and (3) an expected-cost comparison: what showing it costs in phone minutes versus what holding it costs. Each decision is a receipt with a one-line reason. A "twin" What-if screen shows the predicted chain (interruption, browsing, resumption delay) and the counterfactual (held world vs shown world). Opt-in randomized holds with logged propensities give a real causal estimate. All learning is on-device.

---

## C. Step-by-step plan

### Step 0. Environment
1. JDK 17, Android SDK (platform 34, build-tools 34.0.0), `adb` on PATH. If Android Studio is not installed, install command-line tools and accept licenses (`sdkmanager --licenses`).
2. Create the project folder `AttentionTwin/` with the files below. Generate the Gradle wrapper: `gradle wrapper --gradle-version 8.7`.
3. If a dependency version fails to resolve, bump to the nearest stable version that is compatible (Kotlin, KSP and the Compose compiler version must match each other).

### Step 1. Gradle files

`settings.gradle.kts`
```kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "AttentionTwin"
include(":app")
```
`build.gradle.kts` (root)
```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
```
`app/build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.cascade.attentiontwin"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.cascade.attentiontwin"
        minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")   // hackathon: debug-signed is fine
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")        // org.json is stubbed in JVM tests
}
```

### Step 2. Manifest, resources, guard tasks

**2.1 `app/src/main/AndroidManifest.xml`**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
    <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <!-- Safety: strip INTERNET even if a library tries to merge it in -->
    <uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent>
    </queries>
    <application
        android:name=".TwinApp"
        android:label="Attention Twin"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:theme="@style/Theme.AttentionTwin">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <service android:name=".AttentionListener" android:exported="true"
                 android:label="Attention Twin"
                 android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService"/>
            </intent-filter>
        </service>
    </application>
</manifest>
```
**2.2** `res/xml/data_extraction_rules.xml`: exclude all domains from `cloud-backup` and `device-transfer`.
**2.3** `res/values/themes.xml`: `Theme.AttentionTwin` parent `android:Theme.Material.NoActionBar`, window background `#0B0F1A`, status/navigation bar colors the same.
**2.4** Launcher icon: any adaptive icon (cyan circle on `#0B0F1A`). Do not spend time here.
**2.5** In `app/build.gradle.kts` add this task and hook it into `preBuild`:
```kotlin
tasks.register("checkNoContentReads") {
    doLast {
        val banned = listOf("EXTRA_TITLE","EXTRA_TEXT","EXTRA_BIG_TEXT","EXTRA_MESSAGES","EXTRA_SUB_TEXT",
            "EXTRA_INFO_TEXT","EXTRA_SUMMARY_TEXT","EXTRA_TEXT_LINES","tickerText")
        val hits = fileTree("src/main").matching { include("**/*.kt","**/*.java") }.files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, l -> if (banned.any { l.contains(it) }) "${f.name}:${i+1}: $l" else null }
        }
        if (hits.isNotEmpty()) throw GradleException("Notification content access found:\n" + hits.joinToString("\n"))
    }
}
tasks.named("preBuild") { dependsOn("checkNoContentReads") }
```

### Step 3. Data model (Room)

Package `com.cascade.attentiontwin.data`. No title, text, sender or contact column may exist.

```kotlin
@Entity(tableName = "events", indices = [Index("post_ts"), Index("key")])
data class EventRow(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String, val pkg: String, @ColumnInfo(name="channel_id") val channelId: String?,
    @ColumnInfo(name="channel_name") val channelName: String?, @ColumnInfo(name="app_group") val appGroup: String,
    val category: String?, val importance: Int, @ColumnInfo(name="is_conversation") val isConversation: Boolean,
    val ongoing: Boolean, @ColumnInfo(name="post_ts") val postTs: Long, val source: String = "real")

@Entity(tableName = "decisions")
data class DecisionRow(@PrimaryKey @ColumnInfo(name="event_id") val eventId: Long,
    val action: String,                // SHOW | HOLD
    val gate: String, val reason: String, val mode: String,   // SHADOW | SNOOZE | DND_ONLY
    @ColumnInfo(name="phone_state") val phoneState: String,
    val pImpMean: Double, val pImpUp: Double, val pShow: Double, val pCtrl: Double,
    val costShow: Double, val costHold: Double, val benefit: Double,
    val randomized: Boolean, val propensityHold: Double, val policyVersion: String,
    val enforcement: String,           // applied | failed | none
    @ColumnInfo(name="release_at") val releaseAt: Long?, @ColumnInfo(name="released_ts") val releasedTs: Long?,
    @ColumnInfo(name="removal_reason") val removalReason: Int?, @ColumnInfo(name="removed_ts") val removedTs: Long?)

@Entity(tableName = "outcomes")
data class OutcomeRow(@PrimaryKey @ColumnInfo(name="event_id") val eventId: Long,
    val windowEnd: Long, val engaged: Boolean, val clicked: Boolean, val dismissed: Boolean,
    val unlocks10m: Int, val phoneMin10m: Double, val clickedWithin2m: Boolean,
    val digestFeedback: String? = null, val censored: Boolean = false)

@Entity(tableName = "channel_stats", primaryKeys = ["pkg", "channelId"])
data class ChannelStat(val pkg: String, val channelId: String, val group: String,
    var a: Double, var b: Double, var nSeen: Int = 0, var nEngaged: Int = 0,
    var mu: Double, var varPull: Double, var nPull: Int = 0,
    var bShow: Double = 0.0, var pSwitch: Double,
    var prot: Boolean = false, var never: Boolean = false, var updatedTs: Long = 0)

@Entity(tableName = "screen_intervals") data class ScreenInterval(@PrimaryKey(autoGenerate = true) val id: Long = 0, val startTs: Long, var endTs: Long?)
@Entity(tableName = "unlock_events", indices = [Index("ts")]) data class UnlockEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0, val ts: Long)
@Entity(tableName = "ctrl_probes") data class CtrlProbe(@PrimaryKey val ts: Long, val block: Int, val y: Boolean, val phoneMin: Double)
```
Write one `@Dao` with at least: insert/update for each table; `eventsSince(ts)`; `receipts(limit, offset, filter)` (join events+decisions+outcomes into a `ReceiptView` data class); `heldNow()` (HOLD, released_ts null, release_at > now - 60s); `unreleasedHolds()`; `markReleased(key, ts)`; `setRemoval(eventId, reason, ts)`; `unlocksBetween(t0, t1)`; `intervalsOverlapping(t0, t1)`; `pendingWindows(now)` (decisions whose outcome row is missing and `post_ts + 10 min < now`); `ctrlCounts()` (per block n and k); `muSamples()` (avg phone_min where y=1); aggregate queries for Evidence and Insights; `deleteAll()` (use `clearAllTables()` from the database class). `AppDb` is a `RoomDatabase` singleton, `fallbackToDestructiveMigration()`.

### Step 4. Settings

`Settings` data class + `Prefs` wrapper over `SharedPreferences`, exposed as `StateFlow<Settings>`. Fields and defaults: `strictness=BALANCED`, `maxHoldMin=15`, `digestMin=15`, `mode=SNOOZE`, `focusActive=false`, `silenceWhileFocused=false`, `experiments=false`, `epsilon=0.10`, `allowHoldChats=false`, `neverHoldPkgs=emptySet()`, `learningPaused=false`, `includeDemo=false`, `simForceLocked=false`, `onboarded=false`.

```kotlin
enum class Strictness(val tau: Double, val delta: Double) {
    GENTLE(0.25, 0.50), BALANCED(0.35, 0.25), STRICT(0.50, 0.10)
}
```
(delta is in expected phone minutes. It is lower than the original design because real per-notification costs are fractions of a minute; the safety comes from `tau`, not `delta`.)

### Step 5. App groups

`AppGroups.groupOf(pkg: String, channelId: String?): String` returns one of `messaging, social, video, shopping, finance, email, news, productivity, other`.
Order of resolution:
1. If `pkg == ownPackage`: map by channel id: `messages`→messaging, `promotions`→shopping, `likes`→social, `news`→news, `orders`→shopping, `security`→other.
2. Curated map (load `assets/app_groups.json`: `{"com.whatsapp":"messaging", ...}`). Include at least: WhatsApp (+w4b), Telegram, Google Messages, Messenger, Snapchat; Instagram, Facebook, Twitter/X, LinkedIn, Reddit, Pinterest; YouTube, Netflix, Hotstar, Prime Video; Amazon India, Flipkart, Myntra, Swiggy, Zomato, Uber, Ola; PhonePe, Google Pay, Paytm; Gmail, Outlook.
3. Fallback `ApplicationInfo.category`: SOCIAL→social, VIDEO→video, NEWS→news, PRODUCTIVITY→productivity, else `other`.
Also `isDefaultSms(pkg)` via `Telephony.Sms.getDefaultSmsPackage(ctx)` and `isAuthenticator(pkg)` = package name contains `authenticator`, `authy`, or `otp`.

### Step 6. Prior models (JSON in `assets/models/`)

16-feature vector, fixed order: `hour_sin, hour_cos, is_weekend, log_notifs_30m, log_unlocks_30m, log_mins_since_unlock, screen_on, importance_high, grp_messaging, grp_social, grp_video, grp_shopping, grp_finance, grp_email, grp_news, grp_other`.

`engage_show.json`
```json
{"name":"engage_show","version":"prior-1","source":"placeholder",
 "standardize":{"mean":[0,0,0,0,0,0,0],"std":[1,1,1,1,1,1,1]},
 "coef":[0,0,0,0.15,0.45,-0.25,0.6,0.3, 0.8,0.2,0.0,-0.2,0.3,0.1,-0.2,-0.3],
 "intercept":-0.9,"metrics":{}}
```
`engage_ctrl.json`
```json
{"name":"engage_ctrl","version":"prior-1","source":"placeholder",
 "standardize":{"mean":[0,0,0,0,0,0,0],"std":[1,1,1,1,1,1,1]},
 "coef":[0,0,0,0.05,0.5,-0.3,0.5,0, 0,0,0,0,0,0,0,0],
 "intercept":-1.2,"metrics":{}}
```
These are hand-set assumptions (more unlocks recently, screen on, messaging raise engagement; long time since unlock lowers it). `ModelStore.load(assetManager)` parses them; expose `show: Logit`, `ctrl: Logit`, `isPlaceholder`.

```kotlin
class Logit(val coef: DoubleArray, val intercept: Double, val mean: DoubleArray, val std: DoubleArray) {
    fun z(x: DoubleArray): Double {
        var z = intercept
        for (i in x.indices) { val v = if (i < mean.size) (x[i] - mean[i]) / std[i] else x[i]; z += coef[i] * v }
        return z
    }
    fun p(x: DoubleArray, offset: Double = 0.0) = 1.0 / (1.0 + Math.exp(-(z(x) + offset)))
}
fun featureVector(c: Ctx, impHigh: Boolean, group: String?, control: Boolean): DoubleArray {
    val x = DoubleArray(16)
    x[0] = Math.sin(2 * Math.PI * c.hour / 24); x[1] = Math.cos(2 * Math.PI * c.hour / 24)
    x[2] = if (c.weekend) 1.0 else 0.0
    x[3] = Math.log(1.0 + c.notifs30); x[4] = Math.log(1.0 + c.unlocks30)
    x[5] = Math.log(1.0 + minOf(c.minsSinceUnlock, 600.0)); x[6] = if (c.screenOn) 1.0 else 0.0
    if (!control) {
        x[7] = if (impHigh) 1.0 else 0.0
        val gi = GROUPS.indexOf(group ?: "other").let { if (it < 0) 7 else it }   // productivity -> other
        x[8 + gi] = 1.0
    }
    return x
}
val GROUPS = listOf("messaging","social","video","shopping","finance","email","news","other")
```

### Step 7. Policy (pure Kotlin, JVM-testable) `engine/Policy.kt`

Plain data classes:
```kotlin
data class NotifFeatures(val key: String, val pkg: String, val postTime: Long, val channelId: String?, val channelName: String?,
    val category: String?, val importance: Int, val bypassDnd: Boolean, val isConversation: Boolean, val ongoing: Boolean,
    val isGroupSummary: Boolean, val isForegroundService: Boolean, val hasReplyAction: Boolean,
    val isDefaultSms: Boolean = false, val isAuthenticator: Boolean = false)
enum class PhoneState { LOCKED, IN_SOURCE_APP, IN_TASK_APP, IN_OTHER_APP }
data class Ctx(val nowMs: Long, val hour: Int, val weekend: Boolean, val notifs30: Int, val unlocks30: Int,
    val minsSinceUnlock: Double, val screenOn: Boolean, val state: PhoneState, val fgPkg: String?, val fgGroup: String?,
    val usageKnown: Boolean, val waitingForCode: Boolean)
data class Beta(var a: Double, var b: Double) {
    val mean get() = a / (a + b)
    val sd get() = Math.sqrt(a * b / ((a + b) * (a + b) * (a + b + 1)))
    val upper get() = mean + 1.28 * sd
}
data class Decision(val hold: Boolean, val gate: String, val reason: String, val pImpMean: Double, val pImpUp: Double,
    val pShow: Double, val pCtrl: Double, val costShow: Double, val costHold: Double, val benefit: Double,
    val randomized: Boolean, val propensityHold: Double, val phoneState: PhoneState)
```
Constants:
```kotlin
val PROTECTED_CATEGORIES = setOf("call","missed_call","voicemail","alarm","reminder","event","navigation","transport",
    "err","sys","status","service","progress","workout","location_sharing","stopwatch")
val PROTECT_KEYWORDS = listOf("otp","verification","verify","security","secure","login","sign-in","signin","2fa",
    "authenticator","transaction","payment","debit","credit","bank","upi","fraud","account","balance","delivery",
    "ride","cab","booking","ticket","boarding","appointment","emergency","sos","missed call","calendar","alarm")
val PROMO_KEYWORDS = listOf("promo","offer","deal","discount","sale","coupon","recommend","suggest","marketing",
    "newsletter","like","comment","follow","reaction","trending")
val POOL_PRIOR = mapOf(   // group -> (mu minutes, sigma). STARTING ASSUMPTIONS, label as such in UI.
    "messaging" to (1.5 to 1.5), "social" to (5.0 to 5.0), "video" to (8.0 to 7.0), "shopping" to (4.0 to 4.0),
    "finance" to (1.0 to 1.0), "email" to (2.0 to 2.0), "news" to (4.0 to 4.0), "productivity" to (3.0 to 3.0), "other" to (3.0 to 3.0))
```
Functions (write all of them):

1. `keywordHit(f, list)`: lower-case `channelId` and `channelName`, return true if any keyword is contained.
2. `priorBeta(f, group)`:
   - keyword protected → Beta(8,2); else conversational → Beta(6,4); else category in {promo, recommendation} or promo keyword → Beta(1,9); else group in {social, video, news} → Beta(2,8); else Beta(3,3). If `importance >= 4` then `a += 2`.
3. `looksConversational(f)` = `f.isConversation || (f.category == "msg" && f.hasReplyAction)`.
4. `protectedReason(f, group, st, s, ctx): String?` in this order, returning the reason string for the first match:
   ongoing / foreground service / bypassDnd → category in `PROTECTED_CATEGORIES` → `st.never || st.prot` → pkg in `s.neverHoldPkgs` → `f.isDefaultSms || f.isAuthenticator` → protect keyword on channel → `looksConversational(f) && !s.allowHoldChats` → `ctx.state == IN_SOURCE_APP` → `ctx.waitingForCode && (f.isDefaultSms || group == "email")`. Each reason reads like `"Protected: security channel."`.
5. `costs(...)`:
```kotlin
// LOCKED (or sim force-locked): logistic models
val x = featureVector(c, f.importance >= 4, group, false)
val pShow = models.show.p(x, st.bShow)
val pCtrl = personalCtrl ?: models.ctrl.p(featureVector(c, false, null, true))
costShow = pShow * st.mu ;  costHold = pCtrl * muS
// UNLOCKED: switch model
val m = mCtx(c)                       // 1.5 productivity, 1.2 video, 0.8 social, 0.7 launcher/home, else 1.0
val r = if (c.fgGroup == "productivity") 3.0 else 1.5      // resume penalty (assumption, label it)
costShow = st.pSwitch * (st.mu + r) * m ;  costHold = 0.15 * muS ;  pShow = st.pSwitch ; pCtrl = 0.15
```
6. `decide(f, group, c, st, s, models, personalCtrl, muS, rnd): Decision`:
   - `protectedReason` non-null → SHOW, gate `PROTECTED`.
   - `st.nSeen < 3` → SHOW, gate `COLD_START`, reason `"New channel, not enough history; showing while learning."`
   - `imp = Beta(st.a, st.b)`; `tau = s.strictness.tau`, `delta = s.strictness.delta`; **if `!c.usageKnown` then `tau = min(tau, 0.25)`**.
   - costs, `benefit = cShow - cHold`, `policyHold = imp.upper < tau && benefit > delta`.
   - Randomization only when `s.experiments && s.mode == SNOOZE` and eligible (`imp.upper < tau`, group in {social, shopping, news, video, other}): `propensity = if (policyHold) 1 - eps else eps`; `hold = rnd.nextDouble() < propensity`; `randomized = true`. Otherwise `propensity = if (hold) 1.0 else 0.0`.
   - Reason string (always one line, always with numbers):
     - HOLD: `"Low importance (≤ %.2f optimistic); showing now ≈ +%.1f min phone time vs waiting."`
     - SHOW low benefit: `"Low expected cost (%.1f min); not worth holding."`
     - SHOW importance high: `"Might matter (importance up to %.2f); showing."`
7. `digestRelease(now, maxHoldMin, digestMin)`: `next = ceil(now / (digestMin*60000)) * digestMin*60000`; `releaseAt = min(now + maxHoldMin*60000, next)`; `duration = max(releaseAt - now, 60_000)`.

**Unit tests (write these in `src/test`, they must pass):**
```kotlin
@Test fun securityChannelIsProtected()  // channelName "Security alerts" -> hold=false, gate PROTECTED
@Test fun promoHeldWhenLockedStrict()   // shopping, channel "Promotions", Ctx locked, nSeen=10, Beta(1,9), STRICT -> hold=true
@Test fun coldStartShows()              // nSeen=0 -> SHOW, gate COLD_START
@Test fun uncertainChannelNeverHeld()   // Beta(3,3) -> upper > tau -> SHOW
@Test fun inSourceAppProtected()
@Test fun digestReleaseAlignsToSlot()   // now=10:07, digest 15, maxHold 15 -> releaseAt 10:15
@Test fun noInternetNoContent()         // optional: scan sources for banned constants
```
Build a `testCtx()` helper with locked state, `notifs30=2, unlocks30=1, minsSinceUnlock=20, screenOn=false, hour=15`. Verify by hand that the promo test yields `benefit > 0.10`; if it does not, adjust only the placeholder coefficients (not the rule).

### Step 8. Context tracker `engine/ContextTracker.kt`
- Registered in `onListenerConnected()` and unregistered in `onListenerDisconnected()`: dynamic `BroadcastReceiver` for `ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`, `ACTION_USER_PRESENT` (register with `RECEIVER_NOT_EXPORTED` on API 33+).
- On SCREEN_ON: open a `ScreenInterval(startTs=now, endTs=null)` in memory and DB. On SCREEN_OFF: close it. On USER_PRESENT: insert `UnlockEvent(now)`.
- In-memory structures (rebuilt from DB at connect): `ArrayDeque<Long>` of notification timestamps and unlock timestamps for the last 30 min, `lastUnlockTs`, `screenOn` (from `PowerManager.isInteractive`), open-interval start. If an interval is open at connect but the screen is off, close it at now.
- `build(now, pkg, ownPkg): Ctx`:
  - `locked = KeyguardManager.isKeyguardLocked`, or `true` if `settings.simForceLocked && pkg == ownPkg`.
  - `minsSinceUnlock = (now - lastUnlockTs)/60000`, default 600 if none.
  - Foreground app via `UsageHelper.foreground(now)` only if usage access is granted; otherwise `usageKnown=false`, `fgPkg=null`.
  - State: `locked` → LOCKED; else if `fgPkg == pkg && pkg != ownPkg` → IN_SOURCE_APP; else if `AppGroups.groupOf(fgPkg)` in {productivity, video} (or maps/reading categories) → IN_TASK_APP; else IN_OTHER_APP.
  - `waitingForCode` from the same event scan (below).
- `UsageHelper.foreground(now)`: cache the result 5 s. Query `queryEvents(now - 20*60_000, now)`. Iterate: `MOVE_TO_FOREGROUND` (alias `ACTIVITY_RESUMED`) sets `pkg`; `ACTIVITY_PAUSED`/`MOVE_TO_BACKGROUND` for that package clears it. Also set `waitingForCode = true` if any resume event within the last 90 s belongs to group finance/shopping or a package name containing `uber`, `ola`, `swiggy`, `zomato`, `login`, or if the current foreground group is finance/shopping.
- `phoneMinutes(t0, t1)`: sum of overlap of `screen_intervals` with `[t0, t1]` (treat an open interval as ending at `min(now, t1)`), in minutes.

### Step 9. Engine `engine/Engine.kt` and listener

**9.1 `Graph` singleton** (object, initialized in `TwinApp.onCreate`): `db`, `prefs`, `models`, `groups`, `context`, `engine`, `ioScope` (`CoroutineScope(SupervisorJob() + Dispatchers.IO)`), `listenerConnected: MutableStateFlow<Boolean>`. In-memory cache `ConcurrentHashMap<String, ChannelStat>` keyed `pkg|channelId`, loaded from DB at start.

**9.2 `AttentionListener`**
```kotlin
class AttentionListener : NotificationListenerService(), Enforcer {
    override fun onListenerConnected() { Graph.onConnected(this) }
    override fun onListenerDisconnected() {
        Graph.onDisconnected()
        requestRebind(ComponentName(this, AttentionListener::class.java))
    }
    override fun onNotificationPosted(sbn: StatusBarNotification, rm: RankingMap) {
        try { Graph.engine.onPosted(extract(sbn, rm), this) }
        catch (t: Throwable) { Log.w("TwinListener", "decision failed, leaving notification alone", t) }   // unknown = SHOW
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification, rm: RankingMap, reason: Int) {
        try { Graph.engine.onRemoved(sbn.key, reason, System.currentTimeMillis()) } catch (_: Throwable) {}
    }
    override fun snooze(key: String, ms: Long): Boolean = try { snoozeNotification(key, ms); true } catch (e: Exception) { false }
    override fun stillActive(key: String) = try { activeNotifications.any { it.key == key } } catch (e: Exception) { false }
}
interface Enforcer { fun snooze(key: String, ms: Long): Boolean; fun stillActive(key: String): Boolean }
```
`extract` (never read text extras):
```kotlin
private fun extract(sbn: StatusBarNotification, rm: RankingMap): NotifFeatures {
    val r = Ranking(); rm.getRanking(sbn.key, r)
    val n = sbn.notification; val ch = r.channel
    return NotifFeatures(
        key = sbn.key, pkg = sbn.packageName, postTime = sbn.postTime,
        channelId = ch?.id, channelName = ch?.name?.toString(), category = n.category, importance = r.importance,
        bypassDnd = ch?.canBypassDnd() ?: false,
        isConversation = Build.VERSION.SDK_INT >= 30 && r.isConversation,
        ongoing = sbn.isOngoing,
        isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
        isForegroundService = (n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
        hasReplyAction = n.actions?.any { a -> a.remoteInputs?.isNotEmpty() == true } == true,
        isDefaultSms = Graph.groups.isDefaultSms(sbn.packageName),
        isAuthenticator = Graph.groups.isAuthenticator(sbn.packageName))
}
```

**9.3 `Engine.onPosted(f, enf)`** must follow this order and finish in well under 50 ms. Use only in-memory state on this thread; all DB writes go to `ioScope`.
1. `now = currentTimeMillis()`.
2. If `f.channelId == "twin_internal"` or `f.isGroupSummary`: log nothing heavy, return.
3. **Re-post of a held key:** `heldKeys: ConcurrentHashMap<String, Long>` (key → releaseAt, reloaded from `unreleasedHolds()` at connect). If key present: if `now >= releaseAt - 5000` remove it and `markReleased(key, now)` asynchronously; else log "update while held". Return without deciding again.
4. `group = groups.groupOf(f.pkg, f.channelId)`; `st = getOrCreateStat(f, group)` (new stat uses `priorBeta`, `mu/var` from `POOL_PRIOR`, `pSwitch` prior: messaging 0.5, social 0.3, else 0.15).
5. `ctx = context.build(...)`; record the notification timestamp in the 30-minute deque.
6. **Gate 0:** if `!s.focusActive` → SHOW, gate `FOCUS_OFF`, reason `"Focus is off; learning only."` (still write receipt, still learn).
7. Else `d = Policy.decide(...)` with `personalCtrl = ctrlTracker.pCtrl(hourBlock, modelCtrlP)` and `muS = ctrlTracker.muS()`.
8. **Enforce:** if `d.hold` and `s.mode == SNOOZE`: `(releaseAt, dur) = digestRelease(...)`; `ok = enf.snooze(f.key, dur)`; if ok `heldKeys[f.key] = releaseAt`, `enforcement = "applied"`; else `enforcement = "failed"` and treat as SHOW. Post a 2-second delayed check on the main `Handler`: if `enf.stillActive(key)` still true, set `enforcement = "failed"` and remove from `heldKeys`. If mode is SHADOW: no call, `enforcement = "none"`, action stays the decided one (a "would hold"). If mode is DND_ONLY: no per-notification action, log "would hold".
9. `st.nSeen++`.
10. Async: insert `EventRow` + `DecisionRow`, get `eventId`, complete a `CompletableFuture<Long>` stored in `idByKey[f.key]`, schedule `handler.postDelayed({ outcomes.close(eventId) }, 10*60_000)`. `source = if (f.pkg == ownPkg) "sim" else "real"`.
11. `onRemoved(key, reason, now)`: `idByKey[key]?.thenAcceptAsync { setRemoval(it, reason, now) }`. Reason constants: `REASON_CLICK=1`, `REASON_CANCEL=2`, `REASON_CANCEL_ALL=3`, `REASON_SNOOZED=18` (use the `NotificationListenerService.REASON_*` names).

**9.4 `whatIf(ctx, channel)`** reuses `Policy.costs` and `Policy.decide` with `experiments=false`, so the UI and the engine can never disagree. Returns the numbers for both worlds plus the three-stage chain (below).

### Step 10. Outcomes and learning `engine/OutcomeTracker.kt`

`close(eventId)` (idempotent; also run `sweepPending()` on connect, on each post, and when the app opens):
1. Load event, decision, window `[postTs, postTs + 10 min]`. If the service was disconnected during the window (no screen interval data), set `censored=true` and skip learning.
2. `unlocks10m = unlocksBetween(...)`, `phoneMin10m = phoneMinutes(...)`.
3. `clicked = removalReason == CLICK && removedTs in window`; `dismissed = removalReason in {CANCEL, CANCEL_ALL} && removedTs in window`; `clickedWithin2m = clicked && removedTs - postTs < 120_000`.
4. `engaged = unlocks10m > 0 || clicked || dismissed` (engagement proxy, not importance).
5. Insert `OutcomeRow`. Then, unless `learningPaused`:
   - **Action SHOW** (`SGD`): `st.bShow = clip(st.bShow + 0.05*((engaged?1:0) - d.pShow), -2, 2)`. If `engaged`: update pull minutes `n<5: mu=(mu*(n+2)+obs)/(n+3)` else `mu = 0.7*mu + 0.3*obs`; `varPull = 0.7*varPull + 0.3*(obs-mu)^2`; `nPull++`; `nEngaged++`. Importance Beta: `clickedWithin2m → a += 1`; `dismissed → b += 1`; neither → `b += 0.3`. For unlocked-state decisions update `pSwitch = 0.9*pSwitch + 0.1*(clickedWithin2m?1:0)`.
   - **Action HOLD:** no importance update (the user never saw it). If the notification was released and clicked within 60 s of `releasedTs`, treat as "needed" evidence: `a += 1`.
6. Persist `ChannelStat` (async) and keep the cache updated.

`submitFeedback(eventId, kind)`: `mattered → a += 2`; `not_matter → b += 1`; `sooner → a += 3` **and `prot = true` immediately** (also write `never`). Store `digestFeedback`.

**Personal control model `engine/CtrlTracker.kt`** (this replaces any external data):
- `sweepProbes(now)`: for every 15-minute grid time `t` since the last probe up to `now - 10 min`: if the screen was off at `t` (not inside any `screen_interval`) and no notification event in `[t - 10 min, t]`, then `y = an unlock exists in (t, t + 10 min]`, `phoneMin = phoneMinutes(t, t + 10 min)`, insert `CtrlProbe(t, block = hour(t)/4, y, phoneMin)`.
- `pCtrl(block, p0)`: `(k + m*p0) / (n + m)` with `m = 12` pseudo-observations, where `k, n` are the probe counts for that block and `p0` is the prior model probability. This is the "personal control model": the user's own spontaneous pickup rate.
- `muS()`: `(sum(phoneMin where y=1) + 3*3.0) / (count(y=1) + 3)` (prior 3.0 min, 3 pseudo-samples).

**Counterfactual estimator `engine/Effect.kt`** (uses only `randomized = 1` rows whose outcome row exists):
```kotlin
// A=1 held, A=0 shown, e = propensity_hold, Y = phoneMin10m
fun ipw(rows: List<Row>): Pair<Double, Double> {
    val show = rows.filter { !it.held }; val hold = rows.filter { it.held }
    val eShow = show.sumOf { it.y / (1 - it.e) } / show.sumOf { 1 / (1 - it.e) }
    val eHold = hold.sumOf { it.y / it.e } / hold.sumOf { 1 / it.e }
    return eShow to eHold
}
// harm = eShow - eHold; CI: 400 bootstrap resamples, 2.5th and 97.5th percentile
// Display only if n_show >= 30 and n_hold >= 30, else "not enough randomized data yet (n_show=.., n_hold=..)".
```

### Step 11. UI design system (this is where the app wins; do this carefully)

Dark only. Package `ui/theme`, `ui/components`, `ui/screens`.

**Tokens**
```kotlin
val Bg = Color(0xFF0B0F1A); val Surface1 = Color(0xFF121826)
val Cyan = Color(0xFF22D3EE); val HoldGreen = Color(0xFF34D399); val ShowCoral = Color(0xFFF87171)
val ProtectedBlue = Color(0xFF60A5FA); val Amber = Color(0xFFFBBF24)
val TextHi = Color(0xFFE6EAF2); val TextLo = Color(0xFF8B93A7)
```
`darkColorScheme(primary = Cyan, background = Bg, surface = Surface1, onSurface = TextHi, onBackground = TextHi, secondary = HoldGreen, error = ShowCoral)`. Shapes: 20 dp cards, 14 dp chips, full-round buttons. Spacing: 16 dp page padding, 12 dp between cards. Typography: `headlineLarge` 30 sp SemiBold, `titleMedium` 16 sp SemiBold, `bodyMedium` 14 sp, `labelSmall` 11 sp with `letterSpacing = 0.5.sp`, numbers use `fontFeatureSettings = "tnum"`. Honour system font scale; every touch target at least 48 dp; every non-text icon has a `contentDescription`; **never colour alone**, always icon + label.

**Components (`ui/components`)**
```kotlin
@Composable fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f))))
        .border(1.dp, Color.White.copy(0.10f), shape).padding(16.dp), content = content)
}
```
- `DecisionChip(kind)`: HOLD (`Icons.Rounded.PauseCircle`, green, "HELD"), SHOW (`Visibility`, coral, "SHOWN"), PROTECTED (`Shield`, blue, "PROTECTED"), LEARNING (`School`, amber, "LEARNING"). Rounded 14 dp, 12% tinted background, 1 dp tinted border, icon + text.
- `EvidenceBadge(level)`: `PRIOR` (amber), `PILOT` (cyan), `SIMULATED` (grey), `OFFLINE` (blue).
- `StatTile(label, value, icon)` for the three counters.
- `TwinOrb`:
```kotlin
@Composable fun TwinOrb(p: Float, density: Float, dots: Int, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "orb").animateFloat(0f, 1f,
        infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t")
    val phase = if (reduceMotion) 0f else t
    Canvas(modifier.size(240.dp)) {
        val c = center; val maxR = size.minDimension / 2
        val col = lerp(Cyan, Amber, density.coerceIn(0f, 1f))          // cyan -> amber with notification density
        val r = maxR * (0.40f + 0.30f * p.coerceIn(0f, 1f)) * (1f + 0.03f * sin(phase * 2f * PI.toFloat()))
        drawCircle(Brush.radialGradient(listOf(col.copy(0.30f), Color.Transparent), c, maxR), maxR, c)
        drawCircle(Brush.radialGradient(listOf(col.copy(0.95f), col.copy(0.25f)), c, r), r, c)
        repeat(dots.coerceAtMost(12)) { i ->                              // one orbiting dot per notification in the last 30 min
            val a = (i / 12f + phase) * 2f * PI.toFloat()
            drawCircle(Color.White.copy(0.85f), 4.dp.toPx(), Offset(c.x + maxR * 0.9f * cos(a), c.y + maxR * 0.9f * sin(a)))
        }
    }
}
```
`reduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f`.
- `BarChart(values, labels, color)`: `Canvas` with rounded bars scaled to max, label every 3rd hour, value text above the tallest bar. `HBarList(items)` for rankings.
- `RangeBar(point, low, high, max)`: a track, a translucent band from `low` to `high`, a bright dot at `point`; used for the "approximate 80% range".
- `ChannelRow`, `ReceiptRow` (collapsed: app + channel + `DecisionChip` + time; expanded: lifecycle, gate, reason, numbers, enforcement, outcome).

**Navigation**: `Scaffold` with a bottom `NavigationBar` of 5 items: **Now, Held, Twin, Receipts, More**. "More" opens a list: Insights, Evidence, Settings, Simulator, Privacy. `NavHost` start destination = `onboarding` unless `prefs.onboarded`. Use `AnimatedContent` fades (200 ms) between tabs. Dark `SystemBars` via `enableEdgeToEdge()`.

**State plumbing**: `MainViewModel : ViewModel()` exposes `StateFlow<UiState>` (status, counters, ctx snapshot, held list). A `LaunchedEffect` with `repeatOnLifecycle(STARTED)` refreshes `status` every 2 s. Lists (receipts, held, channel stats) are Room `Flow`s collected with `collectAsStateWithLifecycle()`. Add `Dao` methods returning `Flow` for these.

### Step 12. Screens (build in this order)

**12.1 Onboarding.** `HorizontalPager` of 3 glass cards: "What we see" (app name, channel name, category, timing), "What we never see" (message text, titles, senders, contacts; no internet permission), "What we may do" (hold distractions for at most N minutes; originals come back unchanged). Then permission steps, one screen each with a title, a two-line reason, and a primary button that opens the right settings screen; a `LifecycleEventObserver` on `ON_RESUME` re-checks and auto-advances when granted: **Notification access** (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`) → **Usage access** (`ACTION_USAGE_ACCESS_SETTINGS`) → **Post notifications** (runtime request, Android 13+) → **Battery** (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, with a note: on Samsung also add the app to "Never sleeping apps") → **Do-not-disturb access** (optional, only if "Silence while focused" is on; `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`). Usage access is optional: if skipped, show "Limited mode: the app will hold less". Finish with the **Protect list**: installed launcher apps in a `LazyColumn` with a `Switch` per row, pre-ticked for the default SMS app, dialer, authenticators, finance group, and anything in the suggested set. Save to `neverHoldPkgs`. Set `onboarded = true`.
Permission checks:
```kotlin
fun listenerEnabled(c: Context) = NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.packageName)
fun usageGranted(c: Context): Boolean {
    val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), c.packageName) == AppOpsManager.MODE_ALLOWED
}
fun policyGranted(c: Context) = (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted
```

**12.2 Now (home).** Top: title "Attention Twin" and a row of status chips (Listener, Usage, DND, Mode), each icon + word, green/amber/red plus symbol. Centre: `TwinOrb` (`p` = model `p_show` for a generic notification in the current context, `density` = `notifs30/10`, `dots = notifs30`), caption under it: *"Chance you'd pick up your phone for a typical notification right now: 23%"* (computed, with the PRIOR badge until real data exists). Below: the **Focus toggle**, a 72 dp pill (animated thumb, haptic on toggle, label "Focus ON / OFF"). Then three `StatTile`s: Shown, Held, Protected (today). Then a glass card "Right now": phone state, minutes since unlock, notifications in 30 min. If `sum(nSeen) < 60` show an amber learning banner with a progress bar ("Twin is learning you: 23 / 60 notifications"). If the listener is disconnected show a full-width red card with "Fix" opening the settings.

**12.3 Held.** `LazyColumn` of currently held items (app label, channel, held-since, live countdown via `produceState` ticking each second). Empty state: an illustration-free message "Nothing waiting." Below, "Recently released" rows with three outlined buttons: **Mattered**, **Didn't matter**, **Needed it sooner** (the last one has a shield icon and a toast "This channel is now protected"). No content is ever shown. Header caption: "Held notifications return unchanged at the next digest slot (max N min)."

**12.4 Twin (What-if).** The hero screen; make it excellent.
- Controls (glass card): channel picker (dropdown of known channels plus the 6 simulator channels), `SegmentedButton` for phone state (Locked / In task app / In other app), sliders for minutes since unlock (0–120), notifications in 30 min (0–15), unlocks in 30 min (0–10), hour (0–23). Defaults come from the live context ("Use now" button).
- Result (recomputed instantly on every change by calling `engine.whatIf`):
  1. Header: `DecisionChip` + the one-line reason.
  2. **Two lanes**, side by side on wide screens, stacked on phones.
     *If shown:* big `P(engage) 23%` → arrow → `~4.2 min` pull → arrow → `+1.5 min resume`.
     *If held:* `P(pickup anyway) 9%` → `~3.0 min`.
  3. **The PS chain**, a small timeline card with three segments: *Interruption* (about 2 min) → *Browsing* (expected pull minutes) → *Resumption delay* (the resume penalty). Footnote: "Pull minutes and resume delay start as assumptions and are replaced by your own measurements as the twin learns."
  4. **Harm** = `cost_show − cost_hold` in minutes with the `RangeBar` (`harm ± 1.28·sd`, label "approximate 80% range"). Variance per arm: `p·σ² + p(1−p)·μ²`; `harm_sd = sqrt(Var_show + Var_hold)`.
  5. Importance: mean and optimistic bound as two small gauges with the threshold `τ` marked.
  6. Counterfactual panel: "If this had been shown: expected +X min of phone time. Held world: +Y min." with an `EvidenceBadge` (`PRIOR` until enough personal probes exist, then `PILOT`).
- Flip demo: changing phone state from Locked to In task app visibly changes harm and can flip HOLD/SHOW; animate numbers with `animateFloatAsState`.

**12.5 Receipts.** Filter chips (All, Held, Shown, Protected, Failed). Rows as in components. The expanded row shows the lifecycle: posted → decision → snooze → removal reason (`SNOOZED` confirms) → released → reaction → outcome. Show numbers: `p_show, p_ctrl, cost_show, cost_hold, benefit, importance mean/up`, gate, mode, enforcement, `randomized/propensity`. Hide `source='sim'` rows unless `includeDemo`, otherwise badge them `SIMULATED`.

**12.6 Insights** (every figure shows its n; hide a chart with "Not enough data yet (n=…)" when n < 10).
- Notifications by hour (`BarChart`, 24 bars, SQL on `post_ts` with local time).
- Top channels by phone minutes triggered per notification (`HBarList`): `sum(phone_min_10m)/count` over shown, engaged rows.
- **Fragmentation ranking:** apps by (engaged notifications × average pull minutes) descending.
- Channel table: importance mean, optimistic bound, n, hold rate, pull minutes.
- **Best focus windows:** from `unlock_events`, compute the gap to the next unlock for each unlock, attribute the gap to the starting hour, average per hour, show the 3 hours with the longest average unlock-free stretch.
- **Fatigue proxy** (label "proxy, not a measurement"): longest unbroken screen interval today, plus late-hour flag (hour ≥ 22) and unlocks per hour; show as a single status chip with the three inputs.

**12.7 Evidence.** Three blocks, each with an `EvidenceBadge`:
1. **Model:** if `ModelStore.isPlaceholder`, a red banner: "PRIOR MODEL: hand-set assumptions, not trained on data. The twin personalises on this phone." Else show metrics from the model JSON.
2. **On-device pilot (`PILOT`):** seen, held, shown, protected, overrides ("Needed it sooner" taps), **missed protected (must be 0)**, enforcement success rate (holds confirmed by `REASON_SNOOZED` / holds attempted), median hold, median phone minutes per notification by arm.
3. **Counterfactual (`PILOT`):** the IPW estimate with bootstrap CI, or "not enough randomized data yet (n_show=…, n_hold=…)". Never show simulated or observational numbers here.
Rows with `source='sim'` are excluded unless `includeDemo`; when included the block turns grey and shows `SIMULATED`.

**12.8 Settings.** Strictness (3-way segmented), max hold (5/10/15/30), digest interval, silence while focused, mode (Shadow / Snooze / DND-only) with one-line explanations, experiments opt-in with ε slider (0.05–0.30) and a plain-language explanation, "Allow holding chats" (default off), Never-hold list editor, pause learning, include demo data, export CSV (receipts only, no content; write to app cache and share via `Intent.ACTION_SEND` with `FileProvider`), **Delete all data** (confirmation dialog, calls `db.clearAllTables()` and resets prefs and caches), and the privacy panel text below.

**12.9 Privacy panel text (must be true):** "We never read notification text, titles or senders." "This app has no internet permission." "All data stays on this phone. Delete everything with one tap." "Held notifications return unchanged after at most N minutes." Then the list of permissions and why each is requested.

### Step 13. Simulator `engine/Simulator.kt` and screen

- Create notification channels at app start with these exact ids and visible names: `messages` ("Messages"), `promotions` ("Promotions"), `likes` ("Likes and comments"), `security` ("Security alerts"), `news` ("News"), `orders` ("Order updates"). Importance DEFAULT for all except `security` (HIGH). Never set text containing user content; use fixed strings like "Test notification".
- `post(kind)` posts via `NotificationManagerCompat` with the right `category` (`msg` + a `RemoteInput` reply action for messages, `promo` for promotions, `social` for likes, `status`/`msg` for security as appropriate, `recommendation` for news) using the app's own package.
- Buttons: post one of each kind, **burst of 8 promotions**, "post in 10 s" for each (so the tester can lock the phone first), "seed demo data" (inserts rows with `source='sim'` for charts), and a banner: "To test holds: turn Focus on, choose Strict, tap 'Post in 10 s', then lock the phone."
- At first run, pre-seed `ChannelStat` for the six simulator channels with `nSeen = 10` and the priors from `priorBeta`, so the demo does not sit in cold start.
- A debug switch `simForceLocked` (Settings → Developer) treats simulator notifications as arriving on a locked phone, so the demo works while the app is open. Its receipts stay `sim`.
- The `security` channel contains the word "security", so it must be a keyword-protected SHOW. The `messages` channel must be protected by the conversational rule. The `promotions` and `likes` channels must be held in Strict mode.

### Step 14. Build, verify, install
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
aapt dump permissions app/build/outputs/apk/release/app-release.apk   # expect NO android.permission.INTERNET
adb install -r app/build/outputs/apk/release/app-release.apk
# optional shortcuts for the tester (no UI needed):
adb shell cmd notification allow_listener com.cascade.attentiontwin/com.cascade.attentiontwin.AttentionListener
adb shell appops set com.cascade.attentiontwin GET_USAGE_STATS allow
adb logcat -s TwinListener
```
If `aapt` is missing, use `apkanalyzer manifest permissions <apk>`. Copy the final APK to `release/AttentionTwin.apk` in the repo.

### Step 15. Test matrix (run on an emulator for logic, then on the Samsung phone)

| # | Test | Expected |
|---|---|---|
| T1 | Grant notification access | Listener chip turns green on return |
| T2 | Focus ON, Strict, post `Promotions` with the phone locked | HELD, disappears, returns at the next 15-min slot; receipt shows enforcement applied, `REASON_SNOOZED`, then released |
| T3 | Post `Security alerts` | SHOWN, gate PROTECTED (keyword) |
| T4 | Post `Messages` | SHOWN, protected (conversational) |
| T5 | Burst of 8 promotions | Held and returned together; no re-hold loop |
| T6 | Focus OFF | No enforcement; receipts still written, gate FOCUS_OFF |
| T7 | Swipe the app away from recents, post a promo | Still decided and held |
| T8 | Reboot | Listener reconnects |
| T9 | Revoke notification access | Red status, no crash, nothing is held |
| T10 | Open an app, then trigger its notification | SHOWN, IN_SOURCE_APP |
| T11 | Tap "Needed it sooner" on a channel | Channel protected immediately |
| T12 | Pause learning | No updates to channel stats; decisions still safe |
| T13 | Delete all data | Tables empty, stats reset |
| T14 | `aapt dump permissions` | No INTERNET |
| T15 | `./gradlew checkNoContentReads` | Passes (no banned constants) |
| T16 | Battery set to Optimized for 1 hour | Note whether the listener survives; document it |
| T17 | Snooze then app updates the same key | Not re-held; "update while held" logged |
| T18 | Airplane mode | Identical behaviour |
| T19 | What-if: Locked vs In task app for the same channel | Harm and decision change visibly |
| T20 | Snooze fails (simulate by throwing) | Enforcement `failed`, notification remains visible |

**Phase gate before any UI polish:** the human runs T2 on the target Samsung. If snooze does not behave (disappear then reappear exactly once), set the default `mode = SHADOW` and, if Do-not-disturb access is granted, offer DND-only. The rest of the app remains valid.

### Step 16. Build order and time budget for you
1. Steps 0–2 (scaffold, manifest, guard task): get a blank themed app building.
2. Steps 3–7 (data, settings, groups, models, Policy + unit tests): all tests green.
3. Steps 8–10 (context, engine, listener, outcomes): run on an emulator with the simulator; check T2–T6, T17, T20 with `adb logcat`.
4. Step 11–12.2, 12.3, 12.5, 12.8 (design system, Now, Held, Receipts, Settings) plus Onboarding.
5. Step 12.4 (Twin), 12.6, 12.7, then Step 13 polish.
6. Step 14. Finish with a short `README.md` (how to install, permissions, test steps) and `DECISIONS.md`.

**If time is short, cut in this order:** Insights polish, ε experiments UI, CSV export, DND, schedule. **Never cut:** Gate 1, receipts, snooze verification, Delete all, honest evidence badges, the Twin What-if screen.

---

## D. What the team can honestly claim

| We can say | We cannot say |
|---|---|
| "We never read message text; no internet permission." | "We detect OTPs or spam" (we protect classes of senders and learn from behaviour) |
| "Every hold is reversible, capped at N minutes and has a receipt." | "Blocked before it arrives" (the listener acts after posting) |
| "The twin starts from stated assumptions and personalises on-device; it labels what is assumed and what is measured." | Any AUC or accuracy figure, unless a trained model JSON is actually shipped and its metrics are in the file |
| "With randomized holds, the held-versus-shown difference in phone minutes is estimated with a confidence interval." | A causal effect without enough randomized data, or any number from simulated rows |
| "Importance is learned from your behaviour and your feedback." | That engagement equals importance |
| "Phone minutes after a notification is our attention-cost proxy." | That we measure cognition, fatigue or focus directly |

Related research worth citing as context (do not claim the numbers as ours): notification engagement can be predicted from phone-use context (Pielot et al.), importance cannot be derived from interaction alone and benefits from explicit user feedback (hence the feedback buttons and protection rules), and deferring notifications is a recognised design (Snooze studies).

## E. Optional later: a trained prior
If a notification dataset becomes available, train two logistic regressions (shown, control) on the same 16 features and replace the two JSON files (`"source": "mpu_trained"`). No code changes are needed, and the PRIOR banner disappears automatically.
