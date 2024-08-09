package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityFragmentsDashboardBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobCollapsibleBannerAds
import org.dslul.openboard.translator.pro.fragments.CameraFragment
import org.dslul.openboard.translator.pro.fragments.ChatFragment
import org.dslul.openboard.translator.pro.fragments.HomeFragment
import org.dslul.openboard.translator.pro.fragments.PhrasesFragment
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class FragmentsDashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityFragmentsDashboardBinding
    var lastSelectedItem = R.id.home
    var collapsibleBannerView: AdView? = null
    var fragmentChangeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityFragmentsDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!Misc.isFirstTime(this)){
            fragmentChangeCount = 8
        }

        binding.bottomNavigation.menu

        setCurrentFragment(HomeFragment())

        binding.bottomNavigation.selectedItemId = R.id.home

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            if (it.itemId != lastSelectedItem) {
                when (it.itemId) {
                    R.id.home -> setCurrentFragment(HomeFragment())
                    R.id.camera -> setCurrentFragment(CameraFragment())
                    R.id.chat -> setCurrentFragment(ChatFragment())
                    R.id.phrasebook -> setCurrentFragment(PhrasesFragment())
                }
                lastSelectedItem = it.itemId

                if (lastSelectedItem != R.id.home) {
                    if (collapsibleBannerView == null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            collapsibleBannerView = AdmobCollapsibleBannerAds.loadCollapsibleBanner(
                                this,
                                remoteKey = Ads.dashboardBanner,
                                view = binding.llCollapsibleBanner
                            )
                        }, 100)
                    }
                }
            }
            true
        }

    }

    private fun setCurrentFragment(fragment: Fragment) {
        fragmentChangeCount++
        if (fragmentChangeCount > 4) {
            Ads.loadAndShowInterstitial(
                this,
                remoteKey = Ads.dashboardFragmentChangeInt,
                callBack = object : InterstitialCallBack {
                    override fun onDismiss() {
                        val fragmentManager = supportFragmentManager

                        fragmentManager.beginTransaction().apply {
                            replace(R.id.fragmentContainer, fragment)
                            addToBackStack("")
                            commit()
                        }
                    }
                })
            fragmentChangeCount = 0
        } else {
            val fragmentManager = supportFragmentManager

            fragmentManager.beginTransaction().apply {
                replace(R.id.fragmentContainer, fragment)
                addToBackStack("")
                commit()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (lastSelectedItem == R.id.home) {
            startActivity(Intent(this, ExitActivity::class.java))
        } else {
            binding.bottomNavigation.selectedItemId = R.id.home
//            setCurrentFragment(HomeFragment())
        }
    }

}