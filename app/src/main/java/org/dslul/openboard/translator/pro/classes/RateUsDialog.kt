package org.dslul.openboard.translator.pro.classes

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import org.dslul.openboard.inputmethod.latin.R

class RateUsDialog
    (var c: Activity) : Dialog(c), View.OnClickListener {
    var d: Dialog? = null

    private val starIds = intArrayOf(
        R.id.btnRateA, R.id.btnRateB, R.id.btnRateC, R.id.btnRateD, R.id.btnRateE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.rate_us_dialog_box)
        bindStarHighlight()
    }

    /**
     * Fills the stars up to the one under the finger (Figma 9:1755: ic_ui_star_filled in home_star,
     * the rest ic_ui_star in home_text_muted). Visual only: the listener returns false, so the taps
     * still reach the click listeners SettingsActivity sets.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun bindStarHighlight() {
        starIds.forEachIndexed { index, id ->
            findViewById<ImageView>(id)?.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> showStars(index + 1)
                    MotionEvent.ACTION_CANCEL -> showStars(0)
                }
                false
            }
        }
    }

    private fun showStars(count: Int) {
        val filledTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.home_star))
        val emptyTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.home_text_muted))
        starIds.forEachIndexed { index, id ->
            val star = findViewById<ImageView>(id) ?: return@forEachIndexed
            val filled = index < count
            star.setImageResource(if (filled) R.drawable.ic_ui_star_filled else R.drawable.ic_ui_star)
            ImageViewCompat.setImageTintList(star, if (filled) filledTint else emptyTint)
        }
    }

    override fun onClick(v: View) {
    }
}
