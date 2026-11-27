package org.dslul.openboard.translator.pro.learn

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.learn.engine.CoursePlan
import org.dslul.openboard.translator.pro.learn.engine.DayStat
import org.dslul.openboard.translator.pro.learn.engine.ItemProgress
import org.dslul.openboard.translator.pro.learn.engine.LearnDays
import org.dslul.openboard.translator.pro.learn.engine.LessonSession
import org.dslul.openboard.translator.pro.learn.engine.Srs
import org.dslul.openboard.translator.pro.learn.engine.StreakRules
import org.dslul.openboard.translator.pro.learn.engine.Syllabus
import org.dslul.openboard.translator.pro.learn.engine.WeeklyStats
import java.io.File

/** One course's saved progress: finished lessons and item review state. */
class CourseProgress(
    val completed: MutableSet<String> = LinkedHashSet(),
    val items: MutableMap<String, ItemProgress> = HashMap(),
) {
    val isStarted: Boolean get() = completed.isNotEmpty() || items.isNotEmpty()

    fun toJson(): String {
        val o = JsonObject()
        o.add("completed", JsonArray().apply { completed.forEach { add(it) } })
        o.add("items", JsonObject().apply {
            for ((id, p) in items) add(id, JsonArray().apply {
                add(p.box); add(p.due); add(p.seen); add(p.right); add(p.wrong)
            })
        })
        return o.toString()
    }

    companion object {
        fun fromJson(json: String): CourseProgress {
            val o = JsonParser.parseString(json).asJsonObject
            val p = CourseProgress()
            o.getAsJsonArray("completed")?.forEach { p.completed += it.asString }
            o.getAsJsonObject("items")?.entrySet()?.forEach { (id, el) ->
                val a = el.asJsonArray
                p.items[id] = ItemProgress(a[0].asInt, a[1].asLong, a[2].asInt, a[3].asInt, a[4].asInt)
            }
            return p
        }
    }
}

/**
 * Everything the Learn module remembers on this device. Settings, streak and daily counters live
 * in SharedPreferences "learn"; each course's progress is a small JSON file in files/learn/,
 * written atomically after every lesson.
 */
