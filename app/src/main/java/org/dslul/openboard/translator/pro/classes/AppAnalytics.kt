package org.dslul.openboard.translator.pro.classes

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.canhub.cropper.CropImageActivity
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.dslul.openboard.inputmethod.latin.setup.SetupWizardActivity
import org.dslul.openboard.translator.pro.AppLanguageSelectorActivity
import org.dslul.openboard.translator.pro.CameraPermissionActivity
import org.dslul.openboard.translator.pro.DisplayFavoritesActivity
import org.dslul.openboard.translator.pro.DisplayHistoryActivity
import org.dslul.openboard.translator.pro.EnableKeyboardActivity
import org.dslul.openboard.translator.pro.ExitActivity
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.OCRActivity
import org.dslul.openboard.translator.pro.OnBoardingActivity
import org.dslul.openboard.translator.pro.PhrasebookDetailedActivity
import org.dslul.openboard.translator.pro.PreSplashScreenActivity
import org.dslul.openboard.translator.pro.PremiumScreenActivity
import org.dslul.openboard.translator.pro.SettingsActivity
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.UninstallSurveyActivity
import org.dslul.openboard.translator.pro.ViewTranslatedTextActivity
import org.dslul.openboard.translator.pro.WordGameActivity
import org.dslul.openboard.inputmethod.latin.settings.SettingsActivity as KeyboardSettingsActivity

/**
 * Funnel events for Firebase Analytics, in one place so the keys the client builds funnels on are
 * easy to find and stay consistent. Keys and parameter values are snake_case and fixed strings:
 * Firebase drops event names that are not letters, digits and underscores, and counts every
 * distinct name toward its 500-name limit, so nothing here builds a key from user data.
 *
 * The older screen events (`Dashboard`, `EnableKeyboard`, `v_onboarding_*`, ...) are left as they
 * were so existing reports keep working; these sit beside them.
 */
object AppAnalytics {

    /** Intent extra naming where the premium screen was opened from (`source` on its events). */
    const val EXTRA_PREMIUM_SOURCE = "analyticsPremiumSource"

    const val PREMIUM_SOURCE_LAUNCH = "launch"
    const val PREMIUM_SOURCE_ONBOARDING = "onboarding"
    const val PREMIUM_SOURCE_PRO_BUTTON = "pro_button"
    const val PREMIUM_SOURCE_FEATURE_LOCK = "feature_lock"
    const val PREMIUM_SOURCE_IN_APP = "in_app"
    const val PREMIUM_SOURCE_SETTINGS = "settings"

    const val FEATURE_TEXT = "text"
    const val FEATURE_CAMERA = "camera"
    const val FEATURE_CONVERSATION = "conversation"
    const val FEATURE_KEYBOARD = "keyboard"

    const val PURCHASE_SUCCESS = "success"
    const val PURCHASE_PENDING = "pending"
    const val PURCHASE_CANCELLED = "cancelled"
    const val PURCHASE_FAILED = "failed"
    const val PURCHASE_UNAVAILABLE = "unavailable"

    // --- Screen opens -------------------------------------------------------------------------

    /**
     * Implemented by a screen whose `screen_*` event carries parameters. Called from
     * [onActivityCreated], i.e. inside the activity's `super.onCreate()`, so it may read the
     * intent and preferences but not its views.
     */
    interface ScreenParams {
        fun analyticsScreenParams(): Bundle?
    }

    /**
     * Every screen a user can reach → its `screen_<name>` event. Activities left out are
     * transparent trampolines (NotiRouter, QuickAccess), the system permission prompt, and legacy
     * screens nothing opens any more. The dashboard's tabs share one activity and are logged by
     * FragmentsDashboardActivity through [screenView].
     */
    private val activityScreens: Map<Class<out Activity>, String> = mapOf(
        PreSplashScreenActivity::class.java to "splash",
        PremiumScreenActivity::class.java to "premium",
        AppLanguageSelectorActivity::class.java to "app_language",
        OnBoardingActivity::class.java to "onboarding",
        TranslateActivity::class.java to "translate",
        LanguageSelectorActivity::class.java to "language_picker",
        CameraPermissionActivity::class.java to "camera_permission",
        OCRActivity::class.java to "camera_translate",
        CropImageActivity::class.java to "camera_crop",
        ViewTranslatedTextActivity::class.java to "camera_full_text",
        DisplayHistoryActivity::class.java to "history",
        DisplayFavoritesActivity::class.java to "favorites",
        PhrasebookDetailedActivity::class.java to "phrasebook_category",
        SettingsActivity::class.java to "settings",
        EnableKeyboardActivity::class.java to "enable_keyboard",
        KeyboardSettingsActivity::class.java to "keyboard_settings",
        SetupWizardActivity::class.java to "keyboard_setup_wizard",
        WordGameActivity::class.java to "word_game",
        org.dslul.openboard.translator.pro.learn.LearnHomeActivity::class.java to "learn_home",
        org.dslul.openboard.translator.pro.learn.LearnOnboardingActivity::class.java to "learn_onboarding",
        org.dslul.openboard.translator.pro.learn.LessonActivity::class.java to "learn_lesson",
        org.dslul.openboard.translator.pro.learn.LearnSettingsActivity::class.java to "learn_settings",
        org.dslul.openboard.translator.pro.learn.PhrasePracticeActivity::class.java to "phrase_practice",
        org.dslul.openboard.translator.pro.learn.PhraseCardsActivity::class.java to "phrase_cards",
        ExitActivity::class.java to "exit",
        UninstallSurveyActivity::class.java to "uninstall_survey",
    )

