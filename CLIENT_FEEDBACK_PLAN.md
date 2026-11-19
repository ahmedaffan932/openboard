# Client Feedback Implementation Plan (sheet rows 2–20)

**App:** "AI Translator" keyboard and translator (`com.translator.keyboard.translate.all.langugaes`), currently versionCode 16 / versionName 2.5.
**Input:** the client's "Update Points" sheet, rows 2–20.
**Status:** this is analysis and a plan only. Nothing has been implemented yet.
**Baseline:** `master` at `805ae50`. The working tree also has about 23 uncommitted files of AppAnalytics work (a new `AppAnalytics.kt`, plus calls added to `TranslateActivity`, `OCRActivity`, `InputLogic`, and other files). Commit that work before starting here, so this work begins from a clean tree.

Every root cause below was traced in the code. Each one says how sure it is:
- **Confirmed:** proven by reading the code.
- **Probable:** strong evidence, but should be confirmed on a device. Each probable item comes with a one-step check.

All line numbers refer to the current working tree.

---

## 0. Implementation status: Release 2.6 (branch `fix/client-feedback-2.6`)

Built, unit-tested (42 tests) and checked on a **Samsung Galaxy Note 20 Ultra (Android 13)** with a fresh install, then an upgrade install. Version is 2.6 (17).

Confirmed on that phone:

| Rows | Result |
|---|---|
| 12 | "teh" auto-corrects to "The"; "helo" suggests hello |
| 4, 6, 8 | Vibration off/on, number row and emoji key all apply with no mic press |
| 9 | Back from keyboard settings lands in Chrome |
| 11 | "Languages" opens Samsung's languages screen |
| 7 | The globe key appears with 2 languages and switches US → UK |
| 13 | Dark theme applies right away. This test caught unreadable toolbar pills, now fixed. |
| 2 | Swap swaps languages and texts |
| 19 | "rat" accepted for "art" |
| 20 | Swap inside a category reloads; Bengali rows aligned; no crashes |

**Not confirmed:**
- **Row 3 in WhatsApp itself.** WhatsApp wasn't signed in on the test phone. The share sheet did receive the plain text.
- **Row 5's sound.** It can't be observed over adb. It uses the same path as vibration, which was confirmed.

| Row(s) | Status in 2.6 | Notes |
|---|---|---|
| 12 | **Done:** E.1 | Dictionaries load (resource package name fixed). Keyboard-language names fixed too. E.2 (clearing old typing history) **not done**, waiting for the client's decision. |
| 4, 5, 6, 8, 10 | **Done:** A1–A5 | One shared settings instance; vibration and sound update instantly; the layout reloads the next time the keyboard opens; XML and code defaults unified (unit test guards them). |
| 9 | **Done:** B1 | Keyboard settings and the keyboard's language picker open in their own task; no resume ad or "app opened" handling over them; the Up arrow works. |
| 11 | **Done:** B2 quick fix | No task-reset flags; failures are caught with a fallback message. The in-app "Keyboard languages" screen stays in 2.7. |
| 7 | **Done:** B3 | The globe key shows only with 2 or more keyboard languages. |
| 13 | **Done:** C | Dark styles restored; toolbar colours follow the theme; observer leak fixed; users who never picked a theme keep the light look with key borders; the app's own dark-mode switch fixed. Simplifying the Appearance screen (C.4) **not done** (client decision). |
| 2 | **Done:** D1 | Swap swaps the texts too, works from Detect (uses the detected language), and drops stale results. The keyboard toolbar's swap button now works. |
| 3 | **Done:** D2 | One share helper, always plain text. |
| 19 | **Done:** H | Implemented as a precomputed list instead of a runtime dictionary: `tools/wordgame/make_alternates.py` writes `assets/wordgame_alternates.json` (152 puzzles, 216 extra words). The `sorted_word.json` cleanup was **not done**, because removing entries would shift players' saved level index. |
| 20 | **Code part done:** I.1–I.3 | The `bn.json` duplicate key fixed (`tools/phrasebook/fix_bn_duplicate_key.py`); the detail screen reloads on language changes; the adapter no longer crashes; the category comes from the intent. A unit test checks that every language file matches English. Data regeneration **not done** (2.7 / content track). |
| Section 10 | **Partly done** | Wi-Fi-only connectivity check fixed; one language-ID client; debug log removed. Translate POST and 429 backoff stay with G (2.7). |
| 14, 15, 16, 17, 18 | **Not started** | Planned for 2.7. |

---

## 1. Summary

The 19 sheet rows reduce to **12 distinct problems**. The two most important findings:
- **Five keyboard rows (4, 5, 6, 8, 10), and part of 7, have the same three root causes.** One fix in the keyboard's settings plumbing clears all of them.
- **The keyboard has never loaded its word dictionaries** (row 12). A wrong resource package name makes the lookup fail. As a result there is no auto-correct, and suggestions come only from what the user typed before. This is a small fix with the biggest visible improvement in the whole list.

The tester's own comment, "the mic button acts as a refresh button", is the key clue. The mic key does not refresh anything. It switches to Google voice typing and back (`LatinIME.java:1538` → `RichInputMethodManager.switchToShortcutIme`). That switch destroys and re-creates the keyboard service, and the new instance reads the settings fresh.

Two rows are features rather than bugs: **16–17**, the Gboard-style clipboard features (row 18 says "replicate Gboard" for both). One row is mostly a **content problem: 20**, where the phrasebook translations themselves are poor. Both need product decisions (section 14).

### 1.1 At a glance

Sizes: **S** ≤ ½ day, **M** 1–2 days, **L** 3–5 days, for one developer who knows the codebase.

