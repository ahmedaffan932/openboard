package org.dslul.openboard.inputmethod.latin.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class GestureRecognizerTest {

    // A QWERTY layout 100 px per key wide and 150 px tall, like a phone in portrait.
    private val keyWidth = 100f
    private val keys: List<GestureKey> = buildList {
        "qwertyuiop".forEachIndexed { i, c -> add(GestureKey(c.code, 50f + 100f * i, 75f)) }
        "asdfghjkl".forEachIndexed { i, c -> add(GestureKey(c.code, 100f + 100f * i, 225f)) }
        "zxcvbnm".forEachIndexed { i, c -> add(GestureKey(c.code, 200f + 100f * i, 375f)) }
    }
    private val keyByChar = keys.associateBy { it.code.toChar() }

    // Common words, most frequent first; probability falls with rank like a real dictionary.
    private val vocabulary = ("the to and of you it in is that for was on he with as his be at " +
        "by this have from or had not but what all were we when your can said there use an each " +
        "which she do how their if will up other about out many then them these so some her would " +
        "make like him into time has look two more write go see number no way could people my than " +
        "first water been call who now find long down day did get come made may part over new " +
        "sound take only little work know place year live me back give most very after thing our " +
        "just name good sentence man think say great where help through much before line right too " +
        "mean old any same tell boy follow came want show also around form three small set put end " +
        "does another well large must big even such because turn here why ask went men read need " +
        "land different home us move try kind hand picture again change off play spell air away " +
        "animal house point page letter mother answer found study still learn should world high " +
        "hello keyboard typing phone today thanks please morning love food mood tight night light " +
        "hell jello hole wore were word wire ward tie toe top tip type tired").split(" ").distinct()

    private val words = GestureWords.build(vocabulary.mapIndexed { rank, w -> w to 250 - rank * 150 / vocabulary.size })
    private val recognizer = GestureRecognizer(keys, keyWidth)

    /** A wobbly finger path through the word's keys, never exactly on the key centers. */
    private fun swipe(word: String, random: Random): Pair<IntArray, IntArray> {
        val targets = word.map { c ->
            val key = keyByChar.getValue(c)
            floatArrayOf(key.x + random.nextFloat() * 50f - 25f, key.y + random.nextFloat() * 70f - 35f)
        }
        val xs = ArrayList<Int>()
        val ys = ArrayList<Int>()
        for (i in 1 until targets.size) {
            val a = targets[i - 1]
            val b = targets[i]
            val steps = max(1, (hypot(b[0] - a[0], b[1] - a[1]) / 12f).toInt())
            for (s in 0 until steps) {
                val t = s.toFloat() / steps
                val wobble = 8f * sin(t * Math.PI).toFloat()
                xs.add((a[0] + t * (b[0] - a[0])).toInt())
                ys.add((a[1] + t * (b[1] - a[1]) + wobble).toInt())
            }
        }
        xs.add(targets.last()[0].toInt())
        ys.add(targets.last()[1].toInt())
        return xs.toIntArray() to ys.toIntArray()
    }

    private fun recognize(word: String, random: Random): List<String> {
        val (xs, ys) = swipe(word, random)
        return recognizer.recognize(xs, ys, xs.size, words, 5).map { it.word }
    }

    @Test
    fun recognizesClearSwipes() {
        val random = Random(42)
        for (word in listOf("hello", "keyboard", "typing", "phone", "world", "people", "thanks", "morning")) {
            assertEquals(word, recognize(word, random).firstOrNull())
        }
    }

    @Test
    fun mostSwipesLandOnTheIntendedWord() {
        val random = Random(7)
        var top1 = 0
        var top3 = 0
        val misses = ArrayList<String>()
        val testable = vocabulary.filter { w -> w.toSet().size > 1 }
        for (word in testable) {
            val found = recognize(word, random)
            if (found.firstOrNull() == word) top1++ else misses.add("$word -> ${found.take(3)}")
            if (word in found.take(3)) top3++
        }
        println("top1 $top1/${testable.size}, top3 $top3/${testable.size}")
        misses.forEach { println("  $it") }
        assertTrue("top-1 ${top1}/${testable.size}: $misses", top1 >= testable.size * 0.85)
        assertTrue("top-3 ${top3}/${testable.size}: $misses", top3 >= testable.size * 0.97)
    }

    @Test
    fun noKeysOrTooShortPathGivesNothing() {
        assertTrue(GestureRecognizer(emptyList(), keyWidth).recognize(intArrayOf(0, 300), intArrayOf(0, 0), 2, words, 5).isEmpty())
        assertTrue(recognizer.recognize(intArrayOf(50, 52), intArrayOf(75, 76), 2, words, 5).isEmpty())
    }

    @Test
    fun wordListDropsJunkAndKeepsBestProbability() {
        val list = GestureWords.build(listOf("don't" to 100, "don't" to 180, "a" to 255, "abc1" to 200, "x y" to 200, "café" to 90))
        assertEquals(listOf("don't", "café"), list.words.toList())
        assertEquals(180, list.probabilities[0])
        assertEquals('e'.code, GestureWords.baseLetter('É'.code))
        assertEquals(listOf(1), list.candidates(listOf('c'.code), listOf('e'.code)))
        assertEquals(listOf(0), list.candidates(listOf('d'.code), listOf('t'.code)))
    }

    @Test
    fun cachedListReadsBackTheSame() {
        val text = StringBuilder().also { words.write(it) }.toString()
        val back = GestureWords.read((text + "broken line\n\tno word\n").lineSequence())
        assertEquals(words.words.toList(), back.words.toList())
        assertEquals(words.probabilities.toList(), back.probabilities.toList())
    }

    @Test
    fun accentedWordsUseTheBaseLetterKey() {
        val list = GestureWords.build(listOf("café" to 200, "cage" to 100))
        val (xs, ys) = swipe("cafe", Random(1))
        assertEquals("café", recognizer.recognize(xs, ys, xs.size, list, 5).first().word)
    }
}
