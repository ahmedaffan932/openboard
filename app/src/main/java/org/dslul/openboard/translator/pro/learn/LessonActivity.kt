package org.dslul.openboard.translator.pro.learn

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityLearnLessonBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemLearnChoiceBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemLearnExampleBinding
import org.dslul.openboard.inputmethod.latin.databinding.LearnExerciseBinding
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.learn.engine.AnswerChecker
import org.dslul.openboard.translator.pro.learn.engine.CourseContent
import org.dslul.openboard.translator.pro.learn.engine.Exercise
import org.dslul.openboard.translator.pro.learn.engine.LearnDays
import org.dslul.openboard.translator.pro.learn.engine.LessonBuilder
import org.dslul.openboard.translator.pro.learn.engine.LessonSession
import org.dslul.openboard.translator.pro.learn.engine.Srs
import org.dslul.openboard.translator.pro.learn.engine.StudyItem
import org.dslul.openboard.translator.pro.learn.engine.Syllabus
import java.util.Calendar
import kotlin.random.Random

/**
 * Runs one lesson, practice round, test-out or phrase quiz (Figma section 12). The exercise list
 * comes from [LessonBuilder]; this screen renders the current one into learn_exercise, checks
 * the answer, shows the correct/wrong sheet and, at the end, records the session and shows the
 * result with the streak.
 */
class LessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnLessonBinding
    private lateinit var store: LearnStore
    private var speech: LearnSpeech? = null
    private var speechReady = false

    private lateinit var kind: LessonSession.Kind
    private var session: LessonSession? = null
    private var content: CourseContent? = null

    /** Course language for LESSON/PRACTICE/CHECKPOINT, the phrase target language for PHRASES. */
    private lateinit var language: String
    private var unit = 0
    private var lesson = 0
    private var phraseSource = ""
    private var phraseTopic = -1

    private var ex: LearnExerciseBinding? = null
    private var checked = false
    private var renderedAsFallback = false
    private var selected: StudyItem? = null
    private val choiceViews = HashMap<String, ItemLearnChoiceBinding>()
    private val answerTiles = ArrayList<Int>()
    private var matchLeft: Pair<String, TextView>? = null
    private var matchRight: Pair<String, TextView>? = null
    private val matched = HashSet<String>()
    private val mismatched = HashSet<String>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityLearnLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = LearnStore.get(this)
        LearnConfig.load(this)

        kind = LessonSession.Kind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: LessonSession.Kind.LESSON.name)
        unit = intent.getIntExtra(EXTRA_UNIT, 1)
        lesson = intent.getIntExtra(EXTRA_LESSON, 1)
        phraseSource = intent.getStringExtra(EXTRA_PHRASE_SOURCE) ?: "en"
        phraseTopic = intent.getIntExtra(EXTRA_PHRASE_TOPIC, -1)
        language = if (kind == LessonSession.Kind.PHRASES) {
            intent.getStringExtra(EXTRA_PHRASE_TARGET) ?: "es"
        } else {
            store.course ?: run { finish(); return }
        }

        binding.btnClose.setOnClickListener { confirmQuit() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.result.root.visibility == View.VISIBLE) finishWithAd() else confirmQuit()
            }
        })
        binding.btnCheck.setOnClickListener { onCheck() }
        binding.btnFeedbackContinue.setOnClickListener { next() }
        binding.ivFeedbackSpeak.setOnClickListener { currentItem()?.let { speak(it.target) } }
        binding.tvReport.setOnClickListener { report() }
        binding.btnCheck.visibility = View.INVISIBLE

        speech = LearnSpeech(this, language) { ok ->
            runOnUiThread {
                speechReady = ok
                if (!ok && store.listening && ttsWarned.add(language)) {
                    AppAnalytics.learnTtsUnavailable(language)
                    offerVoiceInstall()
                }
                // The first card may have been drawn before the voice was ready. Only an audio
                // question that fell back to reading is redrawn (if untouched); anything else just
                // gets its speaker buttons, so a tap in flight is never lost to a redraw.
                val s = session
                if (ok && s != null && !s.isFinished && !checked && renderedAsFallback && selected == null) {
                    render()
                } else {
                    ex?.let { refreshSpeakers(it) }
                    if (ok && s != null && !s.isFinished && !checked) {
                        when (val e = s.current) {
                            is Exercise.Intro -> speakSoon(e.item.target)
                            else -> Unit
                        }
                    }
                }
            }
        }

        val retainedSession = retained?.takeIf { it.first == sessionKey() }?.second
        lifecycleScope.launch {
            val built = retainedSession ?: buildSession()
            if (built == null || built.total == 0) {
                Toast.makeText(this@LessonActivity, R.string.learn_practice_later, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            session = built
            retained = sessionKey() to built
            if (retainedSession == null) {
                AppAnalytics.learnLessonStart(language, unit, lesson, kind.name.lowercase())
            }
            render()
        }
    }

    override fun onDestroy() {
        speech?.shutdown()
        handler.removeCallbacksAndMessages(null)
        if (isFinishing) retained = null
        super.onDestroy()
    }

    private fun sessionKey() = "$kind|$language|$unit|$lesson|$phraseSource|$phraseTopic"

    // --- Building -------------------------------------------------------------------------------

    private suspend fun buildSession(): LessonSession? {
        val random = Random(System.nanoTime())
        if (kind == LessonSession.Kind.PHRASES) return buildPhraseSession(random)

        val c = LearnRepository.course(this, language).also { content = it }
        val syllabus = c.syllabus
        val progress = store.progress(language)
        val builder = LessonBuilder(random, listening = store.listening, typing = true)
        val today = LearnDays.today()
        return when (kind) {
            LessonSession.Kind.LESSON -> {
                val news = c.items(syllabus.lessonItems(unit, lesson))
                val before = syllabus.itemsBefore(unit, lesson)
                val review = c.items(before.filter { (progress.items[it]?.seen ?: 0) > 0 }
                    .sortedWith(compareBy({ progress.items[it]?.box ?: 0 }, { -(progress.items[it]?.wrong ?: 0) }))
                    .take(8))
                val pool = c.items(before + syllabus.unitItems(unit)).ifEmpty { c.allItems }
                val tip = if (lesson == 1) c.tipFor(unit)?.let { Exercise.TipCard(it, c.items(it.examples)) } else null
                LessonSession(kind, builder.lesson(news, review, if (pool.size < 12) c.allItems else pool, tip))
            }
            LessonSession.Kind.PRACTICE -> {
                val ids = Srs.pickForPractice(progress.items, today, 10, random)
                val items = c.items(ids)
                val pool = c.items(progress.items.keys.toList()).let { if (it.size < 12) c.allItems else it }
                LessonSession(kind, builder.practice(items, pool))
            }
            LessonSession.Kind.CHECKPOINT -> {
                val ids = (1..3).flatMap { syllabus.unitItems(it) }
                val items = c.items(ids).shuffled(random).take(15)
                LessonSession(kind, builder.checkpoint(items, c.items(ids)))
            }
            LessonSession.Kind.PHRASES -> null
        }
    }

    private suspend fun buildPhraseSession(random: Random): LessonSession? {
        val topics = try {
            LearnRepository.phraseTopics(this, phraseSource, language)
        } catch (e: Exception) {
            return null
        }
        val all = topics.flatMap { it.phrases }
        val known = PhraseProgress.known(this, language)
        val learning = PhraseProgress.learning(this, language)
        val pool = if (phraseTopic >= 0) topics.getOrNull(phraseTopic)?.phrases.orEmpty() else all.filter { it.id in learning }
        val items = pool.sortedBy { if (it.id in known) 1 else 0 }.take(10).shuffled(random)
        if (items.isEmpty()) return null
        val builder = LessonBuilder(random, listening = store.listening, typing = false)
        return LessonSession(kind, builder.practice(items, all))
    }

    // --- Rendering ------------------------------------------------------------------------------

    private fun currentItem(): StudyItem? = when (val e = session?.current) {
        is Exercise.Intro -> e.item
        is Exercise.ChooseMeaning -> e.item
        is Exercise.ChooseTarget -> e.item
        is Exercise.Listen -> e.item
        is Exercise.Tiles -> e.item
        is Exercise.Type -> e.item
        else -> null
    }

    private fun render() {
        val s = session ?: return
        if (s.isFinished) {
            finishSession()
            return
        }
        checked = false
        selected = null
        choiceViews.clear()
        answerTiles.clear()
        matched.clear()
        mismatched.clear()
        matchLeft = null
        matchRight = null
        binding.llFeedback.visibility = View.GONE
        binding.btnCheck.visibility = View.VISIBLE
        binding.pbLesson.setProgress((s.progress * 1000).toInt(), true)
        if (kind == LessonSession.Kind.PHRASES) {
            binding.tvCounter.visibility = View.VISIBLE
            binding.tvCounter.text = getString(R.string.learn_topic_progress, s.index + 1, s.total)
        }

        binding.flExercise.removeAllViews()
        val b = LearnExerciseBinding.inflate(LayoutInflater.from(this), binding.flExercise, true)
        ex = b
        binding.svExercise.scrollTo(0, 0)

        var exercise = s.current
        // No voice on this phone: an audio-only question becomes a reading one.
        renderedAsFallback = false
        if (exercise is Exercise.Listen && !speechReady && speech?.available != true) {
            exercise = Exercise.ChooseMeaning(exercise.item, exercise.options)
            renderedAsFallback = true
        }
        when (exercise) {
            is Exercise.TipCard -> renderTip(b, exercise)
            is Exercise.Intro -> renderIntro(b, exercise.item)
            is Exercise.ChooseMeaning -> renderChoice(b, exercise.item, exercise.options, showTarget = true)
            is Exercise.ChooseTarget -> renderChoice(b, exercise.item, exercise.options, showTarget = false)
            is Exercise.Listen -> renderListen(b, exercise)
            is Exercise.Tiles -> renderTiles(b, exercise)
            is Exercise.Type -> renderType(b, exercise.item)
            is Exercise.Match -> renderMatch(b, exercise)
        }
        refreshSpeakers(b)
    }

    private fun setCheck(label: Int, enabled: Boolean) {
        binding.btnCheck.setText(label)
        binding.btnCheck.isEnabled = enabled
        binding.btnCheck.setBackgroundResource(if (enabled) R.drawable.bg_ui_button_primary else R.drawable.bg_learn_button_disabled)
        binding.btnCheck.setTextColor(LearnUi.color(this, if (enabled) R.color.home_on_accent else R.color.home_text_muted))
        binding.btnCheck.elevation = if (enabled) LearnUi.dp(this, 6f).toFloat() else 0f
    }

    /** Speaker buttons follow whether a voice exists; called again once the voice is ready. */
    private fun refreshSpeakers(b: LearnExerciseBinding) {
        val on = speech?.available == true
        val current = session?.current
        val bigCard = b.llBigCard.visibility == View.VISIBLE
        LearnUi.setVisible(b.ivBigSpeak, on && bigCard)
        LearnUi.setVisible(b.tvSlow, on && bigCard)
        LearnUi.setVisible(b.ivPromptSpeak, on && current is Exercise.ChooseMeaning)
        LearnUi.setVisible(binding.ivFeedbackSpeak, on)
        for (i in 0 until b.llTipExamples.childCount) {
            b.llTipExamples.getChildAt(i).findViewById<View>(R.id.ivSpeak)?.visibility = if (on) View.VISIBLE else View.GONE
        }
    }

    private fun renderTip(b: LearnExerciseBinding, tip: Exercise.TipCard) {
        b.tvChip.visibility = View.VISIBLE
        b.tvChip.text = getString(R.string.learn_tip_chip, tip.tip.unit)
        b.tvChip.setBackgroundResource(R.drawable.bg_learn_chip_purple)
        b.tvChip.setTextColor(LearnUi.color(this, R.color.home_purple))
        b.tvChip.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_ui_bulb, 0, 0, 0)
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(b.tvChip, android.content.res.ColorStateList.valueOf(LearnUi.color(this, R.color.home_purple)))
        b.tvTipTitle.visibility = View.VISIBLE
        b.tvTipTitle.text = tip.tip.title
        b.tvTipBody.visibility = View.VISIBLE
        b.tvTipBody.text = tip.tip.body
        if (tip.examples.isNotEmpty()) {
            b.llTipExamples.visibility = View.VISIBLE
            tip.examples.forEachIndexed { i, item ->
                if (i > 0) b.llTipExamples.addView(divider())
                val row = ItemLearnExampleBinding.inflate(layoutInflater, b.llTipExamples, true)
                row.tvText.text = item.target
                row.tvMeaning.text = item.meaning
                row.ivSpeak.setOnClickListener { speak(item.target) }
            }
        }
        setCheck(R.string.learn_got_it, true)
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LearnUi.dp(this@LessonActivity, 1f))
        setBackgroundColor(LearnUi.color(this@LessonActivity, R.color.home_border))
    }

    private fun renderIntro(b: LearnExerciseBinding, item: StudyItem) {
        b.tvChip.visibility = View.VISIBLE
        b.tvChip.setText(if (item.isSentence) R.string.learn_new_phrase else R.string.learn_new_word)
        b.llBigCard.visibility = View.VISIBLE
        b.tvBigText.text = item.target
        b.tvBigText.textSize = if (item.target.length > 18) 24f else 30f
        b.tvBigMeaning.text = item.meaning
        b.ivBigSpeak.setOnClickListener { speak(item.target) }
        b.tvSlow.setOnClickListener { speak(item.target, slow = true) }
        speakSoon(item.target)
        setCheck(R.string.learn_continue, true)
    }

    private fun renderChoice(b: LearnExerciseBinding, item: StudyItem, options: List<StudyItem>, showTarget: Boolean) {
        b.tvPrompt.visibility = View.VISIBLE
        b.tvPrompt.setText(if (showTarget) R.string.learn_select_meaning else R.string.learn_select_translation)
        b.llPromptCard.visibility = View.VISIBLE
        b.tvPromptText.text = if (showTarget) item.target else item.meaning
        b.ivPromptSpeak.visibility = if (showTarget) View.VISIBLE else View.GONE
        b.ivPromptSpeak.setOnClickListener { speak(item.target) }
        b.llOptions.visibility = View.VISIBLE
        options.forEachIndexed { i, option ->
            val row = ItemLearnChoiceBinding.inflate(layoutInflater, b.llOptions, true)
            row.tvLetter.text = ('A' + i).toString()
            row.tvText.text = if (showTarget) option.meaning else option.target
            row.root.setOnClickListener {
                if (checked) return@setOnClickListener
                select(option)
                if (!showTarget) speak(option.target)
            }
            choiceViews[option.id] = row
        }
        setCheck(R.string.learn_check, false)
    }

    private fun renderListen(b: LearnExerciseBinding, e: Exercise.Listen) {
        b.tvPrompt.visibility = View.VISIBLE
        b.tvPrompt.setText(R.string.learn_what_do_you_hear)
        b.llBigCard.visibility = View.VISIBLE
        b.tvBigText.visibility = View.GONE
        b.tvBigMeaning.visibility = View.GONE
        b.ivBigSpeak.layoutParams = b.ivBigSpeak.layoutParams.apply {
            width = LearnUi.dp(this@LessonActivity, 88f); height = width
        }
        b.ivBigSpeak.setPadding(LearnUi.dp(this, 24f), LearnUi.dp(this, 24f), LearnUi.dp(this, 24f), LearnUi.dp(this, 24f))
        (b.ivBigSpeak.parent as LinearLayout).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        (b.tvSlow.layoutParams as LinearLayout.LayoutParams).apply { marginStart = 0; topMargin = LearnUi.dp(this@LessonActivity, 14f) }
        b.ivBigSpeak.setOnClickListener { speak(e.item.target) }
        b.tvSlow.setOnClickListener { speak(e.item.target, slow = true) }
        speakSoon(e.item.target)

        b.llOptions.visibility = View.VISIBLE
        val rows = e.options.chunked(2)
        for (pair in rows) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            b.llOptions.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            pair.forEachIndexed { i, option ->
                val tile = ItemLearnChoiceBinding.inflate(layoutInflater, row, false)
                tile.tvLetter.visibility = View.GONE
                tile.tvText.text = option.target
                tile.tvText.gravity = android.view.Gravity.CENTER
                tile.root.minimumHeight = LearnUi.dp(this, 64f)
                row.addView(tile.root, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i == 1) marginStart = LearnUi.dp(this@LessonActivity, 10f)
                    bottomMargin = LearnUi.dp(this@LessonActivity, 10f)
                })
                tile.root.setOnClickListener {
                    if (checked) return@setOnClickListener
                    select(option)
                    speak(option.target)
                }
                choiceViews[option.id] = tile
            }
        }
        setCheck(R.string.learn_check, false)
    }

    private fun select(option: StudyItem) {
        selected = option
        for ((id, row) in choiceViews) styleChoice(row, if (id == option.id) ChoiceState.SELECTED else ChoiceState.IDLE)
        setCheck(R.string.learn_check, true)
    }

    private enum class ChoiceState { IDLE, SELECTED, CORRECT, WRONG }

    private fun styleChoice(row: ItemLearnChoiceBinding, state: ChoiceState) {
        val (bg, text, letterBg, letterText) = when (state) {
            ChoiceState.IDLE -> listOf(R.drawable.bg_ui_card_r16_clickable, R.color.home_text_primary, R.drawable.bg_learn_letter, R.color.home_text_secondary)
            ChoiceState.SELECTED -> listOf(R.drawable.bg_learn_option_selected, R.color.home_accent, R.drawable.bg_learn_letter_selected, R.color.home_on_accent)
            ChoiceState.CORRECT -> listOf(R.drawable.bg_learn_option_correct, R.color.home_success, R.drawable.bg_learn_status_success, R.color.home_on_accent)
            ChoiceState.WRONG -> listOf(R.drawable.bg_learn_option_wrong, R.color.home_danger, R.drawable.bg_learn_status_danger, R.color.home_on_accent)
        }
        row.root.setBackgroundResource(bg)
        row.tvText.setTextColor(LearnUi.color(this, text))
        row.tvLetter.setBackgroundResource(letterBg)
        row.tvLetter.setTextColor(LearnUi.color(this, letterText))
    }

    private fun renderTiles(b: LearnExerciseBinding, e: Exercise.Tiles) {
        b.tvPrompt.visibility = View.VISIBLE
        b.tvPrompt.setText(R.string.learn_translate_sentence)
        b.llPromptCard.visibility = View.VISIBLE
        b.tvPromptText.text = e.item.meaning
        b.ivPromptSpeak.visibility = View.GONE
        b.llTiles.visibility = View.VISIBLE
        e.tiles.forEachIndexed { index, word ->
            val tile = tileView(word)
            tile.setOnClickListener {
                if (checked || index in answerTiles) return@setOnClickListener
                answerTiles += index
                speak(word)
                syncTiles(b, e)
            }
            b.cgBank.addView(tile)
        }
        setCheck(R.string.learn_check, false)
    }

    private fun tileView(word: String): TextView = LearnUi.text(this, R.style.HomeText_BodyStrong, word).apply {
        setBackgroundResource(R.drawable.bg_learn_tile)
        val h = LearnUi.dp(context, 14f); val v = LearnUi.dp(context, 10f)
        setPadding(h, v, h, v)
        elevation = LearnUi.dp(context, 1f).toFloat()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) outlineSpotShadowColor = LearnUi.color(context, R.color.home_shadow)
    }

    private fun syncTiles(b: LearnExerciseBinding, e: Exercise.Tiles) {
        b.cgAnswer.removeAllViews()
        for ((pos, index) in answerTiles.withIndex()) {
            val tile = tileView(e.tiles[index])
            tile.setOnClickListener {
                if (checked) return@setOnClickListener
                answerTiles.removeAt(pos)
                syncTiles(b, e)
            }
            b.cgAnswer.addView(tile)
        }
        for (i in 0 until b.cgBank.childCount) {
            val tile = b.cgBank.getChildAt(i) as TextView
            val used = i in answerTiles
            tile.setBackgroundResource(if (used) R.drawable.bg_learn_tile_used else R.drawable.bg_learn_tile)
            tile.setTextColor(LearnUi.color(this, if (used) R.color.home_surface_muted else R.color.home_text_primary))
            tile.elevation = if (used) 0f else LearnUi.dp(this, 1f).toFloat()
        }
        setCheck(R.string.learn_check, answerTiles.isNotEmpty())
    }

    private fun renderType(b: LearnExerciseBinding, item: StudyItem) {
        b.tvPrompt.visibility = View.VISIBLE
        b.tvPrompt.text = getString(R.string.learn_write_in, LearnUi.languageName(this, language))
        b.llPromptCard.visibility = View.VISIBLE
        b.tvPromptText.text = item.meaning
        b.ivPromptSpeak.visibility = View.GONE
        b.llType.visibility = View.VISIBLE
        b.etAnswer.hint = getString(R.string.learn_type_hint, LearnUi.languageName(this, language))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            b.etAnswer.imeHintLocales = LocaleList(LearnSpeech.localeFor(language))
        }
        b.etAnswer.doAfterTextChanged { setCheck(R.string.learn_check, !it.isNullOrBlank()) }
        b.etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && b.etAnswer.text.isNotBlank()) {
                onCheck(); true
            } else false
        }
        val keys = SPECIAL_KEYS[language].orEmpty()
        LearnUi.setVisible(b.cgKeys, keys.isNotEmpty())
        LearnUi.setVisible(b.tvTypeHint, keys.isNotEmpty())
        for (k in keys) {
            val key = LearnUi.text(this, R.style.HomeText_BodyStrong, k).apply {
                setBackgroundResource(R.drawable.bg_learn_key)
                gravity = android.view.Gravity.CENTER
                minWidth = LearnUi.dp(context, 34f)
                minHeight = LearnUi.dp(context, 38f)
                setOnClickListener {
                    val et = b.etAnswer
                    val start = et.selectionStart.coerceAtLeast(0)
                    et.text.replace(start, et.selectionEnd.coerceAtLeast(start), k)
                }
            }
            b.cgKeys.addView(key)
        }
        b.etAnswer.requestFocus()
        b.etAnswer.postDelayed({
            if (!checked && b.etAnswer.isAttachedToWindow) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(b.etAnswer, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }, 250)
        setCheck(R.string.learn_check, false)
    }

    private fun renderMatch(b: LearnExerciseBinding, e: Exercise.Match) {
        b.tvPrompt.visibility = View.VISIBLE
        b.tvPrompt.setText(R.string.learn_match_pairs)
        b.llMatch.visibility = View.VISIBLE
        val random = Random(e.items.hashCode())
        for (item in e.items.shuffled(random)) b.llMatchLeft.addView(matchTile(item.target, item.id, left = true, e))
        for (item in e.items.shuffled(random)) b.llMatchRight.addView(matchTile(item.meaning, item.id, left = false, e))
        setCheck(R.string.learn_continue, false)
    }

    private fun matchTile(text: String, id: String, left: Boolean, e: Exercise.Match): TextView =
        LearnUi.text(this, R.style.HomeText_BodyStrong, text).apply {
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_ui_card_r16_clickable)
            minHeight = LearnUi.dp(context, 56f)
            val p = LearnUi.dp(context, 10f)
            setPadding(p, p, p, p)
            elevation = LearnUi.dp(context, 1f).toFloat()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = LearnUi.dp(context, 10f)
            }
            setOnClickListener { onMatchTap(this, id, left, e) }
        }

    private fun styleMatch(tile: TextView, state: ChoiceState, done: Boolean = false) {
        val (bg, color) = when (state) {
            ChoiceState.IDLE -> R.drawable.bg_ui_card_r16_clickable to R.color.home_text_primary
            ChoiceState.SELECTED -> R.drawable.bg_learn_option_selected to R.color.home_accent
            ChoiceState.CORRECT -> R.drawable.bg_learn_option_correct to R.color.home_success
            ChoiceState.WRONG -> R.drawable.bg_learn_option_wrong to R.color.home_danger
        }
        tile.setBackgroundResource(bg)
        tile.setTextColor(LearnUi.color(this, color))
        tile.alpha = if (done) 0.6f else 1f
    }

    private fun onMatchTap(tile: TextView, id: String, left: Boolean, e: Exercise.Match) {
        if (id in matched && tile.alpha < 1f) return
        if (left) {
            matchLeft?.second?.let { styleMatch(it, ChoiceState.IDLE) }
            matchLeft = id to tile
            e.items.firstOrNull { it.id == id }?.let { speak(it.target) }
        } else {
            matchRight?.second?.let { styleMatch(it, ChoiceState.IDLE) }
            matchRight = id to tile
        }
        styleMatch(tile, ChoiceState.SELECTED)
        val l = matchLeft ?: return
        val r = matchRight ?: return
        matchLeft = null
        matchRight = null
        if (l.first == r.first) {
            matched += l.first
            styleMatch(l.second, ChoiceState.CORRECT, done = true)
            styleMatch(r.second, ChoiceState.CORRECT, done = true)
            l.second.isEnabled = false
            r.second.isEnabled = false
            LearnUi.haptic(tile, true, store.vibration)
            if (matched.size == e.items.size) setCheck(R.string.learn_continue, true)
        } else {
            mismatched += l.first
            mismatched += r.first
            styleMatch(l.second, ChoiceState.WRONG)
            styleMatch(r.second, ChoiceState.WRONG)
            LearnUi.haptic(tile, false, store.vibration)
            handler.postDelayed({
                if (l.first !in matched) styleMatch(l.second, ChoiceState.IDLE)
                if (r.first !in matched) styleMatch(r.second, ChoiceState.IDLE)
            }, 450)
        }
    }

    // --- Checking -------------------------------------------------------------------------------

    private fun onCheck() {
        val s = session ?: return
        val b = ex ?: return
        if (checked) return
        when (val e = s.current) {
            is Exercise.TipCard, is Exercise.Intro -> next()
            is Exercise.Match -> {
                s.answer(mismatched.isEmpty(), mismatched)
                next()
            }
            is Exercise.ChooseMeaning, is Exercise.ChooseTarget, is Exercise.Listen -> {
                val item = currentItem() ?: return
                val pick = selected ?: return
                val ok = pick.id == item.id
                choiceViews[pick.id]?.let { styleChoice(it, if (ok) ChoiceState.CORRECT else ChoiceState.WRONG) }
                if (!ok) choiceViews[item.id]?.let { styleChoice(it, ChoiceState.CORRECT) }
                val showsMeaning = e is Exercise.ChooseMeaning ||
                    (e is Exercise.Listen && speech?.available != true)
                feedback(ok, item, AnswerChecker.Verdict.CORRECT, item.target, meaningAnswer = showsMeaning)
            }
            is Exercise.Tiles -> {
                val built = answerTiles.joinToString(" ") { e.tiles[it] }
                val result = AnswerChecker.check(built, e.item.accepted)
                feedback(result.verdict == AnswerChecker.Verdict.CORRECT, e.item, result.verdict, result.closest)
            }
            is Exercise.Type -> {
                val result = AnswerChecker.check(b.etAnswer.text.toString(), e.item.accepted)
                hideKeyboard(b.etAnswer)
                b.etAnswer.isEnabled = false
                feedback(result.isAccepted, e.item, result.verdict, result.closest)
            }
        }
    }

    private fun feedback(ok: Boolean, item: StudyItem, verdict: AnswerChecker.Verdict, closest: String, meaningAnswer: Boolean = false) {
        val s = session ?: return
        checked = true
        s.answer(ok)
        LearnUi.haptic(binding.btnCheck, ok, store.vibration)
        binding.btnCheck.visibility = View.INVISIBLE
        val f = binding
        f.llFeedback.visibility = View.VISIBLE
        f.llFeedback.setBackgroundResource(if (ok) R.drawable.bg_learn_sheet_success else R.drawable.bg_learn_sheet_danger)
        f.flFeedbackIcon.setBackgroundResource(if (ok) R.drawable.bg_learn_status_success else R.drawable.bg_learn_status_danger)
        f.ivFeedbackIcon.setImageResource(if (ok) R.drawable.ic_ui_check else R.drawable.ic_ui_x)
        val accent = LearnUi.color(this, if (ok) R.color.home_success else R.color.home_danger)
        f.tvFeedbackTitle.setTextColor(accent)
        f.tvFeedbackLabel.setTextColor(accent)
        f.ivFeedbackSpeak.setColorFilter(accent)
        f.tvFeedbackTitle.setText(
            if (ok) listOf(R.string.learn_great_job, R.string.learn_correct, R.string.learn_nice).random() else R.string.learn_not_quite
        )
        val label: String?
        val answer: String
        var note: String? = null
        if (ok) {
            label = when (verdict) {
                AnswerChecker.Verdict.ACCENT -> getString(R.string.learn_accent_note)
                AnswerChecker.Verdict.TYPO -> getString(R.string.learn_typo_note)
                else -> null
            }
            answer = if (verdict == AnswerChecker.Verdict.CORRECT) item.target else closest
            note = getString(R.string.learn_meaning_label, item.meaning)
        } else {
            label = getString(R.string.learn_correct_answer)
            answer = if (meaningAnswer) item.meaning else item.target
            note = if (meaningAnswer) item.target else if (kind != LessonSession.Kind.CHECKPOINT) getString(R.string.learn_ask_again) else null
        }
        LearnUi.setVisible(f.tvFeedbackLabel, label != null)
        f.tvFeedbackLabel.text = label
        f.tvFeedbackAnswer.text = answer
        LearnUi.setVisible(f.tvFeedbackNote, note != null)
        f.tvFeedbackNote.text = note
        f.btnFeedbackContinue.setBackgroundResource(if (ok) R.drawable.bg_learn_button_success else R.drawable.bg_ui_button_danger)
        f.btnFeedbackContinue.setText(if (ok) R.string.learn_continue else R.string.learn_got_it)
        if (ok && speech?.available == true) speak(item.target)
    }

    private fun next() {
        val s = session ?: return
        handler.removeCallbacksAndMessages(null)
        speech?.stop()
        if (s.next()) render() else finishSession()
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // --- Audio ----------------------------------------------------------------------------------

    private fun speak(text: String, slow: Boolean = false) {
        speech?.speak(text, slow)
    }

    private fun speakSoon(text: String) {
        if (!store.listening) return
        handler.postDelayed({ speak(text) }, 350)
    }

    /**
     * No voice for the course language (common on Samsung's own engine): once per language, offer
     * the engine's free voice download; afterwards only a short toast. Lessons work without it.
     */
    private fun offerVoiceInstall() {
        val name = LearnUi.languageName(this, language)
        val prefs = getSharedPreferences(LearnStore.PREFS, MODE_PRIVATE)
        val key = "voiceOffered_$language"
        if (prefs.getBoolean(key, false)) {
            Toast.makeText(this, getString(R.string.learn_tts_missing, name), Toast.LENGTH_LONG).show()
            return
        }
        prefs.edit().putBoolean(key, true).apply()
        LearnUi.messageDialog(
            this,
            icon = R.drawable.ic_ui_speaker,
            iconTint = R.color.home_accent,
            iconBackground = R.drawable.bg_ui_circle_accent,
            title = getString(R.string.learn_tts_missing_title, name),
            message = getString(R.string.learn_tts_install_desc, name),
            primary = getString(R.string.learn_tts_install),
            secondary = getString(R.string.learn_not_now),
            onPrimary = {
                try {
                    startActivity(Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.learn_tts_missing, name), Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    // --- Report a problem -----------------------------------------------------------------------

    private fun report() {
        val item = currentItem() ?: return
        val reasons = arrayOf(
            getString(R.string.learn_report_my_answer),
            getString(R.string.learn_report_translation),
            getString(R.string.learn_report_audio)
        )
        val codes = arrayOf("answer_not_accepted", "translation_wrong", "audio_wrong")
        AlertDialog.Builder(this)
            .setTitle(R.string.learn_report_title)
            .setItems(reasons) { _, which ->
                AppAnalytics.learnReportIssue(language, item.id, codes[which])
                Toast.makeText(this, R.string.learn_report_sent, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // --- Quit / finish ----------------------------------------------------------------------------

    private fun confirmQuit() {
        val s = session
        if (s == null || s.index == 0 || s.isFinished) {
            finish()
            return
        }
        val streak = store.streak()
        val message = if (streak > 0 && !store.didLessonToday()) {
            resources.getQuantityString(R.plurals.learn_quit_desc_streak, streak, streak)
        } else {
            getString(R.string.learn_quit_desc)
        }
        LearnUi.messageDialog(
            this,
            icon = R.drawable.ic_ui_flame,
            iconTint = R.color.home_orange,
            iconBackground = R.drawable.bg_ui_circle_orange,
            title = getString(R.string.learn_quit_title),
            message = message,
            primary = getString(R.string.learn_keep_learning),
            secondary = getString(R.string.learn_quit_lesson),
            secondaryDanger = true,
            onPrimary = {},
            onSecondary = {
                AppAnalytics.learnLessonQuit(language, unit, lesson, kind.name.lowercase(), s.index)
                finish()
            }
        )
    }

    private fun finishSession() {
        val s = session ?: return
        binding.llFeedback.visibility = View.GONE
        binding.pbLesson.setProgress(1000, true)
        val seconds = ((System.currentTimeMillis() - s.startedAt) / 1000).toInt()
        val outcome = when (kind) {
            LessonSession.Kind.PHRASES -> {
                PhraseProgress.recordQuiz(this, language, s.itemResults)
                store.recordActivity(s.xp)
            }
            else -> {
                val syllabus = content?.syllabus ?: return
                val key = if (kind == LessonSession.Kind.LESSON) Syllabus.lessonKey(unit, lesson) else null
                val testOut = if (kind == LessonSession.Kind.CHECKPOINT && s.passed) 3 else 0
                if (testOut > 0 && testOut + 1 > LearnConfig.freeUnits) {
                    // Passing the test skips the free units; don't land the learner on a lock.
                    store.unlockUnit(language, testOut + 1, System.currentTimeMillis() + LearnConfig.unlockHours * 3_600_000L)
                    AppAnalytics.learnUnitUnlock(language, testOut + 1, "test_out")
                }
                store.record(language, syllabus, s, key, testOut)
            }
        }
        LearnReminder.schedule(this)
        AppAnalytics.learnLessonComplete(language, unit, lesson, kind.name.lowercase(), s.accuracy, seconds, s.xp)
        if (outcome.streakExtended) AppAnalytics.learnStreak(outcome.streak)
        retained = null
        showResult(s, outcome, seconds)
    }

    private fun showResult(s: LessonSession, outcome: LearnStore.Outcome, seconds: Int) {
        val r = binding.result
        r.root.visibility = View.VISIBLE
        binding.llLesson.visibility = View.GONE
        val languageName = LearnUi.languageName(this, language)
        when (kind) {
            LessonSession.Kind.LESSON -> {
                r.tvResultTitle.setText(R.string.learn_lesson_complete)
                val n = s.newItems.size
                r.tvResultDesc.text = resources.getQuantityString(R.plurals.learn_learned_new, n, n, languageName)
            }
            LessonSession.Kind.CHECKPOINT -> {
                r.tvResultTitle.setText(if (s.passed) R.string.learn_test_passed else R.string.learn_test_failed)
                r.tvResultDesc.text = if (s.passed) getString(R.string.learn_test_passed_desc) else getString(R.string.learn_test_failed_desc, s.accuracy)
                if (s.passed) {
                    r.flHalo.setBackgroundResource(R.drawable.bg_ui_circle_purple)
                    r.ivResultIcon.setImageResource(R.drawable.ic_ui_trophy)
                    r.ivResultIcon.setColorFilter(LearnUi.color(this, R.color.home_purple))
                }
            }
            else -> {
                r.tvResultTitle.setText(if (kind == LessonSession.Kind.PHRASES) R.string.learn_practice_done else R.string.learn_practice_complete)
                val n = s.itemResults.size
                r.tvResultDesc.text = resources.getQuantityString(
                    if (kind == LessonSession.Kind.PHRASES) R.plurals.learn_reviewed_phrases else R.plurals.learn_reviewed, n, n
                )
            }
        }
        r.tvStatXp.text = "+${outcome.xp}"
        r.tvStatAccuracy.text = "${s.accuracy}%"
        r.tvStatTime.text = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
        r.tvStreakTitle.text = resources.getQuantityString(R.plurals.learn_streak_days, outcome.streak, outcome.streak)
        val goal = store.dailyGoal
        r.tvStreakGoal.text = if (outcome.lessonsToday >= goal) {
            getString(R.string.learn_goal_reached, outcome.lessonsToday.coerceAtMost(goal), goal)
        } else {
            getString(R.string.learn_goal_today, outcome.lessonsToday, goal)
        }
        r.tvStreakGoal.setTextColor(LearnUi.color(this, if (outcome.lessonsToday >= goal) R.color.home_success else R.color.home_text_secondary))
        fillWeek(r.llWeek)
        r.btnResultContinue.setOnClickListener { finishWithAd() }
        r.btnResultClose.setOnClickListener { finishWithAd() }
        LearnAds.bottomNative(this, r.flResultAd)
    }

    /** Monday-to-Sunday dots for this week: done days filled, today ringed. */
    private fun fillWeek(row: LinearLayout) {
        row.removeAllViews()
        val today = LearnDays.today()
        val active = store.activeDays()
        val frozen = store.frozenDays()
        val cal = Calendar.getInstance()
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        val monday = today - dow
        val names = java.text.DateFormatSymbols.getInstance(java.util.Locale(org.dslul.openboard.translator.pro.classes.Misc.getAppLanguage(this))).shortWeekdays
        for (i in 0 until 7) {
            val day = monday + i
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            val size = LearnUi.dp(this, 28f)
            val dot = android.widget.FrameLayout(this)
            dot.setBackgroundResource(
                when {
                    day in active -> R.drawable.bg_learn_week_done
                    day in frozen -> R.drawable.bg_learn_week_frozen
                    day == today -> R.drawable.bg_learn_week_today
                    else -> R.drawable.bg_learn_week_empty
                }
            )
            if (day in frozen && day !in active) {
                val flake = android.widget.ImageView(this).apply {
                    setImageResource(R.drawable.ic_ui_snowflake)
                    setColorFilter(LearnUi.color(this@LessonActivity, R.color.home_accent))
                }
                dot.addView(flake, android.widget.FrameLayout.LayoutParams(LearnUi.dp(this, 14f), LearnUi.dp(this, 14f), android.view.Gravity.CENTER))
            }
            if (day in active) {
                val check = android.widget.ImageView(this).apply {
                    setImageResource(R.drawable.ic_ui_check)
                    setColorFilter(LearnUi.color(this@LessonActivity, R.color.home_on_accent))
                }
                dot.addView(check, android.widget.FrameLayout.LayoutParams(LearnUi.dp(this, 14f), LearnUi.dp(this, 14f), android.view.Gravity.CENTER))
            }
            col.addView(dot, LinearLayout.LayoutParams(size, size))
            val calIndex = (i + 1) % 7 + 1 // Calendar.MONDAY = 2 ... SUNDAY = 1
            val label = LearnUi.text(this, R.style.HomeText_CaptionMedium, names[calIndex].take(1).uppercase())
            if (day == today) label.setTextColor(LearnUi.color(this, R.color.home_orange))
            col.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = LearnUi.dp(this@LessonActivity, 4f)
            })
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private var finishing = false

    private fun finishWithAd() {
        if (finishing) return
        finishing = true
        LearnAds.afterLesson(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        const val EXTRA_KIND = "learnKind"
        const val EXTRA_UNIT = "learnUnit"
        const val EXTRA_LESSON = "learnLesson"
        const val EXTRA_PHRASE_SOURCE = "learnPhraseSource"
        const val EXTRA_PHRASE_TARGET = "learnPhraseTarget"
        const val EXTRA_PHRASE_TOPIC = "learnPhraseTopic"

        /** Accent keys under the answer field: characters a learner's keyboard may hide. */
        private val SPECIAL_KEYS = mapOf(
            "es" to listOf("á", "é", "í", "ó", "ú", "ñ", "ü", "¿", "¡"),
            "fr" to listOf("é", "è", "ê", "à", "â", "ç", "î", "ô", "û", "ù", "ë", "œ"),
            "de" to listOf("ä", "ö", "ü", "ß", "Ä", "Ö", "Ü"),
            "it" to listOf("à", "è", "é", "ì", "ò", "ù"),
        )

        /** Languages whose missing voice was already reported in this process. */
        private val ttsWarned = HashSet<String>()

        /** The running session, kept across a configuration change (theme switch) in this process. */
        private var retained: Pair<String, LessonSession>? = null

        fun lesson(context: Context, unit: Int, lesson: Int): Intent =
            Intent(context, LessonActivity::class.java)
                .putExtra(EXTRA_KIND, LessonSession.Kind.LESSON.name)
                .putExtra(EXTRA_UNIT, unit)
                .putExtra(EXTRA_LESSON, lesson)

        fun practice(context: Context): Intent =
            Intent(context, LessonActivity::class.java).putExtra(EXTRA_KIND, LessonSession.Kind.PRACTICE.name)

        fun checkpoint(context: Context): Intent =
            Intent(context, LessonActivity::class.java).putExtra(EXTRA_KIND, LessonSession.Kind.CHECKPOINT.name)

        /** [topic] = phrasebook category index, or -1 for the "still learning" review. */
        fun phrases(context: Context, source: String, target: String, topic: Int): Intent =
            Intent(context, LessonActivity::class.java)
                .putExtra(EXTRA_KIND, LessonSession.Kind.PHRASES.name)
                .putExtra(EXTRA_PHRASE_SOURCE, source)
                .putExtra(EXTRA_PHRASE_TARGET, target)
                .putExtra(EXTRA_PHRASE_TOPIC, topic)
    }
}
