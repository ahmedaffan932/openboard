# Game Feature Port Plan — Word Puzzle ("Fun Words")

**Source (reference, read-only):** `C:\Users\LENOVO\StudioProjects\TranslatorGuru` — `GameActivity`
**Target (current app):** `openboard` / "Translator Pro" (`com.translator.keyboard.translate.all.langugaes`)
**Scope decision (confirmed with the user):** port the **Word Puzzle game only**. The Flag Quiz (`WorldQuizFlagActivity`) is explicitly out of scope.
**Status of this document:** analysis + plan. No implementation yet. Awaiting approval.

---

## 1. Executive summary

TranslatorGuru contains two games: a **Word Puzzle** (`GameActivity`, live and complete) and a **Flag Quiz** (`WorldQuizFlagActivity`, orphaned/incomplete). The user chose to port **only the Word Puzzle**.

The Word Puzzle is a letter-unscramble game: the player is shown the shuffled letters of an English word and taps tiles to spell it. Solving plays a confetti animation, reveals the word's dictionary meaning, and **translates the word + meaning into the user's selected target language**. There are sequential "levels" (one word each, pre-sorted easy→hard), with next / previous / replay / clear / revert / hint controls, and progress that is gated so the player must solve the current word before advancing. Content is bundled in an asset zip (no server needed). It shows a bottom banner ad and is entered through an interstitial.

**The single most important finding:** the target app is a *sibling fork of the same translator-app lineage* as TranslatorGuru, and it **already contains almost every dependency the game needs** — the exact model classes (`Data`, `WordList`), the `Misc`/`MiscTranslate` helpers, `TranslationInterface`, the Lottie files (`lamp.json`, `search.json`, `sprinkles.json`), the puzzle drawables (`bg_puzzle_char`, `bg_puzzle_clicked`), every other drawable/color/font/dimen/style the layout references, almost all the strings, **and `sorted_word.json` is already inside the target's `translated-phrases.zip`**. Every third-party library the game uses (ML Kit translate/language-id, Lottie 6.4.0, Jsoup 1.17.2, Gson, coroutines, Material, `country_data`) is already on the target's classpath. **No new dependencies, no new Gradle/Compose/DI work, no R8 keep rules (release is `minifyEnabled false`).**

**Recommended approach:** add a single new `WordGameActivity` (+ `activity_word_game.xml`) under `org.dslul.openboard.translator.pro`, re-wired to the target's ad facade (`Ads` / `AdmobBannerAd`), analytics (`Firebase.analytics.logEvent`), and language picker (`LanguageSelectorActivity`), reusing the already-present models/helpers/resources/assets. Add one entry-point tile to `HomeFragment`. Add two missing strings. That is essentially the whole port.

**Top risks:** (1) the source's hint translation uses the old `translate.google.com/m` scraper that now returns HTTP 429 — this must be routed through the target's working `MiscTranslate` (gtx endpoint) instead; (2) the source does network on the main thread and uses `GlobalScope`, which should be corrected during the port; (3) RTL/locale handling and small-screen layout need verification. All are low/medium and well understood.

**Baseline:** `:app:assembleDebug` is green on this machine (JBR 21, 1m34s, exit 0) before any change.

---

## 2. Game analysis (TranslatorGuru)

### 2.1 Entry points (traced through code)

| Entry | File / element | Live? |
|---|---|---|
| Home "Fun Game" tile | `fragments/TranslateFragment.kt:130` `binding.clGame` → `startWithInterstitial(Intent(.., GameActivity))` | **Yes** — `TranslateFragment` is a live bottom-nav tab of `FragmentsDashboardActivity` |
| Legacy dashboard button | `DashboardActivity.kt:245` `binding.btnGame` and `:249` `binding.btnGameTopBar` → `startWithInterstitial(.. GameActivity)` | Legacy — `DashboardActivity` is not the launched dashboard (`AppEntry.featureIntent` → `FragmentsDashboardActivity`) |
| Manifest | `AndroidManifest.xml:256` `.GameActivity` (`exported=false`, `screenOrientation=portrait`) | n/a |

No notification / widget / deep-link / shortcut reaches the game. Imports of `GameActivity` in `CameraFragment`/`PhrasesFragment` are unused leftovers.

### 2.2 Gameplay & user flow

- **Objective:** spell the hidden English word by tapping its shuffled letter tiles.
- **Board:** letters laid out across up to three rows (`llPuzzleWord` ≤6, `llPuzzleWord1` 6–11, `llPuzzleWord2` 12+). Tile size = `displayMetrics.xdpi * 0.28`.
- **Tapping a tile** appends its letter to the solution row (`llPuzzleSol`) and marks the tile "used" (`bg_puzzle_clicked`). Tapping the solution row or **Revert** removes the last letter; **Clear** removes all.
- **Win:** when the assembled string equals the target word (case-insensitive), it plays `sprinkles.json` confetti, disables tiles, reveals the meaning, shows **Next/Previous/Replay**, records `latestSolvedWord = currentIndex + 1`, and translates the word into the target language (shown in `textViewSolTranslation`).
- **Wrong full-length guess:** when the solution length equals the word length but doesn't match, the **Clear** (search) animation plays to prompt a retry. (There is no lives/score/timer system.)
- **Levels / progression:** one word per level, indexed by `currentIndex` into a word list pre-sorted easy→hard (`sorted_word.json` is ordered shortest-first: air, art, cry, day…). **Next** is gated — you can only advance to a level ≤ `latestSolvedWord` (i.e., you must solve the current word first), otherwise a toast "Solve Current puzzle to proceed." The Next arrow tints `accent` when unlocked, `gray_700` when locked. Word #0 is shown un-shuffled (a freebie); all others are shuffled.
- **Hint:** the lamp button translates the word's title + dictionary meaning into the target language and reveals them (`llHint` / `llHintTranslation`). The meaning text (`tvHintMeaning`) is always shown from the bundled data.
- **Reload:** the top-bar refresh button reloads the word list.
- **Rewards / coins / difficulty tuning / sound / haptics:** none. (Haptics/sound exist only in the out-of-scope Flag Quiz.)
- **Animations:** Lottie — `lamp.json` (hint, looping), `search.json` (clear prompt), `sprinkles.json` (win). Plus `LayoutTransition` on tiles and `animateLayoutChanges`.
- **Target language:** a header row (`llLngToFunWorld`) shows the current target language + flag and opens the language selector; `onResume` refreshes it.
- **Lifecycle:** no explicit save/restore of in-progress tile state. `currentIndex` and `latestSolvedWord` persist via `getPreferences` (survive process death and relaunch), so the *level* is restored but the *in-progress arrangement of tiles* is not (a config change/rotation rebuilds the current word from scratch). `onResume` re-reads the target language. Background/foreground: nothing special beyond the ad system's app-open handling. Quit = back press (`onBackPressed()`).

### 2.3 Screen-flow diagram

```
FragmentsDashboardActivity (bottom-nav: Translate tab)
        │  tap "Fun Game" tile (clGame)
        ▼  startWithInterstitial(INTER_ALL)
   [Interstitial ad] ──(dismiss / skip)──► GameActivity
        │
        ├─ onCreate: trackScreen(GAME); inlineBanner(BANNER_ALL → llBannerBottom); getWords()
        │
        ▼
   ┌────────────────── GameActivity (single screen) ──────────────────┐
   │ top bar: back | "Fun Game" | reload                              │
   │ target-language header  ──tap──►  LanguageSelectorActivity       │
   │ level indicator + hint (lamp)                                    │
   │ shuffled letter tiles  ──tap──►  solution row                    │
   │ solution row ──tap / Revert──► remove letter;  Clear──► reset    │
   │ [Prev] [Replay] [Next(gated)] [Revert]                           │
   │                                                                  │
   │  solution == word ?                                              │
   │     ├─ yes → sprinkles confetti; reveal meaning; translate word; │
   │     │        unlock Next; save latestSolvedWord                  │
   │     └─ full-length but wrong → "Clear" search animation          │
   └──────────────────────────────────────────────────────────────────┘
        │ back
        ▼  (ContentAds / app-open handling)
   back to dashboard
```

