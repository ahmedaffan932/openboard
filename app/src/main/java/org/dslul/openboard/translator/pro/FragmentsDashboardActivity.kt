package org.dslul.openboard.translator.pro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityFragmentsDashboardBinding
import org.dslul.openboard.translator.pro.classes.AppAnalytics
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
import org.dslul.openboard.translator.pro.notifications.PinnedNoti
import org.dslul.openboard.translator.pro.widget.WidgetPrompt

class FragmentsDashboardActivity : AppCompatActivity() {

    companion object {
        /** Opens a tab on arrival (notifications, the pinned card, launcher shortcuts). */
        const val EXTRA_OPEN_TAB = "openTab"
        const val TAB_CAMERA = "camera"
        const val TAB_CHAT = "chat"
        const val TAB_PHRASEBOOK = "phrasebook"

        private const val PREFS_NOTI_PERMISSION = "notiPermission"
        private const val KEY_ASKED = "asked"
    }

    /**
     * Android 13+ shows no notification at all until this is granted. Asked once, on the first
     * dashboard visit (the end of first open); the pinned Quick Access card follows the answer.
     */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) PinnedNoti.post(this)
        }

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
        applySystemBarInsets()

        if(!Misc.isFirstTime(this)){
            fragmentChangeCount = 8
        }

        binding.bottomNavigation.menu

        setCurrentFragment(HomeFragment())
        if (savedInstanceState == null) AppAnalytics.screenView(tabScreen(R.id.home))
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

        if (savedInstanceState == null) openTabFromIntent(intent)
        requestNotificationsOrPostPinned()
        // The "Add widget" card. Arms on the first visit, shows on a later one, so it never stacks
        // on the notification-permission dialog this visit may raise.
        if (savedInstanceState == null) WidgetPrompt.showIfDue(this)
    }

    /** A notification or the pinned card reached a dashboard that is already open (SINGLE_TOP). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTabFromIntent(intent)
    }

    /**
     * The tab named by [EXTRA_OPEN_TAB], under the same premium rule as a tap on it: a locked
     * camera or chat tab gets the upgrade / rewarded-ad offer, never the feature for free. No
     * every-sixth-click interstitial - the entry itself was the user's tap on a notification.
     */
    private fun openTabFromIntent(intent: Intent) {
        val itemId = when (intent.getStringExtra(EXTRA_OPEN_TAB)) {
            TAB_CAMERA -> R.id.camera
            TAB_CHAT -> R.id.chat
            TAB_PHRASEBOOK -> R.id.phrasebook
            else -> return
        }
        intent.removeExtra(EXTRA_OPEN_TAB)
        if (isLockedPremiumItem(itemId) && !canOpenLockedPremiumItem(itemId)) {
            showUnlockPremiumDialog(itemId)
            return
        }
        binding.bottomNavigation.menu.findItem(itemId).isChecked = true
        openDashboardItem(itemId)
    }

    private fun requestNotificationsOrPostPinned() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            PinnedNoti.post(this)
            return
        }
        val prefs = getSharedPreferences(PREFS_NOTI_PERMISSION, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) return
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun applySystemBarInsets() {
//        val view = binding.main
//        val initialPaddingLeft = view.paddingLeft
//        val initialPaddingTop = view.paddingTop
//        val initialPaddingRight = view.paddingRight
//        val initialPaddingBottom = view.paddingBottom
//        val initialBottomNavigationMargin =
//            (binding.bottomNavigation.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

//        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
//            val bars = windowInsets.getInsets(
//                WindowInsetsCompat.Type.systemBars() or
//                    WindowInsetsCompat.Type.displayCutout()
//            )
//
////            v.updatePadding(
////                left = initialPaddingLeft + bars.left,
////                top = initialPaddingTop + bars.top,
////                right = initialPaddingRight + bars.right,
////                bottom = initialPaddingBottom,
////            )
//
////            binding.bottomNavigation.updateLayoutParams<ViewGroup.MarginLayoutParams> {
////                bottomMargin = initialBottomNavigationMargin + bars.bottom
////            }
//
//            windowInsets
//        }

//        ViewCompat.requestApplyInsets(view)
    }

    private fun openDashboardItem(itemId: Int) {
        when (itemId) {
            R.id.home -> setCurrentFragment(HomeFragment())
            R.id.camera -> setCurrentFragment(CameraFragment())
            R.id.chat -> setCurrentFragment(ChatFragment())
            R.id.phrasebook -> setCurrentFragment(PhrasesFragment())
        }
        lastSelectedItem = itemId
        AppAnalytics.screenView(tabScreen(itemId))
    }

    /** The tab's screen event: `screen_home`, `screen_camera`, `screen_conversation` or `screen_phrasebook`. */
    private fun tabScreen(itemId: Int): String = when (itemId) {
        R.id.chat -> "conversation"
        else -> tabName(itemId)
    }

    /** The tab's name in analytics: `home`, `camera`, `chat` or `phrasebook`. */
    private fun tabName(itemId: Int): String = when (itemId) {
        R.id.home -> "home"
        R.id.camera -> TAB_CAMERA
        R.id.chat -> TAB_CHAT
        R.id.phrasebook -> TAB_PHRASEBOOK
        else -> "other"
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

        val feature = tabName(targetItemId)

        dialog.findViewById<TextView>(R.id.btnUpgrade)?.setOnClickListener {
            dialog.dismiss()
            AppAnalytics.featureLockAction(feature, "upgrade")
            startActivity(
                Intent(this, PremiumScreenActivity::class.java).putExtra(
                    AppAnalytics.EXTRA_PREMIUM_SOURCE,
                    AppAnalytics.PREMIUM_SOURCE_FEATURE_LOCK
                )
            )
        }

        dialog.findViewById<TextView>(R.id.btnWatchAd)?.setOnClickListener {
            dialog.dismiss()
            AppAnalytics.featureLockAction(feature, "watch_ad")
            loadAndShowUnlockRewardedAd(targetItemId)
        }

        if (!isFinishing && !isDestroyed) {
            dialog.show()
            AppAnalytics.featureLockView(feature)
        }
    }

    private fun loadAndShowUnlockRewardedAd(targetItemId: Int) {
        val feature = tabName(targetItemId)

        if (!Ads.unlockPremiumRewardedAd.contains("am")) {
            AppAnalytics.featureUnlockFailed(feature, "remote_off")
            Toast.makeText(this, R.string.rewarded_ad_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        if (!Misc.checkInternetConnection(this)) {
            AppAnalytics.featureUnlockFailed(feature, "no_internet")
            Toast.makeText(this, R.string.rewarded_ad_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = Misc.LoadingAdDialog(this)
        loadingDialog.setCancelable(false)

        if (!isFinishing && !isDestroyed) {
            loadingDialog.show()
        }

        // Guard so only the first of {loaded, failed, timeout} takes effect.
        var settled = false
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (settled) return@Runnable
            settled = true

            if (!isFinishing && !isDestroyed && loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }

            Log.d(Misc.logKey, "Unlock rewarded ad timed out. Unlocking feature.")
            unlockWithoutAd(targetItemId, feature, "ad_timeout")
        }
        timeoutHandler.postDelayed(timeoutRunnable, Ads.adLoadTimeoutMs)

        AdmobRewardedAds.loadRewardedAd(
            context = this,
            adIds = AdIds.rewardedAdIdUnlockPremium,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    if (settled) return
                    settled = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)

                    if (!isFinishing && !isDestroyed && loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }

                    AdmobRewardedAds.showRewardedAd(
                        activity = this@FragmentsDashboardActivity,
                        callBack = object : RewardedAdCallBack {
                            override fun onRewardEarned() {
                                AppAnalytics.featureUnlock(feature, "rewarded")
                                unlockPremiumFeatureByReward(targetItemId)
                                Log.d(Misc.logKey, "Premium feature unlocked by rewarded ad.")

                                runOnUiThread {
                                    binding.bottomNavigation.menu.findItem(targetItemId).isChecked = true
                                    openDashboardItem(targetItemId)
                                }
                            }

                            override fun onFailed() {
                                Log.d(Misc.logKey, "Unlock rewarded ad failed to show. Unlocking feature.")
                                runOnUiThread { unlockWithoutAd(targetItemId, feature, "show_failed") }
                            }
                        }
                    )
                }

                override fun onFailed() {
                    if (settled) return
                    settled = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)

                    if (!isFinishing && !isDestroyed && loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }

                    // No ad to show (e.g. "no fill", common on a new ad account): let the
                    // user in, as on a timeout, instead of leaving the feature locked.
                    Log.d(Misc.logKey, "Unlock rewarded ad failed to load. Unlocking feature.")
                    unlockWithoutAd(targetItemId, feature, "ad_failed")
                }
            }
        )
    }

    /**
     * Opens a locked feature although no reward was earned: the user tapped "Watch Ad" but no ad
     * could be loaded or shown, so they are not left stuck behind the lock.
     */
    private fun unlockWithoutAd(targetItemId: Int, feature: String, reason: String) {
        AppAnalytics.featureUnlock(feature, reason)
        unlockPremiumFeatureByReward(targetItemId)
        if (!isFinishing && !isDestroyed) {
            binding.bottomNavigation.menu.findItem(targetItemId).isChecked = true
            openDashboardItem(targetItemId)
        }
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
