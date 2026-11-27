# Language Learning Module Plan — "Learn"

**Target app:** `openboard` / "Translator Pro" (`com.translator.keyboard.translate.all.langugaes`, v2.9)
**Status of this document:** v1 is designed and implemented (2026-11-24, uncommitted). See **§0** for what was built and what is still unverified. §1–§12 are the original plan.
**Date:** 2026-11-24
**Constraints:** no paid services or APIs. Content, audio and translation must use free routes.

---

## 0. Implementation status (v1, 2026-11-24)

### 0.1 Decisions taken (answers to §12)

| Question | Answer |
|---|---|
| Launch courses | English, Spanish, French, German, Italian |
| Entry point | Home "Learn a language" card + Phrasebook "Practice phrases" card. No new tab. |
| Free tier | **Changed 2026-11-26:** Learn and Phrase Practice are Pro features, locked like Camera and Conversation. Pro opens them for good; a rewarded ad opens them until the app is closed. No per-unit locks. Interstitial every 2 lessons, never inside a lesson. Listed on the premium screen as "Language Courses & Phrase Practice". |
| Phrase Practice (58 languages) | In |
| Keyboard layout auto-switch | Out. Typing fields still set `imeHintLocales`, so Gboard switches by itself. |
| Speaking exercises | Out |
| Native-speaker review | Not done yet: all course text is AI-drafted, see §0.5 |

### 0.2 Design (Figma TranslatorProZyro, page "App Screens")

- **"11 · Learn · Entry & Course":** Home card (new / in progress), onboarding (language → level → daily goal + reminder), course map, unit-locked dialog, course switcher sheet, learning settings.
- **"12 · Learn · Lessons":** tip, new word, choose translation, listen & choose, word tiles, match pairs, type it, correct sheet, wrong sheet, quit dialog, lesson complete.
- **"13 · Learn · Phrase Practice":** Phrasebook entry card, topic picker, flashcard front/back, practice done.
- Every screen also has a dark copy. New icons were added to the Design System page: `Icon/flame`, `Icon/graduation-cap`, `Icon/target`.

### 0.3 Code

All new code is in `app/src/main/java/org/dslul/openboard/translator/pro/learn/`.

- **`engine/` (pure Kotlin + Gson, JVM unit-tested):**
  - `LearnContent` (models + parser)
  - `AnswerChecker` (case / punctuation / accent / typo tolerant)
  - `Srs` (Leitner 1-2-4-8-16 days, streak rules, local days)
  - `LessonBuilder` (generates all exercise types)
  - `LessonSession` (queue, retry-once, accuracy, XP)
  - `CoursePlan` (next lesson, unit state, locks)
  - `PhraseDeck` (pairs two phrasebook languages)
- **Screens:**
  - `LearnHomeActivity` (course map)
  - `LearnOnboardingActivity`
  - `LessonActivity` (lessons, practice, test-out, phrase quiz, result)
  - `LearnSettingsActivity`
  - `PhrasePracticeActivity`
  - `PhraseCardsActivity`
  - plus `CourseSwitcher` and `LearnEntryCards` (Home / Phrasebook cards)
- **Services:**
  - `LearnStore` (prefs + per-course progress file)
  - `LearnRepository` (asset loading)
  - `LearnSpeech` (device TTS)
  - `LearnAds` (reuses the existing placements: Fun Words native, every-sixth-click interstitial, unlock-premium rewarded)
  - `LearnReminder` + `LearnReminderReceiver` (daily streak reminder)
  - `LearnConfig` (Remote Config)
  - `PhraseProgress`
- **Edited existing files:**
  - `AndroidManifest.xml`
  - `HomeFragment` (1 line) + `fragment_home.xml`
  - `PhrasesFragment` (1 line) + `fragment_phrases.xml`
  - `AppAnalytics` (learn events + screen names)
  - `PreSplashScreenActivity` (applies `LearnConfig`)
  - `NotiTargets` (`learn` target)
  - `BootReceiver` (re-arms the reminder)
- **Resources:**
  - layouts `activity_learn_*`, `learn_*`, `item_learn_*`, `activity_phrase_*`, `item_phrase_topic`, `dialog_learn_message`, `sheet_learn_courses`
  - drawables `bg_learn_*`, `ic_ui_flame`, `ic_ui_graduation_cap`, `ic_ui_target`
  - `values/strings_learn.xml` + 8 translations
  - `values/styles_learn.xml`

### 0.4 Content

`app/src/main/assets/learn/` (about 250 KB) holds:
- the syllabus: 12 units × 5 lessons × 6 items = 360 concepts;
- course text for en / es / fr / de / it, which doubles as the meaning text for learners in those languages;
- meaning-only text for pt / ru / hi / ko;
- 7 grammar tips per course, in English, plus the English course's tips translated into the 8 other app languages.

`python tools/learn-content/validate.py` reports 0 errors. Authoring rules are in `tools/learn-content/README.md`.

