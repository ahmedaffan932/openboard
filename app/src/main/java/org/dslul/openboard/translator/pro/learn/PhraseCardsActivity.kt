package org.dslul.openboard.translator.pro.learn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPhraseCardsBinding
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.learn.engine.PhraseTopic
import org.dslul.openboard.translator.pro.learn.engine.StudyItem

/**
 * Phrase flashcards (Figma "Practice · Flashcard front/back", "Practice · Done"): the phrase in
 * the learner's language, tap to reveal it in the target language with audio, then "Still
 * learning" (comes back in Mixed review) or "I know it". Ten cards per round.
 */
class PhraseCardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhraseCardsBinding
    private var speech: LearnSpeech? = null
    private lateinit var source: String
    private lateinit var target: String
    private var topicIndex = -1
    private var topic: PhraseTopic? = null
    private var cards: List<StudyItem> = emptyList()
    private var index = 0
    private var revealed = false
    private var knownCount = 0
    private var learningCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityPhraseCardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        source = intent.getStringExtra(EXTRA_SOURCE) ?: "en"
        target = intent.getStringExtra(EXTRA_TARGET) ?: "es"
        topicIndex = intent.getIntExtra(EXTRA_TOPIC, -1)

        speech = LearnSpeech(this, target) { ok -> runOnUiThread { if (!ok) binding.llSpeak.visibility = View.GONE } }
        binding.btnClose.setOnClickListener { finish() }
        binding.llCard.setOnClickListener { if (!revealed) reveal() else current()?.let { speech?.speak(it.target) } }
        binding.btnShow.setOnClickListener { reveal() }
        binding.ivSpeak.setOnClickListener { current()?.let { speech?.speak(it.target) } }
        binding.tvSlow.setOnClickListener { current()?.let { speech?.speak(it.target, slow = true) } }
        binding.btnKnow.setOnClickListener { answer(true) }
        binding.btnLearning.setOnClickListener { answer(false) }
        binding.btnAgain.setOnClickListener { start() }
        binding.btnBackToPhrasebook.setOnClickListener {
            // Back to the dashboard's Phrasebook tab, closing the practice screens on the way.
            startActivity(
                org.dslul.openboard.translator.pro.notifications.NotiTargets.dashboard(
                    this, org.dslul.openboard.translator.pro.FragmentsDashboardActivity.TAB_PHRASEBOOK
                )
            )
            finish()
        }
        binding.tvHint.text = getString(R.string.learn_tap_to_reveal, LearnUi.languageName(this, target))
        start()
    }

    override fun onDestroy() {
        speech?.shutdown()
        super.onDestroy()
    }

    private fun current(): StudyItem? = cards.getOrNull(index)

    private fun start() {
        lifecycleScope.launch {
            val topics = try {
                LearnRepository.phraseTopics(this@PhraseCardsActivity, source, target)
            } catch (e: Exception) {
                emptyList()
            }
            val known = PhraseProgress.known(this@PhraseCardsActivity, target)
            val learning = PhraseProgress.learning(this@PhraseCardsActivity, target)
            topic = topics.getOrNull(topicIndex)
            val pool = topic?.phrases ?: topics.flatMap { it.phrases }.filter { it.id in learning }
            // Unknown phrases first, so a round always moves the topic forward.
            cards = (pool.filter { it.id !in known }.shuffled() + pool.filter { it.id in known }.shuffled()).take(10)
            if (cards.isEmpty()) {
                Toast.makeText(this@PhraseCardsActivity, R.string.learn_phrases_unavailable, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            index = 0
            knownCount = 0
            learningCount = 0
            binding.llDone.visibility = View.GONE
            binding.llCards.visibility = View.VISIBLE
            show()
        }
    }

    private fun show() {
        val card = current() ?: return
        revealed = false
        binding.pbCards.progress = index * 100 / cards.size
        binding.tvCounter.text = getString(R.string.learn_topic_progress, index + 1, cards.size)
        binding.tvTopic.text = topic?.title ?: getString(R.string.learn_mixed_review)
        binding.tvFront.text = card.meaning
        binding.tvFront.setTextAppearance(R.style.HomeText_Display)
        binding.vDivider.visibility = View.GONE
        binding.tvBack.visibility = View.GONE
        binding.llSpeak.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE
        binding.btnShow.visibility = View.VISIBLE
        binding.llAnswer.visibility = View.GONE
    }

    private fun reveal() {
        val card = current() ?: return
        revealed = true
        binding.tvFront.setTextAppearance(R.style.HomeText_BodyLarge)
        binding.tvFront.setTextColor(LearnUi.color(this, R.color.home_text_secondary))
        binding.vDivider.visibility = View.VISIBLE
        binding.tvBack.visibility = View.VISIBLE
        binding.tvBack.text = card.target
        binding.tvHint.visibility = View.GONE
        binding.llSpeak.visibility = if (speech?.available == true) View.VISIBLE else View.GONE
        binding.btnShow.visibility = View.GONE
        binding.llAnswer.visibility = View.VISIBLE
        if (LearnStore.get(this).listening) speech?.speak(card.target)
    }

    private fun answer(knowIt: Boolean) {
        val card = current() ?: return
        PhraseProgress.mark(this, target, card.id, knowIt)
        if (knowIt) knownCount++ else learningCount++
        speech?.stop()
        index++
        if (index < cards.size) show() else done()
    }

    private fun done() {
        LearnStore.get(this).recordActivity(5)
        binding.llCards.visibility = View.GONE
        binding.llDone.visibility = View.VISIBLE
        val pair = getString(R.string.learn_pair, LearnUi.languageName(this, source), LearnUi.languageName(this, target))
        binding.tvDoneDesc.text = topic?.let { "${it.title} · $pair" } ?: pair
        binding.tvKnown.text = knownCount.toString()
        binding.tvLearning.text = learningCount.toString()
        val known = PhraseProgress.known(this, target)
        val t = topic
        binding.tvTopicDone.text = if (t != null) "${t.phrases.count { it.id in known }}/${t.phrases.size}" else "$knownCount/${cards.size}"
        LearnAds.bottomNative(this, binding.flAd)
    }

    companion object {
        private const val EXTRA_SOURCE = "phraseSource"
        private const val EXTRA_TARGET = "phraseTarget"
        private const val EXTRA_TOPIC = "phraseTopic"

        fun intent(context: Context, source: String, target: String, topic: Int): Intent =
            Intent(context, PhraseCardsActivity::class.java)
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_TARGET, target)
                .putExtra(EXTRA_TOPIC, topic)
    }
}
