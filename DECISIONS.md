# Decisions Log

## Step 0: Environment
- **JDK**: Using Android Studio's bundled JBR (JDK 21) at `C:\Program Files\Android\Android Studio\jbr`. JDK 17 was specified but not installed; JDK 21 is backward-compatible and is the standard JBR shipped with Android Studio Koala+. Set via `org.gradle.java.home` in `gradle.properties`.
- **Android SDK**: Found at `C:\Users\aditi\AppData\Local\Android\Sdk` with platform-34 and build-tools 34.0.0 present.
- **Gradle wrapper**: `gradle` CLI not installed on the system. Created wrapper files manually targeting Gradle 8.7.

## Steps 1 & 2: Gradle setup, Manifest, guard tasks
- Configured compileSdk 34, minSdk 26, targetSdk 34, Jetpack Compose with Material 3, Room 2.6.1 with KSP 1.9.24-1.0.20.
- Implemented `checkNoContentReads` task in `app/build.gradle.kts` hooked into `preBuild` to strictly forbid content-reading constants (`EXTRA_TITLE`, `EXTRA_TEXT`, etc.).
- Stripped `android.permission.INTERNET` in `AndroidManifest.xml` using `tools:node="remove"`.
- Set dark window background `#0B0F1A` and strict extraction rules in `data_extraction_rules.xml`.

## Step 3: Room Data Model
- Created 7 entities in `Entities.kt` (`EventRow`, `DecisionRow`, `OutcomeRow`, `ChannelStat`, `ScreenInterval`, `UnlockEvent`, `CtrlProbe`).
- Created `ReceiptView` joined data class and typed query result classes (`CtrlCount`, `MuSample`, `TodayCounts`, `HourCount`, `ChannelPhoneMin`, `PilotStats`).
- Implemented `TwinDao` with all required CRUD, flow streams, and statistical aggregations.
- Setup `AppDb` RoomDatabase singleton with destructive migration fallback.

## Step 4: Settings & Preferences
- Implemented `Settings` data class and `Prefs` wrapper with `StateFlow<Settings>` backed by `SharedPreferences`.

## Step 5: App Groups Classification
- Implemented `AppGroups` with 3-tier resolution: own package channel mapping -> curated `app_groups.json` -> `ApplicationInfo.category` fallback.
- Added `isDefaultSms` and `isAuthenticator` package pattern recognition.

## Step 6: Prior Models
- Hand-set JSON priors stored in `assets/models/engage_show.json` and `engage_ctrl.json`.
- Implemented `Logit` logistic regression inference engine with standardization and `featureVector` generating the 16-feature vector.
- Added `ModelStore` with asset loading and test factory.

## Step 7: Policy Engine
- Implemented pure Kotlin JVM-testable `Policy` with Gate 1 protection rules, optimistic Beta(a,b) bound calculation, cost-of-interruption engine for locked and unlocked states, cold start handling, and randomization for causal estimation.
- 7 unit tests implemented in `PolicyTest.kt` covering keyword protection, locked strict holds, cold start, Beta uncertainty, in-source app protection, digest slot alignment, and no-content static check. All passed.
