package org.dslul.openboard.inputmethod.latin.gesture

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** A key that can be part of a swipe: its character and its center on the keyboard. */
class GestureKey(val code: Int, val x: Float, val y: Float)

/**
 * Turns a swipe path into words. Each candidate word is drawn as the ideal path through the
 * centers of its keys and compared with the finger's path: how far apart the two paths are
 * (location), whether they have the same outline (shape), and whether the finger passed near
 * every letter in order. The dictionary probability breaks ties between similar paths.
 *
 * Only words starting near the first touch and ending near the last one are compared, so a swipe
 * looks at a few hundred words rather than the whole dictionary.
 */
class GestureRecognizer(keys: List<GestureKey>, private val keyWidth: Float) {

    class Result(val word: String, val score: Double)

    private val keys: List<GestureKey> = keys.filter { it.code > 0 }
    private val keyByCode = HashMap<Int, GestureKey>()
    private val keyByBase = HashMap<Int, GestureKey>()

    init {
        for (key in this.keys) {
            keyByCode.putIfAbsent(Character.toLowerCase(key.code), key)
            keyByBase.putIfAbsent(GestureWords.baseLetter(key.code), key)
        }
    }

    fun recognize(xs: IntArray, ys: IntArray, count: Int, words: GestureWords, maxResults: Int): List<Result> {
        if (keys.isEmpty() || keyWidth <= 0f || count < 2) return emptyList()
        val raw = ArrayList<FloatArray>(count)
        for (i in 0 until count) {
            val x = xs[i].toFloat()
            val y = ys[i].toFloat()
            val prev = raw.lastOrNull()
            if (prev == null || prev[0] != x || prev[1] != y) raw.add(floatArrayOf(x, y))
        }
        if (raw.size < 2) return emptyList()
        val gestureLength = pathLength(raw)
        if (gestureLength < keyWidth * 0.5f) return emptyList()
        val gesture = resample(raw, SAMPLES)
        val gestureShape = normalizedShape(gesture)

        val firstLetters = nearbyLetters(raw.first())
        val lastLetters = nearbyLetters(raw.last())
        val candidates = words.candidates(firstLetters, lastLetters)

        val results = ArrayList<Result>()
        for (id in candidates) {
            val word = words.words[id]
            val path = idealPath(word) ?: continue
            val wordLength = pathLength(path)
            if (abs(wordLength - gestureLength) > LENGTH_TOLERANCE * max(wordLength, gestureLength) + keyWidth * 1.5f) continue

            val ideal = resample(path, SAMPLES)
            var location = 0.0
            for (i in 0 until SAMPLES) location += distance(gesture[i], ideal[i])
            location /= SAMPLES * keyWidth

            val idealShape = normalizedShape(ideal)
            var shape = 0.0
            for (i in 0 until SAMPLES) shape += distance(gestureShape[i], idealShape[i])
            shape /= SAMPLES

            val missed = missedKeysPenalty(gesture, path)
            val extra = extraPathPenalty(gesture, path)

            val score = -(location * location) / (2 * LOCATION_SIGMA * LOCATION_SIGMA) -
                    SHAPE_WEIGHT * (shape * shape) / (2 * SHAPE_SIGMA * SHAPE_SIGMA) -
                    MISSED_KEY_WEIGHT * missed -
                    EXTRA_PATH_WEIGHT * extra +
                    FREQUENCY_WEIGHT * words.probabilities[id] / 255.0
            results.add(Result(word, score))
        }
        results.sortByDescending { it.score }
        return if (results.size > maxResults) results.subList(0, maxResults).toList() else results
    }

    /** Base letters of the keys close enough to [point] for a swipe to start or end there. */
    private fun nearbyLetters(point: FloatArray): Set<Int> {
        val byDistance = keys.map { it to distance(point, it.x, it.y) }.sortedBy { it.second }
        val nearest = byDistance.first().second
        val limit = max(nearest + keyWidth * 0.5, keyWidth * 0.9)
        return byDistance.asSequence().takeWhile { it.second <= limit }.take(MAX_END_KEYS)
            .map { GestureWords.baseLetter(it.first.code) }.toSet()
    }

    /** Key centers the word passes through, or null if one of its letters has no key here. */
    private fun idealPath(word: String): List<FloatArray>? {
        val path = ArrayList<FloatArray>(word.length)
        var previous: GestureKey? = null
        var i = 0
        while (i < word.length) {
            val cp = word.codePointAt(i)
            i += Character.charCount(cp)
            if (GestureWords.isSkippedOnPath(cp)) continue
            val key = keyByCode[Character.toLowerCase(cp)] ?: keyByBase[GestureWords.baseLetter(cp)] ?: return null
            if (key !== previous) path.add(floatArrayOf(key.x, key.y))
            previous = key
        }
        return if (path.size < 2) null else path
    }