    /**
     * From App's activity lifecycle callbacks: one `screen_*` event each time a listed screen is
     * opened. A screen recreated by a rotation or a process restore is not counted again.
     */
    fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        val screen = activityScreens[activity.javaClass] ?: return
        screenView(screen, (activity as? ScreenParams)?.analyticsScreenParams())
    }

    /** A screen opened: logs `screen_<screen>`. */
    fun screenView(screen: String, params: Bundle? = null) = log("screen_$screen", params)

    // --- Onboarding ---------------------------------------------------------------------------

    /** App-language screen's Save/Next tapped. [language] is the app language code, e.g. `es`. */
    fun appLanguageSaved(language: String, firstOpen: Boolean) =
        log("app_language_saved", Bundle().apply {
            putString("language", language)
            putString("first_open", firstOpen.toString())
        })

    /**
     * An onboarding slide shown; [slide] counts from 1. Logs `screen_onboarding_<slide>` (one
     * event name per slide, so a funnel needs no parameter filter) and `onboarding_slide_view`
     * with a `slide` parameter, kept for existing reports.
     */
    fun onboardingSlideView(slide: Int) {
        screenView("onboarding_$slide")
        log("onboarding_slide_view", Bundle().apply { putLong("slide", slide.toLong()) })
    }

    /** The user finished the onboarding slides (Continue, or Next on the last slide). */
    fun onboardingComplete() = log("onboarding_complete", null)

    // --- Premium ------------------------------------------------------------------------------

    /** A plan card or the Get Pro button tapped, before Google Play's purchase sheet opens. */
    fun premiumBuyTap(productId: String, source: String) =
        log("premium_buy_tap", Bundle().apply {
            putString("plan", planName(productId))
            putString("source", source)
        })

    /**
     * What came back from Google Play: [status] is one of the `PURCHASE_*` values. [errorCode] is
     * Play's BillingResponseCode, sent only for a failure.
     */
    fun premiumPurchaseResult(productId: String?, status: String, errorCode: Int? = null) =
        log("premium_purchase_result", Bundle().apply {
            putString("plan", productId?.let { planName(it) } ?: "unknown")
            putString("status", status)
            errorCode?.let { putLong("error_code", it.toLong()) }
        })

    /** Premium screen closed without buying (close button or Back). */
    fun premiumDismiss(source: String) =
        log("premium_dismiss", Bundle().apply { putString("source", source) })

    /** The "unlock this feature" dialog shown for a locked dashboard tab (`camera` or `chat`). */
    fun featureLockView(feature: String) =
        log("feature_lock_view", Bundle().apply { putString("feature", feature) })

    /** A button on that dialog: [action] is `upgrade` or `watch_ad`. */
    fun featureLockAction(feature: String, action: String) =
        log("feature_lock_action", Bundle().apply {
            putString("feature", feature)
            putString("action", action)
        })

    /** Feature unlocked without buying: [method] is `rewarded` or `ad_timeout`. */
    fun featureUnlock(feature: String, method: String) =
        log("feature_unlock", Bundle().apply {
            putString("feature", feature)
            putString("method", method)
        })

    /** The rewarded unlock did not work: [reason] is `remote_off`, `no_internet`, `ad_failed` or `show_failed`. */
    fun featureUnlockFailed(feature: String, reason: String) =
        log("feature_unlock_failed", Bundle().apply {
            putString("feature", feature)
            putString("reason", reason)
        })

    // --- Keyboard setup -----------------------------------------------------------------------

    /** The keyboard was switched on in the system's keyboard settings from the setup screen. */
    fun keyboardEnabled() = log("keyboard_enabled", null)

    /** The keyboard was picked as the current keyboard from the setup screen. */
    fun keyboardSelected() = log("keyboard_selected", null)

    // --- Using the app ------------------------------------------------------------------------

    /** A translation language picked. [side] is `from` or `to`; [language] is its code, e.g. `fr`. */
    fun translateLanguageSelect(language: String, side: String) =
        log("translate_language_select", Bundle().apply {
            putString("language", languageCode(language))
            putString("side", side)
        })

    /** A translation came back. [feature] is one of the `FEATURE_*` values. */
    @JvmStatic
    fun translateDone(feature: String, from: String?, to: String?) =
        log("translate_done", translateParams(feature, from, to))

    /** A translation failed (no internet and no offline model, or the service errored). */
    @JvmStatic
    fun translateFailed(feature: String, from: String?, to: String?) =
        log("translate_failed", translateParams(feature, from, to))

    // --- Learn module -------------------------------------------------------------------------

    /** Onboarding answer: [step] `course` / `level` / `goal` / `reminder`, [value] the choice. */
    fun learnOnboardingStep(step: String, value: String) =
        log("learn_onboarding_step", Bundle().apply {
            putString("step", step)
            putString("value", value.take(40))
        })

    /** A course picked (onboarding or switcher); [base] is the learner's app language. */
    fun learnCourseSelect(course: String, base: String) =
        log("learn_course_select", Bundle().apply {
            putString("course", course)
            putString("base", base)
        })

    /** [kind] is `lesson`, `practice`, `checkpoint` or `phrases`. */
    fun learnLessonStart(course: String, unit: Int, lesson: Int, kind: String) =
        log("learn_lesson_start", learnParams(course, unit, lesson, kind))

    fun learnLessonComplete(course: String, unit: Int, lesson: Int, kind: String, accuracy: Int, seconds: Int, xp: Int) =
        log("learn_lesson_complete", learnParams(course, unit, lesson, kind).apply {
            putLong("accuracy", accuracy.toLong())
            putLong("seconds", seconds.toLong())
            putLong("xp", xp.toLong())
        })

    /** Quit with the dialog; [atExercise] is how far they got (0-based). */
    fun learnLessonQuit(course: String, unit: Int, lesson: Int, kind: String, atExercise: Int) =
        log("learn_lesson_quit", learnParams(course, unit, lesson, kind).apply {
            putLong("at_exercise", atExercise.toLong())
        })

    /** The streak grew to [days] with today's first finished session. */
    fun learnStreak(days: Int) = log("learn_streak", Bundle().apply { putLong("days", days.toLong()) })

    /** A streak freeze earned; [method] `rewarded`, `ad_timeout`, `ad_failed`, `show_failed`, `no_ad` or `premium_weekly`. */
    fun learnStreakFreezeEarned(method: String) =
        log("learn_streak_freeze_earned", Bundle().apply { putString("method", method) })

    /** Streak freezes spent automatically on [days] missed days. */
    fun learnStreakFreezeUsed(days: Int) =
        log("learn_streak_freeze_used", Bundle().apply { putLong("days", days.toLong()) })

    /** No text-to-speech voice for [language]: listening questions are skipped. */
    fun learnTtsUnavailable(language: String) =
        log("learn_tts_unavailable", Bundle().apply { putString("language", language.take(20)) })

    /** "Report" on a lesson answer: free content QA. [concept] is a course id or phrase id. */
    fun learnReportIssue(course: String, concept: String, reason: String) =
        log("learn_report_issue", Bundle().apply {
            putString("course", course.take(20))
            putString("concept", concept.take(60))
            putString("reason", reason)
        })

    /** Phrase Practice started: [mode] `flashcards` or `quiz`, [topic] category index or -1 (review). */
    fun phrasePracticeStart(target: String, topic: Int, mode: String) =
        log("phrase_practice_start", Bundle().apply {
            putString("target", target.take(20))
            putLong("topic", topic.toLong())
            putString("mode", mode)
        })

    private fun learnParams(course: String, unit: Int, lesson: Int, kind: String) = Bundle().apply {
        putString("course", course.take(20))
        putLong("unit", unit.toLong())
        putLong("lesson", lesson.toLong())
        putString("kind", kind)
    }

    // --- Helpers ------------------------------------------------------------------------------

    private fun translateParams(feature: String, from: String?, to: String?) = Bundle().apply {
        putString("feature", feature)
        putString("lang_from", languageCode(from))
        putString("lang_to", languageCode(to))
    }

    /** The app stores "detect language" as [Misc.defaultLanguage]; report it as `auto`. */
    private fun languageCode(code: String?): String = when {
        code.isNullOrBlank() || code == Misc.defaultLanguage -> "auto"
        else -> code.take(20)
    }

    private fun planName(productId: String): String = when (productId) {
        Misc.weeklyKey -> "weekly"
        Misc.monthlyKey -> "monthly"
        Misc.yearlyKey -> "yearly"
        else -> productId.take(40)
    }

    private fun log(event: String, params: Bundle?) {
        try {
            Firebase.analytics.logEvent(event, params)
        } catch (e: Exception) {
            // Analytics must never break a screen, a purchase or a keystroke.
            Log.w(Misc.logKey, "Analytics event $event not logged: ${e.message}")
        }
    }
}