| Row | Problem (short) | Root cause (short) | Workstream | Size | Confidence |
|---|---|---|---|---|---|
| 2 | Swap changes languages but not the text | Swap re-translates the *same input* in the new direction instead of swapping input and output | D1 | S | Confirmed |
| 3 | Share to WhatsApp says "empty message" | Spanned text (not a `String`) is put in `EXTRA_TEXT`, so WhatsApp's `getStringExtra` returns null | D2 | S | Confirmed |
| 4 | Vibration continues after turning it off | Keyboard holds stale settings (A-R2, A-R3) | A | M | Confirmed + Probable |
| 5 | Keypress sound does not work after turning it on | Default mismatch (A-R1) plus stale settings (A-R2, A-R3) | A | — | Confirmed |
| 6 | Number row does not appear | Stale settings; the layout is not rebuilt (A-R2, A-R3) | A | — | Confirmed + Probable |
| 7 | Language-switch key visible but does nothing | Key is shown when *other keyboard apps* exist, but only cycles *this* keyboard's languages, and there is only one (B3). Default mismatch too (A-R1) | B3 + A | S | Confirmed |
| 8 | Emoji key still shows after turning it off | Default mismatch (A-R1) plus stale settings | A | — | Confirmed |
| 9 | Back after settings opens the app, not WhatsApp | Keyboard settings open inside the translator app's own task; an app-open ad may also show | B1 | S | Confirmed |
| 10 | First change never applies until the mic is pressed | The keyboard's settings object is detached from the one the settings screen writes (A-R3) | A | — | Probable (one-step check) |
| 11 | "Languages" in keyboard settings crashes or loops | Fragile system intent with task-reset flags; no fallback | B2 | S (quick) / L (proper) | Confirmed (flags); OEM crash to verify |
| 12 | Suggestions based on history, not correctness | **The main dictionaries never load** (wrong resource package name), so only learned history is suggested and auto-correct is off | E | S (+M tuning) | Confirmed |
| 13 | Manual light/dark theme does not work | "Dark" is wired to the **Light** style in code; reload issue; hard-coded toolbar colours | C | M | Confirmed |
| 14 | Camera translates line by line | OCR results consumed per *line*; one translation request per line | G | L (14+15) | Confirmed |
| 15 | Landscape photo translates bottom-up; words not fully translated | No orientation handling anywhere; fragments, per-line language detection, mixed engines | G | — | Confirmed (cause); Probable (exact order) |
| 16 | Show copied link as a suggestion | Feature missing | F2 | M | — |
| 17 | Clipboard with last 10 copied items | A history exists but expires after 10 min, is lost on restart, has no count cap, and has a wrong-item tap bug | F1 | M | Confirmed |
| 18 | "Replicate Gboard" | Same as 16 and 17 | F | — | — |
| 19 | Fun Words rejects real words (rat, owl) | Only the one stored answer is accepted; 235 of 1,385 puzzles have other valid anagrams | H | M | Confirmed |
| 20 | Phrasebook sentences wrong or badly translated | Bengali file misaligned (duplicate key); detail screen ignores language changes; poor baked machine translations and English typos | I | M (code) + L (data) | Confirmed |

---

## 2. Phase 0: before writing code

1. **Commit or park the in-flight AppAnalytics work.** Then branch: `fix/client-feedback-2.6`.
2. **Version.** Bump to versionCode 17 / versionName 2.6 (`app/build.gradle`). Fill the sheet's "Version" column with it.
3. **Test devices.**
   - Pixel 3a (Android 12) is already set up (see the build memory: JBR 21, adb under the SDK).
   - Rows 9 and 11 behave differently per manufacturer, so also get **one Samsung (One UI)** and **one Xiaomi (MIUI/HyperOS)**. A friend's phone or a cloud device farm both work.
   - Install WhatsApp on at least one device. Rows 3 and 9 name it.
4. **Two install scenarios for every keyboard test.**
   - **Fresh install:** `adb uninstall` then install.
   - **Upgrade over 2.5:** `adb install -r`.
   - Rows 4–10 are fresh-install bugs. An upgraded test device can hide them.
5. **One-step confirmation for A-R3** (section 3), before fixing it. This is temporary logging; remove it afterwards.
   - Log `System.identityHashCode(mPrefs)` in `Settings.onCreate` (`Settings.java:172`).
   - Log `System.identityHashCode(getSharedPreferences())` in `SubScreenFragment.onCreate`.
   - Log when `DeviceProtectedUtils.getSharedPreferences` reaches its `moveSharedPreferencesFrom` branch.
   - Do a fresh install, enable the keyboard, open its settings, and compare the hashes. Different hashes confirm A-R3.

---

## 3. Workstream A: keyboard settings apply immediately (rows 4, 5, 6, 8, 10, part of 7)

### 3.1 Root causes

**A-R1: The settings screen and the keyboard disagree on defaults (Confirmed).**
The settings screen's XML defaults differ from the defaults the keyboard code uses for three switches:

| Setting | Settings screen shows (XML default) | Keyboard actually uses (code default) |
|---|---|---|
| Sound on keypress (`sound_on`) | **ON** (`prefs_screen_preferences.xml:42–44`) | **OFF** (`config_default_sound_enabled = false`, `config-per-form-factor.xml:26`, read in `Settings.readKeypressSoundEnabled`) |
| Emoji key (`pref_show_emoji_key`) | **OFF** (`prefs_screen_preferences.xml:77–79`) | **ON** (`SettingsValues.java:160`) |
| Language-switch key (`pref_show_language_switch_key`) | **OFF** (`prefs_screen_preferences.xml:71–73`) | **ON** (`SettingsValues.java:159`) |

Effects on a fresh install:
- The keyboard shows an emoji key and a language key whose switches read OFF.
- Sound reads ON but is silent.
- Just *opening* the settings screen writes the XML defaults to storage (`TwoStatePreference.onSetInitialValue` persists them). The keyboard can therefore change state without the user touching anything.

That is exactly what the tester describes in rows 5, 7 and 8.

**A-R2: A running keyboard does not apply changed settings (Confirmed).**
`Settings.onSharedPreferenceChanged` (`Settings.java:181`) rebuilds the settings values but tells nobody else:
- `AudioAndHapticFeedbackManager` keeps its own reference to the *old* values object. It is only refreshed from `LatinIME.loadSettings()` (`LatinIME.java:697–704`). So vibration and sound stay as they were (rows 4 and 5).
- The key layout (number row, emoji key, language key) is only rebuilt by `KeyboardSwitcher.loadKeyboard`. That is called from `onStartInputViewInternal` only when the keyboard opens on a *different* text field (`LatinIME.java:1066–1081`). The AOSP comment there even says: "ALERT: settings have not been reloaded and there is a chance they may be stale".
- The visibility of the toolbar's voice and clipboard keys is only updated in `SuggestionStripView.updateVisibility`.

**A-R3: The keyboard's settings object can become detached from the one the settings screen writes (Probable; the Phase 0 log confirms it).**
- `DeviceProtectedUtils.getSharedPreferences` (`DeviceProtectedUtils.java:31–41`) calls `moveSharedPreferencesFrom(context, …)` whenever the stored settings are empty.
- The manifest sets `android:defaultToDeviceProtectedStorage="true"`, so the source and target are **the same file**. If that file exists but is empty, Android copies it onto itself, deletes it, and evicts its in-memory cache.
- The `SharedPreferences` instance that `Settings` already holds (`Settings.java:172`) is then orphaned. The settings screen loads a *new* instance and writes to it. The keyboard never sees those writes, not even when it reloads.
- When the keyboard service is re-created (the mic round trip), `Settings.init` picks up the live instance. From then on everything updates immediately.

That matches row 10 word for word: "after that keyboard starts fetching the settings immediately". It also explains why the bug only appears on a fresh install.

### 3.2 Implementation

**A1. Make `DeviceProtectedUtils` safe.**
- Keep one process-wide `SharedPreferences` instance, created once.
- Migrate from credential-encrypted storage only when the source really is a different context: `context.createCredentialProtectedStorageContext()`, only if the user is unlocked, and only once (remember it with a flag).
- Never call `moveSharedPreferencesFrom` when the context is already device-protected, which is always the case in this app.
- After any migration, re-fetch the instance.

