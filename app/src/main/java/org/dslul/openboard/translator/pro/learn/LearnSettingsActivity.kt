package org.dslul.openboard.translator.pro.learn

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityLearnSettingsBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemLearnSettingRowBinding
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import java.util.Calendar

/** Learning settings (Figma "Learn · Settings"). */
class LearnSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnSettingsBinding
    private lateinit var store: LearnStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityLearnSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = LearnStore.get(this)
        binding.btnBack.setOnClickListener { finish() }

        row(binding.rowCourse, R.drawable.ic_ui_languages, R.drawable.bg_ui_tile_accent, R.color.home_accent, R.string.learn_course)
        binding.rowCourse.root.setOnClickListener { CourseSwitcher.show(this) { bind() } }

        row(binding.rowGoal, R.drawable.ic_ui_target, R.drawable.bg_ui_tile_purple, R.color.home_purple, R.string.learn_daily_goal)
        binding.rowGoal.root.setOnClickListener { pickGoal() }

        row(binding.rowReminder, R.drawable.ic_ui_bell, R.drawable.bg_ui_tile_orange, R.color.home_orange, R.string.learn_reminder)
        binding.rowReminder.tvSubtitle.visibility = View.VISIBLE
        binding.rowReminder.tvSubtitle.setText(R.string.learn_reminder_keeps)
        binding.rowReminder.swValue.visibility = View.VISIBLE
        binding.rowReminder.root.setOnClickListener { binding.rowReminder.swValue.toggle() }
        binding.rowReminder.swValue.setOnCheckedChangeListener { _, on ->
            store.reminderOn = on
            LearnReminder.schedule(this)
            bind()
        }

        row(binding.rowReminderTime, R.drawable.ic_ui_calendar_clock, R.drawable.bg_ui_tile_orange, R.color.home_orange, R.string.learn_reminder_time)
        binding.rowReminderTime.root.setOnClickListener { pickTime() }

        row(binding.rowWordOfDay, R.drawable.ic_ui_sparkles, R.drawable.bg_ui_tile_purple, R.color.home_purple, R.string.learn_word_of_day)
        binding.rowWordOfDay.tvSubtitle.visibility = View.VISIBLE
        binding.rowWordOfDay.tvSubtitle.setText(R.string.learn_word_of_day_setting_desc)
        binding.rowWordOfDay.swValue.visibility = View.VISIBLE
        binding.rowWordOfDay.root.setOnClickListener { binding.rowWordOfDay.swValue.toggle() }
        binding.rowWordOfDay.swValue.setOnCheckedChangeListener { _, on ->
            store.wordOfDayOn = on
            LearnReminder.schedule(this)
        }

        row(binding.rowWeekly, R.drawable.ic_ui_calendar_clock, R.drawable.bg_ui_tile_purple, R.color.home_purple, R.string.learn_weekly)
        binding.rowWeekly.tvSubtitle.visibility = View.VISIBLE
        binding.rowWeekly.tvSubtitle.setText(R.string.learn_weekly_setting_desc)
        binding.rowWeekly.swValue.visibility = View.VISIBLE
        binding.rowWeekly.root.setOnClickListener { binding.rowWeekly.swValue.toggle() }
        binding.rowWeekly.swValue.setOnCheckedChangeListener { _, on ->
            store.weeklyOn = on
            LearnReminder.schedule(this)
        }

        row(binding.rowListening, R.drawable.ic_ui_speaker, R.drawable.bg_ui_tile_teal, R.color.home_teal, R.string.learn_listening)
        binding.rowListening.tvSubtitle.visibility = View.VISIBLE
        binding.rowListening.tvSubtitle.setText(R.string.learn_listening_desc)
        binding.rowListening.swValue.visibility = View.VISIBLE
        binding.rowListening.root.setOnClickListener { binding.rowListening.swValue.toggle() }
        binding.rowListening.swValue.setOnCheckedChangeListener { _, on -> store.listening = on }

        row(binding.rowVibration, R.drawable.ic_ui_zap, R.drawable.bg_ui_tile_teal, R.color.home_teal, R.string.learn_vibration)
        binding.rowVibration.tvSubtitle.visibility = View.VISIBLE
        binding.rowVibration.tvSubtitle.setText(R.string.learn_vibration_desc)
        binding.rowVibration.swValue.visibility = View.VISIBLE
        binding.rowVibration.root.setOnClickListener { binding.rowVibration.swValue.toggle() }
        binding.rowVibration.swValue.setOnCheckedChangeListener { _, on -> store.vibration = on }

        row(binding.rowReset, R.drawable.ic_ui_trash, R.drawable.bg_ui_tile_danger, R.color.home_danger, R.string.learn_reset)
        binding.rowReset.tvTitle.setTextColor(LearnUi.color(this, R.color.home_danger))
        binding.rowReset.tvSubtitle.visibility = View.VISIBLE
        binding.rowReset.tvSubtitle.setText(R.string.learn_reset_desc)
        binding.rowReset.root.setOnClickListener { confirmReset() }

        row(binding.rowAbout, R.drawable.ic_ui_info, R.drawable.bg_learn_letter, R.color.home_text_secondary, R.string.learn_about_content)
        binding.rowAbout.tvSubtitle.visibility = View.VISIBLE
        binding.rowAbout.tvSubtitle.setText(R.string.learn_about_content_desc)
        binding.rowAbout.root.isClickable = false

        binding.rowReminder.swValue.isChecked = store.reminderOn
        binding.rowWordOfDay.swValue.isChecked = store.wordOfDayOn
        binding.rowWeekly.swValue.isChecked = store.weeklyOn
        binding.rowListening.swValue.isChecked = store.listening
        binding.rowVibration.swValue.isChecked = store.vibration
    }

    override fun onResume() {
        super.onResume()
        bind()
    }

    private fun row(r: ItemLearnSettingRowBinding, icon: Int, bg: Int, tint: Int, title: Int) {
        r.flIcon.setBackgroundResource(bg)
        r.ivIcon.setImageResource(icon)
        r.ivIcon.setColorFilter(LearnUi.color(this, tint))
        r.tvTitle.setText(title)
    }

    private fun value(r: ItemLearnSettingRowBinding, text: String) {
        r.tvValue.visibility = View.VISIBLE
        r.tvValue.text = text
        r.ivChevron.visibility = View.VISIBLE
    }

    private fun bind() {
        val course = store.course ?: return
        val name = LearnUi.languageName(this, course)
        val from = LearnUi.languageName(this, LearnRepository.appLanguage(this))
        binding.rowCourse.tvSubtitle.visibility = View.VISIBLE
        binding.rowCourse.tvSubtitle.text = getString(R.string.learn_course_from, name, from)
        value(binding.rowCourse, getString(R.string.learn_change))

        val goal = store.dailyGoal
        binding.rowGoal.tvSubtitle.visibility = View.VISIBLE
        binding.rowGoal.tvSubtitle.text = getString(
            R.string.learn_goal_value, getString(goalName(goal)), resources.getQuantityString(R.plurals.learn_lessons_a_day, goal, goal)
        )
        value(binding.rowGoal, getString(R.string.learn_edit))

        val m = store.reminderMinutes
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60) }
        value(binding.rowReminderTime, DateFormat.getTimeFormat(this).format(cal.time))
        binding.rowReminderTime.root.alpha = if (store.reminderOn) 1f else 0.5f

        binding.rowReset.tvTitle.text = getString(R.string.learn_reset, name)
    }

    private fun goalName(goal: Int) = when (goal) {
        1 -> R.string.learn_goal_casual
        3 -> R.string.learn_goal_serious
        else -> R.string.learn_goal_regular
    }

    private fun pickGoal() {
        val labels = (1..3).map {
            getString(R.string.learn_goal_value, getString(goalName(it)), resources.getQuantityString(R.plurals.learn_lessons_a_day, it, it))
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.learn_daily_goal)
            .setSingleChoiceItems(labels, store.dailyGoal - 1) { d, which ->
                store.dailyGoal = which + 1
                d.dismiss()
                bind()
            }
            .show()
    }

    private fun pickTime() {
        val m = store.reminderMinutes
        TimePickerDialog(this, { _, h, min ->
            store.reminderMinutes = h * 60 + min
            store.reminderOn = true
            binding.rowReminder.swValue.isChecked = true
            LearnReminder.schedule(this)
            bind()
        }, m / 60, m % 60, DateFormat.is24HourFormat(this)).show()
    }

    private fun confirmReset() {
        val course = store.course ?: return
        val name = LearnUi.languageName(this, course)
        LearnUi.messageDialog(
            this,
            icon = R.drawable.ic_ui_trash,
            iconTint = R.color.home_danger,
            iconBackground = R.drawable.bg_ui_circle_danger,
            title = getString(R.string.learn_reset_confirm_title, name),
            message = getString(R.string.learn_reset_confirm_desc),
            primary = getString(R.string.learn_cancel),
            secondary = getString(R.string.learn_reset_button),
            secondaryDanger = true,
            onPrimary = {},
            onSecondary = {
                store.reset(course)
                Toast.makeText(this, R.string.learn_reset_done, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