### 0.5 Not done / still to check

- **Device test on the Samsung Note 20 (2026-11-24): passed.**
  - **Covered:**
    - Home card (new and in-progress states) and onboarding
    - Every exercise type, right/wrong/accent feedback, retry-once and the quit dialog
    - Lesson and practice results, streak and week dots, daily goal
    - Interstitial after the 2nd session, locked-unit dialog and rewarded unlock
    - Learning settings and the course switcher
    - Phrase Practice (flashcards, quiz, mixed review)
    - Dark mode
    - Hindi UI with the English course
    - The streak reminder, which fired at the set time
  - **Fixed during the test:**
    - Round flags
    - Match pairs no longer pair identical texts ("No"/"No")
    - Taller word-tile answer area
    - "Back to phrasebook" returns to the Phrasebook tab
    - "You reviewed N phrases" wording
    - Lost first tap while the voice loads
    - Course falls back when the app language becomes the course language
    - Install-voice dialog when the phone has no voice for the course
  - **Not tested:**
    - The Pixel 3a.
    - Tapping the reminder on a phone that has finished the app's own first-run onboarding. On this test phone the app is still in first open, and the existing splash logic drops notification targets during first open.
    - A filled rewarded ad: the test ad unit returned "No fill", so the app used its unlock-anyway path.
- **Samsung voices:** the Note 20's Samsung TTS engine had no French voice. The app now offers the free voice download once, and lessons work without it (listening questions become reading questions).
- **Native-speaker spot check of units 1–3 per course:** in-app "Report" sends `learn_report_issue` to Firebase to help find problems.
- **Remote Config:** add the `learn_*` keys to the console if you want to tune them. The defaults work without them.
- **Known content choices** (documented by the content authors):
  - French and Spanish use the same text for some greetings (e.g. "Buenas noches" for good evening and good night). The engine never offers two identical answers in one question.
  - German has no female forms for `müde`, `krank` and similar words, so there are none in the text.
- **Departures from the design:**
  - The settings row "Sound effects" became "Vibration" (no sound assets are shipped).
  - "Content credits" became "About the lessons", because no third-party sentence data is used.
  - Figma was updated to match both.

### 0.7 Learn is a Pro feature (2026-11-26)

- **Lock:** `LearnAccess` guards every way in: the Home card, the Phrasebook "Practice phrases" card, and the course map itself (for notification and shortcut entries).
  - Free users see the dashboard's existing "Unlock Premium Access" dialog: **Upgrade to Pro**, or **Watch Ad**.
  - **Watch Ad** opens Learn and Phrase Practice until the app is closed.
  - When Remote Config switches the Pro screen off (`Misc.isProScreenEnabled`), Learn is open to everyone, like the Camera and Conversation tabs.
  - Analytics use the existing `feature_lock_*` / `feature_unlock` events with features `learn` and `phrase_practice`.
- **Per-unit locks removed:**
  - unit PRO chips;
  - the locked-unit dialog;
  - the test-out unit unlock;
  - `learn_free_units` / `learn_unlock_hours`;
  - `learn_unit_unlock` / `learn_unit_lock_view`;
  - their strings in 9 languages.
- **Premium screen:** a "Language Courses & Phrase Practice" row (Basic 🔒, Pro ✓), translated into the 8 languages. The Home card subtitle now reads "5 minutes a day · beginner courses".
- **Figma:**
  - Premium (light and dark) has the new row;
  - "Learn · Pro lock" replaces "Learn · Unit locked";
  - course maps show all units open.
- **Tested on the Note 20:**
  - the premium row;
  - the lock on the Home card and the close button;
  - Watch Ad unlocking Learn;
  - all units open on the course map;
  - Phrase Practice open in the same session and locked again after an app restart.

### 0.6 Rest of the plan, added after PR #1 (2026-11-24)

- **Streak freeze:**
  - A learner can hold up to 2 freezes. Each one covers one missed day, and they are spent automatically.
  - How to get one: tap the streak chip on the course map, then watch the unlock rewarded ad. Pro users get a free one each week.
  - Frozen days show as snowflakes in the week row. A toast says when a freeze saved the streak.
  - Analytics: `learn_streak_freeze_earned` and `learn_streak_freeze_used`.
  - Figma: "Learn · Streak & freeze" in section 11.
- **Grammar tips in the learner's language:** the Spanish, French, German and Italian courses now have tips in all 8 app languages (28 new files). English stays the fallback.
- **Word of the day and weekly summary notifications:**
  - Word of the day: 9:00, showing a word the learner has met, preferably one due for review.
  - Weekly summary: Sunday 18:00, with lessons, XP and streak, sent only after an active week.
  - Each has its own switch in Learning settings.
  - Both are re-armed at every app start.
  - The debug build's `DebugNotiReceiver` can fire them with types `learnReminder`, `learnWord`, `learnWeekly`.
