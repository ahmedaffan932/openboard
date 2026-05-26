package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityFragmentsDashboardBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobRewardedAds
import org.dslul.openboard.translator.pro.fragments.CameraFragment
import org.dslul.openboard.translator.pro.fragments.ChatFragment
import org.dslul.openboard.translator.pro.fragments.HomeFragment
import org.dslul.openboard.translator.pro.fragments.PhrasesFragment
import org.dslul.openboard.translator.pro.interfaces.RewardedAdCallBack

class FragmentsDashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityFragmentsDashboardBinding
    var lastSelectedItem = R.id.home
    var collapsibleBannerView: AdView? = null
    var fragmentChangeCount = 0
    private var isCameraUnlockedByReward = false
    private var isChatUnlockedByReward = false

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
        preloadUnlockPremiumRewardedAd()

        binding.bottomNavigation.selectedItemId = R.id.home
//        collapsibleBannerView = AdmobCollapsibleBannerAds.loadCollapsibleBanner(
//            this,
//            remoteKey = Ads.dashboardBanner,
//            view = binding.llCollapsibleBanner
//        )

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            if (isLockedPremiumItem(it.itemId) && !canOpenLockedPremiumItem(it.itemId)) {
                showUnlockPremiumDialog(it.itemId)
                return@setOnNavigationItemSelectedListener false
            }

            Ads.runWithEverySixthClickInterstitial(this) {
                openDashboardItem(it.itemId)
            }
            true
        }

        binding.bottomNavigation.setOnItemReselectedListener {
            if (isLockedPremiumItem(it.itemId) && !canOpenLockedPremiumItem(it.itemId)) {
                showUnlockPremiumDialog(it.itemId)
                return@setOnItemReselectedListener
            }

            Ads.runWithEverySixthClickInterstitial(this) {
                openDashboardItem(it.itemId)
            }
        }

    }

    private fun openDashboardItem(itemId: Int) {
        when (itemId) {
            R.id.home -> setCurrentFragment(HomeFragment())
            R.id.camera -> setCurrentFragment(CameraFragment())
            R.id.chat -> setCurrentFragment(ChatFragment())
            R.id.phrasebook -> setCurrentFragment(PhrasesFragment())
        }
        lastSelectedItem = itemId
    }

    private fun isLockedPremiumItem(itemId: Int): Boolean {
        return itemId == R.id.camera || itemId == R.id.chat
    }

    private fun canOpenLockedPremiumItem(): Boolean {
        return !Misc.isProScreenEnabled || Misc.getPurchasedStatus(this)
    }

    private fun canOpenLockedPremiumItem(itemId: Int): Boolean {
        return canOpenLockedPremiumItem() ||
            when (itemId) {
                R.id.camera -> isCameraUnlockedByReward
                R.id.chat -> isChatUnlockedByReward
                else -> true
            }
    }

    private fun preloadUnlockPremiumRewardedAd() {
        if (canOpenLockedPremiumItem()) return

        if (!Ads.unlockPremiumRewardedAd.contains("am")) {
            Log.d(Misc.logKey, "Unlock premium rewarded preload skipped: remote off.")
            return
        }

        if (!Misc.checkInternetConnection(this)) {
            Log.d(Misc.logKey, "Unlock premium rewarded preload skipped: no internet.")
            return
        }

        AdmobRewardedAds.loadRewardedAd(
            context = this,
            adIds = AdIds.rewardedAdIdUnlockPremium,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    Log.d(Misc.logKey, "Unlock premium rewarded ad preloaded.")
                }

                override fun onFailed() {
                    Log.d(Misc.logKey, "Unlock premium rewarded ad failed to preload.")
                }
            }
        )
    }

    private fun showUnlockPremiumDialog(targetItemId: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.camera_translate_pro_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)

        dialog.findViewById<TextView>(R.id.tvDescription)?.text =
            getString(R.string.unlock_premium_feature_description)

        dialog.findViewById<TextView>(R.id.btnUpgrade)?.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, PremiumScreenActivity::class.java))
        }

        dialog.findViewById<TextView>(R.id.btnWatchAd)?.setOnClickListener {
            dialog.dismiss()
            loadAndShowUnlockRewardedAd(targetItemId)
        }

        if (!isFinishing && !isDestroyed) {
            dialog.show()
        }
    }

    private fun loadAndShowUnlockRewardedAd(targetItemId: Int) {
        if (!Ads.unlockPremiumRewardedAd.contains("am")) {
            Toast.makeText(this, R.string.rewarded_ad_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(this, R.string.rewarded_ad_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = Misc.LoadingAdDialog(this)
        loadingDialog.setCancelable(false)

        if (!isFinishing && !isDestroyed) {
            loadingDialog.show()
        }

        AdmobRewardedAds.loadRewardedAd(
            context = this,
            adIds = AdIds.rewardedAdIdUnlockPremium,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    if (!isFinishing && !isDestroyed && loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }

                    AdmobRewardedAds.showRewardedAd(
                        activity = this@FragmentsDashboardActivity,
                        callBack = object : RewardedAdCallBack {
                            override fun onRewardEarned() {
                                unlockPremiumFeatureByReward(targetItemId)
                                Log.d(Misc.logKey, "Premium feature unlocked by rewarded ad.")

                                runOnUiThread {
                                    binding.bottomNavigation.menu.findItem(targetItemId).isChecked = true
                                    openDashboardItem(targetItemId)
                                }
                            }

                            override fun onFailed() {
                                Toast.makeText(
                                    this@FragmentsDashboardActivity,
                                    R.string.rewarded_ad_not_available,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                override fun onFailed() {
                    if (!isFinishing && !isDestroyed && loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }

                    Toast.makeText(
                        this@FragmentsDashboardActivity,
                        R.string.rewarded_ad_not_available,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun unlockPremiumFeatureByReward(itemId: Int) {
        when (itemId) {
            R.id.camera -> isCameraUnlockedByReward = true
            R.id.chat -> isChatUnlockedByReward = true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) {
        fragmentChangeCount++
//        if (fragmentChangeCount > 4) {
//            Ads.loadAndShowInterstitial(
//                this,
//                remoteKey = Ads.dashboardFragmentChangeInt,
//                callBack = object : InterstitialCallBack {
//                    override fun onDismiss() {
//                        val fragmentManager = supportFragmentManager
//
//                        fragmentManager.beginTransaction().apply {
//                            replace(R.id.fragmentContainer, fragment)
//                            addToBackStack("")
//                            commit()
//                        }
//                    }
//                })
//            fragmentChangeCount = 0
//        } else {
            val fragmentManager = supportFragmentManager

            fragmentManager.beginTransaction().apply {
                replace(R.id.fragmentContainer, fragment)
                addToBackStack("")
                commit()
            }
//        }
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
