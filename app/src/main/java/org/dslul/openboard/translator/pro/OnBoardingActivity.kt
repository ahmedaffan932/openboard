package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityOnBoardingBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobBannerAds
import org.dslul.openboard.translator.pro.fragments.OnBaordingFragment

class OnBoardingActivity : AppCompatActivity() {
    lateinit var binding: ActivityOnBoardingBinding
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
//
//        if (Ads.onBoardingNative.contains("native")) {
//            Ads.loadAndShowNativeAd(
//                this,
//                remoteKey = Ads.onBoardingNative,
//                frameLayout = binding.nativeAdFrameLayout,
//                adLayout = R.layout.admob_small_native_ad_hctr,
//                shimmerLayout = R.layout.small_native_shimmer
//            )
//        }

        binding.splashViewPager.adapter = FragmentAdapter(this)

        TabLayoutMediator(
            binding.splashTabLayout,
            binding.splashViewPager
        ) { tab, position -> }.attach()

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
            startActivity(Intent(this, FragmentsDashboardActivity::class.java))
        }

        binding.btnNext.setOnClickListener {
            if (binding.splashViewPager.currentItem < 2) {
                binding.splashViewPager.setCurrentItem(
                    binding.splashViewPager.currentItem + 1,
                    true
                )
            } else {
                startActivity(Intent(this, FragmentsDashboardActivity::class.java))
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

}