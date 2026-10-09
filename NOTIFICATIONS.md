# Notifications

Ported from TranslatorGuru's `xtech` notification system (lock-screen engine + Figma templates), adapted to
this app's own AdMob code, premium subscription and dashboard. Code: `app/src/main/java/org/dslul/openboard/translator/pro/notifications/`.

## Notifications

| id | what | trigger | channel | opens |
|---|---|---|---|---|
| 2001 | Lock-screen notification (Figma "Lockscreen 1-3"), wakes the screen 20 s | slots `noti_lockscreen_slots(_new)` | `lockscreen` HIGH, public | camera / voice / phrasebook / translate |
| 2002 | Win-Back "Travelling soon?" | slots `noti_winback_slots`, +1 re-send 30 min later if the app was not opened | `updates_news` HIGH | dashboard |
| 2003 | Clicked-Ads "Before you go" | an ad click, then the app left for 3 s | `updates_news` HIGH | dashboard |
| 2004 | On-open reminder "Translate on the go!" | app opened (15-min throttle) | `reminders` LOW | dashboard |
| 2005 | Daily "Phrase of the day" | slots `noti_daily_slots` | `daily_tips` DEFAULT | Translate screen, translated and read aloud |
| 2007 | Kill-App "Point and read anything" | app swiped from Recents (24 h cooldown) | `updates_news` HIGH | camera tab |
| 1234 | Pinned Quick Access card (4 tiles) | splash / dashboard after first open; Settings switch | `quick_access` LOW, ongoing | Translate, Voice, Camera, File |

Marketing notifications (2001, 2002, 2003, 2005, 2007) skip purchased users unless `notiForPremiumUsers` is true.
Win-Back and Daily also skip while the app is on screen.

## Flow

- Tap → `NotiRouterActivity`. App on screen → the feature directly. Otherwise → `PreSplashScreenActivity` with
  `notiTarget`: splash app-open ad, then straight to the feature (no premium / language screens). During first
  open the target is dropped and onboarding runs.
- Camera, conversation and phrasebook open as **dashboard tabs** (`FragmentsDashboardActivity.EXTRA_OPEN_TAB`), so the
  camera/chat premium lock (upgrade or rewarded ad) still applies.
- Notification entries and pinned tiles skip TranslateActivity's entry interstitial (`EXTRA_SKIP_ENTRY_AD`).
- `NotiRouterActivity` and `QuickAccessActivity` (transparent trampolines) are excluded from the resume app-open
  ad (`App.onMoveToForeground`).
- **No full-screen intents.** xtech's full-screen lock-screen card (`LockscreenCardActivity`) was removed on
  2026-10-07: `USE_FULL_SCREEN_INTENT` requires the Play Console full-screen-intent declaration, which is for
  calling/alarm apps. The manifest strips that permission with `tools:node="remove"`, so no library can add it back.
- The keyboard runs in this process, so `App.onCreate` (channels, config load, alarm scheduling) also runs whenever
  the keyboard starts.
- The app asks for POST_NOTIFICATIONS once, on the first dashboard visit (it never asked before).

## Remote Config keys

Read in `PreSplashScreenActivity.getRemoteConfigValues()` → `NotiConfig.applyFrom`, persisted to `noti_config`
prefs for alarm/keyboard-started processes. Only keys present on the remote are applied.

| key | default |
|---|---|
| `noti_lockscreen_slots` / `noti_lockscreen_slots_new` | `11:30,17:00,20:00` |
| `notiLockscreenReplace` | `false` (skip while the previous one shows) |
| `noti_lockscreen_wake_seconds` | `20` |
| `noti_lockscreen_content` | `""` — JSON `[{id,title,desc,button,target}]` replaces the bundled rotation |
| `noti_new_user_days` | `2` |
| `notiForPremiumUsers` | `false` |
| `noti_winback_slots` | `09:00,11:00,18:00` |
| `noti_daily_slots` | `08:00,19:00` |
| `noti_kill_app_cooldown_hours` | `24` |
| `notiLockscreenEnabled` `notiWinbackEnabled` `notiDailyEnabled` `notiClickedAdsEnabled` `notiKillAppEnabled` `notiOnOpenEnabled` `notiShortcutsEnabled` | `true` |
| `notiUninstallShortcutEnabled` (the red Uninstall shortcut) `notiWidgetPromptEnabled` (the "Add widget" card) | `true` |

## QA (debug builds)

```
adb shell am broadcast -n com.translator.keyboard.translate.all.langugaes/org.dslul.openboard.translator.pro.notifications.DebugNotiReceiver -a com.translatorpro.debug.FIRE_NOTI --es type lockscreen
```

Types: `lockscreen winback winbackFollowUp daily clickedads killapp onopen pinned reschedule replaceOn replaceOff`.
Lock the phone (screen off) before `lockscreen` to see the screen wake.

## Play policy notes

- No full-screen-intent declaration needed: the app does not declare `USE_FULL_SCREEN_INTENT`.
- Forced screen wake (20 s) and the ad-click retargeting notification are part of the client spec.
- The **"Uninstall" long-press shortcut** that opens the app (survey) instead of uninstalling is a deceptive-pattern
  risk. It ships enabled per client spec, but keeps a genuine escape hatch (the survey's "Uninstall anyway" fires
  the real system uninstaller) and has a remote kill switch (`notiUninstallShortcutEnabled`).

## Long-press shortcuts & widget

- **App-icon long-press shortcuts** (`notifications/AppShortcuts`): Camera, Voice chat, and a red **Uninstall**
  entry. Each routes through the splash like a notification tap. The whole set is behind
  `notiShortcutsEnabled`; the Uninstall entry has its own switch (`notiUninstallShortcutEnabled`).
  - Uninstall opens `UninstallSurveyActivity` — a retention survey (reasons, a "you'll be leaving behind" feature
    reminder, a shared-bottom native ad), with **Keep the app** and an **Uninstall anyway** link that fires the
    *real* system uninstaller (`ACTION_DELETE`). It is excluded from the resume app-open ad.
- **Home-screen widget** (`widget/TranslateWidgetProvider`, `widget_translate.xml`): a Quick-Translate pill +
  mic + camera. Declaring the provider is also what puts the **"Widgets"** entry in the long-press menu. Each tile
  routes through `NotiRouterActivity.widgetPendingIntent` (source `widget`) and the splash.
- **"Add widget" prompt** (`widget/WidgetPromptDialog`, behind `notiWidgetPromptEnabled`): a frosted-glass card on
  the dashboard with the three manual steps and a native ad. Armed on the first dashboard visit, shown on a later
  one (so it never stacks on the POST_NOTIFICATIONS dialog); **Got it** opens the system pin dialog
  (`requestPinAppWidget`) directly. Unlike xtech, openboard does **not** gate the pin behind a rewarded ad.

## Not ported

- xtech's FCM changes: this app's `CustomMessagingService` is not registered in the manifest and never subscribes to
  a topic, so FCM is inactive here.
- The full-screen lock-screen card (see Flow).