- **"Continue lesson" app shortcut:** appears once the learner has a course. It goes through the splash, like the other shortcuts.
- **Backup:**
  - `allowBackup` is now on, but the rules include only Learn's files: `files/learn/`, `learn.xml`, `phrase_practice.xml`. Nothing else the app stores is backed up.
  - The keyboard-listener library's `allowBackup="false"` is overridden with `tools:replace`.
- **Checks:**
  - The build passes, 110 unit tests pass, and `validate.py` reports 0 errors.
  - **Tested on the Note 20 (2026-11-25):**
    - A freeze covered a missed day: the streak was kept and the "saved your streak" toast appeared.
    - The streak dialog matches Figma. Earning a freeze took the count from 0 to 1.
    - The two new settings switches appear.
    - The shortcut is registered with its icon.
    - The French course's tip shows in German with a German app language.
    - Word of the day and the weekly summary posted with correct German text.
    - The three alarms are armed: 20:00 reminder, 09:00 word of the day, Sunday 18:00 summary.
    - Android reports `ALLOW_BACKUP` for the app.
  - **Not tested:** an actual cloud backup and restore round trip. That needs a Google backup transport.

---

## 1. Executive summary

Add a **"Learn"** module: short daily lessons (3–5 minutes) that teach beginner vocabulary and everyday phrases (CEFR A1, then A2). It includes spaced-repetition review, streaks, listening through the device's text-to-speech, and optional speaking practice. Everything runs **offline from bundled content**, with no server, no paid API and no new library.

**How many languages to start with:** **5 courses at launch: English, Spanish, French, German and Italian.**

- All five are Latin-script, so they share 100% of the lesson engine and answer-checking rules.
- Each one already has a UI locale in the app (or is the base language), a layout in **our own keyboard**, Google TTS voices on practically every phone, and deep free sentence data.
- Together they are 5 of the 7 most-studied languages in the world (§2).
- Lessons are explained in the **app's UI language** (English + the 8 translator locales). Word meanings can be shown in any of the **58 phrasebook languages**, so "Learn English" works for Urdu, Arabic or Indonesian speakers too.

**Bonus, almost free:** a **"Phrase Practice"** mode (flashcards and quizzes) over the existing phrasebook, available in **all 58 languages from day one**. It reuses content the app already ships, so it adds no new translation risk.

**Next wave (Phase 8):** Portuguese, Russian and Hindi first (the keyboard already has their layouts), then Korean and Japanese (which need script units plus keyboard work), then others based on analytics.

**Rough effort for v1:** about **8–10 developer-weeks** for one developer, with content written in parallel.

---

## 2. Why 5 languages, and why these 5

### 2.1 Learner demand (external data)

- Duolingo's **2024** report ranks: English, Spanish, French, German, Japanese, Italian, Korean, Chinese, Portuguese, Hindi. English was the top language in 135 countries.
- Secondary sources on the **2025** report say Japanese passed German for #4 and Korean passed Italian for #6. This is not yet confirmed against Duolingo's own English page.

### 2.2 Fit with this app

| Language | Demand rank | Translator UI locale | Our keyboard layout | Free content depth | TTS voice | Script work | Verdict |
|---|---|---|---|---|---|---|---|
| English | #1 | base | `en_US/GB/IN/AU` | very deep | universal | none | **Launch** |
| Spanish | #2 | `values-es` | `es`, `es_US` | very deep | universal | none | **Launch** (also the app's default "to" language) |
| French | #3 | `values-fr` | `fr`, `fr_CA`, `fr_CH` | very deep | universal | none | **Launch** |
| German | #4–5 | `values-de` | `de`, `de_DE`, `de_CH` | very deep | universal | none | **Launch** |
| Italian | #6–7 | `values-it` | `it`, `it_CH` | very deep | universal | none | **Launch** |
| Japanese | #4–5 | — | **none** | deep | most devices | 3 scripts (kana + kanji) | Phase 8b |
| Korean | #6–7 | `values-ko` | **none** (no Hangul subtype in `res/xml/method.xml`) | thinner | most devices | Hangul unit | Phase 8b |
| Portuguese | #9 | `values-pt` | `pt_BR`, `pt_PT` | deep | universal | none | Phase 8a |
| Russian | ~#11 | `values-ru` | `ru` | deep | universal | Cyrillic unit | Phase 8a |
| Hindi | #10 | `values-hi` | `hi`, `hi_ZZ` | moderate | most devices | Devanagari unit | Phase 8a |
| Chinese | #8 | — | **none** | deep | most devices | characters + tones | Phase 9 |

### 2.3 Why not fewer, why not more

- **Why not 1–2:** the engine is language-agnostic, so each extra Latin-script course costs mostly content and QA (about 1 week each). A "translate all languages" app that teaches one language looks thin.
- **Why not 10+:** every taught language needs **verified** text, its own answer-checking quirks, a TTS check on real devices, and grammar tips. Japanese, Korean and Chinese also need script-intro units, and **our keyboard cannot type them yet**, so typing exercises would break. Five shared-engine languages give the best demand coverage per week of work.

