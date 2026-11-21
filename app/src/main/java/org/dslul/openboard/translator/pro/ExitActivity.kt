package org.dslul.openboard.translator.pro

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityExitBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.Ads

class ExitActivity : AppCompatActivity() {
    private var reviewManager: ReviewManager? = null
    private lateinit var binding: ActivityExitBinding

    /** Stars chosen so far (0 = not rated yet); a tap on the same star again does nothing. */
    private var rating = 0

    private val stars: List<ImageView> by lazy {
        listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
    }

    private fun init() {
        reviewManager = ReviewManagerFactory.create(this)
        if (Misc.isFirstTime(this)) {
            showRateApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityExitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

//        if(!Misc.isItemClicked) {
//        }
        Ads.loadAndShowSharedBottomNativeAd(this, binding.nativeAdFrameLayout)

        // The five stars replace the old RatingBar: 4-5 stars open the in-app review, fewer
        // thank the user (same as before).
        stars.forEachIndexed { index, star ->
            star.contentDescription = getString(R.string.ui_launch_exit_star_desc, index + 1)
            star.setOnClickListener { onRated(index + 1) }
        }

        binding.btnYes.setOnClickListener {
            finishAffinity()
        }
        binding.btnNo.setOnClickListener {
            finish()
        }
    }

    private fun onRated(count: Int) {
        if (count == rating) return
        rating = count
        showRating(count)
        if (count > 3) {
            showRateApp()
        } else {
            Toast.makeText(this, R.string.thanks_for_your_review, Toast.LENGTH_SHORT).show()
        }
    }

    /** Figma "Exit · Rated": filled stars, thank-you title, and a line under the stars. */
    private fun showRating(count: Int) {
        stars.forEachIndexed { index, star ->
            val isFilled = index < count
            star.setImageResource(if (isFilled) R.drawable.ic_ui_star_filled else R.drawable.ic_ui_star)
            ImageViewCompat.setImageTintList(
                star,
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (isFilled) R.color.home_star else R.color.home_text_muted
                    )
                )
            )
        }
        binding.tvRateTitle.setText(R.string.ui_launch_exit_rated_title)
        val isHappy = count > 3
        binding.tvRateHint.setText(
            if (isHappy) R.string.ui_launch_exit_rated_good else R.string.ui_launch_exit_rated_low
        )
        binding.tvRateHint.setTextColor(
            ContextCompat.getColor(this, if (isHappy) R.color.home_success else R.color.home_text_muted)
        )
    }

    private fun showRateApp() {
        val request: Task<ReviewInfo> =
            reviewManager!!.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo> ->
            if (task.isSuccessful) {
                // Getting the ReviewInfo object
                val reviewInfo: ReviewInfo = task.result
                reviewInfo.let { reviewManager?.launchReviewFlow(this, it) }
                    ?.addOnCompleteListener { task1: Task<Void> -> }
            }
        }
    }

}
