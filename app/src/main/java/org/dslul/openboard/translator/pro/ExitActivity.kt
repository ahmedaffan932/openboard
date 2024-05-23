package org.dslul.openboard.translator.pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RatingBar
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityExitBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads

class ExitActivity : AppCompatActivity() {
    private var reviewManager: ReviewManager? = null
    private lateinit var binding: ActivityExitBinding

    private fun init() {
        reviewManager = ReviewManagerFactory.create(this)
        if (Misc.isFirstTime(this)) {
            showRateApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        if(!Misc.isItemClicked) {
            Ads.loadAndShowInterstitial(this, Ads.exitInt, AdIds.interstitialAdIdAdMobExit)
        }
        Ads.loadAndShowNativeAd(this, AdIds.nativeAdIdAdMobExit, Ads.exitNative,binding.nativeAdFrameLayout,
            R.layout.admob_native_hctr, R.layout.large_native_shimmer)

        binding.exitRating.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, p1, _ ->
                if (p1 > 3f) {
                    showRateApp()
                } else {
                    Toast.makeText(this, "Thanks for your review.", Toast.LENGTH_SHORT).show()
                }
            }

        binding.btnYes.setOnClickListener {
            finishAffinity()
        }
        binding.btnNo.setOnClickListener {
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