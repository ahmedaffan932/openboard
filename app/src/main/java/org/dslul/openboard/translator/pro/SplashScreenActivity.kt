package org.dslul.openboard.translator.pro


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_splash_screen.btnContinue
import kotlinx.android.synthetic.main.activity_splash_screen.nativeAdFrameLayout
import kotlinx.android.synthetic.main.activity_splash_screen.splashTabLayout
import kotlinx.android.synthetic.main.activity_splash_screen.splashViewPager
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.dslul.openboard.translator.pro.fragments.SplashFragment

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash_screen)

        Firebase.analytics.logEvent("SplashScreenStarted", null)

        splashViewPager.adapter = FragmentAdapter(this)

        Ads.showNativeAd(this, Ads.onBoardingNative, nativeAdFrameLayout)

        TabLayoutMediator(splashTabLayout, splashViewPager) { tab, position -> }.attach()

        object : CountDownTimer(3000, 1500){
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                btnContinue.visibility = View.VISIBLE
            }

        }.start()

        btnContinue.setOnClickListener {
            if (splashViewPager.currentItem < 2) {
                splashViewPager.setCurrentItem(splashViewPager.currentItem + 1, true)
            } else {
                startActivity(Intent(this, NewDashboardActivity::class.java))
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
            return SplashFragment.newInstance(position.toString())
        }
    }

}