### 2.4 Validate before locking slot #5

The app already logs `translate_language_select` with `side=to` (`AppAnalytics.translateLanguageSelect`). Check the top "to" languages in Firebase:

- If Italian is far down and Japanese or Korean is near the top **for our users**, swap it in for slot #5.
- That course would then use tiles and choice exercises only (no typing) until a layout exists.

---

## 3. What the app already has (reuse map)

| Existing piece | Where | Reuse in Learn |
|---|---|---|
| Parallel phrasebook: **58 languages × 223 phrases**, 9 categories (Store, Hotel, Bar, Restaurant, Essentials, Medical, Problems, Time/Date/Numbers, Travel), keyed by position (`v1…vN`) | `assets/translated-phrases.zip`, `classes/PhrasesAssetReader.kt` | Seed content for 7 of 12 units. Meaning glosses in 58 languages. Phrase Practice for all 58. |
| **1,385 English words** with definitions | `translated-phrases/sorted_word.json` | English vocabulary pool, word of the day |
| Word game UI patterns + Lottie (`sprinkles`, `green_tick`, `lamp`) | `WordGameActivity.kt`, `classes/WordGameLogic.kt`, `assets/` | Correct-answer and lesson-complete animations |
| TextToSpeech | `TranslateActivity`, `ConversationActivity`, adapters | Listen exercises and audio for every item |
| Speech recognition (`RecognizerIntent`) | `HomeFragment`, `TranslateActivity`, `ConversationActivity` | Speak exercises |
| ML Kit offline translate (model download) | `adaptor/LanguagesAdapter.kt`, `classes/MiscTranslate.kt` | **Fallback only**, for glosses in languages without content. Never for the taught text. |
| Daily phrase notification, scheduler, channels, prefs, local epoch-day helper | `notifications/PhraseOfTheDay.kt`, `NotiScheduler.kt`, `NotiChannels.kt`, `NotiPrefs.kt` | Streak reminder, word of the day from the user's course |
| Feature-lock dialog + rewarded unlock + premium screen | `FragmentsDashboardActivity.kt` (lock dialog, `loadAndShowUnlockRewardedAd`), `Ads.unlockPremiumRewardedAd`, `PremiumScreenActivity` | Lock later units: "Watch ad (24h)" or "Upgrade" |
| Analytics helper with an automatic screen-name map | `classes/AppAnalytics.kt` | `screen_learn_*` plus learn events (§6.6) |
| Remote Config fetch | `classes/App.kt` | `learn_*` kill switch and tuning keys |
| WorkManager (`work-runtime-ktx 2.9.0`) | `app/build.gradle` | Daily streak check / reminder fallback |
| App shortcuts | `notifications/AppShortcuts.kt` | "Continue lesson" long-press shortcut |
| Keyboard subtypes for 80+ locales; a TODO to honor `EditorInfo.hintLocales` | `res/xml/method.xml`, `inputmethod/latin/LatinIME.java:1039` | Switch the keyboard to the target language during typing exercises (§6.5) |

**Missing:**
- No database library (no Room), so storage needs a decision (§6.2).
- No CJK keyboard layouts.
- No learning-specific design in Figma yet.

---

## 4. Product design

### 4.1 User flow

```
Home "Learn a language" card / Phrasebook "Practice" / notification / app shortcut
        │
        ▼
First run? ──yes──► Onboarding: 1) I want to learn [flags]  2) My level (new / know some)
        │                         3) Daily goal (5/10/15 min)   4) Reminder time (+ noti permission)
        no                                   │
        ▼                                    ▼
Course map (units as a vertical path, streak 🔥 + today's XP header, "Practice" button)
        │
        ├─► Lesson (10–15 exercises, progress bar, Check → feedback sheet with answer + 🔊)
        │        └─► Result: XP, accuracy, time, streak animation → (free users) interstitial per cap
        ├─► Practice (spaced-repetition review of due items)
        └─► Locked unit → feature-lock dialog: Watch ad (unlock 24h) / Upgrade
```

"Know some" opens the unit-1 checkpoint (a 15-question test). Passing it unlocks units 1–3.

### 4.2 Curriculum (same concept skeleton for every course)

| # | Unit | Source |
|---|---|---|
| 1 | Basics & greetings | Phrasebook Essentials/Greetings + new |
| 2 | Introductions (name, country, languages) | new |
| 3 | Numbers & time | Phrasebook Time, Date and Numbers |
| 4 | Food & restaurant | Phrasebook Restaurant + Bar |
| 5 | Shopping & money | Phrasebook Store + Essentials/Money |
| 6 | Travel & directions | Phrasebook While Traveling + Essentials/Directions |
| 7 | Hotel | Phrasebook Hotel |
| 8 | Family & people | new |
| 9 | Daily routine (present-tense verbs) | new |
| 10 | Health & emergencies | Phrasebook Medical Help + Problems |
| 11 | Days, months, weather | Phrasebook Days/Months + new |
| 12 | Describing things (colors, adjectives) | new |

