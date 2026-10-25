<div align="center">

# 🪞 Attention Twin

### A personal, on-device simulator of your attention

**Attention Twin learns how *your* phone pulls you away from what you're doing, predicts what a notification will cost you *right now*, and holds the ones that aren't worth it until the next digest slot, with a receipt for every decision.**

`Android` · `Kotlin + Jetpack Compose` · `On-device learning` · `No INTERNET permission` · `Never reads notification text`

*Cascade '26 · Theme 3 · Problem 3: The Attention Digital Twin*

</div>

---

## Table of Contents

1. [The Problem](#1-the-problem)
2. [The Idea](#2-the-idea)
3. [What It Is, and What It Is Not](#3-what-it-is-and-what-it-is-not)
4. [Why This Is Different](#4-why-this-is-different)
5. [Problem Statement Coverage](#5-problem-statement-coverage)
6. [Features](#6-features)
7. [How the Twin Decides](#7-how-the-twin-decides)
8. [The Counterfactual Engine](#8-the-counterfactual-engine)
9. [Data and User Input](#9-data-and-user-input)
10. [Screens and UX](#10-screens-and-ux)
11. [Design System](#11-design-system)
12. [Architecture and Tech Stack](#12-architecture-and-tech-stack)
13. [Privacy and Hard Rules](#13-privacy-and-hard-rules)
14. [Cold Start and Demo Strategy](#14-cold-start-and-demo-strategy)
15. [What We Can and Cannot Claim](#15-what-we-can-and-cannot-claim)
16. [Risks and Mitigations](#16-risks-and-mitigations)
17. [Build, Install and Test](#17-build-install-and-test)
18. [Scope and Roadmap](#18-scope-and-roadmap)
19. [Inspirations](#19-inspirations)

---

## 1. The Problem

Everyone's attention works differently, but today's tools treat everyone the same:

- **Do Not Disturb and Focus Modes** are blunt switches. They block too much or too little.
- **Screen-time dashboards** report what already happened, after the damage is done.
- **Notification rankers** score importance but ignore what an interruption *costs you* in your current state.

Nobody answers the question that matters:

> **"If I let this notification through right now, what is likely to happen to me?"**

## 2. The Idea

Attention Twin builds a personal model of your attention and simulates two futures for any notification:

| | Predicted chain |
|---|---|
| **If shown** | Interruption → you pick up your phone → browsing → delay before you resume your task |
| **If held (counterfactual)** | The notification waits for the next digest slot. You keep working, with a small chance you pick up your phone anyway |

During a **Focus Session**, the twin acts on that prediction: it either **shows** the notification or **holds** it. A held notification is snoozed until the next 15-minute digest slot (at most 30 minutes) and returns **intact**. Every decision gets a **receipt** with a one-line reason and the numbers behind it.

```
Notification posted
   → Gate 0: is Focus on?
   → Gate 1: is it protected (call, alarm, OTP, chat, the app you're in…)?
   → Learned importance (per channel, with an optimistic bound)
   → Expected cost: show vs. hold, in phone minutes
   → SHOW or HOLD → receipt → measure the outcome → learn
```

## 3. What It Is, and What It Is Not

**It is** a metadata-only attention assistant. It sees the app, the channel name, the category, the timing and how you react. It learns on your phone, from your behavior and your feedback.

**It is not:**
- A reader of your messages. It never accesses notification text, titles or senders.
- A spam or OTP detector. It protects *classes* of notifications by rule and learns the rest from behavior.
- A measurement of focus, fatigue or cognition. Those are **proxies**, and the UI labels them as proxies.
- A way to block notifications before they arrive. The listener acts after posting, using Android's snooze.

## 4. Why This Is Different

| Category | Examples | What they do | What they miss |
|---|---|---|---|
| App blockers and focus tools | Opal, Digital Wellbeing, ScreenZen, Forest | Block apps, set limits, score screen time | Rule-based and reactive. They never predict the cost of a *specific* notification. |
| Notification rankers | Gmail priority notifications, AI inboxes | Score importance | Don't model *your state* or the cost of an interruption. |
| Research prototypes | PrefMiner and similar | Predict notification engagement | Not a product, no hold action, no counterfactuals. |
| **Attention Twin** | n/a | **Predicts the consequence chain and a held-vs-shown counterfactual, then acts and shows its work** | n/a |

**Pitch line:** *Opal shows you your focus. Your twin predicts and protects it, and shows you the receipts.*

---

## 5. Problem Statement Coverage

| # | Requirement from the brief | How Attention Twin delivers it (and what's a proxy) |
|---|---|---|
| 1 | When the user **focuses best** | **Best focus windows:** the three hours with the longest average unlock-free stretch, computed from unlock events |
| 2 | What causes **interruptions** | Each notification's 10-minute outcome window records unlocks, clicks and phone minutes. Triggers are ranked by phone minutes caused per notification |
| 3 | What content causes **prolonged sessions** | Per-channel and per-app-group "pull minutes" (expected phone time after an engaged notification), learned on-device |
| 4 | Which notifications **actually matter** | A per-channel Beta importance estimate, updated from clicks, dismissals and your *Mattered / Didn't matter / Needed it sooner* feedback |
| 5 | When the user is **mentally fatigued** | A **fatigue proxy** (labeled as such): longest unbroken screen interval today, late-hour flag, unlocks per hour. Not a measurement |
| 6 | Which apps **fragment concentration** | **Fragmentation ranking:** engaged notifications × average pull minutes, by app |
| 7 | Simulate "**If I allow this now, what happens?**" | The **Twin (What-if)** screen computes both worlds live from your current context |
| 8 | A multi-step outcome (interruption → app → browsing → delay) | A three-segment chain: *Interruption → Browsing → Resumption delay* |
| 9 | **Suppress** the interruption | HOLD via `snoozeNotification` until the next digest slot, with a receipt |
| 10 | **Counterfactual** predictions | A personal control model, a held-vs-shown What-if and an opt-in randomized-hold estimator with a confidence interval ([Section 8](#8-the-counterfactual-engine)) |

---

## 6. Features

> **Status legend:** ✅ in the build plan · 🔶 optional or stretch · 🔭 future work

### 6.1 Notification Triage

| Feature | Description | Status |
|---|---|---|
| **SHOW / HOLD decision** | Per notification, using metadata only. HOLD snoozes to the next digest slot and the original returns unchanged | ✅ |
| **Hard protection rules (Gate 1)** | Calls, alarms, reminders, navigation, transport, ongoing and foreground-service notifications, "bypass DND" channels, default SMS, authenticators, security and payment keyword channels, chats, and any notification from the app you are currently in. Never model-driven | ✅ |
| **Learned importance** | Per-channel Beta distribution, with an optimistic upper bound. A channel is held only if even the optimistic bound is low | ✅ |
| **Expected-cost decision** | Compares the phone-minute cost of showing vs. holding, with strictness-dependent thresholds | ✅ |
| **Digest slots** | Held items return at the next 15-minute slot (max 30 min), with the original notification intact | ✅ |
| **Strictness levels** | Gentle / Balanced / Strict | ✅ |
| **Modes** | **Snooze** (acts), **Shadow** (logs "would hold" only), **DND-only** | ✅ |
| **Unknown means SHOW** | Any error, disconnect or low confidence leaves the notification alone | ✅ |
| **Cold-start protection** | A channel with fewer than 3 observations is always shown while the twin learns | ✅ |
| **Never-hold list** | User-chosen protected apps, pre-ticked with sensible defaults at onboarding | ✅ |
| **Silence while focused** | Optional Do-Not-Disturb integration during a Focus Session | 🔶 |

### 6.2 Simulation and Counterfactuals

| Feature | Description | Status |
|---|---|---|
| **What-if simulator (Twin screen)** | Pick a channel and a phone state (Locked / In task app / In other app), adjust the context sliders, and see both worlds update instantly | ✅ |
| **Outcome chain** | Interruption → Browsing → Resumption delay, with expected minutes | ✅ |
| **Harm estimate with range** | `cost_show − cost_hold` in minutes, with an approximate 80% range | ✅ |
| **Personal control model** | Learned on-device from your "locked phone, no notification" moments | ✅ |
| **Randomized-hold experiments** | Opt-in. Eligible low-importance notifications are randomly shown or held with logged propensities | 🔶 |
| **Causal estimate (IPW + bootstrap CI)** | Shown only with enough randomized data (n ≥ 30 per arm) | 🔶 |

### 6.3 Learning (all on-device)

| Feature | Description | Status |
|---|---|---|
| **Hand-set prior models** | Clearly labeled **PRIOR, not trained**. Shipped as JSON so a trained model can be dropped in later without code changes | ✅ |
| **Per-channel statistics** | Importance (Beta), pull minutes, switch probability, engagement bias | ✅ |
| **Outcome tracking** | A 10-minute window after each notification: unlocks, phone minutes, click / dismiss | ✅ |
| **User feedback loop** | *Mattered*, *Didn't matter* and *Needed it sooner* (which protects the channel immediately) | ✅ |
| **Pause learning** | One toggle. Decisions stay safe | ✅ |

### 6.4 Transparency and Trust

| Feature | Description | Status |
|---|---|---|
| **Receipts for every decision** | One-line reason with numbers, gate, mode, enforcement result, the full lifecycle (posted → held → released → reaction → outcome) | ✅ |
| **Evidence badges** | `PRIOR` · `PILOT` · `SIMULATED` · `OFFLINE`: every number says where it came from | ✅ |
| **Held inbox** | Everything currently held, with live countdowns. Nothing is lost | ✅ |
| **Enforcement verification** | A hold only counts if Android confirms the snooze. Failures fall back to SHOW | ✅ |
| **"Missed protected" counter** | Must always be 0, and is shown on the Evidence screen | ✅ |
| **Full data control** | CSV export (no content), delete all data in one tap | ✅ |

### 6.5 Insights

| Feature | Description | Status |
|---|---|---|
| **Notifications by hour** | 24-bar chart | ✅ |
| **Top channels by phone minutes triggered** | Ranked list | ✅ |
| **Fragmentation ranking** | Apps by engaged notifications × pull minutes | ✅ |
| **Channel table** | Importance, optimistic bound, n, hold rate, pull minutes | ✅ |
| **Best focus windows** | From unlock-free stretches | ✅ |
| **Fatigue proxy chip** | With its three inputs shown | ✅ |
| **Rule for every chart** | Show the sample size, and hide the chart with "Not enough data yet (n=…)" below n = 10 | ✅ |

### 6.6 Visual Identity

| Feature | Description | Status |
|---|---|---|
| **Twin Orb** | A living orb whose size reflects the chance you'd pick up your phone for a typical notification, whose color shifts cyan → amber with notification density, and which has one orbiting dot per recent notification | ✅ |
| **Focus toggle** | A large pill with haptic feedback | ✅ |
| **Status chips** | Listener, Usage, DND and Mode, each with icon + word | ✅ |
| **Learning banner** | "Twin is learning you: 23 / 60 notifications" | ✅ |

### 6.7 Built-in Simulator (for testing and the demo)

| Feature | Description | Status |
|---|---|---|
| **Six test channels** | Messages, Promotions, Likes and comments, Security alerts, News, Order updates | ✅ |
| **Post one / burst of 8 / post in 10 s** | Lets the tester lock the phone first | ✅ |
| **Force-locked debug switch** | Treats simulator notifications as arriving on a locked phone | ✅ |
| **Demo data seeding** | Rows tagged `source='sim'`, badged `SIMULATED`, excluded from Evidence unless the user opts in | ✅ |

### 6.8 Future Work (not in the current build)

🔭 Health Connect (sleep / heart rate) · calendar awareness · activity recognition and location context · trained models from a notification dataset · streaks, challenges and widgets · app timers, bedtime mode and Deep-Focus friction.

---

## 7. How the Twin Decides

```
onNotificationPosted
 ├─ Gate 0   Focus off?                      → SHOW (learn only)
 ├─ Gate 1   Protected class?                → SHOW (rule, never a model output)
 ├─ Cold start (< 3 observations)?           → SHOW
 ├─ Importance: Beta(a, b)  →  mean and optimistic bound (mean + 1.28 sd)
 ├─ Costs (in expected phone minutes)
 │     Locked phone:   cost_show = P(engage | shown) × pull minutes
 │                     cost_hold = P(pickup anyway)  × spontaneous-pickup minutes
 │     Unlocked:       cost_show = P(switch) × (pull + resume penalty) × context multiplier
 │                     cost_hold = small constant
 ├─ benefit = cost_show − cost_hold
 └─ HOLD only if  optimistic importance < τ  AND  benefit > δ
```

**Strictness presets**

| Preset | τ (importance threshold) | δ (min benefit, minutes) |
|---|---|---|
| Gentle | 0.25 | 0.50 |
| Balanced | 0.35 | 0.25 |
| Strict | 0.50 | 0.10 |

If usage access isn't granted, the twin runs in **limited mode** with a stricter threshold (τ capped at 0.25) and holds less.

**Receipts read like this:**
- HOLD: *"Low importance (≤ 0.18 optimistic); showing now ≈ +2.1 min phone time vs waiting."*
- SHOW (low benefit): *"Low expected cost (0.3 min); not worth holding."*
- SHOW (might matter): *"Might matter (importance up to 0.62); showing."*
- SHOW (protected): *"Protected: security channel."*

---

## 8. The Counterfactual Engine

We never observe what would have happened *without* a notification, so we answer it at three levels of evidence, each labeled honestly in the UI:

1. **Personal control model (`PRIOR` → `PILOT`).** Every 15 minutes, if your screen was off and no notification arrived in the previous 10 minutes, the twin records whether you picked up your phone in the next 10 minutes and how many phone minutes followed. This is your *spontaneous* pickup rate by time-of-day block, blended with a prior. It's the baseline for the "held world".
2. **Held-vs-shown What-if (`PRIOR` → `PILOT`).** The Twin screen computes both worlds from your current context using the same functions as the live engine, so the UI and the decisions can never disagree.
3. **Randomized holds (`PILOT`, opt-in).** For eligible low-importance notifications, the twin randomizes show vs. hold with a logged propensity ε. The held-vs-shown difference in phone minutes is then estimated with inverse-propensity weighting, with a 400-sample bootstrap confidence interval. It is displayed **only** when n ≥ 30 in each arm. Otherwise the app says "not enough randomized data yet". Protected classes are never randomized.

> *"If this had been shown: expected +4.2 min of phone time. Held world: +0.9 min. Harm ≈ 3.3 min (approximate 80% range 1.8 to 4.8)."*

---

## 9. Data and User Input

### 9.1 Passive signals (metadata only)

| Signal | Android API | Used for |
|---|---|---|
| App, channel id and name, category, importance, ongoing / conversation flags, whether it has a reply action | `NotificationListenerService` ranking and flags | Triage and importance |
| Posted / removed events and the removal reason (click, cancel, snoozed) | `onNotificationPosted` / `onNotificationRemoved` | Outcomes and enforcement verification |
| Screen on/off and unlock events | Dynamic `BroadcastReceiver` | Phone minutes, pickups, focus windows |
| Lock state | `KeyguardManager` | Phone state |
| Foreground app (optional) | `UsageStatsManager` | In-source-app protection, task-app state |
| Default SMS app, authenticator apps | `Telephony`, package-name rules | Protection |

### 9.2 What we never read

Notification text, titles, big text, message lists, sub-text, summary text and ticker text. A Gradle task (`checkNoContentReads`) **fails the build** if any of those constants appears in the source.

### 9.3 Active input

- **Focus toggle** (the one big switch).
- **Held-item feedback:** *Mattered*, *Didn't matter*, *Needed it sooner*.
- **Onboarding protect list** and settings (strictness, max hold, digest interval, mode).
- **Optional:** opt in to randomized-hold experiments.

---

## 10. Screens and UX

Bottom navigation: **Now · Held · Twin · Receipts · More** (More → Insights, Evidence, Settings, Simulator, Privacy).

| Screen | What the user sees |
|---|---|
| **Onboarding** | Three cards: *What we see / What we never see / What we may do*, then one permission per screen (Notification access → Usage access → Post notifications → Battery → optional DND access) that auto-advances when granted, then the **Protect list** |
| **Now** | Status chips, the **Twin Orb** with a plain-language caption, the **Focus toggle**, three counters (Shown / Held / Protected today), a "Right now" context card, and a learning banner. A red "Fix" card appears if the listener disconnects |
| **Held** | Currently held notifications with live countdowns (no content shown), and "Recently released" rows with the three feedback buttons |
| ⭐ **Twin (What-if)** | Context controls, then two lanes (*If shown* / *If held*), the three-segment chain, the harm estimate with its range bar, importance gauges against the threshold, and the counterfactual panel with its evidence badge. Changing the phone state visibly flips the decision |
| **Receipts** | Filterable list (All / Held / Shown / Protected / Failed). Expanding a row shows the lifecycle, the numbers, the gate, the mode, enforcement and the outcome |
| **Insights** | The charts in [Section 6.5](#65-insights), each with its sample size |
| **Evidence** | Model status, the on-device pilot numbers, and the counterfactual estimate |
| **Settings** | Strictness, max hold, digest interval, mode, experiments opt-in, "allow holding chats" (default off), never-hold list, pause learning, include demo data, CSV export, **Delete all data** |
| **Simulator** | The six test channels, burst and delayed posting, and demo-data seeding |
| **Privacy** | Plain-language statements that are literally true, plus why each permission is requested |

**Signature moment:** flip the What-if phone state from *Locked* to *In task app*, and watch the harm number, the lanes and the HOLD / SHOW decision change together.

---

## 11. Design System

- **Dark only**, with glass-style cards: a vertical white-alpha gradient, a hairline border, 20 dp corners.
- **Palette:** background `#0B0F1A` · surface `#121826` · cyan `#22D3EE` (primary) · green `#34D399` (held) · coral `#F87171` (shown) · blue `#60A5FA` (protected) · amber `#FBBF24` (learning / prior).
- **Colour is never the only signal.** Every state is an icon plus a label: `HELD`, `SHOWN`, `PROTECTED`, `LEARNING`.
- **Typography:** 30 sp semibold headlines, tabular numerals for all figures, system font scaling respected.
- **Accessibility:** touch targets of at least 48 dp, content descriptions on all icons, and a reduce-motion path that freezes the orb animation.
- **Charts:** custom Compose `Canvas` (bar chart, ranked bar list, range bar). No chart library.
- **Tone:** supportive and plain-language, with no shaming.

---

## 12. Architecture and Tech Stack

```
┌──────────────────────── Android app, one process, fully on-device ────────────────────────┐
│ UI (Jetpack Compose, Material 3, Navigation)                                              │
│   Now · Held · Twin · Receipts · Insights · Evidence · Settings · Simulator · Onboarding  │
│                          │ StateFlow / Room Flow                                          │
│ Graph (in-process singleton shared by UI and listener)                                    │
│   Engine ── Policy (pure Kotlin, unit-tested) ── ModelStore (JSON priors)                 │
│   ContextTracker ── OutcomeTracker ── CtrlTracker ── Effect (IPW + bootstrap)             │
│ AttentionListener (NotificationListenerService)  →  snoozeNotification                    │
│ Room (SQLite): events · decisions · outcomes · channel_stats · screen_intervals ·         │
│                unlock_events · ctrl_probes   (no title/text/sender columns)               │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

| Layer | Choice |
|---|---|
| Language / UI | Native **Kotlin + Jetpack Compose (Material 3)**. No Flutter, no bridge |
| SDK | minSdk 26 · targetSdk 34 · compileSdk 34 · JVM 17 |
| Storage | Room for data, `SharedPreferences` for settings |
| Models | Hand-set logistic priors in `assets/models/*.json`, a Beta importance estimate per channel, and a personal control model learned on-device |
| Background | `NotificationListenerService` with `requestRebind` on disconnect, plus a battery-optimization exemption flow |
| Charts | Custom Compose `Canvas` |
| Performance | The decision path is in-memory only and targets well under 50 ms. All database writes are asynchronous |

---

## 13. Privacy and Hard Rules

These rules are enforced in the build, not just in documentation:

1. **Never read notification text.** The build fails if any content-extra constant appears in `src/main`.
2. **No `INTERNET` permission** in the release APK. It's stripped with `tools:node="remove"` and verified with `aapt dump permissions`.
3. **Unknown means SHOW.** Any exception, missing data, disconnect or low confidence leaves the notification alone.
4. **Protected classes are never held.** That's a rule, never a model output.
5. **Every decision writes a receipt.**
6. **Simulated data is badged** (`source='sim'`) and excluded from Evidence unless the user opts in.
7. **No overclaiming** (see [Section 15](#15-what-we-can-and-cannot-claim)).

Also: backups are disabled, no content is ever stored, and **Delete all data** wipes every table, the in-memory caches and the preferences.

---

## 14. Cold Start and Demo Strategy

**Real use:** the app ships with hand-set priors, labeled `PRIOR`. It shows new channels while learning (fewer than 3 observations), shows a visible learning progress bar, and replaces assumptions with your own measurements as data accumulates.

**Hackathon demo:**
- The six simulator channels are **pre-seeded** with 10 observations each, so the demo isn't stuck in cold start.
- The **force-locked** debug switch lets the demo work while the app is open.
- Expected behavior in **Strict** mode: **Promotions** and **Likes** are held, **Security alerts** is protected by keyword, **Messages** is protected as a conversation.

**Suggested 3-minute demo:**
1. **Now:** the orb, the status chips and the learning banner.
2. **Twin:** change the phone state and watch the harm and the decision flip. Point out the evidence badge ("PRIOR: assumptions, labeled").
3. Turn **Focus ON** and choose **Strict**.
4. **Simulator:** "post in 10 s" for Promotions, lock the phone, and watch it disappear.
5. Post **Security alerts** and **Messages**. Both come straight through.
6. **Held:** the countdown is running. Tap **Needed it sooner** and the channel becomes protected.
7. **Receipts:** open the held receipt and show the lifecycle ending in `SNOOZED → released`.
8. **Evidence:** "missed protected = 0", enforcement success and the honest "not enough randomized data yet".

---

## 15. What We Can and Cannot Claim

| We can say | We cannot say |
|---|---|
| "We never read message text, and the app has no internet permission." | "We detect OTPs or spam." (We protect *classes* of notifications and learn from behavior.) |
| "Every hold is reversible, capped at N minutes, and has a receipt." | "Blocked before it arrives." (The listener acts after posting.) |
| "The twin starts from stated assumptions and personalizes on-device. It labels what's assumed and what's measured." | Any accuracy or AUC figure, unless a trained model with metrics is actually shipped. |
| "With randomized holds, the held-vs-shown difference in phone minutes is estimated with a confidence interval." | A causal effect without enough randomized data, or any number from simulated rows. |
| "Importance is learned from your behavior and your feedback." | "It knows what's important" or "engagement equals importance". |
| "Phone minutes after a notification is our attention-cost proxy." | "We measure focus, fatigue or cognition." |

*Related research for context (not our numbers):* notification engagement can be predicted from phone-use context (Pielot et al.), importance can't be derived from interaction alone (hence the feedback buttons and protection rules), and deferring notifications is an established design.

---

## 16. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Wrongly holding something important | Gate 1 protection rules, optimistic-bound decisions, cold-start SHOW, held inbox, **Needed it sooner** with instant protection, max-hold cap |
| Snooze doesn't behave on a given OEM | **Phase gate:** test on the target phone first. If it fails, default to **Shadow mode**, or **DND-only** if access is granted |
| Listener killed by battery optimization | Rebind on disconnect, battery-exemption onboarding step and OEM-specific guidance (e.g. Samsung "Never sleeping apps"). Disconnected state is shown in red |
| Sensitive permissions | Plain-language explanations, no internet, metadata only, sideloaded APK |
| Sparse data early | Priors, labeled honestly, with a visible learning banner and pre-seeded simulator channels |
| Counterfactuals are unobservable | Personal control model, randomized holds with logged propensities, and a refusal to show an estimate without enough data |
| Re-posted or updated held notifications | A held-key map prevents re-hold loops |
| Prediction overconfidence | Ranges instead of point estimates, and evidence badges |

---

## 17. Build, Install and Test

```bash
# Unit tests (policy engine, protection rules, digest slot alignment)
./gradlew :app:testDebugUnitTest

# Release APK (debug-signed is fine for the hackathon)
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Verify there is no INTERNET permission
aapt dump permissions app/build/outputs/apk/release/app-release.apk

# Install
adb install -r app/build/outputs/apk/release/app-release.apk

# Optional tester shortcuts
adb shell cmd notification allow_listener com.cascade.attentiontwin/com.cascade.attentiontwin.AttentionListener
adb shell appops set com.cascade.attentiontwin GET_USAGE_STATS allow
adb logcat -s TwinListener
```

**Build guard:** `./gradlew checkNoContentReads` runs before every build and fails if notification-content constants appear in the source.

**Key tests**

| Test | Expected |
|---|---|
| Focus ON, Strict, post Promotions with the phone locked | HELD, disappears, returns at the next slot. Receipt shows enforcement applied → `REASON_SNOOZED` → released |
| Post Security alerts | SHOWN, gate PROTECTED (keyword) |
| Post Messages | SHOWN, protected (conversation) |
| Burst of 8 promotions | Held and returned together, with no re-hold loop |
| Focus OFF | No enforcement, receipts still written |
| Revoke notification access | Red status, no crash, nothing held |
| Open an app, then trigger its notification | SHOWN (in source app) |
| *Needed it sooner* | Channel protected immediately |
| Snooze throws | Enforcement `failed`, notification stays visible |
| What-if: Locked vs. In task app | Harm and decision change visibly |
| Airplane mode | Identical behavior |
| `aapt dump permissions` | No `INTERNET` |

---

## 18. Scope and Roadmap

**Build order**
1. Scaffold, manifest and the no-content guard task
2. Data model, settings, app groups, prior models and the **Policy** with unit tests
3. Context tracker, engine, listener and outcome tracking (verified on an emulator with the simulator)
4. Design system, then Now, Held, Receipts, Settings and Onboarding
5. Twin (What-if), Insights, Evidence and Simulator polish
6. Release build, verification and docs

**If time is short, cut in this order:** Insights polish → randomized-hold experiments UI → CSV export → DND-only → schedules.
**Never cut:** Gate 1 protection, receipts, snooze verification, Delete all data, honest evidence badges and the Twin What-if screen.

**Later:** drop in a model trained on a notification dataset (the PRIOR banner disappears automatically), then the future-work items in [Section 6.8](#68-future-work-not-in-the-current-build).

---

## 19. Inspirations

| Reference | What we took |
|---|---|
| **Opal** | A hero visual on the home screen, focus sessions as the core interaction, and clear daily counters. *Not adopted for now:* Deep Focus friction, app timers and leaderboards |
| **Google Digital Wellbeing** | Per-app notification and usage thinking, Focus Mode as the mental model, and the notification-mailbox idea (our Held inbox) |
| **Forest** | Emotional, living feedback. The Twin Orb |
| **Whoop / Oura** | Plain-language captions under big visual gauges |
| **RescueTime** | "Best focus windows" thinking |
| **Gmail priority notifications / AI inboxes** | Learned importance and digest bundling |
| **Research:** PrefMiner, Pielot et al., snooze studies | Evidence that on-device notification prediction is feasible, and that explicit feedback is needed for importance |
| **Open-source references (patterns only, check licenses)** | `eret9616/HideOngoing` (snooze handling), `PinetheApple/notif-filter` (listener resilience, battery flow) |

> **What none of them do:** predict the consequence chain of a *specific* notification, compare it with a counterfactual, and show a receipt for every decision. That's Attention Twin's contribution.

---

## 20. Related Work & How Attention Twin Differs

While existing mobile wellbeing and notification research has explored parts of the attention problem, Attention Twin is architected around four distinct differentiators:

1. **Cost-of-Interruption Model vs Static Priority**:
   Prior work (e.g., SmartNotify, PrefMiner) ranks notifications by static priority levels (LOW/MEDIUM/HIGH) or predicts attendance alone (Pielot et al.). Attention Twin instead evaluates an **expected-cost tradeoff in phone minutes**: comparing what an interruption costs right now given user state (locked vs in active task app with resumption penalties) versus the cost of holding it until the next digest slot.
2. **Counterfactual Prediction**:
   Attention Twin simulates both futures live—the shown world and the held counterfactual world—and computes the attention harm saved (`cost_show − cost_hold`). With opt-in randomized holds, it estimates causal harm via Inverse Propensity Weighting (IPW) and bootstrap confidence intervals.
3. **Auditable Receipts for Every Decision**:
   Unlike black-box notification filters or silent DND schedulers, Attention Twin writes an immutable, auditable receipt for every decision with its one-line mathematical rationale, gating state, and complete lifecycle trace (`posted → snoozed → released → outcome`).
4. **100% On-Device & Zero-Content Inspection**:
   In stark contrast to cloud-based LLM triage systems (e.g., Notification-HQ) that ingest sensitive conversation bodies, Attention Twin operates without network access (`INTERNET` permission removed), never inspects message titles or text, and performs all Bayesian and SGD updates locally on the device.

---

<div align="center">

**Built for Cascade '26 · The Attention Digital Twin**

*Don't just track your attention. Simulate it, and show your work.*

</div>
