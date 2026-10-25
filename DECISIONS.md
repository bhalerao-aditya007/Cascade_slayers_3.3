# Decisions Log

## Step 0: Environment
- **JDK**: Using Android Studio's bundled JBR (JDK 21) at `C:\Program Files\Android\Android Studio\jbr`. JDK 17 was specified but not installed; JDK 21 is backward-compatible and is the standard JBR shipped with Android Studio Koala+. Set via `org.gradle.java.home` in `gradle.properties`.
- **Android SDK**: Found at `C:\Users\aditi\AppData\Local\Android\Sdk` with platform-34 and build-tools 34.0.0 present.
- **Gradle wrapper**: `gradle` CLI not installed on the system. Created wrapper files targeting Gradle 8.7.

## Steps 1 & 2: Gradle setup, Manifest, Guard Tasks
- Configured compileSdk 34, minSdk 26, targetSdk 34, Jetpack Compose with Material 3, Room 2.6.1 with KSP 1.9.24-1.0.20.
- Implemented `checkNoContentReads` task in `app/build.gradle.kts` hooked into `preBuild` to strictly forbid content-reading constants (`EXTRA_TITLE`, `EXTRA_TEXT`, etc.).
- Stripped `android.permission.INTERNET` in `AndroidManifest.xml` using `tools:node="remove"`.
- Set dark window background `#0B0F1A` and strict extraction rules in `data_extraction_rules.xml`.
- Configured `FileProvider` (`cache-path`) for CSV export of receipts.

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

## Step 8: Context Tracking
- Implemented `UsageHelper` using `UsageStatsManager` event scans to detect foreground apps and `waitingForCode` flows within 90 seconds.
- Implemented `ContextTracker` registering dynamic `BroadcastReceiver` for `ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`, and `ACTION_USER_PRESENT` (with `RECEIVER_NOT_EXPORTED` on API 33+).
- In-memory deques track last 30 minutes of unlocks and notifications, measuring phone minutes overlapping screen intervals.

## Step 9: Decision Engine, Listener, and Architecture
- `Graph` singleton instantiated in `TwinApp.onCreate()`, sharing in-memory `ChannelStat` cache between the listener and UI.
- `AttentionListener` (`NotificationListenerService`) implements `Enforcer` with `requestRebind` resilience and safe `snoozeNotification` execution.
- Order of execution under 50ms: checks internal channels -> checks held key re-posts -> evaluates Gate 0 focus -> executes Gate 1 and Policy costs -> enforces snooze -> writes receipts asynchronously.
- Implemented 2-second verification Handler check on active notifications to guarantee snooze succeeded or mark enforcement as failed.

## Step 10: Outcomes and On-Device Learning
- `OutcomeTracker.close()` runs on a 10-minute delayed Handler after each notification. Idempotent measurement of clicks (`REASON_CLICK`), dismissals (`REASON_CANCEL`), and unlock events.
- Performs online Bayesian updates to channel importance Beta and SGD adjustments to `bShow` and `pSwitch`.
- `CtrlTracker` sweeps 15-minute grid probes of spontaneous pickup rates when screen was off and no notifications arrived, blending empirical counts with prior assumptions.
- `Effect.kt` computes Inverse Propensity Weighting (IPW) causal harm with a 400-sample bootstrap confidence interval for randomized holds.

## Step 11: Design System & Visual Tokens
- Dark-only visual identity: background `#0B0F1A`, surface `#121826`, primary cyan `#22D3EE`, held green `#34D399`, coral `#F87171`, blue `#60A5FA`, amber `#FBBF24`.
- Reusable glass card system with white alpha vertical gradient and hairline borders.
- Custom `Canvas` implementations:
  - `TwinOrb`: Living orb scaling with pickup probability, cyan-to-amber density shifts, and orbiting notification dots.
  - `BarChart`: Custom 24-bar distribution for hourly notifications.
  - `HBarList`: Horizontal ranking bars for fragmentation metrics.
  - `RangeBar`: Translucent 80% confidence interval band with central estimate dot.
  - `DecisionChip` and `EvidenceBadge`: Icon + label pairs ensuring color is never the sole information carrier.

## Step 12: Screens & User Experience
- **NowScreen**: Status chips, Twin Orb with plain-language caption, 72dp focus toggle pill, daily counters, and context status.
- **HeldScreen**: Live countdown timer per held item using `produceState`, with one-tap feedback buttons (*Mattered*, *Didn't matter*, *Needed it sooner* with instant protection).
- **TwinScreen (What-if)**: Interactive consequence simulator with channel picker, phone state switcher, live harm estimation, and animated consequence chain.
- **ReceiptsScreen**: Filterable lifecycle audit trail with full mathematical transparency.
- **InsightsScreen**: Best focus windows, fragmentation ranking, hourly histogram, and fatigue proxy.
- **EvidenceScreen**: Model provenance disclosure, on-device pilot statistics with missed protected counter (=0), and causal estimates.
- **SettingsScreen**: Policy strictness presets, mode switches, randomized experiment opt-in, CSV export, and complete data deletion.
- **PrivacyScreen**: Plain-language guarantees, zero-content verification explanation, and permission disclosures.
- **OnboardingScreen**: 3-card transparent orientation, auto-advancing permission steps, and app protection setup.

## Step 13: Simulator
- 6 pre-configured channels (`messages`, `promotions`, `likes`, `security`, `news`, `orders`) pre-seeded with 10 observations each.
- Delayed 10-second posting triggers allowing phone locking, burst-of-8 promotion tests, and demo data generator.

## Step 14: Release Build & Verification
- Release APK built at `release/AttentionTwin.apk`.
- Zero `INTERNET` permission verified via Android SDK manifest inspection.