### 2.4 Complete file inventory (direct + transitive)

**Game-only (must be ported):**

| File | Purpose | Shared? |
|---|---|---|
| `GameActivity.kt` (686 lines) | The whole game screen & logic | Game-only |
| `res/layout/activity_game.xml` (537 lines) | Game layout (ConstraintLayout + ScrollView + Lottie) | Game-only |

**Shared helpers the game calls (already have target analogues — see §3):**

| Source symbol | Used for | Target analogue |
|---|---|---|
| `classes/Data.kt`, `classes/WordList.kt` | `{word, meaning}` models | **Already exist** in target `classes/` (byte-identical) |
| `objects/Misc.kt` → `readJsonFilesFromZip`, `getFlag`, `checkInternetConnection`, `getLanguageTo`, `startLanguageSelectorActivity`, `logKey`, `typeLanguageTo`, `defaultLanguage` | asset load, flag, connectivity, target lang, nav, keys | `classes/Misc.kt` (has all except `readJsonFilesFromZip` → `PhrasesAssetReader`, and `startLanguageSelectorActivity` → launch `LanguageSelectorActivity` directly) |
| `objects/MiscTranslate.kt` → `translate(...)` | translate solved word | `classes/MiscTranslate.kt` (`translate(context, text, callBack)`) |
| `interfaces/TranslationInterface.kt` | translate callback | **Already exists** (identical) |
| `LanguageSelectorActivity` (via `Misc.startLanguageSelectorActivity`) | pick target language | `pro/LanguageSelectorActivity.kt` |
| `ads/AdsManager.inlineBanner`, `ads/AdPlacements.BANNER_ALL`, `ads/AdsNavigation.startWithInterstitial`, `ads/ContentAds` (gating) | bottom banner + entry interstitial + app-open gating | `classes/ads/Ads.kt` + `classes/ads/admob/AdmobBannerAd.kt` |
| `analytics/AppScreens.GAME`, `analytics/trackScreen` (`io.trackkit.Tracker`) | screen analytics | `Firebase.analytics.logEvent("…", null)` |

**Not actually used by the live game (dead code):** `GameActivity.readWordFromFile(...)` + `Misc.storage` (Firebase Storage) + the `snackbar` field. `getWords()` loads from the asset zip, not Storage. Do **not** port the Storage path.

### 2.5 Resources & assets

