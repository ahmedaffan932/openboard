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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityOnBoardingBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.fragments.OnBaordingFragment
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class OnBoardingActivity : AppCompatActivity() {
    lateinit var binding: ActivityOnBoardingBinding
    private val shownOnboardingNativePages = mutableSetOf<Int>()

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

        TabLayoutMediator(
            binding.splashTabLayout,
            binding.splashViewPager
        ) { tab, position -> }.attach()

        binding.splashViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    showOnboardingNativeForPage(position)
                }
            }
        )

        binding.splashTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(p0: TabLayout.Tab?) {
                when (p0?.position) {
                    0 -> {
                        changeImage(R.drawable.on_boarding_tab_one)
                    }

                    1 -> {
                        changeImage(R.drawable.on_boarding_tab_two)
                    }

                    else -> {
                        changeImage(R.drawable.on_boarding_tab_three)
                    }
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {
            }

        })

        object : CountDownTimer(3000, 1500) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                binding.btnContinue.visibility = View.VISIBLE
            }

        }.start()

        binding.btnContinue.setOnClickListener {
            Ads.loadAndShowInterstitial(
                this,
                Ads.dashboardInt,
                AdIds.interstitialAdIdAdMobSplash,
                object : InterstitialCallBack {
                    override fun onDismiss() {
                        openNextScreenAfterOnboarding()
                    }
                })
        }

        binding.btnNext.setOnClickListener {
            if (binding.splashViewPager.currentItem < 2) {
                binding.splashViewPager.setCurrentItem(
                    binding.splashViewPager.currentItem + 1,
                    true
                )
            } else {
                Ads.loadAndShowInterstitial(
                    this,
                    Ads.dashboardInt,
                    AdIds.interstitialAdIdAdMobSplash,
                    object : InterstitialCallBack {
                        override fun onDismiss() {
                            openNextScreenAfterOnboarding()
                        }
                    })
            }
        }
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

    private fun changeImage(newImage: Int) {
        binding.btnNext.animate().alpha(0.5f).setDuration(150)
            .withEndAction {
                binding.btnNext.setImageResource(newImage)

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
        if (shownOnboardingNativePages.contains(position)) return

        if (!Ads.onBoardingNative.contains("native")) {
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
            shownOnboardingNativePages.add(position)
            Log.d(Misc.logKey, "Onboarding native shown for page: $position")

            AdmobNativeAds.showNativeAd(
                context = this,
                remoteKey = Ads.onBoardingNative,
                amLayout = binding.nativeAdFrameLayout,
                nextPreloadAdIds = nextAdIds
            )
            return
        }

        Log.d(Misc.logKey, "Onboarding native load requested for page: $position")

        AdmobNativeAds.loadAdmobNative(
            context = this,
            adIds = adIds,
            remoteKey = Ads.onBoardingNative,
            frameLayout = binding.nativeAdFrameLayout,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    if (
                        binding.splashViewPager.currentItem != position ||
                        shownOnboardingNativePages.contains(position)
                    ) {
                        return
                    }

                    shownOnboardingNativePages.add(position)
                    Log.d(Misc.logKey, "Onboarding native loaded and shown for page: $position")

                    AdmobNativeAds.showNativeAd(
                        context = this@OnBoardingActivity,
                        remoteKey = Ads.onBoardingNative,
                        amLayout = binding.nativeAdFrameLayout,
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

    private fun getNextOnboardingNativeAdIds(position: Int): Array<String>? {
        return when (position) {
            0 -> AdIds.nativeAdIdOB2
            1 -> AdIds.nativeAdIdOB3
            else -> null
        }
    }

}