    /**
     * How badly the finger missed the word's middle letters: each one must be passed near, in
     * order (a letter that appears twice must be passed twice). A letter within half a key costs
     * nothing. Solved as an in-order alignment of letters to swipe points.
     */
    private fun missedKeysPenalty(gesture: Array<FloatArray>, path: List<FloatArray>): Double {
        val n = gesture.size
        var previous = DoubleArray(n) // best total for the letters so far, ending at point i
        val current = DoubleArray(n)
        for (k in 1 until path.size - 1) {
            var bestBefore = Double.MAX_VALUE
            for (i in 0 until n) {
                bestBefore = min(bestBefore, previous[i])
                val miss = max(0.0, distance(gesture[i], path[k][0], path[k][1]) / keyWidth - 0.5)
                current[i] = bestBefore + miss * miss
            }
            previous = current.copyOf()
        }
        return if (path.size <= 2) 0.0 else previous.min()
    }

    /** How far the swipe wanders from the word's path (detours the word doesn't explain). */
    private fun extraPathPenalty(gesture: Array<FloatArray>, path: List<FloatArray>): Double {
        var penalty = 0.0
        for (p in gesture) {
            var best = Double.MAX_VALUE
            for (s in 1 until path.size) best = min(best, distanceToSegment(p, path[s - 1], path[s]))
            val off = max(0.0, best / keyWidth - 0.5)
            penalty += off * off
        }
        return penalty / gesture.size
    }

    companion object {
        private const val SAMPLES = 64
        private const val MAX_END_KEYS = 4
        private const val LENGTH_TOLERANCE = 0.45f
        private const val LOCATION_SIGMA = 0.45
        private const val SHAPE_SIGMA = 0.12
        private const val SHAPE_WEIGHT = 0.5
        private const val MISSED_KEY_WEIGHT = 6.0
        private const val EXTRA_PATH_WEIGHT = 12.0
        private const val FREQUENCY_WEIGHT = 2.5

        private fun distance(a: FloatArray, b: FloatArray) = hypot((a[0] - b[0]).toDouble(), (a[1] - b[1]).toDouble())
        private fun distance(a: FloatArray, x: Float, y: Float) = hypot((a[0] - x).toDouble(), (a[1] - y).toDouble())

        private fun distanceToSegment(p: FloatArray, a: FloatArray, b: FloatArray): Double {
            val dx = b[0] - a[0]
            val dy = b[1] - a[1]
            val lengthSquared = dx * dx + dy * dy
            if (lengthSquared == 0f) return distance(p, a)
            val t = (((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / lengthSquared).coerceIn(0f, 1f)
            return distance(p, a[0] + t * dx, a[1] + t * dy)
        }

        private fun pathLength(points: List<FloatArray>): Float {
            var length = 0.0
            for (i in 1 until points.size) length += distance(points[i - 1], points[i])
            return length.toFloat()
        }

        /** [n] points spread evenly along the path. */
        private fun resample(points: List<FloatArray>, n: Int): Array<FloatArray> {
            val total = pathLength(points)
            if (total <= 0f) return Array(n) { points[0].copyOf() }
            val step = total / (n - 1)
            val out = ArrayList<FloatArray>(n)
            out.add(points[0].copyOf())
            var carried = 0f
            var px = points[0][0]
            var py = points[0][1]
            var i = 1
            while (i < points.size && out.size < n) {
                val qx = points[i][0]
                val qy = points[i][1]
                val segment = hypot(qx - px, qy - py)
                if (carried + segment >= step && segment > 0f) {
                    val t = (step - carried) / segment
                    px += t * (qx - px)
                    py += t * (qy - py)
                    out.add(floatArrayOf(px, py))
                    carried = 0f
                } else {
                    carried += segment
                    px = qx
                    py = qy
                    i++
                }
            }
            val last = points.last()
            while (out.size < n) out.add(last.copyOf())
            return out.toTypedArray()
        }

        /** The path moved to its centroid and scaled so its larger side is 1. */
        private fun normalizedShape(points: Array<FloatArray>): Array<FloatArray> {
            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            var cx = 0f; var cy = 0f
            for (p in points) {
                minX = min(minX, p[0]); maxX = max(maxX, p[0])
                minY = min(minY, p[1]); maxY = max(maxY, p[1])
                cx += p[0]; cy += p[1]
            }
            cx /= points.size; cy /= points.size
            val scale = max(maxX - minX, maxY - minY).let { if (it <= 0f) 1f else it }
            return Array(points.size) { floatArrayOf((points[it][0] - cx) / scale, (points[it][1] - cy) / scale) }
        }
    }
}
