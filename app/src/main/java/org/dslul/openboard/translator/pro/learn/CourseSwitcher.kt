package org.dslul.openboard.translator.pro.learn

import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.learn.engine.CoursePlan

/** The "Your courses" bottom sheet (Figma "Learn · Switch course"). Each course keeps its own progress. */
object CourseSwitcher {

    fun show(activity: AppCompatActivity, onChanged: () -> Unit) {
        val store = LearnStore.get(activity)
        activity.lifecycleScope.launch {
            val courses = LearnRepository.availableCourses(activity)
            val syllabus = LearnRepository.course(activity, courses.first()).syllabus
            val dialog = BottomSheetDialog(activity)
            val view = activity.layoutInflater.inflate(R.layout.sheet_learn_courses, null)
            dialog.setContentView(view)
            (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
            val list = view.findViewById<LinearLayout>(R.id.llCourses)
            for (code in courses) {
                val row = LearnUi.option(list)
                row.ivFlag.visibility = View.VISIBLE
                row.ivFlag.setImageResource(LearnUi.flag(activity, code))
                row.tvTitle.text = LearnUi.languageName(activity, code)
                val p = store.progress(code)
                row.tvSubtitle.text = if (!p.isStarted) {
                    activity.getString(R.string.learn_not_started)
                } else {
                    val next = CoursePlan.nextLesson(syllabus, p.completed)
                    activity.getString(
                        R.string.learn_course_progress,
                        next?.first ?: syllabus.unitCount,
                        CoursePlan.percent(syllabus, p.completed)
                    )
                }
                LearnUi.setOptionSelected(row, code == store.course)
                row.root.setOnClickListener {
                    dialog.dismiss()
                    if (code != store.course) {
                        store.course = code
                        AppAnalytics.learnCourseSelect(code, LearnRepository.appLanguage(activity))
                        LearnReminder.schedule(activity)
                        onChanged()
                    }
                }
            }
            if (!activity.isFinishing && !activity.isDestroyed) dialog.show()
        }
    }
}
