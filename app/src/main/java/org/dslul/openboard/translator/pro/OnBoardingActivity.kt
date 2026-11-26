package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.translatorguru.ads.admob.LoadAdCallBack
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityOnBoardingBinding
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.fragments.OnBaordingFragment
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class OnBoardingActivity : AppCompatActivity() {
    lateinit var binding: ActivityOnBoardingBinding
    private val loggedSlides = mutableSetOf<Int>()
    private var isOnboardingCompleteLogged = false
    private var nextButtonLabel = R.string.next

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        if (Ads.onBoardingNative.contains("collapsible")) {
//            AdmobBannerAds.loadCollapsibleBanner(
//                this,
//                AdIds.collapsibleBannerAdIdAdOnboarding,
//                Ads.onBoardingNative,
//                binding.llCollapsibleBanner
//            )
//        }

        binding.splashViewPager.adapter = FragmentAdapter(this)
        showOnboardingNativeForPage(0)
        logSlideView(0)

        showPageIndicator(0)

        binding.splashViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    showOnboardingNativeForPage(position)
                    logSlideView(position)
                    showPageIndicator(position)
                }
            }
        )

        object : CountDownTimer(3000, 1500) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                binding.btnContinue.visibility = View.VISIBLE
            }

        }.start()

        binding.btnContinue.setOnClickListener {
            finishOnboarding()
        }

        binding.btnNext.setOnClickListener {
            if (binding.splashViewPager.currentItem < LAST_PAGE) {
                binding.splashViewPager.setCurrentItem(
                    binding.splashViewPager.currentItem + 1,
                    true
                )
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        // Logged once even if the button is tapped again while the interstitial loads.
        if (!isOnboardingCompleteLogged) {
            isOnboardingCompleteLogged = true
            AppAnalytics.onboardingComplete()
        }
        Ads.loadAndShowInterstitial(
            this,
            Ads.dashboardInt,
            AdIds.interstitialAdIdOB3,
            object : InterstitialCallBack {
                override fun onDismiss() {
                    openNextScreenAfterOnboarding()
                }
            })
    }

    /**
     * One `screen_onboarding_<n>` + `onboarding_slide_view` per slide per visit; swiping back does
     * not count it twice.
     */
    private fun logSlideView(position: Int) {
        if (loggedSlides.add(position)) AppAnalytics.onboardingSlideView(position + 1)
    }

    override fun onBackPressed() {
        if (!Misc.splashScreenOnBackPressDoNothing)
            super.onBackPressed()
    }


    private inner class FragmentAdapter(fa: FragmentActivity) :
        FragmentStateAdapter(fa) {
        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            return OnBaordingFragment.newInstance(position.toString())
        }
    }

    /** Page dots (the current one wide and tinted) and the button: Next, Get started on the last page. */
    private fun showPageIndicator(position: Int) {
        val density = resources.displayMetrics.density
        listOf(binding.dot1, binding.dot2, binding.dot3).forEachIndexed { index, dot ->
            val isCurrent = index == position
            dot.layoutParams = dot.layoutParams.apply {
                width = ((if (isCurrent) 22 else 6) * density).toInt()
            }
            dot.setBackgroundResource(
                if (isCurrent) R.drawable.ui_launch_dot_active else R.drawable.ui_launch_dot_inactive
            )
        }

        val labelRes = if (position >= LAST_PAGE) R.string.ui_launch_get_started else R.string.next
        if (labelRes == nextButtonLabel) return
        nextButtonLabel = labelRes
        // A newer swipe cancels this fade, and with it this end action.
        binding.btnNext.animate().alpha(0.5f).setDuration(150)
            .withEndAction {
                binding.btnNext.setText(labelRes)
                binding.btnNext.animate().alpha(1f).setDuration(100).start()
            }.start()
    }

    private fun openNextScreenAfterOnboarding() {
        val premiumShownBeforeLanguage =
            intent.getBooleanExtra(Misc.premiumShownBeforeLanguage, false)

        if (Misc.isProScreenEnabled && !premiumShownBeforeLanguage) {
            startActivity(
                Intent(
                    this@OnBoardingActivity,
                    PremiumScreenActivity::class.java
                ).putExtra(Misc.data, Misc.data)
            )
        } else {
            startActivity(
                Intent(
                    this@OnBoardingActivity,
                    FragmentsDashboardActivity::class.java
                )
            )
        }
    }

    private fun showOnboardingNativeForPage(position: Int) {
        val remoteKey = getOnboardingRemoteKey(position)

        if (!remoteKey.contains("am")) {
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            Log.d(Misc.logKey, "Onboarding native skipped for page: $position")
            return
        }

        if (!Misc.checkInternetConnection(this) || Misc.getPurchasedStatus(this)) {
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            Log.d(Misc.logKey, "Onboarding native skipped for page: $position")
            return
        }

        val adIds = getOnboardingNativeAdIds(position)
        val nextAdIds = getNextOnboardingNativeAdIds(position)

        if (AdmobNativeAds.isNativeAdAvailableFor(adIds)) {
            Log.d(Misc.logKey, "Onboarding native shown for page: $position")

            AdmobNativeAds.showNativeAd(
                context = this,
                remoteKey = remoteKey,
                amLayout = binding.nativeAdFrameLayout,
                adIds = adIds,
                nextPreloadAdIds = nextAdIds
            )
            return
        }

        Log.d(Misc.logKey, "Onboarding native load requested for page: $position")

        AdmobNativeAds.loadAdmobNative(
            context = this,
            adIds = adIds,
            remoteKey = remoteKey,
            frameLayout = binding.nativeAdFrameLayout,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    if (binding.splashViewPager.currentItem != position) {
                        return
                    }

                    Log.d(Misc.logKey, "Onboarding native loaded and shown for page: $position")

                    AdmobNativeAds.showNativeAd(
                        context = this@OnBoardingActivity,
                        remoteKey = remoteKey,
                        amLayout = binding.nativeAdFrameLayout,
                        adIds = adIds,
                        nextPreloadAdIds = nextAdIds
                    )
                }

                override fun onFailed() {
                    if (binding.splashViewPager.currentItem == position) {
                        binding.nativeAdFrameLayout.removeAllViews()
                        binding.nativeAdFrameLayout.visibility = View.GONE
                    }

                    Log.d(Misc.logKey, "Onboarding native failed for page: $position")
                }
            }
        )
    }

    private fun getOnboardingNativeAdIds(position: Int): Array<String> {
        return when (position) {
            1 -> AdIds.nativeAdIdOB2
            2 -> AdIds.nativeAdIdOB3
            else -> AdIds.nativeAdIdOB1
        }
    }

    private fun getOnboardingRemoteKey(position: Int): String {
        return when (position) {
            1 -> Ads.onBoardingNative2
            2 -> Ads.onBoardingNative3
            else -> Ads.onBoardingNative
        }
    }

    private fun getNextOnboardingNativeAdIds(position: Int): Array<String>? {
        val nextPosition = position + 1
        if (nextPosition > 2 || !getOnboardingRemoteKey(nextPosition).contains("am")) {
            return null
        }
        return getOnboardingNativeAdIds(nextPosition)
    }

    companion object {
        /** The third (last) page's index; its button reads Get started. */
        private const val LAST_PAGE = 2
    }
}
