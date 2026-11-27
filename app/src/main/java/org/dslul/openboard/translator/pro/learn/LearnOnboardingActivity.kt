package org.dslul.openboard.translator.pro.learn

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityLearnOnboardingBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemLearnOptionBinding
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import java.util.Calendar

/**
 * First-run setup for Learn (Figma "Learn · Choose language" → "Your level" → "Daily goal"):
 * pick a course, say whether you are new or know some (a test-out), choose a daily goal and the
 * reminder. "Start learning" opens the course map with the first lesson (or the test) on top.
 */
class LearnOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnOnboardingBinding
    private lateinit var store: LearnStore
    private var step = 1
    private var course: String? = null
    private var knowsSome = false
    private var goal = 2
    private val courseRows = LinkedHashMap<String, ItemLearnOptionBinding>()
    private val goalRows = LinkedHashMap<Int, ItemLearnOptionBinding>()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityLearnOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = LearnStore.get(this)
        goal = store.dailyGoal

        binding.btnBack.setOnClickListener { back() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })
        binding.btnNext.setOnClickListener { nextStep() }
        binding.llLevelNew.setOnClickListener { setLevel(false) }
        binding.llLevelSome.setOnClickListener { setLevel(true) }
        binding.swReminder.isChecked = store.reminderOn
        binding.swReminder.setOnCheckedChangeListener { _, _ -> updateReminderText() }
        binding.llReminder.setOnClickListener { pickTime() }
        updateReminderText()
        buildGoals()

        lifecycleScope.launch {
            val own = LearnRepository.appLanguage(this@LearnOnboardingActivity)
            val explain = if (own in LearnRepository.manifest(this@LearnOnboardingActivity).glossLanguages) own else "en"
            binding.tvCourseDesc.text = getString(R.string.learn_ob_course_desc, LearnUi.languageName(this@LearnOnboardingActivity, explain))
            val units = LearnRepository.course(this@LearnOnboardingActivity, "en").syllabus.unitCount
            buildCourses(LearnRepository.availableCourses(this@LearnOnboardingActivity), units)
        }
        showStep()
    }

    private fun buildCourses(courses: List<String>, units: Int) {
        binding.llCourses.removeAllViews()
        courseRows.clear()
        courses.forEachIndexed { i, code ->
            val row = LearnUi.option(binding.llCourses)
            row.ivFlag.visibility = View.VISIBLE
            row.ivFlag.setImageResource(LearnUi.flag(this, code))
            row.tvTitle.text = LearnUi.languageName(this, code)
            row.tvSubtitle.text = getString(R.string.learn_course_beginner, units)
            if (i == 0) {
                row.tvBadge.visibility = View.VISIBLE
                row.tvBadge.setText(R.string.learn_popular)
            }
            row.root.setOnClickListener { selectCourse(code) }
            courseRows[code] = row
        }
        selectCourse(store.course?.takeIf { it in courses } ?: courses.firstOrNull())
    }

    private fun selectCourse(code: String?) {
        course = code
        for ((c, row) in courseRows) LearnUi.setOptionSelected(row, c == code)
    }

    private fun buildGoals() {
        val goals = listOf(
            Triple(1, R.string.learn_goal_casual, R.string.learn_goal_casual_desc),
            Triple(2, R.string.learn_goal_regular, R.string.learn_goal_regular_desc),
            Triple(3, R.string.learn_goal_serious, R.string.learn_goal_serious_desc),
        )
        for ((value, title, desc) in goals) {
            val row = LearnUi.option(binding.llGoals)
            row.flIcon.visibility = View.VISIBLE
            row.ivIcon.setImageResource(R.drawable.ic_ui_target)
            row.tvTitle.setText(title)
            row.tvSubtitle.setText(desc)
            if (value == 2) {
                row.tvBadge.visibility = View.VISIBLE
                row.tvBadge.setText(R.string.learn_popular)
            }
            row.root.setOnClickListener { selectGoal(value) }
            goalRows[value] = row
        }
        selectGoal(goal)
    }

    private fun selectGoal(value: Int) {
        goal = value
        for ((g, row) in goalRows) {
            LearnUi.setOptionSelected(row, g == value)
            row.flIcon.setBackgroundResource(if (g == value) R.drawable.bg_ui_circle_surface_plain else R.drawable.bg_learn_letter)
            row.ivIcon.setColorFilter(LearnUi.color(this, if (g == value) R.color.home_accent else R.color.home_text_secondary))
        }
    }

    private fun setLevel(some: Boolean) {
        knowsSome = some
        binding.llLevelNew.setBackgroundResource(if (!some) R.drawable.bg_learn_option_selected else R.drawable.bg_ui_card_r16_clickable)
        binding.llLevelSome.setBackgroundResource(if (some) R.drawable.bg_learn_option_selected else R.drawable.bg_ui_card_r16_clickable)
        binding.ivLevelNewRadio.setImageResource(if (!some) R.drawable.ui_radio_on else R.drawable.ui_radio_off)
        binding.ivLevelSomeRadio.setImageResource(if (some) R.drawable.ui_radio_on else R.drawable.ui_radio_off)
        binding.tvLevelNew.setTextColor(LearnUi.color(this, if (!some) R.color.home_accent else R.color.home_text_primary))
        binding.tvLevelSome.setTextColor(LearnUi.color(this, if (some) R.color.home_accent else R.color.home_text_primary))
    }

    private fun showStep() {
        LearnUi.setVisible(binding.stepCourse, step == 1)
        LearnUi.setVisible(binding.stepLevel, step == 2)
        LearnUi.setVisible(binding.stepGoal, step == 3)
        binding.step2.setBackgroundResource(if (step >= 2) R.drawable.bg_learn_step_on else R.drawable.bg_learn_step_off)
        binding.step3.setBackgroundResource(if (step >= 3) R.drawable.bg_learn_step_on else R.drawable.bg_learn_step_off)
        val name = course?.let { LearnUi.languageName(this, it) }
        binding.tvTitle.text = if (step == 1 || name == null) getString(R.string.learn_title) else name
        binding.btnNext.setText(if (step == 3) R.string.learn_start_learning else R.string.learn_continue)
        if (step == 2 && name != null) {
            binding.tvLevelTitle.text = getString(R.string.learn_ob_level_title, name)
            binding.tvLevelNew.text = getString(R.string.learn_ob_level_new, name)
            binding.tvLevelSome.text = getString(R.string.learn_ob_level_some, name)
            setLevel(knowsSome)
        }
    }

    private fun nextStep() {
        when (step) {
            1 -> {
                val c = course ?: return
                AppAnalytics.learnOnboardingStep("course", c)
                step = 2
            }
            2 -> {
                AppAnalytics.learnOnboardingStep("level", if (knowsSome) "some" else "new")
                step = 3
            }
            else -> {
                AppAnalytics.learnOnboardingStep("goal", goal.toString())
                AppAnalytics.learnOnboardingStep("reminder", if (binding.swReminder.isChecked) "on" else "off")
                if (binding.swReminder.isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    start()
                }
                return
            }
        }
        showStep()
    }

    private fun back() {
        if (step > 1) {
            step--
            showStep()
        } else {
            finish()
        }
    }

    private fun start() {
        val c = course ?: return
        store.course = c
        store.dailyGoal = goal
        store.reminderOn = binding.swReminder.isChecked
        store.onboarded = true
        AppAnalytics.learnCourseSelect(c, LearnRepository.appLanguage(this))
        LearnReminder.schedule(this)
        org.dslul.openboard.translator.pro.notifications.AppShortcuts.setup(this)
        val home = Intent(this, LearnHomeActivity::class.java)
        val first = if (knowsSome) LessonActivity.checkpoint(this) else LessonActivity.lesson(this, 1, 1)
        startActivities(arrayOf(home, first))
        finish()
    }

    private fun updateReminderText() {
        val m = store.reminderMinutes
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, m / 60)
            set(Calendar.MINUTE, m % 60)
        }
        binding.tvReminderAt.text = getString(R.string.learn_reminder_at, DateFormat.getTimeFormat(this).format(cal.time))
        binding.tvReminderAt.alpha = if (binding.swReminder.isChecked) 1f else 0.5f
    }

    private fun pickTime() {
        val m = store.reminderMinutes
        TimePickerDialog(this, { _, h, min ->
            store.reminderMinutes = h * 60 + min
            binding.swReminder.isChecked = true
            updateReminderText()
        }, m / 60, m % 60, DateFormat.is24HourFormat(this)).show()
    }
}
