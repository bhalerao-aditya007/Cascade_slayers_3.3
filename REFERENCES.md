# References and Prior Art Analysis

This document analyzes the open-source reference repositories and research literature that informed Attention Twin's architecture.

---

## 1. Open Source Reference Repositories

| # | Repository | Licence | Pattern Learned | Deliberately NOT Copied |
|---|---|---|---|---|
| 1 | `PinetheApple/notif-filter` | MIT | Listener resilience, battery-optimization exemption flow (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`), and local event history store. | Static keyword-only blacklists. Attention Twin replaces heuristic filtering with a probabilistic cost-of-interruption model and auditable receipts. |
| 2 | `eret9616/HideOngoing` | MIT | Kotlin + Compose structure using `snoozeNotification`, key tracking, and managing the returning notification. | Suppressing ongoing/system notifications. In Attention Twin, ongoing notifications and foreground services are hard-protected (Gate 1). |
| 3 | `NotiFilter` (`co.adityarajput.notifilter`) | GPL-3.0 | Digest duration alignment, batching, debounce actions, and offline-only storage. | Copied zero code due to GPL-3.0 licence. Did not adopt fixed schedule delays without attention-cost estimation. |
| 4 | `MrLaki5/notification-prune` | MIT | Debounce concept: burst of 8 promotional notifications should be batched and returned as one digest slot, rather than 8 re-holds or re-rings. | Sound/audio manager muting hacks. We use Android's official system snooze. |
| 5 | `rm-Umar/faarigh` | Apache-2.0 | Material 3 Jetpack Compose layout, `UsageStatsManager` query events scan, and wellbeing UI structuring. | Did NOT adopt its `AccessibilityService` approach. Attention Twin strictly avoids accessibility spying, relying only on standard metadata APIs. |
| 6 | `BoardPandas/notification-hub` | MIT | Clean Kotlin, Compose M3, Room DB, and ViewModel + Flow reactive streams surrounding `NotificationListenerService`. | Content storage. Notification-hub stores titles and text; Attention Twin strictly enforces zero content access via compiler checks. |
| 7 | `thomasjbarrett82/ExtraLifeNotifications` | MIT | `requestRebind(ComponentName)` reconnect pattern on `onListenerDisconnected()`. | Notification content parsing and custom foreground services. |
| 8 | `fjsr3125/DopWatch` | MIT | UsageStats aggregation with Room and DataStore. | Network telemetry, server synchronization, and external analytics SDKs. |
| 9 | `prasannanaidukalva-alt/NotificationManagmentSystem` (SmartNotify) | MIT | Reference for existing classification methods. | Static LOW/MEDIUM/HIGH classifier. SmartNotify has no user state model, no cost-of-interruption calculation, no counterfactual, and no receipts. |
| 10 | `Notification-HQ` | Closed / Unlicensed | Analyzed as an architectural antithesis. | Cloud-based LLM analysis of notification text. Attention Twin uses 0% cloud, 0% content inspection, and has no `INTERNET` permission. |
| 11 | `Alfio010/notification-listener-android` | GPL-3.0 | Basic `NotificationListenerService` lifecycle handling. | Zero code copied due to GPL-3.0 licence. |

---

## 2. Scientific Research & Foundations

Attention Twin is built upon empirical findings from mobile human-computer interaction (HCI) research:

- **Pielot et al., "Continual Prediction of Notification Attendance"**:
  - *Key Finding*: Users typically attend to incoming notifications within 10 minutes if they are going to attend at all.
  - *Adoption*: Defined Attention Twin's 10-minute outcome observation window (`postTs + 10m`) for measuring engagement, unlock counts, and session phone minutes.
- **InterruptMe (Pejovic & Musolesi)**:
  - *Key Finding*: Interruption cost varies dynamically based on user activity, phone state (locked vs active app), and time of day.
  - *Adoption*: Dual-state expected cost model: locked logistic model vs unlocked context-switching model with resumption penalties.
- **Turner et al., "Decomposing responses to mobile notifications"**:
  - *Key Finding*: High variance exists between notification categories; promotional notifications cause high resumption delays, whereas messaging has lower pull variance.
  - *Adoption*: Prior Beta distributions per group and group-specific pull minute priors (`POOL_PRIOR`).
- **PrefMiner (Mehrotra et al.)**:
  - *Key Finding*: Users are willing to defer non-urgent notifications if they are assured critical communications are never delayed.
  - *Adoption*: Hard Gate 1 protection rules that are never overridden by learned models, and 15-minute capped digest slots.