**A2. Use one source of truth for defaults.**
- Add `<bool>` resources: `config_default_sound_enabled`, `config_default_show_emoji_key`, `config_default_show_language_switch_key`, `config_default_show_number_row`.
- Reference them from **both** `prefs_screen_preferences.xml` (`android:defaultValue="@bool/…"`) and `SettingsValues` / `Settings`.
- Recommended values, Gboard-like (needs the client's OK, section 14):
  - sound OFF
  - vibration ON
  - emoji key ON
  - number row OFF
  - language key: automatic, shown only when 2 or more keyboard languages are enabled (see B3)
- Go through the other preference screens for the same mismatch. Correction, gesture and advanced look consistent; check them anyway.

**A3. Apply changes live.**
- Have `LatinIME` listen for settings changes. Reuse the `Settings` listener with a callback, posted to the main thread. On any change:
  1. Call `loadSettings()`. This also refreshes `AudioAndHapticFeedbackManager`.
  2. If the keyboard view exists, rebuild it with `mKeyboardSwitcher.loadKeyboard(getCurrentInputEditorInfo(), mSettings.getCurrent(), …)`, the same call `loadKeyboard()` already makes (`LatinIME.java:1804`).
  3. Call `mSuggestionStripView.updateVisibility(…)`.
  4. If the theme key changed, call `mKeyboardSwitcher.updateKeyboardTheme()` and then reload.
- As a safety net, keep a `mSettingsDirty` flag. Set it in the callback. In `onStartInputViewInternal`, treat `dirty` like `isDifferentTextField` (`LatinIME.java:1066`).

**A4. Read audio and haptic settings at use time.**
`AudioAndHapticFeedbackManager` should read `Settings.getInstance().getCurrent()` when it plays feedback, not keep a copy. Keep the ringer-mode check.

**A5. Do not re-trigger theme or layout work on keystrokes.**
Settings keys written during typing are not user settings. Ignore them in the listener: emoji recents (`emoji_recent_keys`, `last_shown_emoji_*`), clipboard pins, and similar.

**A6. Remove the Phase 0 logging.**

### 3.3 Acceptance checks (fresh install *and* upgrade)
With the keyboard open in WhatsApp, open the gear → change a setting → press Back → type. The change must apply with **no mic press**. Check each of these:
- vibration on/off
- sound on/off
- number row on/off
- emoji key on/off
- language key on/off
- theme
- keyboard height

Each settings switch must match what the keyboard shows the very first time the screen is opened.

**Unit test.** Add a plain-JVM test that reads the default for each key from `res/xml/prefs_screen_preferences.xml` (from disk) and asserts it references the same `@bool` the code uses. This stops the mismatch from coming back.

---

## 4. Workstream B: keyboard navigation and languages (rows 7, 9, 11)

### B1. Return to the app you were typing in (row 9): S, Confirmed

**Cause.**
- `LatinIME.startSettingsActivity` (`LatinIME.java:1623`) starts `.settings.SettingsActivity` with `FLAG_ACTIVITY_NEW_TASK`.
- That activity has the app's default task affinity (`AndroidManifest.xml:366`), so Android puts it **into the translator app's existing task**, on top of the dashboard. Pressing Back finishes the settings screen and reveals the dashboard: "the app opens".
- `LatinIME.startSelectLanguageActivity` (`LatinIME.java:1589`, which opens `LanguageSelectorActivity`, `AndroidManifest.xml:195`) has the same problem.
- In addition, `App.onMoveToForeground` (`App.kt:89`) treats these screens as "the user opened the app". It may show the **resume app-open ad** and `OnOpenReminderNoti` over a keyboard screen.

**Fix.**
1. Manifest:
   - Give `.settings.SettingsActivity` (and `SpellCheckerSettingsActivity`) `android:taskAffinity=""` and keep `excludeFromRecents`.
   - Launch it from the keyboard with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, so a stale settings task is never resumed.
   - Do **not** use `singleTask`. The PreferenceActivity opens its sub-screens as new instances of itself.
   - Sub-screens launched from inside stay in that task. Back from the top screen ends the task, and Android returns to WhatsApp.
2. `LanguageSelectorActivity`: same treatment.
   - `taskAffinity=""` is harmless for its in-app uses. Affinity only matters with `NEW_TASK`, and in-app launches don't use it.
   - Pass an extra such as `EXTRA_FROM_KEYBOARD`. When it is set, finish without routing to the dashboard.
3. `App.onMoveToForeground` / `onAppOpened`: skip the resume ad, the reminder notification and `KillAppWatcherService.start` for keyboard-launched screens.
   - Use a small marker interface (e.g., `KeyboardSurface`) on these activities, or check that the activity's package starts with `org.dslul.openboard.inputmethod`.
4. Handle the action-bar "Up" arrow in `.settings.SettingsActivity`. It is enabled (`SettingsActivity.java` `onCreate`) but never handled; make it call `finish()`.
5. Remove the dead `Misc.data` branch in `SettingsActivity.onCreate`. Nothing passes that extra.

**Check.** WhatsApp → keyboard gear → change something → open a sub-screen → Back → Back. You must land in WhatsApp with the keyboard up and no ad. Repeat via the From/To language pickers on the keyboard toolbar.

### B2. "Languages" in keyboard settings (row 11)

**Cause (Confirmed for the flags; the OEM crash still needs to be seen on a device).**
- The "Languages" row is added by `InputMethodSettingsImpl.init` (`InputMethodSettingsImpl.kt:36–41`).
- It fires the **system** screen `Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS` with `NEW_TASK | RESET_TASK_IF_NEEDED | CLEAR_TOP`.
- `RESET_TASK_IF_NEEDED` brings back an existing system-Settings task **in whatever state it was left**. That is usually the "On-screen keyboard" page the user saw while enabling the keyboard. Result: "it just keeps opening the same settings main tab".
- Several OEM Settings apps (MIUI, some One UI builds) crash or lack this page. Android's crash dialog then reads like "there's a bug, inform the provider". There is no `try/catch` or fallback.
- `SetupWizardActivity.java:292` uses the same intent.

**Fix, step 1: quick (S, ship in 2.6).**
- Launch the intent with `FLAG_ACTIVITY_NEW_TASK` only.
- Check `resolveActivity` first.
- Catch `ActivityNotFoundException` / `SecurityException`.
- Fall back to `ACTION_INPUT_METHOD_SETTINGS` with a toast explaining where to go.
- Apply the same change to `SetupWizardActivity`.

**Fix, step 2: proper (L, recommended; the client should choose the release).** Build an in-app **"Keyboard languages"** screen. This is the approach other OpenBoard forks took for the same reason.
- List all 90 layouts declared in `res/xml/method.xml`, grouped by language, with search and on/off switches.
- Store the enabled set in the keyboard's own prefs (e.g., `pref_enabled_subtypes`).
- `RichInputMethodManager.getMyEnabledInputMethodSubtypeList` returns this set when it is present, so the system's enabled-subtype list no longer matters.
- Switch with `InputMethodService.switchInputMethod(imeId, subtype)` (API 28+). On API 27 (minSdk) use `InputMethodManager.setInputMethodAndSubtype(token, …)`.
- Default set: the system-locale layout plus `en_US`.
- The language key, space-bar long-press and the subtype picker all use this list.
- Note for the client: 20 languages ship a word dictionary (bg, bn, da, de, el, en, eo, es, fr, hu, hy, it, nl, pl, pt-BR, pt-PT, ro, ru, sv, tr). They only work once E is fixed. All other layouts type correctly but show **no word suggestions**.
- The list's display names depend on the same fix (E.1). Without it, some English and Spanish entries would show a literal code string.

### B3. Language-switch key does nothing (row 7): S, Confirmed

**Cause.** The rule for showing the key and the rule for what it does disagree:
- **Shown:** `SettingsValues.isLanguageSwitchKeyEnabled` (`SettingsValues.java:293–302`). `mIncludesOtherImesInLanguageSwitchList` is forced to `true` on every Android version above 4.4 (`SettingsValues.java:155`, via `ENABLE_SHOW_LANGUAGE_SWITCH_KEY_SETTINGS`). So the key appears whenever the phone has **any other keyboard app** enabled, for example Gboard.
- **Action:** `LatinIME.switchToNextSubtype` (`LatinIME.java:1495–1500`) calls `switchToNextInputMethod(token, onlyCurrentIme = true)`. That cycles only **this keyboard's** languages. There is one, so nothing happens.
- The deprecated token-based `InputMethodManager` calls are also used instead of the `InputMethodService` APIs.

**Fix (Gboard-like).**
- **Tap:** cycle this keyboard's enabled languages (the B2 list).
- **Long-press:** open the picker. It already does this via `CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER` (`PointerTracker.java:1087`).
- **Show the key** only when 2 or more keyboard languages are enabled, or when the user forced it on.
- Use `InputMethodService.switchToNextInputMethod(onlyCurrentIme)` on API 28+.
- The default comes from A2.

---

## 5. Workstream C: keyboard theme (row 13): M, Confirmed

**Causes.**
1. **"Dark" is wired to the Light style.** In `KeyboardTheme.java:84–97`, `THEME_ID_LXX_DARK`, `THEME_ID_LXX_DARK_AMOLED` and `THEME_ID_LXX_DARK_BORDER` all point at `KeyboardTheme_LXX_Light*`. The real Dark styles are commented out next to them. Choosing Dark therefore can never produce a dark keyboard.
   - "Auto" works because `KeyboardTheme_LXX_Auto` maps to `LXX_Dark` in `values-night/themes-lxx-auto.xml`. That is the tester's "auto works but manual doesn't".
2. **The default is "Dark", which looks light** (`DEFAULT_THEME_ID = THEME_ID_LXX_DARK`, `KeyboardTheme.java:69`). The settings screen shows "Dark" while the keyboard is light.
3. **Probable reason Dark was disabled:** the custom translate toolbar has hard-coded colours.
   - `suggestions_strip.xml:64, 78, 100` use `@color/black` for the language names and the swap icon.
   - `SuggestionStripView.onTextCopy` (`SuggestionStripView.java:157–160`) uses `gray_800`.
   - On a dark keyboard these are unreadable.
4. **"Works sometimes."** `updateKeyboardTheme()` creates a new input view in `onStartInputViewInternal`. When the field is the same one, the layout is not reloaded into it (A-R2), and A-R3 can hide the change entirely.
5. **Leak on every theme change.** The `SuggestionStripView` constructor calls `Misc.isTranslated.observeForever` and `Misc.isAppInForeground.observeForever` (`SuggestionStripView.java:210–225`) and never removes them. Every theme change creates a new strip, and the old ones stay alive and keep receiving updates.

**Fix.**
1. Restore the real Dark, Dark-AMOLED and Dark-Border styles in `KEYBOARD_THEMES`.
2. Make the toolbar themeable:
   - Add attributes such as `toolbarTextColor`, `toolbarIconTint`, `toolbarPillColor` and `toolbarBackground` to each `KeyboardTheme.*` style.
   - Use `?attr/…` in `suggestions_strip.xml`.
   - Resolve them in `SuggestionStripView` instead of `R.color.gray_800`.
   - This works because the strip is inflated from the keyboard theme context (`KeyboardSwitcher.java:533–536`).
3. **Migrate the default without surprising existing users.** Change `DEFAULT_THEME_ID` to **Light**, or to **Follow system** if the client prefers.
   - Users who never touched the setting have no stored value, so they keep today's light look.
   - Users who explicitly chose Dark get the dark keyboard they asked for.
4. Simplify the Appearance screen (client decision): **Theme: Light / Dark / Follow system**, plus "Key borders". Hide the family and AMOLED options if they are not wanted.
5. Move the strip's LiveData observers to `onAttachedToWindow` / `onDetachedFromWindow`.
6. A theme change goes through A3 (full reload).
7. **Related app-side issue.** The translator app's own dark-mode switch (`translator/pro/SettingsActivity.kt:102`) ignores the switch's `isChecked` and flips a stored flag instead. When the two get out of sync, a tap appears to "not respond". Use the `isChecked` value. This is a small change; include it in case the tester meant the app theme.

**Check.** For every theme × system light/dark combination, the keyboard and toolbar are readable. Switching takes effect the next time the keyboard opens, with no mic press.

---

## 6. Workstream D: translator screen (rows 2, 3)

### D1. Swap languages should also swap the text (row 2): S, Confirmed

**Today.**
- `TranslateActivity.kt:270–305` and `ViewTranslatedTextActivity.kt:208–240` swap the saved From/To languages, then re-translate the **same input**. The text is now treated as being in the *other* language, which gives a wrong or unchanged result.
- `HomeFragment.kt:202` swaps only the languages, which is fine because there is no output there.

**Change (Google Translate behaviour).** On swap, when there is a translation:
- The **output becomes the new input**: put it in `etText` with the cursor at the end, so the user can edit and add words.
- The **old input becomes the new output**. No network call is needed, and the user's original text comes back exactly.
- When there is input but **no translation yet**, swap only the languages and do not auto-translate.
- **Detect as source:** today swap is blocked with a toast.
  - Let `MiscTranslate.translate` report the language it detected. It already detects one via ML Kit language ID; add it as a second callback parameter or a returned field.
  - Swap then uses the detected language.
  - Keep the toast only when detection failed.
- **Ignore stale results.** Add a request token: increment it on every translate or swap, and drop callbacks that carry an old token.
- **Share the logic** in one helper used by `TranslateActivity`, `ViewTranslatedTextActivity`, `OCRActivity` (its swap does not re-translate today), `PhrasebookDetailedActivity` and `HomeFragment`.
- **Keyboard toolbar:** its swap button is visible but its listener is commented out (`SuggestionStripView.java:258–263`). Wire it to swap the languages, or hide it.

### D2. Share sends an "empty message" to WhatsApp (row 3): S, Confirmed

**Cause.**
- `textViewTextTranslatedFrag` has a `doOnTextChanged` watcher (`TranslateActivity.kt:171`, `ViewTranslatedTextActivity.kt:117`).
- A `TextView` with a watcher stores its text as an `Editable`, not a `String`.
- The share code puts that object straight into the intent: `putExtra(EXTRA_TEXT, binding.textViewTextTranslatedFrag.text)` (`TranslateActivity.kt:255`, `ViewTranslatedTextActivity.kt:194`). It arrives as a `SpannableString`.
- WhatsApp reads it with `getStringExtra()`. That fails the `String` cast and returns null, so WhatsApp says "can't send empty message". Gmail and others read it as a `CharSequence`, which is why only some apps fail.
- The phrasebook share (`CustomExpandableListAdapter.kt:118–122`) has the same pattern.

**Fix.**
- Add one helper, `ShareUtils.shareText(context, text: CharSequence?)`. It:
  - converts with `.toString().trim()`
  - shows a toast ("Nothing to share yet") when the text is blank
  - sets type `text/plain` and opens the chooser
- Use it at all share sites, including the history and favourites adapters, for consistency.
- Optional: a "Share original + translation" choice.

**Check.** Share to WhatsApp, Telegram, Gmail and Messages from Translate, View-all (OCR), Phrasebook, History and Favourites.

---

## 7. Workstream E: keyboard suggestions (row 12): S fix + M tuning, Confirmed

### Root cause: the keyboard has never loaded its main dictionaries

**The lookup uses the wrong package name.**
- `DictionaryInfoUtils.java:54` sets `RESOURCE_PACKAGE_NAME = R.class.getPackage().getName()`. That is the Gradle **namespace**, `org.dslul.openboard.inputmethod.latin`.
- The app's resource table is named after the **applicationId**, `com.translator.keyboard.translate.all.langugaes`. This was checked in the shipped `app/release/app-release.aab`, in the `base/resources.pb` header.
- `Resources.getIdentifier(name, "raw", package)` only finds a resource when the package name matches exactly. Every main-dictionary lookup therefore returns 0 (`DictionaryInfoUtils.java:275, 282, 302`; also `BinaryDictionaryFileDumper.java:328`).
- `BinaryDictionaryGetter` then logs "Resource not found: 0" and loads nothing.
- The mismatch has existed since the initial commit.

**What this does to the keyboard:**
- `hasAtLeastOneInitializedMainDictionary()` is false, and every reload attempt fails again.
- **Auto-correction never runs.** `Suggest.java:225–232`: no main dictionary means no auto-correct.
- **Gesture typing is disabled.** It needs a main dictionary.
- **Suggestions come only from the user-history and personal dictionaries.** With no main dictionary, every word the user types is learned and offered back later, misspellings included. That is exactly "suggestions based on our previous activity, not on whether the word is correct".
- 20 dictionaries actually ship in `res/raw` and none of them load: bg, bn, da, de, el, en, eo, es, fr, hu, hy, it, nl, pl, pt_br, pt_pt, ro, ru, sv, tr.
- The keyboard's spell-checker service uses the same lookup, so it is broken the same way.

**The same bug in keyboard-language names.**
- `SubtypeLocaleUtils.java:46` and `:109–138` use the same wrong package name.
- A previous commit (`0e059ad`) papered over the resulting crash: `SubtypeLocaleUtils.java:227` now returns the literal text `"res.getString(exceptionalNameResId)"` as the display name of English (US/UK), Spanish (US) and similar layouts.
- That text would surface in any language list, including the planned in-app list from B2.

### Fix
1. **Fix the package name.**
   - Replace `RESOURCE_PACKAGE_NAME` in `DictionaryInfoUtils` and `SubtypeLocaleUtils` with the real resource package: `res.getResourcePackageName(R.raw.main_en)` or `context.getPackageName()`. The working code in `KeyboardLayoutSet.java:414–416` and `KeyCodeDescriptionMapper.kt:143–146` already does this.
   - Revert the `SubtypeLocaleUtils.java:227` hack to `res.getString(exceptionalNameResId)`, guarded against a 0 id.
2. **Clean up the history once** (client decision; recommended).
   - On the first launch of 2.6, clear the **user-history** dictionaries with `PersonalizationHelper.removeAllUserHistoryDictionaries`. They were trained without a main dictionary and contain learned misspellings.
   - Keep the user's **personal** dictionary (words they added on purpose).
3. **Tune after the fix.**
   - The default auto-correct strength is "modest" (`config-auto-correction-thresholds.xml`). Turning auto-correct on is a visible change for existing users. Decide the default with the client, and mention it in the release notes.
   - Re-check the defaults for next-word prediction, personalized suggestions and blocking offensive words.
4. **Remove leftover debug code and review fork changes.**
   - Remove `Log.e("testingSuggestion"…)` in `InputAttributes.java` (around line 306).
   - Review `InputAttributes.java:85`, which forces suggestions on in number, phone and date fields. Upstream turns them off there.
5. **Optional: space.** The suggestion row also holds the settings, clipboard and voice icons, so only 3 suggestions fit in a reduced width. If the client wants Gboard's look, move the settings icon to the upper (translate) row.
6. **Layouts without a dictionary.** Languages beyond the 20 above have no suggestions. If the client wants more (for example Hindi or Arabic), add `.dict` files from AOSP/OpenBoard dictionary sources. That is a separate, size-sensitive task: each dictionary is 0.2–6 MB.

### Check
- Before the fix: `adb logcat | grep -E "BinaryDictionaryGetter|Resource not found"` shows the failure. After the fix it does not.
- Typing "teh " auto-corrects to "the".
- "helo" suggests "hello".
- A misspelling typed before the upgrade no longer appears as a top suggestion.
- The spell checker underlines misspellings in apps when it is selected as the system spell checker.
- Test German and Spanish layouts too.

---

## 8. Workstream F: clipboard features (rows 16, 17, 18): M + M

### What already exists (Confirmed)
- **A clipboard history already exists** (OpenBoard's `ClipboardHistoryManager` / `ClipboardHistoryView`). It opens from the clipboard key in the keyboard toolbar. A long-press on that key pastes the current clip. So row 17 is mostly "make it behave like Gboard", not "build from zero".
- **Its gaps:**
  - Entries expire after **10 minutes** by default (`config-common.xml:28`), so the tester rarely sees a history.
  - **Unpinned entries are lost when the process dies.** Only pinned entries are saved, in `filesDir/pinned_clips.data`.
  - There is **no count limit**.
  - Duplicates are detected by timestamp only, so the same text can appear twice. Pinning rewrites the timestamp, which also causes duplicates.
  - **Sensitive clips are not filtered.** Passwords marked `ClipDescription.EXTRA_IS_SENSITIVE` (Android 13+) show up.
  - **Wrong item on tap.** `ClipboardAdapter.kt:62–66` captures the bind-time `position`, so after an insert a tap can paste the wrong item.
  - `ClipboardAdapter` runs ML Kit language ID plus translation **for every row it binds, on the main thread**, and sets `StrictMode` to permit-all (`:77–161`).
  - With "show clipboard key" off, `SuggestionStripView.updateVisibility` (`:322`) still shows the key when the voice key is hidden.
  - The pinned-clips file lives in device-protected storage (because of `defaultToDeviceProtectedStorage`), which is readable before the phone is unlocked. Clipboard content is sensitive.
- **Platform limit (Android 10+):** only the *current default keyboard* can read the clipboard, and it only receives "clip changed" callbacks while its process is running. Copies made while another keyboard is selected cannot be captured. The current clip is still read when the keyboard opens.
- **There is no "recent clip" chip.** The only existing hook is `LatinIME.java:648–655` → `SuggestionStripView.onTextCopy()`, which just animates the clipboard icon.

### F1. Clipboard history like Gboard: last 10 items (rows 17, 18)
1. **Capacity:** keep the **10 most recent** unpinned items (the client's number) plus pinned items, up to 20. Change the default retention from 10 minutes to "no time limit, count-capped". Optionally offer "Delete after 1 hour" as Gboard does.
2. **Persist the history,** pinned and unpinned, in **credential-encrypted** storage via `createCredentialProtectedStorageContext()`. Load it after unlock, and move the existing pinned file there.
3. **Deduplicate by content.** Copying an existing text moves it to the top. Pinning no longer creates a duplicate.
4. **Never store sensitive content:**
   - clips with `EXTRA_IS_SENSITIVE`
   - clips copied while a password field is focused
   - clips longer than about 5,000 characters (store a truncated preview instead)
5. **Fix the item-position bug** (`bindingAdapterPosition`).
6. **Handle the per-row translation.** Run it off the main thread with cancellation, or move it behind a "Translate" action on each item. Remove the `StrictMode` permit-all.
7. **UI:**
   - an empty state ("Copied text will appear here")
   - long-press an item to Pin / Delete
   - "Clear all" with a confirmation
   - Fix the key-visibility logic.
   - Optional: make the clipboard key show a small dot when there is a new clip, extending `onTextCopy`.

### F2. Recently copied text or link as a suggestion chip (row 16)
1. **When it shows.** In `onStartInputViewInternal`, after `setNeutralSuggestionStrip()` (`LatinIME.java:1101`), check the primary clip:
   - It must be younger than a short window, about 3 minutes.
   - It must not have been offered already. Track the last offered clip's timestamp.
   - It must not be sensitive.
   - The field must not be a password field.
   - Read the clip's age from `ClipDescription.getTimestamp()` without reading its content first.
2. **What it looks like.** A dedicated chip view in `suggestions_strip.xml`, not a slot in the word list.
   - The word-suggestion pipeline (`postResumeSuggestionsForStartInput`, next-word prediction) would otherwise overwrite it immediately.
   - The chip shows a link icon for URLs (`Patterns.WEB_URL`, plus `ClipDescription.getConfidenceScore(TYPE_URL)` on Android 12+), otherwise a text icon, and the truncated text.
3. **Behaviour:**
   - **Tap:** insert through `onTextInput`, so the URL is not learned as a word, and hide the chip.
   - **Long-press:** open the clipboard history.
   - The chip hides on the first keystroke, on a field change, or after the time window.
4. **Privacy.** The content is only read locally while this keyboard is the default, never logged and never sent anywhere. Mention it in the privacy policy if the client wants.

### Check
- Copy a link in Chrome, switch to WhatsApp, and open the keyboard: the chip shows the link, and tapping it inserts the link.
- The chip does not show for an old clip, a sensitive clip or a password field.
- Copy 12 texts: the history shows the latest 10, newest first, with pins on top.
- The history survives a reboot.
- Tapping inserts the right item.

---

## 9. Workstreams G, H, I: camera, Fun Words, phrasebook

### G. Camera translation (rows 14, 15): L, Confirmed

**How it works today.**
`CameraFragment` → CameraX `takePicture` → cropper → `OCRActivity.handleSelectedImageForOCR` → ML Kit Text Recognition v2 (`text-recognition:16.0.1`, plus the Chinese, Japanese and Korean variants) → one row per **line** in a bottom-sheet list (`OCRResultAdapter`). Translations are never drawn on the image; the image only shows blue line boxes.

**Root causes.**

| # | Cause | Evidence |
|---|---|---|
| G-R1 | Lines are used, not paragraphs. Block grouping is dropped, and every line is its own translation request, so sentences that wrap are translated as unrelated fragments (row 14). | `OCRActivity.kt:265–273` collects `line.text`; `translateNow` (`:292–357`) makes one `MiscTranslate.translate` call per line. |
| G-R2 | No orientation handling. Every screen is locked to portrait, `ImageCapture` gets no target rotation (`CameraFragment.kt:164`), there is no orientation listener, and ML Kit is told every image is upright (`InputImage.fromBitmap(bitmap, 0)`, `OCRActivity.kt:226`). With a sideways page, ML Kit's top-to-bottom and left-to-right ordering runs over the page's lines in reverse (row 15, "bottom to top"). | The cause is Confirmed. The exact ordering is Probable: confirm by logging `line.boundingBox` and `line.angle`. |
| G-R3 | In Detect mode the language is detected **per line**. Short lines come back as "und" or the wrong language, so lines go through different engines (ML Kit on-device vs online) with different source languages. Result: "random results". | `MiscTranslate.kt:134–156`. |
| G-R4 | One failed line throws away all results. The chain stops and the list is only shown on full success. | `OCRActivity.kt:313–346`. |
| G-R5 | "Words not fully translated": half-words at hyphenated line ends sent separately; a source language that defaults to `en` for non-English photos; on-device models that copy unknown words through unchanged; mixed engines. | Inferred from G-R1 and G-R3. The display never clips: there is no `maxLines` or `ellipsize`. |
| G-R6 | **App-wide:** `Misc.checkInternetConnection` (`Misc.kt:488–499`) calls `getNetworkInfo(TYPE_MOBILE)!!`. On Wi-Fi-only devices (tablets) that throws, so the check returns "offline" and every online translation fails. | Confirmed. |

**Plan.**
1. **Upright the image.**
   - At capture: an `OrientationEventListener` keeps `imageCapture.targetRotation` current, so the JPEG's EXIF is correct and the cropper shows it upright.
   - At recognition, which also covers gallery images: compute the dominant `Line.angle` from a first pass. If it is about 90°, 180° or 270°, rotate the bitmap and recognise again. A cheaper alternative is to pass `rotationDegrees` to `InputImage.fromBitmap`.
2. **Build paragraphs.**
   - Iterate `TextBlock`s in reading order: sort by top, then left, after uprighting.
   - Join each block's lines with a space.
   - Remove hyphenation: a line ending in "-" followed by a lowercase start joins without the hyphen.
   - For Chinese, Japanese and Korean, join lines without spaces.
   - Each block becomes one row in the list and one unit of translation.
3. **Detect the language once,** on the full recognised text. Use that one source language for the whole job. Show it in the header ("Detected: French").
4. **Translate robustly.**
   - One request per paragraph, at most 2–3 at a time, keeping the order.
   - Show rows as they finish.
   - If a row fails, show it with a per-row Retry button. Never discard the rest.
   - Back off and retry on HTTP 429 (Google's rate limit).
5. **Engine policy (client decision; recommended).** Use the online engine first when connected, and on-device ML Kit only when offline. ML Kit's on-device models are clearly weaker, and today they are used whenever a model happens to be downloaded. This improves quality across the whole app, not only the camera.
6. **Harden `MiscTranslate`.**
   - Use POST, or split text above about 4,000 characters, instead of putting it in the URL.
   - Reuse and close the `LanguageIdentification` client instead of creating a new one per call.
   - Replace `GlobalScope` with a lifecycle scope where possible.
7. **Fix `checkInternetConnection`** with `NetworkCapabilities` (`NET_CAPABILITY_INTERNET` + `VALIDATED`).
8. **"View all"** uses the same paragraph text, joined with blank lines.
9. **Optional script coverage:** add `text-recognition-devanagari` for Hindi, Marathi and Nepali. In Detect mode, pick the recogniser from a first Latin pass plus language ID.
10. **Legacy screen:** `CameraTranslationActivity` is only reachable from the legacy dashboards, and its preview never starts. Delete it, or confirm it is unreachable and leave it.

**Check.** Test with:
- one page in portrait, one in landscape-left, one in landscape-right
- a page with hyphenation
- a two-column page
- a Spanish page in Detect mode
- airplane mode in the middle of a translation

Expected: paragraphs read top to bottom, whole sentences are translated, and nothing is lost when one row fails.

### H. Fun Words rejects real words (row 19): M, Confirmed

**Cause.**
- Each level's tiles are exactly one stored word, shuffled.
- A full-length guess is compared only to that word (`WordGameActivity.kt:260`): `currentWord.equals(solWord, true)`. Anything else plays the "wrong" animation (`:274–276`).
- 235 of the 1,385 puzzles have other valid English words from the same letters. For example, level 2 "art" accepts neither "rat" nor "tar", and level 7 "low" rejects "owl". These are exactly the tester's examples.

**Fix.**
1. **Accept any valid English word that uses all the tiles.** Treat it as solving the level, then show: "✓ 'rat' is correct too! Puzzle word: **art**". Reveal and translate the puzzle word's meaning as today.
2. **Word list.** Use a bundled asset, `wordgame_words_en.txt`.
   - Generate it once, with a script in `tools/`, from `dictionaries/en_US_wordlist.combined.gz`.
   - Keep 3–12-letter words.
   - Drop entries flagged `offensive`, `nonword`, `abbreviation`, `medical` or `babytalk`.
   - Apply a frequency floor so obscure words don't count.
   - Load it lazily into a `HashSet`. Pure Kotlin, so it can be unit-tested.
   - **Alternative:** query the already-shipped `res/raw/main_en.dict` through `ReadOnlyBinaryDictionary.isInDictionary`. No new asset, but it needs JNI and has no offensive-word filter.
3. **Clean `sorted_word.json`** (inside `assets/translated-phrases.zip`):
   - 19 duplicate words
   - 22 entries that are not pure letters (`deja-vu`, `Vis-à-vis`, `harbor/harbour`, `L’esprit-de-l’escalier`)
4. **Small fixes:**
   - Row-splitting for long words (`:224–228`).
   - Consistent `Locale.ROOT` lowercasing (`:118` vs `:171`).
5. **Unit tests** for `WordGameLogic.isAcceptedGuess(target, guess, dictionary)`: art/rat/tar, low/owl, wrong words rejected, case handling.

### I. Phrasebook (4th tab) wrong or badly translated (row 20): M (code) + L (data), Confirmed

**Causes.**
1. **Bengali data is misaligned.**
   - In `bn.json`, Problems → Authority, two entries share the key `"ভি"`, so one silently overwrites the other.
   - From row 5 on, every Bengali phrase sits next to the wrong English phrase.
   - Row 9 is out of range. `CustomExpandableListAdapter.kt:135–138` catches that exception and then does `return convertView!!`. That either shows a recycled, wrong pair or crashes the app.
   - The other 56 language files are aligned.
2. **The detail screen ignores language changes.**
   - `PhrasebookDetailedActivity.onResume` → `setSelectedLng()` (`:234–275`) **replaces** the listeners set in `onCreate`.
   - The From/To pickers become plain `startActivity` calls (no result), and the swap button never reloads the list.
   - Changing languages inside a category updates the header, but rows stay in the old language. Text-to-speech then speaks them in the *new* language's voice.
3. **The baked translations are poor** (pre-generated machine translation in `assets/translated-phrases.zip`, 57 languages × 223 phrases):
   - **English source errors** carried into every language: "sggest", "inlcuded", "resturaunt", "vegitarian", "Ninteen", "Thankyou", "soar throat", "allergic to antibodies" (should be antibiotics), "I need to got to", "My name is ." with no blank, a duplicate soya line.
   - **No context:** "Right" (direction) translated as "correct" in fr, de, it, pt, tr, zh, id, nl and ar.
   - **Left in English:** "Ninteen" in about 22 languages, plus several others.
   - **Garbled Latin fragments** in the gu, kn, mr and bn files.
4. **Lower confidence:**
   - Process death resets `Misc.phrasebookPosition` to 0, which shows the wrong category.
   - `PhrasesFragment` can load twice at once.

**Fix, code (2.6).**
1. Set the detail screen's listeners up in one place. On any language change or swap, reload with `getAllText()`. Use the Activity Result API.
2. Make the adapter bounds-safe:
   - no `!!` on `convertView`
   - inflate when null
   - when a translation is missing, show the English and a "translation unavailable" hint, or translate it live.
3. Pass the category through the Intent and `savedInstanceState` instead of the static `Misc.phrasebookPosition`.

**Fix, data (needs a client decision on budget and quality bar).**
1. **Fix the English source** using the list above. Make ambiguous phrases explicit: "Turn right", "I have a sore throat", "My name is ___".
2. **Give phrases stable IDs.**
   - New format: one canonical list `[{id, category, subcategory, en}]`, plus per-language maps `{id: text}`.
   - The adapter looks phrases up by ID instead of by position, so a bad file can never shift the rows again.
3. **Regenerate the translations** with a script in `tools/`, using a production engine (Google Cloud Translation v3 or DeepL; costs and keys come from the client) and the context-rich English.
4. **Automated checks in the script:**
   - same ID set in every language
   - no duplicate keys
   - the output differs from the English input
   - no Latin-script words inside non-Latin scripts
   - the length ratio is within bounds
5. **Human spot-check** of the top 5–10 languages by user count (from Firebase Analytics).
6. **Unit test:** parse every language file and assert the same ID set as English and no duplicate keys. Use Gson's `JsonReader` to detect duplicates, because `org.json` is stubbed in JVM tests.

---

## 10. Other defects found along the way

These are worth fixing in the same release, because each one feeds a sheet row.

| Defect | Where | Why it matters |
|---|---|---|
| The online-connectivity check fails on Wi-Fi-only devices | `Misc.kt:488–499` | Every online translation fails on tablets |
| Translation text sent in the URL, no 429 backoff, new `LanguageIdentification` client per call | `MiscTranslate.kt` | Long-text failures, rate-limit failures, leaks |
| `observeForever` leaks in the keyboard strip | `SuggestionStripView.java:210–225` | Memory and stale-view updates after theme changes |
| Old `CameraTranslationActivity`: preview never starts | `CameraTranslationActivity.kt` | Dead code that looks live |
| `PhrasebookDetailedActivity` / `PhrasesFragment` double loads | see I.4 | Rare wrong-category display |
| Contacts dictionary hard-coded off (`useContactsDict = false`, `LatinIME.java:767, 784, 2059`) | `LatinIME.java` | No contact-name suggestions. Probably intended (it needs the READ_CONTACTS permission); confirm with the client |

---

## 11. Suggested order and releases

The order is chosen so that the shared root causes go first, which clears the most sheet rows soonest.

**Release 2.6: "everything the tester can feel" (about 8–10 dev days + 2 days QA)**

| Order | Workstream | Rows cleared |
|---|---|---|
| 1 | E.1–E.2: load the dictionaries, clean the history once (tiny change, biggest visible gain) | 12 |
| 2 | A: settings apply live (A1–A6) | 4, 5, 6, 8, 10 |
| 3 | B1: return to WhatsApp; B2 quick fix; B3 language key | 9, 11 (partly), 7 |
| 4 | C: theme | 13 |
| 5 | D1 + D2: swap and share | 2, 3 |
| 6 | H: Fun Words | 19 |
| 7 | I code fixes (I.1–I.3), plus a quick hot-fix of `bn.json` | 20 (partly) |
| 8 | Section 10 defects | — |

**Release 2.7: features and the bigger rework (about 8–12 dev days + QA)**

| Workstream | Rows |
|---|---|
| G: camera translation | 14, 15 |
| F1 + F2: clipboard history and recent-clip chip | 16, 17, 18 |
| B2 proper: in-app "Keyboard languages" | 11 (fully) |
| E.3–E.6: suggestion tuning and extra dictionaries | 12 (polish) |

**Content track, in parallel:** the I data regeneration for row 20. It depends on the client's translation budget and reviewers.

If the client wants a single release, 2.6 and 2.7 can merge. Budget about 4 weeks including QA.

---

## 12. Test plan

**Device matrix:**
- Pixel 3a (Android 12)
- one Samsung (One UI 5+)
- one Xiaomi (MIUI/HyperOS)
- ideally one Android 14/15 device
- one Wi-Fi-only tablet, or a phone with mobile data off and the SIM removed, for G-R6

**Install scenarios:** fresh install, and upgrade from the 2.5 Play build. Both for every keyboard row.

**Per-row acceptance:** each row's "Check" above is the pass condition. Write the result in the sheet's Status column and the build number in the Version column.

**Regression checklist (every build):**
- typing, gesture typing, auto-correct
- the keyboard Translate pill
- the language pickers on the keyboard toolbar
- emoji and clipboard views
- the voice key
- rotation
- ads: no app-open ad over keyboard screens; ads unchanged elsewhere
- notifications, widget
- in-app purchase screen

**Automated:** the plain-JVM unit tests added above (preference-default consistency, Fun Words acceptance, phrasebook ID and duplicate checks, OCR paragraph builder and de-hyphenation, swap logic). Run them with `./gradlew :app:testDebugUnitTest`.

**Build:** `:app:assembleDebug` and `lintVitalRelease` stay green. `lintVitalRelease` failed before (see commit `3e15aca`), so run it before every release build.

---

## 13. Risks

| Risk | Mitigation |
|---|---|
| A1 changes how settings storage is opened; a bug there could reset users' keyboard settings | Never delete; only read and copy. Test the upgrade over 2.5 with non-default settings. |
| Theme default change (C.3) alters what existing users see | Only the *unset* default changes, and Light keeps today's look |
| In-app languages (B2 proper) touches subtype switching, which behaves differently per OEM | Ship behind a pref; test on all three OEMs; keep the system screen reachable as a fallback |
| Turning on the dictionaries (E) also turns on auto-correct, which existing users have never seen | Pick the default strength with the client; mention it in the release notes; the setting stays user-switchable |
| Clipboard reading is restricted on Android 10+ and is privacy-sensitive | Only read while this is the default keyboard; skip sensitive clips and password fields; store history in credential-encrypted storage; never log or upload it |
| Online-first translation (G.5) increases traffic to an unofficial Google endpoint | Back-off and retry, offline fallback; consider the paid Cloud Translation API for heavy features |
| Phrasebook regeneration cost and quality | Script + automated checks + human spot-check; ship per language as each is verified |

---

## 14. Decisions needed from the client

1. **Keyboard defaults (A2):** sound OFF, vibration ON, emoji key ON, number row OFF, language key automatic. OK?
2. **Theme (C):** default Light or Follow system? Simplify the Appearance screen to Light / Dark / Follow system?
3. **Languages (B2):** quick fix only in 2.6, with the in-app "Keyboard languages" screen in 2.7? Or both in 2.6?
4. **Language key behaviour (B3):** cycle this keyboard's languages (Gboard-like, recommended), or jump to the next keyboard app?
5. **Translation engine policy (G.5):** online first when connected (recommended), or keep on-device first?
6. **Fun Words (H):** should another valid word *solve* the level (recommended), or only earn a "valid word, try again" message?
7. **Phrasebook data (I):** budget for a paid translation API, and human reviewers for the top languages?
8. **Clipboard (F):**
   - Keep the last 10 items with no time limit (recommended), or Gboard's "delete after 1 hour"?
   - How long should the copied-link chip stay offered? About 3 minutes is recommended.
9. **Suggestions (E):**
   - Clear users' learned typing history once in 2.6 (recommended; it is full of learned misspellings)?
   - Which default auto-correct strength?
   - Should more languages get dictionaries?

---

## 15. Sheet update template

When an item ships, write in the sheet:
- **Version:** 2.6 (17)
- **Status:** Done / In QA
- **Comments:** a one-line root cause, for example:
  - Row 4: "Keyboard now applies settings instantly. The cause was a stale settings cache; no mic refresh needed."