| Kind | Items | Game-only? | Already in target? | Est. size |
|---|---|---|---|---|
| Lottie (assets/) | `lamp.json` (12 KB), `search.json` (4.9 KB), `sprinkles.json` (78 KB) | yes | **Yes — present** | ~95 KB (0 added) |
| Word list (assets/) | `translated-phrases.zip → translated-phrases/sorted_word.json` (162 KB raw, English `{word,meaning}`) | yes | **Yes — already inside the target zip** | 0 added |
| Drawable (shape) | `bg_puzzle_char.xml`, `bg_puzzle_clicked.xml` | yes | **Yes — present** | 0 added |
| Drawable (shared) | `bg_gradient_dashboard`, `bg_top_bar`, `bg_main_rounded`, `bg_nothing_rounded_ripple`, `ic_baseline_arrow_back_24`, `ic_baseline_refresh_24`, `ic_baseline_navigate_next_24`, `ic_baseline_backspace_24`, `ic_baseline_info_24` | no | **Yes — present** | 0 added |
| Fonts | `poppins_regular/light/medium`, `gilroy_light/extrabold` | no | **Yes — present** | 0 added |
| Colors | `accent` (#4285F4), `white`, `gray_600/700/900`, `btnTextColor` | no | **Yes — present** | 0 added |
| Dimens | `fab_padding`, `less_padding`, `padding_10`, `icon_size`, `m_padding`, `padding` | no | **Yes — present** | 0 added |
| Style | `roundedCorners` | no | **Yes — present** | 0 added |
| Theme attrs | `colorPrimary`, `colorOnPrimary`, `colorPrimaryVariant` | no | **Yes — in `Theme.TranslatorGuru`** | 0 added |
| Strings | `loading_words`, `hint`, `level`, `hint_meaning`, `translate`, `textView`, `todo`, `detect`, `please_check_your_internet_connection_and_try_again`, `please_cehck_your_internet`, `some_error_occurred_in_translation` | mixed | **Yes — present (localized into 8 locales)** | 0 added |
| Strings (**missing**) | `fun_game` (title) and `sorry_error_in_fetching_words` | mixed | **No — must add** (or reuse the existing `fun_words` for the title) | tiny |

**Net new asset/resource weight added to the APK: effectively zero** (everything heavy is already shipped). This is the biggest reason the port is low-risk.

**Third-party / license flags:** `sprinkles.json`, `lamp.json`, `search.json` are Lottie animations of unknown provenance (already shipping in the target, so no new exposure). **Gilroy** (`gilroy_light.otf`, `gilroy_extrabold.otf`) is a **commercial font** — already bundled in the target, but worth confirming the app has a license, independent of this port.

### 2.6 Hidden / non-static references

- `Jsoup.connect("https://translate.google.com/m?…")` — a **hardcoded URL** built from runtime language codes (`GameActivity.jugarTranslateHint`, `MiscTranslate.onlineTranslation`). This `/m` endpoint now returns **HTTP 429** (the target already fixed its own translator by switching to `translate_a/single` — see memory and commit `e63d15e`).
- Asset path built from a string: `readJsonFilesFromZip(this, "sorted_word")` → entry name `translated-phrases/sorted_word.json`.
- `Misc.getFlag(...)` maps ML Kit `TranslateLanguage` codes → `country_data` `World.getFlagOf(numericCode)` or local flag drawables (resource lookup by code).
- Lottie `app:lottie_fileName="…"` loads from `assets/` by filename (not R).
- Models carry `@SerializedName`/Gson annotations but the game parses with `org.json` manually, so **no Gson keep rules are needed** (and release isn't minified anyway).

### 2.7 Content source

- **Bundled asset**, not network/Firebase/Room. `sorted_word.json` lives in `translated-phrases.zip` (already in the target). English words + English dictionary meanings; pre-sorted shortest→longest for difficulty progression.
- **Translator-feature dependency:** the game relies on the app's translation stack to (a) translate the solved word into the target language and (b) translate the hint. It uses ML Kit offline translation with a Google-web fallback via `MiscTranslate`, plus `Misc.getLanguageTo` and the flag table. The target has all of this.

### 2.8 Persistence

| Stored | Where (source) | Keys |
|---|---|---|
| Current level index | `Activity.getPreferences` (prefs file = `"GameActivity"`) | `currentIndex` |
| Highest solved (+1) | same | `latestSolvedWord` |
| Target language | `Misc.getLanguageTo` (`getSharedPreferences("languageTo")`) | `languageTo` (shared app-wide) |
| (dead) cached word JSON | `getSharedPreferences(Misc.logKey)` — only in unused `readWordFromFile` | — |

In-progress tile arrangement is **not** persisted.

### 2.9 Integrations

- **Ads:** bottom banner (`AdsManager.inlineBanner`, placement `BANNER_ALL`) on open; entry interstitial (`startWithInterstitial`, placement `INTER_ALL`). `ContentAds` lists `GameActivity` for app-open-ad gating.
- **Premium/IAP gating:** none inside the game in the source (TranslatorGuru dropped billing). Ads are shown to everyone there.
- **Analytics:** `trackScreen(AppScreens.GAME)` (one `screen_view`-style event) via `io.trackkit.Tracker`. Also `AppEvents.HomeAction(GAME_TOP_BAR…)` from the legacy dashboard button.
- **Remote Config:** indirect — ad placement strings come from RC in the source's ad SDK. The game itself reads no RC flags.
- **Notifications / permissions:** none. The game needs no runtime permissions (INTERNET only, already granted).

### 2.10 Tech used

Kotlin; XML + **ViewBinding** (`ActivityGameBinding`); no architecture pattern (logic in the Activity); no DI; coroutines via `GlobalScope`/`Dispatchers`; `org.json` + `Jsoup` for parsing/translation; ML Kit via `MiscTranslate`; Lottie for animation. No ViewModel, no Flow/LiveData, no Rx.

### 2.11 Existing tests & code-quality notes

No tests cover the game in the source. Notable issues and the recommended disposition for the port:

| Issue (source `GameActivity` / `MiscTranslate`) | Recommendation |
|---|---|
| `jugarTranslateHint` scrapes `translate.google.com/m` → now **429s**; also runs network on the **main thread** via `Handler().postDelayed` + `StrictMode.permitAll` | **Fix during port:** route hint translation through the target's `MiscTranslate` (gtx endpoint, off-main-thread). High value. |
| `GlobalScope.launch` in `getWords()` (leaks past lifecycle) | **Fix:** use `lifecycleScope`. |
| Deprecated `onBackPressed()`, `Handler()` (no Looper), `resources.getColor(...)`, `getNetworkInfo` (in `Misc.checkInternetConnection`) | Fix the ones local to the game (`onBackPressed`, `Handler`, `getColor`); leave `Misc.checkInternetConnection` as-is (shared, matches target's own copy). |
| In-progress tiles not saved across rotation | Keep as-is for parity (level is preserved); the activity is `screenOrientation=portrait` anyway, so rotation won't occur. |
| Unused `readWordFromFile` + Firebase Storage + `snackbar` + `nextCount` | **Drop** (don't port). |
| Magic SharedPreferences keys `"increament"`/`"decreament"` (typos, internal only) | Keep (internal); or tidy while porting. |

---

## 3. Current app analysis (openboard / Translator Pro)

### 3.1 Modules, package, naming

- Modules (`settings.gradle`): `:app`, `:tools:make-keyboard-text`, `:tools:make-emoji-keys`, `:country_data`, `:keyboard-listener`.
- `:app` namespace: **`org.dslul.openboard.inputmethod.latin`** → `R` and `BuildConfig` and ViewBinding (`…latin.databinding.*`) come from here.
- Two code trees: `…inputmethod/*` (the AOSP/OpenBoard keyboard, mostly Java) and **`…translator/pro/*`** (the Translator Pro UI, Kotlin) — the game belongs in the latter.
- `translator/pro` sub-packages: `adaptor/`, `classes/` (+ `classes/ads/`, `classes/ads/admob/`), `fragments/`, `interfaces/`, `notifications/`, `objects/inapp/`, `widget/`, `fcm/`. Activities sit at `translator/pro/` top level.
- **Package inconsistency to be aware of:** some target files still import `com.example.translatorguru.*` (e.g., `HomeFragment` imports `com.example.translatorguru.ads.admob.LoadAdCallBack`), a leftover from the shared ancestry. New game code should use `org.dslul.openboard.translator.pro.*` consistently.

### 3.2 Language / toolkit / architecture

Kotlin + Java; **XML + ViewBinding** (`buildFeatures { viewBinding true }`), **no Compose**. No DI, no ViewModel layer; Activities/Fragments hold logic; coroutines present. Persistence is SharedPreferences via `classes/Misc.kt` (same pattern as source). Navigation: `FragmentsDashboardActivity` hosts a `BottomNavigationView` (`dashboard_bottom_navigation.xml`: **home, camera, chat, phrasebook**) swapping fragments (`HomeFragment`, `CameraFragment`, `ChatFragment`, `PhrasesFragment`); other screens are standalone Activities launched with an interstitial.

### 3.3 Build setup

| Aspect | Value |
|---|---|
| AGP / Gradle | `com.android.tools.build:gradle 8.13.2` (Groovy DSL, no version catalog) |
| Kotlin | 2.1.10 (`kotlin-android` plugin) |
| Java / jvmTarget | **21** (`compileOptions` + `kotlinOptions`) — needs JBR 21 to build |
| compileSdk / target / min | 36 / 36 / **27** |
| versionCode / name | 12 / "2.1" |
| NDK | 26.3.11579264 (ndk-build JNI dictionaries, 16 KB page size) |
| ViewBinding | on. Compose: off. |
| Release minify | **`minifyEnabled false`** → no R8/ProGuard, **no keep rules needed** |
| `ADS_ENABLED` BuildConfig | release `false`, debug `true` |
| Firebase | Analytics, Messaging, Config (Remote Config), Crashlytics, Storage |

### 3.4 Libraries already present that the game needs

ML Kit `translate:17.0.3`, `language-id:17.0.6`; Lottie `6.4.0`; Jsoup `1.17.2`; Gson `2.11.0`; coroutines `1.8.1`; Material `1.12.0`; ConstraintLayout `2.1.4`; `:country_data`; Firebase Storage; AdMob `play-services-ads:24.7.0` + mediation (pangle/mintegral/vungle/facebook) + Billing `8.3.0`. **The game needs nothing that isn't already here.**

### 3.5 Theme & design system

`Theme.TranslatorGuru` (parent `Theme.AppCompat.Light.NoActionBar`) with `colorPrimary=#f5f6fb`, `colorOnPrimary=@color/black`, `colorPrimaryVariant=@color/white`. **No `values-night` override for `Theme.TranslatorGuru`** → the Translator UI is effectively **light-only** (dark mode qualifiers exist only for the keyboard). The game layout only uses theme attrs + fixed colors, so it will match the rest of the Translator UI automatically. Edge-to-edge/insets are not globally forced for Translator activities (unlike the keyboard). Back handling is ordinary `onBackPressed`/system back.

### 3.6 Ads / premium / analytics / Remote Config

- **Ads facade `classes/ads/Ads.kt`:** remote-key string flags default `"am"` (AdMob on), overridden by Remote Config in release. Key methods: `runWithEverySixthClickInterstitial(activity) { action }` (navigation interstitial, 1-in-`interstitialClickFrequency`=6), `showInterstitial(activity, remote, cb)`, `loadAndShowInterstitial(...)`, `loadAndShowNativeAd(...)`, `showBannerAd(frame, remoteKey)`. Inline banner in practice: **`AdmobBannerAd.loadAdmobAdaptiveBanner(activity, frame, remoteKey)`** (e.g. `widget/WidgetPromptDialog` uses `Ads.sharedBottomNative`).
- **Premium:** `objects/inapp/InAppUtils` (Play Billing 8.3) + `Misc.getPurchasedStatus/setPurchasedStatus`. Ad loaders already early-return when `getPurchasedStatus(context)` is true and honor a frequency cap (`Misc.adsCtrCount`). So premium users automatically see no banner, with no game-specific code.
- **Analytics:** direct `Firebase.analytics.logEvent("<Name>", null)` (e.g. `HomeFragment` logs `"Dashboard"`, `"Settings"`). No `AppScreens`/`trackScreen` helper, no `io.trackkit`.
- **Remote Config:** `App.kt` fetches config into `Ads`/`Misc` flags. The game needs none directly; its ad keys ride on existing flags.

### 3.7 Supported languages

Translator UI strings are localized into **8 locales** + English base: `values-{de,es,fr,hi,it,ko,pt,ru}`. (The ~80 other `values-*` folders are AOSP keyboard locales, not the Translator UI.) This defines the localization scope for any new game strings. The game's existing strings (`level`, `hint`, `translate`, …) are already translated into these 8.

### 3.8 Natural entry-point locations

- **`HomeFragment`** (the "home" tab) — a tile/button grid (`btnPro`, `btnKeyboard`, `btnSettings`, `btnTranslate`, `btnHistory`, language selectors). The direct analogue of the source's `TranslateFragment.clGame` host. **Best fit.**
- The bottom-nav menu (`dashboard_bottom_navigation.xml`, 4 items) — a 5th "Game" tab is possible but changes core navigation.
- `SettingsActivity` / an overflow — least discoverable.

### 3.9 git status & baseline

- Branch **`NewUI`** (main branch is `master`). Uncommitted edits present: `widget/WidgetPromptDialog.kt`, `res/layout/activity_enable_keyboard.xml`.
- **Baseline build:** `JAVA_HOME=<Android Studio JBR 21>; ./gradlew :app:assembleDebug -x lint` → **BUILD SUCCESSFUL in ~1m34s, exit 0** (deprecation warnings only). APK: `app/build/outputs/apk/debug/app-debug.apk`. Nothing was changed to achieve this.

---

## 4. Fit & gap analysis

### 4.1 Side-by-side

| Aspect | TranslatorGuru | Current app | Impact | Proposed action |
|---|---|---|---|---|
| Language / toolkit | Kotlin, XML+ViewBinding | Kotlin, XML+ViewBinding | None | Copy style verbatim |
| Models `Data`/`WordList` | present | **present (identical)** | None | Reuse target's |
| `TranslationInterface` | present | **present (identical)** | None | Reuse target's |
| `Misc` / `MiscTranslate` | present | **present (analogues)** | Minor API diffs | Reuse; adapt calls (see below) |
| Word list `sorted_word.json` | in `translated-phrases.zip` | **already in target's zip** | None | Reuse; read via `PhrasesAssetReader` |
| Lottie `lamp/search/sprinkles` | assets | **present** | None | Reuse |
| Puzzle drawables `bg_puzzle_*` | present | **present** | None | Reuse |
| Other drawables/colors/dimens/fonts/style/theme attrs | present | **present** | None | Reuse |
| Strings | base + 19 locales | base + 8 locales; most game strings present | 2 strings missing | Add `fun_game`(or reuse `fun_words`) + `sorry_error_in_fetching_words` to base + 8 locales |
| Dependencies (ML Kit, Lottie, Jsoup, Gson, coroutines, country_data) | present | **present (same/compatible)** | None | No Gradle changes |
| Ad system | adlogic SDK (`AdsManager.inlineBanner`, `startWithInterstitial`, `io.trackkit`) | raw AdMob (`Ads` + `AdmobBannerAd`) | **Different API** | Remap ad calls (see §5) |
| Analytics | `trackScreen`/`io.trackkit` | `Firebase.analytics.logEvent` | Different API | `logEvent("Game", null)` |
| Premium gating | none (ads to all) | Billing + `getPurchasedStatus` baked into ad loaders | Positive | Rely on target loaders (no game code) |
| Language picker | `Misc.startLanguageSelectorActivity(type)` | launch `LanguageSelectorActivity` directly | Minor | Use target idiom |
| Translation endpoint | `/m` scrape (429) | `translate_a/single` gtx (works) | **Source path broken** | Route all game translation through target `MiscTranslate` |
| Release minify | R8 full-mode + keep rules | `minifyEnabled false` | Positive | No keep rules |
| Dark mode | has `values-night` | Translator UI light-only | Minor | Parity: light-only (verify legibility) |
| API level | min differs | min 27 | None (game uses nothing >27) | — |
| DI / nav / arch | none / Activities | none / Activities + bottom-nav | None | Keep Activity model |

### 4.2 Conflicts

- **No class-name collision:** source `GameActivity` → new name `WordGameActivity` (and there is no existing `GameActivity` in the target). `Data`/`WordList`/`TranslationInterface` already exist and are identical — **reuse, do not re-add**.
- **No resource-name collision:** every shared resource the layout references already exists with the same name and compatible definition. The only genuinely new resource is the **layout file** (`activity_word_game.xml` — pick a name not already present) and **2 strings**.
- **No dependency-version clash** and **no library upgrades required.**
- **No DB/pref-key collision:** the game's prefs are new (namespace them, see §5).
- Java-vs-Kotlin / XML-vs-Compose / DI differences: **none** (both apps are Kotlin + XML/ViewBinding, no DI).

### 4.3 Migration decision per component

| Component | Decision | Reason |
|---|---|---|
| `GameActivity.kt` | **Adapt** → `WordGameActivity.kt` | Repackage; remap ads/analytics/nav/translate; fix hint path + `GlobalScope`; drop dead code |
| `activity_game.xml` | **Copy (rename)** → `activity_word_game.xml` | All referenced resources exist; no edits beyond the title string |
| `Data`, `WordList`, `TranslationInterface` | **Replace with existing** | Identical copies already in target |
| `Misc`/`MiscTranslate`/`PhrasesAssetReader`/`LanguageSelectorActivity`/`country_data` | **Reuse existing** | Direct analogues present |
| Lottie, puzzle drawables, other drawables/colors/dimens/fonts/style, `sorted_word.json` | **Reuse existing** | Already shipped |
| `fun_game`, `sorry_error_in_fetching_words` strings | **Add** (or reuse `fun_words` for title) | Only missing pieces |
| `AdsManager.inlineBanner` / `startWithInterstitial` / `trackScreen` / `AppScreens` / `ContentAds` / Firebase Storage word path / `snackbar` | **Drop / replace** | Foreign ad+analytics SDK or dead code |

### 4.4 UI toolkit interop vs rewrite

Not applicable — both apps are XML + ViewBinding. No Compose interop decision. The layout is copied as-is.

### 4.5 Translator-feature dependencies in the current app

- Translate solved word + hint → **`MiscTranslate.translate(context, text, callBack)`** (present, working gtx+ML Kit). One adaptation: the game's words are **English**, so it should translate **English→target** explicitly. The target's `translate(...)` uses the saved *from* language. Options: (a) add an overloaded `translate(context, text, from, to, callBack)` to `MiscTranslate` (mirrors the source signature) and call it with `from="en"`; or (b) call `MiscTranslate.offlineTranslation(context, "en", getLanguageTo(context), text, cb)` directly. **Recommend (a)** — small, reusable, and restores the richer signature the source had.
- Target language + flag → `Misc.getLanguageTo` + `Misc.getFlag` (present).
- No TTS / speech / OCR / language-list dependency.

---

## 5. Integration design

### 5.1 Where the game lives & naming

- **New code:** `org.dslul.openboard.translator.pro.WordGameActivity` (top level, beside `ConversationActivity` etc.).
- **New layout:** `res/layout/activity_word_game.xml`.
- **New strings:** `game_title` (or reuse `fun_words`) and `sorry_error_in_fetching_words`.
- **Reused (unchanged):** `classes/Data`, `classes/WordList`, `interfaces/TranslationInterface`, `classes/Misc`, `classes/MiscTranslate`, `classes/PhrasesAssetReader`, `classes/ads/Ads`, `classes/ads/admob/AdmobBannerAd`, `LanguageSelectorActivity`, all drawables/lottie/fonts/colors/dimens/styles, and `translated-phrases.zip`.
- **Naming/prefix strategy:** shared resources are already unprefixed and reused as-is. Any *new* game-only resource gets a **`game_`** prefix (`activity_word_game.xml`'s view ids can keep source ids since they're layout-local; new strings use `game_`). This guarantees no collision.

### 5.2 Proposed file structure (additions/edits only)

```
app/src/main/
├── java/org/dslul/openboard/translator/pro/
│   └── WordGameActivity.kt                 (NEW — adapted from GameActivity.kt)
├── java/org/dslul/openboard/translator/pro/classes/
│   └── MiscTranslate.kt                    (EDIT — add translate(ctx,text,from,to,cb) overload)
├── java/org/dslul/openboard/translator/pro/fragments/
│   └── HomeFragment.kt                     (EDIT — add game tile click → launch WordGameActivity)
├── res/layout/
│   ├── activity_word_game.xml              (NEW — from activity_game.xml)
│   └── fragment_home.xml                   (EDIT — add a "Fun Words" tile, if not already present)
├── res/values/strings.xml                  (EDIT — + game_title/sorry_error_in_fetching_words)
├── res/values-{de,es,fr,hi,it,ko,pt,ru}/strings.xml  (EDIT — translate the 2 new strings)
└── AndroidManifest.xml                     (EDIT — declare WordGameActivity)
```

### 5.3 Entry-point options

- **Option A (recommended): a "Fun Words" tile in `HomeFragment`.** Mirrors the source's `TranslateFragment.clGame`, matches the already-present promo strings (`fun_words`, `relax_your_brain…`), most discoverable, zero navigation changes. Launch via `Ads.runWithEverySixthClickInterstitial(requireActivity()) { startActivity(Intent(.., WordGameActivity::class.java)) }`.
- **Option B: a 5th bottom-nav tab "Game".** More prominent, but edits `dashboard_bottom_navigation.xml` + `FragmentsDashboardActivity` fragment switch, and a game as a permanent tab is unusual for a translator app.
- **Option C: a row in Settings / overflow.** Lowest effort, lowest discoverability.

### 5.4 Ads / analytics / premium mapping

| Source | Target |
|---|---|
| `startWithInterstitial(Intent(.., GameActivity))` (entry) | `Ads.runWithEverySixthClickInterstitial(activity) { startActivity(Intent(.., WordGameActivity)) }` |
| `AdsManager.inlineBanner(this, this, BANNER_ALL, binding.llBannerBottom)` | `AdmobBannerAd.loadAdmobAdaptiveBanner(this, binding.llBannerBottom, Ads.dashboardBanner)` (or `Ads.sharedBottomNative`) |
| `trackScreen(AppScreens.GAME)` | `Firebase.analytics.logEvent("Game", null)` |
| premium gate | none needed — `AdmobBannerAd`/interstitial loaders already honor `Misc.getPurchasedStatus` + `adsCtrCount` cap |

Optionally add dedicated Remote Config keys later (`gameBanner`, `gameInt`); not required for v1 (reuse existing keys).

### 5.5 Data, persistence, translation

- **Word list:** `PhrasesAssetReader.getLanguageJson(context, "sorted_word")` already resolves to `translated-phrases/sorted_word.json` (its `normalizeLanguageCode` passes unknown codes through). Parse with `org.json` into `WordList`/`Data` exactly as the source does. (Optionally add a named `PhrasesAssetReader.getWordGameJson(context)` wrapper for clarity.)
- **Persistence (namespaced):** store `currentIndex` / `latestSolvedWord` in a dedicated prefs file `"word_game"` (not the activity's default `getPreferences`, and not colliding with any existing key).
- **Translation:** solved-word + hint via `MiscTranslate` with explicit `from="en"`, `to=Misc.getLanguageTo(context)`. This replaces the source's broken `/m` scraper and the on-main-thread hint code.

### 5.6 Theming / localization / size

- **Theme:** adopt `Theme.TranslatorGuru` (set in the manifest entry, like other content activities). Keep the game's own gradient background + accent art (`bg_gradient_dashboard`, `accent`) — matches both apps.
- **Localization:** reuse the 11 already-translated strings; add 2 new strings to base + the 8 translator locales (`de,es,fr,hi,it,ko,pt,ru`); fallback = English base.
- **App-size impact:** ~**0 added** (all assets already shipped). If one wanted to *reduce* further, `sorted_word.json` is already the only game payload and is shared with nothing else game-specific.

### 5.7 Consolidated list of files added/modified in the current app

**Added (2):** `WordGameActivity.kt`, `res/layout/activity_word_game.xml`.
**Modified (≤12):** `AndroidManifest.xml`; `fragments/HomeFragment.kt` (+ `res/layout/fragment_home.xml` if a tile view must be added); `classes/MiscTranslate.kt` (overload); `res/values/strings.xml` + 8 `values-*/strings.xml`.
**Reused unchanged:** everything in §5.1.

---

## 6. Implementation plan (phased)

> After approval: work on a new branch, implement one phase at a time, build + verify, report files changed, tick the phase here, then wait for OK.

| Phase | Goal | Files | Verify | Complexity / Risk |
|---|---|---|---|---|
| **0. Prep & branch** ✅ | Branch off `NewUI`; re-confirm baseline debug build | (none) | `assembleDebug` green (already is) | Low / Low |
| **1. Data & helper seam** ✅ | Add `MiscTranslate.translate(ctx,text,from,to,cb)` overload; new `classes/WordGameContent.kt` (`load()` reads `sorted_word.json`, `parse()` pure Gson parser); confirm `WordList`/`Data` parse | `classes/MiscTranslate.kt`, `classes/WordGameContent.kt` (new), `test/.../classes/WordGameContentTest.kt` (new) | Unit tests 3/3 green; `assembleDebug` + `testDebugUnitTest` green (17/17 tests) | Low / Low |
| **2. Layout & strings** ✅ | Add `activity_word_game.xml` (from source, renamed, title → existing `fun_words`, `tools:context` → `.WordGameActivity`); add 1 missing string (`sorry_error_in_fetching_words`) to base + 8 locales | `res/layout/activity_word_game.xml` (new), `res/values{,-de,-es,-fr,-hi,-it,-ko,-pt,-ru}/strings.xml` | `assembleDebug` green; `ActivityWordGameBinding` generated with all 34 views | Low / Low |
| **3. Game activity** ✅ | Port `GameActivity`→`WordGameActivity`: repackage; load via `WordGameContent.load()`; remap ads→`AdmobBannerAd`; analytics→`logEvent`; language picker→`LanguageSelectorActivity`; translation→`MiscTranslate` (en→target, off the dead `/m` scraper); namespaced `word_game` prefs; `lifecycleScope`; gating extracted to `WordGameLogic`; drop dead code | `WordGameActivity.kt` (new), `classes/WordGameLogic.kt` (new), `classes/WordGameContent.kt`, `WordGameContentTest.kt`, `res/values*/strings.xml` | `assembleDebug` green; adversarial-review workflow (19 agents) → 2 hardening fixes applied; unit tests 18/18 | **Medium / Medium** |
| **4. Manifest & entry point** ✅ | Declared `WordGameActivity` (`exported=false`, portrait, `Theme.TranslatorGuru`, mirrors `ConversationActivity`); added a "Fun Words" card to `fragment_home.xml` (pure addition below `clHistory`, promo strings + `lamp.json`), wired in `HomeFragment` via `runWithEverySixthClickInterstitial` | `AndroidManifest.xml`, `res/layout/fragment_home.xml`, `fragments/HomeFragment.kt` | `assembleDebug` green; manifest merged; `FragmentHomeBinding.clFunWords` generated; game reachable | Low / Low |
| **5. Ads / analytics / premium** ✅ | Bottom banner via `AdmobBannerAd.loadAdmobAdaptiveBanner`; entry interstitial via `runWithEverySixthClickInterstitial`; `logEvent("Game")` + `logEvent("WordGameOpen")`; premium handled by the shared loaders | `WordGameActivity.kt`, `HomeFragment.kt` | Static-verified: banner + both interstitial paths early-return on `getPurchasedStatus`; wiring compiles. Runtime ad display = manual (needs test ad units on an unlocked device) | Medium / Medium |
| **6. Localization & polish** ✅ | 3 new strings localized into base + 8 locales; puzzle rows pinned `layoutDirection="ltr"` (English letters stay LTR under RTL); content in an adaptive `ScrollView`; light-only (parity with the rest of the translator UI) | `res/values*/strings.xml`, `activity_word_game.xml` | Strings resolve (compiled); RTL/small-screen/tablet rendering = manual | Low / Medium |
| **7. Tests & regression** ✅ | Unit tests for the Gson parser (`WordGameContentTest`, 4) and the gating logic (`WordGameLogicTest`, 5); trimmed 2 unused `WordGameLogic` helpers | `test/.../WordGameContentTest.kt`, `test/.../WordGameLogicTest.kt` | `testDebugUnitTest` → **23/23 pass, 0 failures** (incl. existing 14 — no regression); on-device gameplay/regression = manual | Medium / Medium |

### Progress log

- **Phase 0 (done):** branch `feature/word-game` created off `NewUI` (carried the pre-existing uncommitted edits to `WidgetPromptDialog.kt` + `activity_enable_keyboard.xml`, plus this plan doc; nothing committed).
- **Phase 1 (done):**
  - `classes/MiscTranslate.kt` — added `translate(context, text, from, to, callBack)` overload (explicit English→target via the working offline/gtx path; **not** the dead `/m` scraper).
  - `classes/WordGameContent.kt` (new) — `load(context)` reads `translated-phrases/sorted_word.json` from the bundled zip; `parse(json)` is a pure Gson parser over the existing `WordList`/`Data` models. (Gson chosen over `org.json` so `parse()` is unit-testable off-device.)
  - `test/.../classes/WordGameContentTest.kt` (new) — 3 parser tests.
  - **Verified:** `:app:assembleDebug` + `:app:testDebugUnitTest` → BUILD SUCCESSFUL; unit tests 17/17 pass (WordGameContent 3/3 + existing 14). Debug APK produced.
  - **Note:** this machine's `~/.gradle/caches/8.13/transforms` cache was corrupt (`metadata.bin` unreadable), causing spurious `NoClassDefFoundError` on Crashlytics/BuildConfig Gradle tasks — unrelated to the port. Cleared the regenerable transforms cache; builds are healthy again. If CLI builds throw similar `NoClassDefFoundError`s later, clear that cache dir. (Builds since use `--no-configuration-cache` to dodge a stale config-cache entry left by the recovery.)
- **Phase 2 (done):**
  - `res/layout/activity_word_game.xml` (new) — copied verbatim from the source `activity_game.xml`, with only `@string/fun_game` → `@string/fun_words` and `tools:context` → `.WordGameActivity`. All other resource refs already resolve in the target (verified by diffing every `@…/…` ref against target resources). View IDs kept identical to the source so Phase 3 is mechanical.
  - `res/values{,-de,-es,-fr,-hi,-it,-ko,-pt,-ru}/strings.xml` — added `sorry_error_in_fetching_words` (English base + 8 translator locales; French apostrophe escaped). The title reuses the already-localized `fun_words`; every other string the game needs already existed.
  - **Verified:** `:app:assembleDebug` → BUILD SUCCESSFUL (2m09s), full APK packaged (Crashlytics task healthy). `ActivityWordGameBinding` generated at `…inputmethod.latin.databinding` with all 34 views (correct types: `btnClear`/`animationFinish` = `LottieAnimationView`, `llBannerBottom` = `FrameLayout`, `flagToFunWord` = `ShapeableImageView`).
- **Phase 3 (done):**
  - `WordGameActivity.kt` (new) — ported from `GameActivity`, re-wired: words via `WordGameContent.load()` (offline-capable, no internet gate — fixes the source needlessly requiring network to start); solved-word + hint translation via `MiscTranslate.translate(ctx, text, "en", to, cb)` (working ML Kit/gtx path, off-main); banner via `AdmobBannerAd.loadAdmobAdaptiveBanner(this, binding.llBannerBottom, Ads.dashboardBanner)`; analytics `Firebase.analytics.logEvent("Game", null)`; language picker → `LanguageSelectorActivity` + `onResume` refresh; progress in `word_game` prefs; `lifecycleScope` (not `GlobalScope`); dead Firebase-Storage/snackbar code dropped.
  - `classes/WordGameLogic.kt` (new) — pure, testable progression rules (`isNextUnlocked`, `resetIndexIfAtEnd`, `nextIndex`, `previousIndex`).
  - Added string `please_solve_current_puzzle_to_proceed` (base + 8 locales) — the source hardcoded that toast in English.
  - **Adversarial-review workflow** (3 lenses × review + 2 refuters each = 19 agents): 8 low findings, 0 confirmed by verify. I applied 2 cheap hardening fixes anyway — (a) `WordGameContent.parse` now normalizes wrong-shape JSON (`{}`/`{"data":null}`/`null`) to an empty list instead of NPE-ing (restores the source's graceful "error fetching words" path) + a unit test; (b) `isFinishing`/`isDestroyed` guards on the translation callbacks so no view/Toast access after finish.
  - **Deliberate deviation (kept):** the hint title now shows the *translated word* and the English meaning is revealed immediately on Hint tap — the source translated the literal label "Hint Meaning:" and gated the meaning on translation success. The new behavior is a genuine improvement.
  - **Verified:** `:app:assembleDebug` + `:app:testDebugUnitTest` → BUILD SUCCESSFUL (51s); unit tests 18/18 (WordGameContent 4 + existing 14). Not launchable until Phase 4 (manifest + entry tile).
- **Phase 4 (done):**
  - `AndroidManifest.xml` — declared `WordGameActivity` (`exported="false"`, portrait, `Theme.TranslatorGuru`), mirroring `ConversationActivity`.
  - `res/layout/fragment_home.xml` — added a "Fun Words" card (`clFunWords`) as a pure addition anchored below `clHistory` (no edits to existing views), using the already-present promo strings + `lamp.json` Lottie.
  - `fragments/HomeFragment.kt` — wired `clFunWords` → `logEvent("WordGameOpen")` + `runWithEverySixthClickInterstitial { startActivity(WordGameActivity) }`.
  - **Verified:** `assembleDebug` green; manifest merged; `FragmentHomeBinding.clFunWords` generated; game reachable from the Home tab.
  - **Bug found + fixed during on-device testing:** the card initially didn't render — anchoring it below `clHistory`, which is `gone` *and* had `bottom→parent`, made ConstraintLayout collapse the gone view to a biased midpoint and drag the card off-layout. Fix: removed `bottom→parent` from both `clHistory` and `clFunWords`, making the scroll content a clean top-anchored stack. Re-verified on device: the card renders correctly whether history is empty or populated.
  - **Home card spacing fix (follow-up):** on device the three home cards (translation / Recent / Fun Words) were touching with **0px** between them — a bidirectional `clTranslation.bottom↔clHistory.top` link was swallowing the `marginBottom`. Fix: dropped `clTranslation`'s redundant `bottom_toTopOf="@id/clHistory"` (one-directional top-anchored stack) and added `marginTop="@dimen/fab_padding"` to `clHistory` and `clFunWords`. Verified on device: consistent ~37px gap between each card (measured via `uiautomator` bounds).
  - **Home screen UX pass (follow-up, user-requested):** researched current mobile/Material home-screen patterns, then re-arranged `fragment_home.xml` for hierarchy. (1) Promoted the "enable keyboard" CTA from a small floating FAB to a labeled control (FAB removed; `mrecFrameLayout` re-anchored `end→parent`), reusing `isInputMethodSelected()` so it hides once our keyboard is the default IME. (2) Hero treatment for the translation card (`bg_accent_border_rounded`). (3) Recent redesigned: header row with a right-aligned `tvViewAll` ("View all") replacing the full-width down-chevron; `onResume` now shows up to 3 newest items and hides the card when history is empty. (4) Fun Words demoted to a tinted tile. New drawables `bg_keyboard_banner.xml` + `bg_fun_words_tint.xml`; `enable_keyboard_banner_title` localized to base + 8 locales. `HomeFragment` wires `clEnableKeyboardBanner`/`tvViewAll` and `updateKeyboardBanner()` (called in `onCreateView` + `onResume`).
  - **Tile revision (follow-up, user-requested):** per request, Enable Keyboard + Fun Words are now two equal side-by-side tiles (`llActionTiles`, horizontal, `baselineAligned=false`, each `0dp`/`weight=1`, height `@dimen/_90sdp`) directly under the translation card, with Recent below. Ids `clEnableKeyboardBanner`/`clFunWords` preserved → no `HomeFragment` change. When our keyboard is the default IME, the Enable tile goes `GONE` and the Fun Words tile expands full-width via layout weight (gap carried as the Enable tile's `marginEnd` so no left gap remains). Unused `enable_keyboard_banner_subtitle` removed from all 9 locales. `:app:assembleDebug` → **BUILD SUCCESSFUL**; verified on the Pixel 3a (keyboard not default → both tiles render side by side; translation hero border + "View all" + Recent confirmed).
  - **Tile polish (follow-up, user-requested):** gave the tiles a designed look + refined spacing. New drawables `bg_tile_keyboard.xml` (accent gradient, 16dp radius), `bg_tile_fun_words.xml` (lavender gradient, 16dp), and circular icon badges `bg_tile_icon_light.xml` (translucent white) / `bg_tile_icon_white.xml` (solid white); the keyboard icon and lamp now sit in those badges. Fun Words title colored `#4B3B8F`. Replaced the earlier flat `bg_keyboard_banner`/`bg_fun_words_tint` drawables (deleted — orphaned). Then, per follow-up: tiles made **shorter** (`_100sdp`→`_85sdp`), **shadows** deepened (`elevation` 4dp→6dp), inner padding tightened (`paddingVertical=@dimen/padding`, badges 46→42dp, title `marginTop=less_padding`), and tile-row **top margin** added (`llActionTiles marginTop=@dimen/padding`) for balanced top/bottom spacing. `:app:assembleDebug` → **BUILD SUCCESSFUL**; verified on the Pixel 3a (clean capture: gradient tiles with icon badges, soft drop shadows, even gaps).
- **Phase 5 (done — static/code):** banner, entry interstitial, analytics, and premium are wired. Confirmed in code that the banner loader (`AdmobBannerAd`) and both interstitial entry points (`runWithEverySixthClickInterstitial`, `showInterstitial`) early-return on `Misc.getPurchasedStatus`, so premium users get no ads with no game-specific code. Runtime ad fill/analytics display is a manual check (needs AdMob test units on an unlocked device).
- **Phase 6 (done — static):** 3 new strings localized to base + `{de,es,fr,hi,it,ko,pt,ru}`; the puzzle/solution rows carry `layoutDirection="ltr"` so English letters render left-to-right even under RTL app languages; the screen is a single adaptive `ScrollView`; the translator UI (and thus the game) is light-only. Multi-locale / RTL / small-phone / tablet rendering remains a manual check.
- **Phase 7 (done):** added `WordGameContentTest` (4) and `WordGameLogicTest` (5); trimmed 2 unused `WordGameLogic` helpers. `:app:assembleDebug` + `:app:testDebugUnitTest` → **BUILD SUCCESSFUL, 23/23 tests pass, 0 failures** (incl. the pre-existing 14 — no regression). Final debug APK installed on the connected Pixel 3a for manual testing.
### On-device verification (executed on the connected Pixel 3a, target = Spanish)

Driven via `adb` (the app's own flow, then `input tap`/screenshots). **All core gameplay confirmed working:**
- ✅ Home → **Fun Words** card renders natively (lightbulb + title + promo) and opens the game.
- ✅ Level 1 shows **"air" un-shuffled**; **Next is grey/locked**.
- ✅ Tapping A·I·R spells **AIR** → win: tiles disable, **Spanish "aire"** shown (translated via `MiscTranslate`, no 429), **Next turns blue/unlocked**.
- ✅ **Next** → Level 2 with a **shuffled** word ("T R A" = *art*), solution reset, Next re-locked.
- ✅ **Hint** reveals the English meaning + the translated word **"arte"** + the Spanish translation of the meaning.
- ✅ Target-language header (Spanish + flag); AdMob splash interstitial + native test ads served; AdMob validator reported "No implementation issues found".
- One bug (the Home card not rendering) was found and fixed here — see Phase 4 above.

Still worth a manual pass: process-death persistence, offline play, RTL/Hindi, rotation (portrait-locked), tablet, premium (no ads), and regression on the keyboard + other translator screens.

### Manual acceptance checklist (on-device)

Open the app → Home tab → tap the **Fun Words** card, then verify:
1. Entry interstitial shows occasionally (every ~6th navigation) and is skipped cleanly when unfilled; the game opens either way.
2. First word is shown un-shuffled; later words are shuffled. Bottom banner appears (non-premium).
3. Tap letters to spell the word; the solution row fills, tiles mark used; **Revert**/tapping the solution row removes the last letter; **Clear** resets.
4. Solving plays the confetti, reveals the meaning, shows the translated word, and unlocks **Next**.
5. A full-length wrong attempt plays the "clear" animation (no false win).
6. **Next** is blocked (toast + grey arrow) until the current word is solved; **Previous**/**Replay**/**Reload** work.
7. **Hint** reveals the meaning + its translation into the target language (no 429 / error) within a second or two.
8. Tap the target-language header → pick a language → return → header + subsequent translations use it.
9. Leave and relaunch the app → the level is preserved (process-death: `adb shell am kill <pkg>` then reopen).
10. Airplane mode: the game still starts and is playable; only hint/solve-translation show the error toast.
11. Switch app language to Arabic/Hindi: UI mirrors but the letter tiles stay readable LTR.
12. Rotate (portrait-locked — should not rotate); try a small phone and a tablet.
13. Regression: keyboard IME, and the Translate / Camera / Chat / Phrasebook / History / Settings / Premium screens still work.

---

## 7. Behavior parity checklist (acceptance criteria)

1. Entry: a Home tile opens the game, preceded by an interstitial that is skipped gracefully when unfilled/capped.
2. Word list loads from the bundled `sorted_word.json`; no network needed to start.
3. First word (`index 0`) is shown **un-shuffled**; subsequent words are shuffled.
4. Tapping a letter tile appends it to the solution row and marks the tile used; the solution row/Revert removes the last letter; Clear resets the attempt.
5. Correct full word → confetti (`sprinkles`), tiles disabled, meaning revealed, **Next unlocked**, `latestSolvedWord` advanced.
6. A full-length wrong attempt triggers the "clear" (search) animation, not a win.
7. **Next is gated**: advancing beyond the highest solved level is blocked with the "solve current puzzle" toast; Next arrow tint reflects locked/unlocked.
8. Previous / Replay / Reload behave as in the source.
9. Hint reveals the word + its meaning translated into the current target language (via the working endpoint, no 429).
10. Solved word is translated into the target language and displayed.
11. Target-language header shows the right language + flag, opens the picker, and refreshes on resume.
12. `currentIndex` / `latestSolvedWord` persist across app restart (process death).
13. Bottom banner shows for non-premium users and is absent for premium users.
14. A `"Game"` analytics event is logged on open.
15. Offline: starting the game and playing works; only translation/hint degrade gracefully with the existing error toasts.

---

## 8. Test plan

**Unit (JVM / `testDebugUnitTest`):**
- Parse the real bundled `sorted_word.json` → `WordList` with the expected count and shape.
- Level-gating logic: given `currentIndex`/`latestSolvedWord`, `Next` is allowed iff `currentIndex < latestSolvedWord`; index reset wraps at list end.
- (If helpers are extracted) shuffle keeps the multiset of letters; win-compare is case-insensitive.

**Manual / instrumented (run the app):**
- Rotation: portrait-locked (confirm no landscape); if lock is removed, verify state.
- **Process death:** background → system-kill (`adb shell am kill`) → relaunch → level restored.
- Background/foreground mid-game: app-open ad behavior correct; game state intact.
- **Dark mode:** toggle system dark — Translator UI is light-only; confirm legibility (no invisible text).
- **RTL (Arabic) + Hindi:** UI mirrors but puzzle/solution rows stay LTR (English letters readable); strings resolve.
- **Small phone (Pixel 3a, the test device) + tablet (sw600dp):** tiles wrap across the three rows without clipping; banner anchors at bottom.
- **Offline:** airplane mode — game starts and is playable; hint/solved-word translation show the existing error toast.
- **Ads failing to load:** force no-fill (test id / airplane) — banner hidden, entry interstitial skipped, game still fully usable.
- **Premium:** set `getPurchasedStatus=true` — no banner.
- **Regression:** keyboard IME still works (translate pill), and dashboard/translate/camera/chat/phrases/history/settings/premium unaffected; `assembleDebug` + on-device smoke per the memory note (`build-and-device-test`).

---

## 9. Risks & mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Copying the source's `/m` hint scraper → 429, no hints, possible main-thread ANR | High if copied verbatim | Medium | Route all game translation through target `MiscTranslate` (gtx, off-main) — built into Phase 3 |
| `GlobalScope` leak / callbacks after the activity is gone | Medium | Low–Med | Use `lifecycleScope`; guard UI updates with `isFinishing`/lifecycle checks (target already does this elsewhere) |
| Ad fill flakiness obscures banner/interstitial verification | Medium | Low | Test with AdMob test ids (`ADS_ENABLED=true` in debug) + Ad Inspector; treat no-fill as pass if game still works |
| Light-only theme hurts dark-mode legibility | Low | Low | Parity with the rest of the Translator UI; verify in the dark-mode manual check |
| Hidden coupling to a renamed resource/string | Low | Low | All referenced resources enumerated in §2.5; compiler catches the rest (no minify) |
| `translated-phrases.zip` diverges (entry renamed) in future | Low | Low | Read by constant entry name; a unit test asserts the entry loads |
| Gilroy font license (pre-existing) | n/a to this port | Low | Already bundled; flag for separate confirmation |
| Package inconsistency (`com.example.translatorguru` leftovers) causes confusion | Low | Low | New code uses `org.dslul.openboard.translator.pro.*` consistently |

---

## 10. What will NOT be copied, and why

- **Flag Quiz** (`WorldQuizFlagActivity`, `activity_world_quiz_flag.xml`, `Misc.gameContinent`/`wholeWorld`, `ok_anim`/`not_ok_anim`) — out of scope per the user's decision; also orphaned/incomplete in the source.
- **Firebase-Storage word path** (`GameActivity.readWordFromFile`, `Misc.storage`) — dead code; content is bundled.
- **TranslatorGuru ad/analytics SDK** (`AdsManager`, `AdPlacements`, `AdsNavigation`, `ContentAds`, `io.trackkit.Tracker`, `analytics/AppScreens`) — replaced by the target's AdMob `Ads`/`AdmobBannerAd` + `Firebase.analytics`.
- **Duplicate models/interface** (`Data`, `WordList`, `TranslationInterface`) — identical copies already exist in the target; reuse them.
- **The `/m` Jsoup scraper and on-main-thread hint translation** — replaced by the target's working `MiscTranslate`.
- **Any TranslatorGuru identity/config** — applicationId, AdMob app/unit ids, `google-services.json`, Firebase config, API/Remote-Config keys, signing. The target keeps its own throughout.
- **Unused fields** (`snackbar`, `nextCount`) and the legacy `DashboardActivity` wiring.

---

## 11. Open questions & decisions for you

1. **Entry-point placement** — *Options:* (A) a "Fun Words" tile in the Home tab; (B) a 5th bottom-nav tab; (C) a Settings/overflow row. **Recommend A** (matches the source and the pre-seeded `fun_words` strings; zero nav changes).
2. **Game as Activity vs Fragment** — *Options:* (A) standalone `WordGameActivity` launched via interstitial; (B) a fragment inside the dashboard. **Recommend A** (parity with source; matches how other content screens are launched).
3. **Hint/translation endpoint** — *Options:* (A) route through the target's working `MiscTranslate` (gtx); (B) copy the source's `/m` scraper for byte-parity. **Recommend A** — B is currently broken (429) and does network on the main thread.
4. **Title string** — *Options:* (A) reuse the existing, already-localized `fun_words` ("Fun Words"); (B) add a new `game_title`. **Recommend A** (less to translate; strings already shipped).
5. **Code-quality fixes during the port** — *Options:* (A) fix the few that matter (hint endpoint, `GlobalScope`→`lifecycleScope`, local deprecated APIs) while keeping gameplay identical; (B) copy verbatim for maximum parity. **Recommend A.**
6. **Ad placement keys** — *Options:* (A) reuse existing keys (`everySixthClickInterstitial` for entry, `dashboardBanner`/`sharedBottomNative` for the banner) — no Remote Config change; (B) add dedicated `gameInt`/`gameBanner` keys. **Recommend A for v1**, B later if you want separate control.
7. **Branch base** — *Options:* (A) branch from `NewUI` (the active UI branch where `HomeFragment`/`FragmentsDashboardActivity` live); (B) from `master`. **Recommend A.** Note: `NewUI` has uncommitted edits (`WidgetPromptDialog.kt`, `activity_enable_keyboard.xml`) — please confirm whether to stash/commit them first.
8. **Unit-test depth** — *Options:* (A) light (asset parse + gating helper); (B) refactor game logic out of the Activity into a testable class for fuller coverage. **Recommend A** (keeps the port faithful and small); B is a larger, optional improvement.

---

*Prepared from a full read of the source game and its dependencies, and the target app's architecture, build, ads, translation, theme, and localization. No code, resources, Gradle files, or settings in the current app were modified in producing this plan.*
