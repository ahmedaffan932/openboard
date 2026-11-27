package org.dslul.openboard.translator.pro.learn

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPhrasePracticeBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemPhraseTopicBinding
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.learn.engine.PhraseTopic

/**
 * Phrase Practice (Figma "Practice · Pick topic"): flashcards or a quiz over the bundled
 * phrasebook in any of its 58 languages. The language pair is the phrasebook's own, changed
 * with the same language picker.
 */
class PhrasePracticeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhrasePracticeBinding
    private var topics: List<PhraseTopic> = emptyList()
    private var adLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityPhrasePracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.llFrom.setOnClickListener {
            startActivity(
                Intent(this, LanguageSelectorActivity::class.java)
                    .putExtra("isPhrasebook", true)
                    .putExtra(Misc.lngTo, false)
            )
        }
        binding.llTo.setOnClickListener { startActivity(Intent(this, LanguageSelectorActivity::class.java)) }
        binding.btnSwap.setOnClickListener {
            val from = Misc.getLanguageFrom(this)
            if (from == Misc.defaultLanguage) return@setOnClickListener
            Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
            Misc.setLanguageTo(this, from)
            refresh()
        }
        binding.tabFlashcards.setOnClickListener { setMode(true) }
        binding.tabQuiz.setOnClickListener { setMode(false) }
        binding.llMixed.setOnClickListener { open(-1) }
        setMode(PhraseProgress.flashcards(this))
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun source(): String = Misc.getLanguageFrom(this).let { if (it == Misc.defaultLanguage) "en" else it }
    private fun target(): String = Misc.getLanguageTo(this)

    private fun setMode(flashcards: Boolean) {
        PhraseProgress.setFlashcards(this, flashcards)
        binding.tabFlashcards.setBackgroundResource(if (flashcards) R.drawable.bg_learn_segment_on else 0)
        binding.tabQuiz.setBackgroundResource(if (!flashcards) R.drawable.bg_learn_segment_on else 0)
        binding.tabFlashcards.setTextColor(LearnUi.color(this, if (flashcards) R.color.home_accent else R.color.home_text_secondary))
        binding.tabQuiz.setTextColor(LearnUi.color(this, if (!flashcards) R.color.home_accent else R.color.home_text_secondary))
    }

    private fun refresh() {
        val src = source()
        val tgt = target()
        binding.ivFrom.setImageResource(LearnUi.flag(this, src))
        binding.tvFrom.text = LearnUi.languageName(this, src)
        binding.ivTo.setImageResource(LearnUi.flag(this, tgt))
        binding.tvTo.text = LearnUi.languageName(this, tgt)
        lifecycleScope.launch {
            topics = try {
                if (src == tgt) emptyList() else LearnRepository.phraseTopics(this@PhrasePracticeActivity, src, tgt)
            } catch (e: Exception) {
                emptyList()
            }
            bindTopics(src, tgt)
            if (!adLoaded) {
                adLoaded = true
                LearnAds.bottomNative(this@PhrasePracticeActivity, binding.flAd)
            }
        }
    }

    private fun bindTopics(src: String, tgt: String) {
        binding.llTopics.removeAllViews()
        val unavailable = topics.isEmpty()
        LearnUi.setVisible(binding.tvUnavailable, unavailable)
        binding.tvUnavailable.setText(if (src == tgt) R.string.learn_same_language else R.string.learn_phrases_unavailable)
        LearnUi.setVisible(binding.llMixed, !unavailable)
        if (unavailable) return

        val known = PhraseProgress.known(this, tgt)
        val learning = PhraseProgress.learning(this, tgt)
        val allIds = topics.flatMap { t -> t.phrases.map { it.id } }.toSet()
        val due = learning.count { it in allIds }
        binding.tvMixed.text = if (due > 0) {
            resources.getQuantityString(R.plurals.learn_mixed_review_due, due, due)
        } else {
            getString(R.string.learn_mixed_review_none)
        }
        binding.llMixed.alpha = if (due > 0) 1f else 0.6f

        for (topic in topics) {
            val row = ItemPhraseTopicBinding.inflate(layoutInflater, binding.llTopics, true)
            val (icon, bg, tint) = STYLES[topic.index % STYLES.size]
            row.flIcon.setBackgroundResource(bg)
            row.ivIcon.setImageResource(icon)
            row.ivIcon.setColorFilter(LearnUi.color(this, tint))
            row.tvName.text = topic.title
            val total = topic.phrases.size
            val k = topic.phrases.count { it.id in known }
            val done = total > 0 && k == total
            row.tvCount.text = if (done) getString(R.string.learn_topic_done) else getString(R.string.learn_topic_progress, k, total)
            row.tvCount.setTextColor(LearnUi.color(this, if (done) R.color.home_success else R.color.home_text_secondary))
            row.pbTopic.progress = if (total == 0) 0 else k * 100 / total
            row.root.setOnClickListener { open(topic.index) }
        }
    }

    private fun open(topic: Int) {
        val src = source()
        val tgt = target()
        if (topic < 0 && PhraseProgress.learning(this, tgt).isEmpty()) {
            Toast.makeText(this, R.string.learn_mixed_review_none, Toast.LENGTH_SHORT).show()
            return
        }
        val flashcards = PhraseProgress.flashcards(this)
        AppAnalytics.phrasePracticeStart(tgt, topic, if (flashcards) "flashcards" else "quiz")
        startActivity(
            if (flashcards) PhraseCardsActivity.intent(this, src, tgt, topic)
            else LessonActivity.phrases(this, src, tgt, topic)
        )
    }

    companion object {
        /** Icon, tile and tint per phrasebook category (same order as the Phrasebook tab). */
        private val STYLES = listOf(
            Triple(R.drawable.ic_ui_cart, R.drawable.bg_ui_tile_orange, R.color.home_orange),
            Triple(R.drawable.ic_ui_hotel, R.drawable.bg_ui_tile_accent, R.color.home_accent),
            Triple(R.drawable.ic_ui_wine, R.drawable.bg_ui_tile_purple, R.color.home_purple),
            Triple(R.drawable.ic_ui_utensils, R.drawable.bg_ui_tile_danger, R.color.home_danger),
            Triple(R.drawable.ic_ui_list_checks, R.drawable.bg_ui_tile_teal, R.color.home_teal),
            Triple(R.drawable.ic_ui_stethoscope, R.drawable.bg_ui_tile_success, R.color.home_success),
            Triple(R.drawable.ic_ui_alert, R.drawable.bg_ui_tile_orange, R.color.home_orange),
            Triple(R.drawable.ic_ui_calendar_clock, R.drawable.bg_ui_tile_purple, R.color.home_purple),
            Triple(R.drawable.ic_ui_plane, R.drawable.bg_ui_tile_accent, R.color.home_accent),
        )
    }
}