class LearnStore private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cache = HashMap<String, CourseProgress>()

    // --- Settings -------------------------------------------------------------------------------

    var course: String?
        get() = prefs.getString("course", null)
        set(v) = prefs.edit().putString("course", v).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()

    /** Lessons per day: 1, 2 or 3. */
    var dailyGoal: Int
        get() = prefs.getInt("goal", 2)
        set(v) = prefs.edit().putInt("goal", v).apply()

    var reminderOn: Boolean
        get() = prefs.getBoolean("reminder", true)
        set(v) = prefs.edit().putBoolean("reminder", v).apply()

    var reminderMinutes: Int
        get() = prefs.getInt("reminderAt", 20 * 60)
        set(v) = prefs.edit().putInt("reminderAt", v).apply()

    var listening: Boolean
        get() = prefs.getBoolean("listening", true)
        set(v) = prefs.edit().putBoolean("listening", v).apply()

    var vibration: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(v) = prefs.edit().putBoolean("vibration", v).apply()

    // --- Totals, streak, today ------------------------------------------------------------------

    val xp: Int get() = prefs.getInt("xp", 0)

    /** The current streak, after spending streak freezes on any missed days (see [settleStreak]). */
    fun streak(today: Long = LearnDays.today()): Int {
        settleStreak(today)
        return StreakRules.current(prefs.getInt("streak", 0), prefs.getLong("streakDay", -1), today)
    }

    // --- Streak freezes -------------------------------------------------------------------------

    /** Streak freezes owned (0..[StreakRules.MAX_FREEZES]). */
    val freezes: Int get() = prefs.getInt("freezes", 0)

    /** Adds one earned freeze; false when already at the maximum. */
    fun addFreeze(): Boolean {
        if (freezes >= StreakRules.MAX_FREEZES) return false
        prefs.edit().putInt("freezes", freezes + 1).apply()
        return true
    }

    /** Premium: one free freeze each Monday-based week (once the learner has a course). */
    fun grantWeeklyFreeze(premium: Boolean, today: Long = LearnDays.today()) {
        if (!premium || !onboarded) return
        val week = StreakRules.week(today)
        if (prefs.getLong("freezeWeek", Long.MIN_VALUE) == week) return
        val edit = prefs.edit().putLong("freezeWeek", week)
        if (freezes < StreakRules.MAX_FREEZES) {
            edit.putInt("freezes", freezes + 1)
            org.dslul.openboard.translator.pro.classes.AppAnalytics.learnStreakFreezeEarned("premium_weekly")
        }
        edit.apply()
    }

    /** Days a freeze covered (the last 14), shown as snowflakes in the week row. */
    fun frozenDays(): Set<Long> =
        prefs.getString("frozenDays", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }.toSet()

    /** How many freezes were used since the course map last said so (then cleared). */
    fun takeFreezeNotice(): Int {
        val n = prefs.getInt("freezeNotice", 0)
        if (n > 0) prefs.edit().putInt("freezeNotice", 0).apply()
        return n
    }

    /** Spends freezes on missed days so the streak survives (idempotent; called by [streak]). */
    fun settleStreak(today: Long = LearnDays.today()) {
        val settled = StreakRules.settle(prefs.getInt("streak", 0), prefs.getLong("streakDay", -1), today, freezes)
        if (settled.freezesUsed == 0) return
        val frozen = (frozenDays() + settled.frozenDays).filter { it > today - 14 }.sorted()
        prefs.edit()
            .putLong("streakDay", settled.lastDay)
            .putInt("freezes", freezes - settled.freezesUsed)
            .putString("frozenDays", frozen.joinToString(","))
            .putInt("freezeNotice", prefs.getInt("freezeNotice", 0) + settled.freezesUsed)
            .apply()
        org.dslul.openboard.translator.pro.classes.AppAnalytics.learnStreakFreezeUsed(settled.freezesUsed)
    }

    // --- Weekly stats and extra notifications ---------------------------------------------------

    /** Per-day lessons and XP for the last 14 days (weekly summary notification). */
    fun dayStats(): List<DayStat> = WeeklyStats.parse(prefs.getString("dayStats", ""))

    var wordOfDayOn: Boolean
        get() = prefs.getBoolean("wordOfDay", true)
        set(v) = prefs.edit().putBoolean("wordOfDay", v).apply()

    var weeklyOn: Boolean
        get() = prefs.getBoolean("weekly", true)
        set(v) = prefs.edit().putBoolean("weekly", v).apply()

    fun lessonsToday(today: Long = LearnDays.today()): Int =
        if (prefs.getLong("todayDay", -1) == today) prefs.getInt("todayLessons", 0) else 0

    fun didLessonToday(today: Long = LearnDays.today()): Boolean = lessonsToday(today) > 0

    /** Local epoch days with at least one finished lesson (the last 14). */
    fun activeDays(): Set<Long> =
        prefs.getString("activeDays", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }.toSet()

    /** Finished lessons since the last between-lessons interstitial. */
    var lessonsSinceAd: Int
        get() = prefs.getInt("lessonsSinceAd", 0)
        set(v) = prefs.edit().putInt("lessonsSinceAd", v).apply()

    // --- Course progress ------------------------------------------------------------------------

    fun progress(course: String): CourseProgress = synchronized(cache) {
        cache.getOrPut(course) {
            try {
                val f = file(course)
                if (f.exists()) CourseProgress.fromJson(AtomicFile(f).readFully().toString(Charsets.UTF_8)) else CourseProgress()
            } catch (e: Exception) {
                Log.w(Misc.logKey, "Learn progress for $course unreadable, starting fresh: ${e.message}")
                CourseProgress()
            }
        }
    }

    fun save(course: String) {
        val p = synchronized(cache) { cache[course] } ?: return
        val f = file(course)
        f.parentFile?.mkdirs()
        val af = AtomicFile(f)
        val out = af.startWrite()
        try {
            out.write(p.toJson().toByteArray(Charsets.UTF_8))
            af.finishWrite(out)
        } catch (e: Exception) {
            af.failWrite(out)
            Log.w(Misc.logKey, "Learn progress for $course not saved: ${e.message}")
        }
    }

    fun reset(course: String) {
        synchronized(cache) { cache[course] = CourseProgress() }
        file(course).delete()
    }

    /** What finishing a session changed, for the result screen. */
    data class Outcome(val xp: Int, val streak: Int, val streakExtended: Boolean, val goalReached: Boolean, val lessonsToday: Int)

    /**
     * Applies a finished session: review state of every answered item, the lesson key (if a
     * lesson), XP, streak, today's count. [testOutUnits] > 0 marks units 1..n done (passed test).
     */
    fun record(
        course: String,
        syllabus: Syllabus,
        session: LessonSession,
        lessonKey: String?,
        testOutUnits: Int = 0,
        today: Long = LearnDays.today(),
    ): Outcome {
        val p = progress(course)
        for ((id, ok) in session.itemResults) {
            p.items[id] = Srs.answered(p.items[id] ?: ItemProgress(), ok, today)
        }
        if (lessonKey != null) p.completed += lessonKey
        if (testOutUnits > 0) {
            p.completed += CoursePlan.testOutKeys(syllabus, testOutUnits)
            for (u in 1..testOutUnits) for (id in syllabus.unitItems(u)) {
                if ((p.items[id]?.seen ?: 0) == 0) p.items[id] = ItemProgress(box = 2, due = today + 2, seen = 1, right = 1)
            }
        }
        save(course)
        return recordActivity(session.xp, today)
    }

    /** Counts a finished session (lesson, practice, phrase quiz) for XP, streak and today's goal. */
    fun recordActivity(xpGained: Int, today: Long = LearnDays.today()): Outcome {
        val before = streak(today)
        val newStreak = StreakRules.afterLesson(prefs.getInt("streak", 0), prefs.getLong("streakDay", -1), today)
        val todayCount = lessonsToday(today) + 1
        val days = (activeDays() + today).filter { it > today - 14 }.sorted()
        prefs.edit()
            .putInt("xp", xp + xpGained)
            .putInt("streak", newStreak)
            .putLong("streakDay", today)
            .putLong("todayDay", today)
            .putInt("todayLessons", todayCount)
            .putString("activeDays", days.joinToString(","))
            .putString("dayStats", WeeklyStats.format(WeeklyStats.add(dayStats(), today, xpGained)))
            .apply()
        return Outcome(xpGained, newStreak, newStreak > before, todayCount == dailyGoal, todayCount)
    }

    private fun file(course: String) = File(File(context.filesDir, "learn"), "$course.json")

    companion object {
        const val PREFS = "learn"

        @Volatile private var instance: LearnStore? = null

        fun get(context: Context): LearnStore =
            instance ?: synchronized(this) {
                instance ?: LearnStore(context.applicationContext).also { instance = it }
            }
    }
}