- About **12 units × 5 lessons × ~7 new items ≈ 400 items** per course, plus a checkpoint per unit.
- The phrasebook already covers ~223 of these items.
- **Grammar tip cards** per unit and per course: gender and articles (es/fr/de/it), formal "you" (usted/vous/Sie/Lei), German cases (light touch), English articles and word order for "Learn English".
- Tips are written in English and translated into the 8 UI locales.

### 4.3 Exercise types

All exercises are **generated** from the content, so nobody writes exercises by hand.

| Type | What the user does | v1 |
|---|---|---|
| New item card | Sees text + gloss, hears TTS (normal and slow) | ✅ |
| Choose the meaning | Target → pick from 4 meanings in their language | ✅ |
| Choose the translation | Their language → pick from 4 target options | ✅ |
| Listen & choose | TTS plays, pick the matching text | ✅ (hidden if no voice) |
| Word tiles | Build the sentence from shuffled tiles + 2 distractors (no typing needed) | ✅ |
| Match pairs | 5 target ↔ meaning pairs | ✅ |
| Type it | Type the translation with tolerant checking | ✅ |
| Speak it | Say the sentence; compare speech-to-text with the expected answer; "Can't speak now" skips it | v1.1 |
| Fill the blank (cloze) | Pick the missing word | v2 |

- **Lesson mix rule:** the first exposure of an item is a card, then choice or listening, then tiles or typing. Wrong answers come back at the end of the lesson.
- **Distractors** come from the same unit and the same kind (word vs sentence), and can never equal the answer after normalization.

