package org.dslul.openboard.translator.pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RatingBar
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.android.synthetic.main.activity_exit.*
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.rateUs
import org.dslul.openboard.translator.pro.classes.admob.Ads

class ExitActivity : AppCompatActivity() {
    private var reviewManager: ReviewManager? = null
    private fun init() {
        reviewManager = ReviewManagerFactory.create(this)
        if (Misc.isFirstTime(this)) {
            showRateApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exit)

        Ads.showNativeAd(this, Ads.exitNative, nativeAdFrameLayout)

        init()

        exit_rating.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, p1, _ ->
                if (p1 > 3f) {
                    showRateApp()
                } else {
                    Toast.makeText(this, "Thanks for your review.", Toast.LENGTH_SHORT).show()
                }
            }

        btnYes.setOnClickListener {
            finishAffinity()
        }
        btnNo.setOnClickListener {
            finish()
        }
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