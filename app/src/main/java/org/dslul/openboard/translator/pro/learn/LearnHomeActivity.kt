package org.dslul.openboard.translator.pro.learn

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityLearnHomeBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemLearnUnitBinding
import org.dslul.openboard.translator.pro.PremiumScreenActivity
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.learn.engine.CourseContent
import org.dslul.openboard.translator.pro.learn.engine.CoursePlan
import org.dslul.openboard.translator.pro.learn.engine.LearnDays
import org.dslul.openboard.translator.pro.learn.engine.Srs
import org.dslul.openboard.translator.pro.learn.engine.Syllabus

/**
 * The course map (Figma "Learn · Course map"): course switcher, streak, XP, daily goal, practice
 * and the 12 units. Units after [LearnConfig.freeUnits] need Premium or a rewarded 24h unlock.
 */
class LearnHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnHomeBinding
    private lateinit var store: LearnStore
    private var content: CourseContent? = null
    private var adLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        store = LearnStore.get(this)
        LearnConfig.load(this)
        if (!store.onboarded || store.course == null) {
            startActivity(Intent(this, LearnOnboardingActivity::class.java))
            finish()
            return
        }
        binding = ActivityLearnHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, LearnSettingsActivity::class.java)) }
        binding.llCourseChip.setOnClickListener { showCourseSwitcher() }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val course = LearnRepository.currentCourse(this@LearnHomeActivity) ?: return@launch
            val c = LearnRepository.course(this@LearnHomeActivity, course)
            content = c
            bind(c)
            if (!adLoaded) {
                adLoaded = true
                LearnAds.bottomNative(this@LearnHomeActivity, binding.flAd)
            }
        }
    }

    private fun bind(c: CourseContent) {
        val course = c.course
        val progress = store.progress(course)
        val today = LearnDays.today()
        val syllabus = c.syllabus

        binding.ivCourseFlag.setImageResource(LearnUi.flag(this, course))
        binding.tvCourseName.text = LearnUi.languageName(this, course)
        binding.tvStreak.text = store.streak(today).toString()
        binding.tvXp.text = getString(R.string.learn_xp, store.xp)

        val goal = store.dailyGoal
        val done = store.lessonsToday(today)
        binding.tvGoal.text = resources.getQuantityString(R.plurals.learn_goal_progress, goal, done.coerceAtMost(goal), goal)
        binding.pbGoal.progress = (done.coerceAtMost(goal) * 100 / goal)

        val seen = progress.items.count { it.value.seen > 0 }
        val due = Srs.dueCount(progress.items, today)
        if (seen == 0) {
            binding.tvPracticeDue.setText(R.string.learn_practice_later)
            binding.tvPracticeStart.visibility = View.INVISIBLE
            binding.llPractice.alpha = 0.6f
            binding.llPractice.setOnClickListener {
                Toast.makeText(this, R.string.learn_practice_later, Toast.LENGTH_SHORT).show()
            }
        } else {
            val count = if (due > 0) due.coerceAtMost(10) else seen.coerceAtMost(10)
            binding.tvPracticeDue.text = resources.getQuantityString(R.plurals.learn_practice_due, count, count)
            binding.tvPracticeStart.visibility = View.VISIBLE
            binding.llPractice.alpha = 1f
            binding.llPractice.setOnClickListener { startActivity(LessonActivity.practice(this)) }
        }

        binding.llUnits.removeAllViews()
        val premium = Misc.getPurchasedStatus(this)
        val now = System.currentTimeMillis()
        var currentView: View? = null
        for (u in syllabus.units) {
            val unit = u.number
            val row = ItemLearnUnitBinding.inflate(LayoutInflater.from(this), binding.llUnits, true)
            val state = CoursePlan.unitState(syllabus, unit, progress.completed)
            val unlockUntil = progress.unlocks[unit] ?: 0L
            val locked = CoursePlan.isLocked(unit, LearnConfig.freeUnits, premium, unlockUntil, now)
            val lessons = syllabus.lessonCount(unit)
            val doneInUnit = CoursePlan.completedInUnit(syllabus, unit, progress.completed)
            val title = LearnUi.unitTitle(this, unit)
            row.tvTitle.text = getString(R.string.learn_unit_title_full, unit, title)

            when {
                state == CoursePlan.UnitState.DONE -> {
                    badge(row, R.drawable.bg_ui_tile_success, R.drawable.ic_ui_check, R.color.home_success)
                    row.tvSubtitle.text = getString(R.string.learn_unit_completed, doneInUnit, lessons)
                    row.tvAction.visibility = View.VISIBLE
                    row.tvAction.setText(R.string.learn_review)
                    row.root.setOnClickListener { startActivity(LessonActivity.practice(this)) }
                }
                locked -> {
                    badge(row, R.drawable.bg_ui_tile_orange, R.drawable.ic_ui_lock, R.color.home_orange)
                    row.tvSubtitle.setText(R.string.learn_unit_locked)
                    row.tvPro.visibility = View.VISIBLE
                    row.root.setOnClickListener { showLocked(course, unit) }
                }
                state == CoursePlan.UnitState.CURRENT -> {
                    currentView = row.root
                    bindCurrent(row, c, unit, doneInUnit, lessons, progress.completed)
                }
                else -> {
                    numberBadge(row, unit)
                    row.tvSubtitle.text = resources.getQuantityString(
                        if (unit <= LearnConfig.freeUnits) R.plurals.learn_unit_lessons_free else R.plurals.learn_unit_lessons,
                        lessons, lessons
                    )
                    row.ivChevron.visibility = View.VISIBLE
                    row.root.setOnClickListener {
                        Toast.makeText(this, R.string.learn_finish_previous, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (!locked && !premium && unit > LearnConfig.freeUnits && unlockUntil > now && state != CoursePlan.UnitState.DONE) {
                val hours = ((unlockUntil - now) / 3_600_000L + 1).toInt()
                row.tvSubtitle.text = getString(R.string.learn_unit_unlocked_for, hours)
                row.tvSubtitle.visibility = View.VISIBLE
            }
        }
        currentView?.let { v -> binding.scrollView.post { if (v.top > binding.scrollView.height) binding.scrollView.smoothScrollTo(0, v.top - 200) } }
    }

    private fun badge(row: ItemLearnUnitBinding, bg: Int, icon: Int, tint: Int) {
        row.flBadge.setBackgroundResource(bg)
        row.ivBadge.visibility = View.VISIBLE
        row.ivBadge.setImageResource(icon)
        row.ivBadge.setColorFilter(LearnUi.color(this, tint))
        row.tvBadge.visibility = View.GONE
    }

    private fun numberBadge(row: ItemLearnUnitBinding, unit: Int) {
        row.flBadge.setBackgroundResource(R.drawable.bg_learn_letter)
        row.ivBadge.visibility = View.GONE
        row.tvBadge.visibility = View.VISIBLE
        row.tvBadge.text = unit.toString()
    }

    private fun bindCurrent(row: ItemLearnUnitBinding, c: CourseContent, unit: Int, done: Int, lessons: Int, completed: Set<String>) {
        row.root.setBackgroundResource(R.drawable.bg_learn_unit_current)
        row.root.isClickable = false
        row.flBadge.visibility = View.GONE
        row.tvLabel.visibility = View.VISIBLE
        row.tvLabel.text = getString(R.string.learn_unit_label, unit)
        row.tvTitle.text = LearnUi.unitTitle(this, unit)
        row.tvTitle.setTextAppearance(R.style.HomeText_CardLarge)
        row.tvSubtitle.visibility = View.GONE
        row.tvAction.visibility = View.VISIBLE
        row.tvAction.text = getString(R.string.learn_lessons_of, done, lessons)
        row.tvAction.setTextColor(LearnUi.color(this, R.color.home_text_secondary))
        row.llCurrent.visibility = View.VISIBLE
        row.pbUnit.progress = done * 100 / lessons

        val next = (1..lessons).firstOrNull { Syllabus.lessonKey(unit, it) !in completed } ?: lessons
        val size = LearnUi.dp(this, 40f)
        for (l in 1..lessons) {
            val key = Syllabus.lessonKey(unit, l)
            val dot = FrameLayout(this)
            when {
                key in completed -> {
                    dot.setBackgroundResource(R.drawable.bg_ui_circle_success)
                    dot.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_ui_check)
                        setColorFilter(LearnUi.color(this@LearnHomeActivity, R.color.home_success))
                    }, FrameLayout.LayoutParams(LearnUi.dp(this, 18f), LearnUi.dp(this, 18f), android.view.Gravity.CENTER))
                    dot.setOnClickListener { startActivity(LessonActivity.lesson(this, unit, l)) }
                }
                l == next -> {
                    dot.setBackgroundResource(R.drawable.bg_ui_circle_primary)
                    dot.elevation = LearnUi.dp(this, 4f).toFloat()
                    dot.addView(TextView(this, null, 0, R.style.HomeText_AppTitle).apply {
                        text = l.toString()
                        setTextColor(LearnUi.color(this@LearnHomeActivity, R.color.home_on_accent))
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER))
                    dot.setOnClickListener { startActivity(LessonActivity.lesson(this, unit, l)) }
                }
                else -> {
                    dot.setBackgroundResource(R.drawable.bg_learn_dot_upcoming)
                    dot.addView(TextView(this, null, 0, R.style.HomeText_Label).apply {
                        text = l.toString()
                        setTextColor(LearnUi.color(this@LearnHomeActivity, R.color.home_text_muted))
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER))
                }
            }
            dot.contentDescription = getString(R.string.learn_lesson_n, l)
            row.llLessons.addView(dot, LinearLayout.LayoutParams(size, size))
            if (l < lessons) row.llLessons.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        }

        val tip = c.tipFor(unit)
        if (tip == null) {
            row.llTip.visibility = View.GONE
        } else {
            row.tvTip.text = getString(R.string.learn_tip_prefix, tip.title)
            row.llTip.setOnClickListener {
                LearnUi.messageDialog(
                    this, R.drawable.ic_ui_bulb, R.color.home_purple, R.drawable.bg_ui_circle_purple,
                    tip.title, tip.body, getString(R.string.learn_got_it), null, onPrimary = {}
                )
            }
        }
        row.btnContinue.text = getString(if (done == 0) R.string.learn_start_lesson_n else R.string.learn_continue_lesson_n, next)
        row.btnContinue.setOnClickListener { startActivity(LessonActivity.lesson(this, unit, next)) }
    }

    private fun showLocked(course: String, unit: Int) {
        AppAnalytics.learnUnitLockView(course, unit)
        val hours = LearnConfig.unlockHours
        LearnUi.messageDialog(
            this,
            icon = R.drawable.ic_ui_lock,
            iconTint = R.color.home_orange,
            iconBackground = R.drawable.bg_ui_circle_orange,
            title = getString(R.string.learn_locked_title, unit),
            message = getString(R.string.learn_locked_desc, LearnConfig.freeUnits, unit, hours),
            primary = getString(R.string.learn_watch_ad_unlock, hours),
            secondary = getString(R.string.learn_not_now),
            onPrimary = {
                LearnAds.rewardedUnlock(this) { method ->
                    store.unlockUnit(course, unit, System.currentTimeMillis() + hours * 3_600_000L)
                    AppAnalytics.learnUnitUnlock(course, unit, method)
                    Toast.makeText(this, getString(R.string.learn_unit_unlocked_toast, unit, hours), Toast.LENGTH_SHORT).show()
                    content?.let { bind(it) }
                }
            },
            extraButton = getString(R.string.learn_go_pro) to {
                startActivity(
                    Intent(this, PremiumScreenActivity::class.java)
                        .putExtra(AppAnalytics.EXTRA_PREMIUM_SOURCE, AppAnalytics.PREMIUM_SOURCE_FEATURE_LOCK)
                )
            }
        )
    }

    private fun showCourseSwitcher() = CourseSwitcher.show(this) { refresh() }
}