**Answer checking (pure Kotlin, unit-tested):**
1. Unicode NFC, case-fold, trim, collapse spaces, strip punctuation.
2. Accept anything in `alt`.
3. Accent-insensitive match → accepted, with the note "Watch the accent: é".
4. One-edit typo on answers longer than 4 characters → accepted as "Almost! Typo: …".
5. Contractions (I'm / I am) are handled through `alt`.

### 4.4 Spaced repetition

Use **Leitner, 5 boxes**, with review intervals of 1, 2, 4, 8 and 16 days. It is simpler and more predictable than SM-2, and good enough for A1.

- Per item: `box`, `due` (local epoch day, the same approach as `PhraseOfTheDay.localEpochDay`), `seen`, `right`, `wrong`.
- A wrong answer sends the item to box 1.
- **Practice** shows due items first, then the weakest ones.

### 4.5 Motivation

- XP: 10 per lesson, +5 for a perfect lesson.
- Daily goal and a **streak** based on the local date, safe across timezone changes.
- **Streak freeze:** earned with a rewarded ad (free) or 1 per week (premium).
- Lottie lesson-complete animation and a weekly summary notification.

**Not in v1:**
- Hearts/lives: they punish beginners and drive churn.
- Leaderboards: they need a backend.

---

## 5. Content

### 5.1 Data model (concept-keyed parallel content, the same idea as the phrasebook)

```
assets/learn.zip
└── learn/
    ├── manifest.json            {"version":1,"courses":[{"lang":"es","version":1,"units":12,"beta":false}, …]}
    ├── concepts.json            [{"id":"greet.hello","kind":"word","en":"Hello","unit":1,"lesson":1,
    │                              "phrasebook":"Essentials/Greetings/v1"}, …]
    ├── course/<lang>.json       units → lessons → concept ids, tip ids, checkpoint ids
    ├── text/<lang>.json         {"greet.hello":{"t":"Hola","alt":["¡Hola!"],"tiles":null,"rom":null,"note":null,"review":false}}
    └── tips/<uiLang>/<lang>.json   grammar tips in the explanation language
```

- `text/<lang>.json` serves two purposes:
  - the **taught text** when `<lang>` is the course;
  - the **gloss** when `<lang>` is the user's language.
- `tiles` (pre-split tokens) and `rom` (romanization) are unused in v1. They are in the schema now so that Korean, Japanese and Chinese need no schema change later.
- **Gloss lookup order:**
  1. `text/<userLang>`
  2. phrasebook entry via the `phrasebook` ref (58 languages)
  3. ML Kit offline translation, cached on device and marked "auto-translated"
  4. English
- **Size:** text only, about 0.5 MB uncompressed and roughly 150–250 KB zipped. Audio comes from device TTS, so there are **no audio files**.

### 5.2 Content pipeline (free route)

1. **Syllabus:** write `concepts.json` (English) from the phrasebook, the 1,385-word list and standard A1 topics. Claude can draft this directly in the repo.
2. **Taught text per course:**
   - Phrasebook items: reuse the existing translations, which already ship.
   - New sentences: match from **Tatoeba**, which is human-written and human-translated and licensed **CC BY 2.0 FR** (attribution required).
   - Gaps: drafted by Claude and marked `"review": true`.
3. **Human check:** a native speaker spot-checks at least units 1–3 of each course, since every user sees those. Who does this is the client's call (§12).
4. **Validator + packer** (`tools/learn-content/`, a Python script or Gradle task) checks that:
   - every concept exists in every launch course, with no empty `t`;
   - `alt` entries are normalized and unique;
   - tile counts are sane and options do not duplicate each other;
   - every unit has at least N items;
   - no blocked/offensive words are used (reuse the keyboard's blocked list).

   It then builds `assets/learn.zip`. This runs before each release.

### 5.3 Licensing

- Tatoeba text: **CC BY 2.0 FR** → add a "Content credits" row in Settings.
- Tatoeba **audio is not used**: each recording has its own license, some non-commercial, and this app shows ads.
- No CC BY-NC content anywhere.

---

## 6. Technical design

### 6.1 Package layout (all new code under `translator/pro/learn/`)

```
translator/pro/learn/
├── LearnHomeActivity.kt         course map, streak/XP header, Practice button
├── LearnOnboardingActivity.kt   course → level → goal → reminder
├── LessonActivity.kt            hosts exercise fragments, progress bar, Check button, feedback sheet
├── LessonResultActivity.kt      XP / accuracy / streak animation, native ad slot
├── exercise/                    ExerciseFragment (base) + IntroCard, Choice, Listen, Tiles, Match, Type, Speak
├── engine/
│   ├── LessonBuilder.kt         concepts → exercise list (mix rules, seeded shuffle)
│   ├── DistractorPicker.kt
│   ├── AnswerChecker.kt         normalization, alt, accents, typo tolerance
│   └── Srs.kt                   Leitner scheduling
├── data/
│   ├── LearnContent.kt          models
│   ├── LearnContentReader.kt    reads learn.zip (same pattern + cache as PhrasesAssetReader)
│   ├── GlossProvider.kt         lookup order from §5.1
│   ├── LearnProgressStore.kt    per-course progress file
│   └── LearnPrefs.kt            course, goal, reminder time, streak, XP (prefs file "learn")
├── LearnSpeech.kt               one shared TTS: availability check, slow mode, release on destroy
└── LearnNoti.kt                 streak reminder + word of the day, plugged into NotiScheduler/NotiChannels
```

**Existing files to edit:**
- `AndroidManifest.xml`: 4 activities.
- `fragments/HomeFragment.kt` + `fragment_home.xml`: Learn card, placed next to `clFunWords`.
- `PhrasebookDetailedActivity.kt`: "Practice these phrases" button.
- `classes/AppAnalytics.kt`: screen map + events.
- `classes/App.kt`: Remote Config keys.
- `classes/ads/Ads.kt`: learn ad keys.
- `notifications/NotiScheduler.kt`, `NotiChannels.kt`, `NotiTargets.kt`: reminder type.
- `notifications/AppShortcuts.kt`: "Continue lesson".
- `SettingsActivity.kt`: reminder, goal, reset progress, content credits.
- `res/values/strings.xml` + 8 `values-{de,es,fr,hi,it,ko,pt,ru}`: about 70 new strings.

### 6.2 Persistence

The project has no Room, and each course has at most ~500 progress rows.

- **Recommendation:** one JSON file per course in `filesDir/learn/`, written atomically (`AtomicFile`) after every lesson. Streak, XP and goal go in a `"learn"` SharedPreferences file.
  - No new dependencies and no kapt/KSP setup in the Groovy + NDK build.
  - Small enough to load fully into memory.
- **Move to Room only if** cross-course queries or sync are ever needed.
- **Check `allowBackup` / backup rules** so progress survives reinstall and phone migration.

### 6.3 Text-to-speech

- One shared `TextToSpeech` instance.
- `isLanguageAvailable(locale)` is checked per course:
  - Missing voice data → offer `ACTION_INSTALL_TTS_DATA`.
  - If that is declined → hide Listen exercises for the course.
- Slow button uses `setSpeechRate(0.7f)`.
- **Must test on the Samsung Note 20**, which defaults to the Samsung TTS engine with different voices and quirks.

### 6.4 Speech (v1.1)

- `RecognizerIntent` with `EXTRA_LANGUAGE` = target tag and `EXTRA_PREFER_OFFLINE`.
- Compare the top results with `AnswerChecker` in loose mode: word-level match ≥ 80%, punctuation ignored.
- Many devices need network for this, so hide the exercise when offline.
- The mic permission flow already exists elsewhere in the app.

### 6.5 Keyboard integration (the differentiator: we own the keyboard)

1. **Auto-switch layout in typing exercises.** The Type exercise's `EditText` sets `imeHintLocales = LocaleList(targetLocale)`.
   - Implement the TODO at `LatinIME.java:1039`: when a hint locale matches an *enabled* subtype, switch to it for that field, then restore the previous one.
   - If the subtype is not enabled, show a one-tap "Add the Spanish layout" hint.
   - Gboard users are unaffected; they switch by hand.
2. **Word-of-the-day chip** in the keyboard toolbar, opt-in. It opens Learn.
3. **(Phase 7b, opt-in, off by default) "Learn while you type".** When the user types a word from their course vocabulary in their own language, the suggestion strip also shows the target-language word.
   - Purely local lookup in the bundled vocabulary; nothing is logged or sent.
   - Disabled in password and sensitive fields (the IME already detects these).
   - Respects the blocked-words filter.

### 6.6 Ads, premium, analytics, Remote Config

**Ads:**
- **Never inside a lesson.**
- Interstitial after the result screen every N lessons (`learn_interstitial_every`, default 2), through the existing `Ads` facade, which already skips premium users and applies frequency caps.
- Small native ad on the course map and the result screen.
- Rewarded ads: streak freeze, unlock a locked unit for 24h, extra practice. Reuse the feature-lock dialog with a new `FEATURE_LEARN = "learn"`.

**Free vs premium:**
- Free: units 1–3 of every course fully (`learn_free_units`). Each later unit is unlocked for 24h with a rewarded ad. *(Superseded 2026-11-26 by §0.7: all of Learn is a Pro feature.)*
- Premium: all units, no ads, weekly streak freeze, speaking exercises.

**Analytics events** (bundle params, under Firebase limits; no per-tap event spam):

| Event | Params |
|---|---|
| `learn_onboarding_step` | `step`, `value` |
| `learn_course_select` | `course`, `base` |
| `learn_lesson_start` | `course`, `unit`, `lesson`, `kind` (lesson/practice/checkpoint) |
| `learn_lesson_complete` | `course`, `unit`, `lesson`, `accuracy`, `seconds`, `xp` |
| `learn_lesson_quit` | `course`, `unit`, `lesson`, `at_exercise` |
| `learn_streak` / `learn_streak_lost` | `days` |
| `learn_unit_unlock` | `course`, `unit`, `method` (rewarded/premium/checkpoint) |
| `learn_tts_unavailable` | `course`, `engine` |
| `learn_report_issue` | `course`, `concept`, `reason` (in-app "Report a problem" on each exercise: a free content QA loop) |

Screens: `screen_learn_home`, `screen_learn_onboarding`, `screen_lesson`, `screen_lesson_result`, added through the `AppAnalytics` activity map.

**Remote Config:**
- `learn_enabled` (kill switch / staged rollout)
- `learn_courses` (`"en,es,fr,de,it"`)
- `learn_free_units`
- `learn_interstitial_every`
- `learn_unlock_hours`
- `learn_phrase_practice_enabled`
- ad unit keys

**Success metrics, 4 weeks after full rollout:**
- ≥ 15% of DAU open Learn.
- ≥ 50% of learners finish their first lesson.
- Learner D7 retention clearly above the non-learner baseline.
- ≥ 1 lesson per learner per active day.
- No crash-free-rate regression.

### 6.7 Entry points

- **v1:**
  - A "Learn a language" card on the Home tab. After the first lesson it becomes "Continue Spanish · 🔥 4 · 1 lesson left today".
  - A "Practice these phrases" button in the phrasebook.
  - The reminder notification.
  - A "Continue lesson" app shortcut.
- **Later:** if the metrics hold, promote Learn to a bottom-nav tab. This changes core navigation and needs a design pass.

### 6.8 Design

- Screens are needed in the **TranslatorProZyro** Figma file: onboarding (4), course map, 7 exercise types, correct/wrong feedback sheets, result, practice, lock dialog variant, settings rows.
- They reuse the redesign's tokens, cards and fonts.
- Either the designer makes them, or Claude drafts them in Figma for review (§12).

### 6.9 Size and performance

- About **+250 KB assets** and **+150 KB code/resources**, with **no new dependencies**.
- Lessons work fully offline.
- Content is read lazily per course and cached, the same way as the phrasebook.

---

## 7. Phrase Practice (all 58 languages, optional v1 extra)

A lighter mode reached from the phrasebook ("Practice"):

- Pick a category, then flashcards, Listen & choose and Choose the translation over the existing 223 phrases.
- Uses the same engine and SRS, with the phrasebook as the content source.
- About **3 days** of work on top of the engine.
- Gives the app a "learn any of 58 languages" promise without new content risk.
- Gated by `learn_phrase_practice_enabled`.

---

## 8. Phased implementation plan

> Work on a feature branch, one phase at a time. Build, run on the Pixel 3a + Note 20 and report after each phase.

| Phase | Goal | Main work | Est. |
|---|---|---|---|
| 0 | Decisions & design | Answer §12, check Firebase language data, Figma screens, freeze content schema | 3–5 days |
| 1 | Content foundation | Schema, `concepts.json` syllabus, validator/packer tool, **Spanish + English units 1–4** draft | 1–1.5 wk (parallel with 2) |
| 2 | Engine | Reader, `LessonBuilder`, `DistractorPicker`, `AnswerChecker`, `Srs`, progress store + JVM unit tests | 1.5–2 wk |
| 3 | Lesson UI | Onboarding, course map, lesson host + 7 exercise types, feedback, result, Practice, TTS | 2–3 wk |
| 4 | App integration | Home card, phrasebook button, reminder + word of the day, settings, analytics, Remote Config, ads/locks/premium, strings in 8 locales | 1 wk |
| 5 | Content completion | French, German, Italian; units 5–12 for all 5; native spot checks; Phrase Practice | ~1 wk per course (parallel to 3–4) |
| 6 | QA & rollout | Device matrix, offline, process death mid-lesson, TalkBack/large fonts, localized screenshots; release behind `learn_enabled`, staged 10% → 50% → 100% | 1 wk |
| 7 | Keyboard integration | `hintLocales` subtype switching, word-of-day chip; (7b) "Learn while you type", opt-in | 1 wk (+1 wk for 7b) |
| 8 | Expansion | 8a: Portuguese, Russian, Hindi (layouts exist; Cyrillic/Devanagari intro units). 8b: Korean, Japanese (script units + new keyboard layouts or a tiles-only mode) | ~1–2 wk per course |
| 9 | Data-driven additions | Chinese, Arabic (RTL), Turkish, Indonesian… ordered by `learn_course_select` and translate-language data | — |

**v1 = phases 0–6: about 8–10 weeks.**

---

## 9. Test plan

- **JVM unit tests:**
  - `AnswerChecker`: accents, `alt`, typos, punctuation, contractions, NFC vs NFD input.
  - `Srs`: box moves; due dates across midnight, DST and timezone change.
  - `LessonBuilder`: no duplicate options, distractors never equal the answer, deterministic with a seed.
  - Streak rules.
- **Content tests:** the validator runs as part of the build or release checklist and fails on missing or empty items.
- **Manual / on device (Pixel 3a + Note 20):**
  - Every exercise type × 5 courses.
  - Samsung TTS vs Google TTS.
  - Missing voice data path.
  - Speech offline.
  - Kill the app mid-lesson and resume.
  - Notification deep links.
  - Locked unit → rewarded → unlocked for 24h.
  - Premium → no ads and all units.
  - The 8 UI locales.
  - Large font / TalkBack.

---

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Wrong text in the taught language (bad reviews) | Human-translated sources first, `review` flags, native check of units 1–3, in-app "Report a problem" → `learn_report_issue` |
| No or poor TTS voice (Samsung, cheap devices) | Per-course availability check, install prompt, hide Listen exercises |
| Scope creep (this is a Duolingo-sized category) | Strict v1 cut list (§11); 12 A1 units only |
| Ads hurting learning retention | No ads inside lessons; frequency tunable remotely; watch `learn_lesson_quit` |
| Paid-service creep (Cloud Translation, hosted audio, Firebase Storage needing the Blaze plan) | Content bundled in the APK; device TTS; ML Kit offline only for glosses; no server |
| Free `gtx` endpoint 429s | Never used by Learn at runtime |
| License violations | Only own content + CC BY text; credits screen; no CC BY-NC; no Tatoeba audio |
| Keyboard can't type CJK | Those courses wait for Phase 8b or launch tiles-only |

---

## 11. Not in v1

Accounts and cloud sync, leaderboards and social features, AI chat tutor (paid APIs), recorded native audio, hearts/lives, stories/dialogues, a full placement test (unit checkpoints only), CJK/RTL courses, remote content updates (ship content with app updates).

---

## 12. Decisions needed (you / the client)

1. **Launch set:** confirm **English, Spanish, French, German, Italian**, or swap Italian for Japanese/Korean after checking Firebase `translate_language_select` (to-side) data.
2. **Entry point:** Home card (recommended) or a 5th bottom-nav tab.
3. **Free tier:** how many free units (proposed 3), and is a 24h rewarded unlock acceptable?
4. **Native review:** can the client arrange a native-speaker spot check per language (units 1–3 minimum)?
5. **Phrase Practice for all 58 languages in v1:** yes/no.
6. **Design:** does the designer make the Figma screens, or should Claude draft them in TranslatorProZyro?
7. **"Learn while you type"** (keyboard suggestions in the target language): include in Phase 7b or drop?

---

### Sources

- [Duolingo 2024 Language Report](https://blog.duolingo.com/2024-duolingo-language-report/)
- [Duolingo 2025 language report coverage (secondary)](https://duolingoguides.com/duolingo-language-report/)
- [Tatoeba downloads and license](https://tatoeba.org/ca/downloads)
- [Tatoeba (Wikipedia)](https://en.wikipedia.org/wiki/Tatoeba)
