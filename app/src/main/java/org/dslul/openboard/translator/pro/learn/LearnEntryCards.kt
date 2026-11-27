package org.dslul.openboard.translator.pro.learn

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.LearnHomeCardBinding
import org.dslul.openboard.inputmethod.latin.databinding.LearnPhrasebookCardBinding
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.learn.engine.CoursePlan

/**
 * The two ways into Learn from the dashboard: the Home tab's "Learn a language" card and the
 * Phrasebook tab's "Practice phrases" card. Both hide when Remote Config turns Learn off.
 */
object LearnEntryCards {

    /** Called from HomeFragment.onResume. */
    fun bindHome(fragment: Fragment, card: LearnHomeCardBinding) {
        val context = fragment.context ?: return
        LearnConfig.load(context)
        if (!LearnConfig.enabled) {
            card.root.visibility = View.GONE
            return
        }
        card.root.visibility = View.VISIBLE
        val store = LearnStore.get(context)
        val course = store.course
        val started = store.onboarded && course != null
        LearnUi.setVisible(card.llLearnNew, !started)
        LearnUi.setVisible(card.llLearnProgress, started)

        // Learn is a Pro feature: every way in goes through the Pro lock first (LearnAccess).
        val openHome = View.OnClickListener {
            val activity = fragment.activity ?: return@OnClickListener
            LearnAccess.guard(activity, LearnAccess.FEATURE_LEARN) { afterAd ->
                val go = { if (fragment.isAdded) fragment.startActivity(Intent(activity, LearnHomeActivity::class.java)) }
                if (afterAd) go() else Ads.runWithEverySixthClickInterstitial(activity) { go() }
            }
        }
        card.root.setOnClickListener(openHome)

        if (!started) {
            card.btnLearnStart.setOnClickListener {
                val activity = fragment.activity ?: return@setOnClickListener
                LearnAccess.guard(activity, LearnAccess.FEATURE_LEARN) {
                    if (fragment.isAdded) fragment.startActivity(Intent(activity, LearnOnboardingActivity::class.java))
                }
            }
            card.root.setOnClickListener { card.btnLearnStart.performClick() }
            if (card.llLearnFlags.childCount == 0) {
                val own = LearnRepository.appLanguage(context)
                val size = LearnUi.dp(context, 24f)
                for (code in listOf("es", "fr", "de", "it", "en").filter { it != own }) {
                    card.llLearnFlags.addView(ShapeableImageView(context).apply {
                        setImageResource(LearnUi.flag(context, code))
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(RelativeCornerSize(0.5f)).build()
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(size, size).apply { marginEnd = LearnUi.dp(context, 6f) })
                }
            }
            return
        }

        card.tvLearnStreak.text = store.streak().toString()
        val goal = store.dailyGoal
        val done = store.lessonsToday().coerceAtMost(goal)
        card.tvLearnGoal.text = context.resources.getQuantityString(R.plurals.learn_goal_progress, goal, done, goal)
        card.pbLearnGoal.progress = done * 100 / goal
        card.btnLearnContinue.setOnClickListener(openHome)

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val course = LearnRepository.currentCourse(context) ?: return@launch
            card.ivLearnFlag.setImageResource(LearnUi.flag(context, course))
            card.tvLearnCourse.text = LearnUi.languageName(context, course)
            val content = LearnRepository.course(context, course)
            val progress = store.progress(course)
            val next = CoursePlan.nextLesson(content.syllabus, progress.completed)
            if (next == null) {
                card.tvLearnUnit.setText(R.string.learn_course_done)
                return@launch
            }
            val (unit, lesson) = next
            card.tvLearnUnit.text = context.getString(R.string.learn_unit_title_full, unit, LearnUi.unitTitle(context, unit))
            // Straight into the next lesson, with the course map behind it (after the Pro lock).
            card.btnLearnContinue.setOnClickListener {
                val activity = fragment.activity ?: return@setOnClickListener
                LearnAccess.guard(activity, LearnAccess.FEATURE_LEARN) {
                    activity.startActivities(
                        arrayOf(Intent(activity, LearnHomeActivity::class.java), LessonActivity.lesson(activity, unit, lesson))
                    )
                }
            }
        }
    }

    /** Called from PhrasesFragment once its view exists. */
    fun bindPhrasebook(fragment: Fragment, card: LearnPhrasebookCardBinding) {
        val context = fragment.context ?: return
        LearnConfig.load(context)
        if (!LearnConfig.enabled || !LearnConfig.phrasePracticeEnabled) {
            card.root.visibility = View.GONE
            return
        }
        card.root.visibility = View.VISIBLE
        card.root.setOnClickListener {
            val activity = fragment.activity ?: return@setOnClickListener
            LearnAccess.guard(activity, LearnAccess.FEATURE_PHRASES) { afterAd ->
                val go = { if (fragment.isAdded) fragment.startActivity(Intent(activity, PhrasePracticeActivity::class.java)) }
                if (afterAd) go() else Ads.runWithEverySixthClickInterstitial(activity) { go() }
            }
        }
    }
}
