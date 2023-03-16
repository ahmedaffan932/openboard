package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.blongho.country_data.World
import com.example.translatorguru.classes.admob.BannerAds
import com.example.translatorguru.classes.admob.InterstitialAd
import com.example.translatorguru.classes.admob.NativeAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.activity_dashboard.view.quitBottomSheet
import kotlinx.android.synthetic.main.bottom_sheet_quit.view.*
import kotlinx.android.synthetic.main.dailog_custom.view.btnNo
import kotlinx.android.synthetic.main.dailog_custom.view.btnYes
import org.dslul.openboard.inputmethod.latin.R
import com.example.translatorguru.classes.Misc

class DashboardActivity : AppCompatActivity() {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>


    @SuppressLint("SetTextI18n", "RemoteViewLayout")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        Misc.setIsFirstTime(this, false)

        Firebase.analytics.logEvent("Dashboard", null)

        showNativeAd()

        showDashboardInterstitial()

        World.init(applicationContext)
        Misc.selectThemeMode(this)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.quitBottomSheet))
        quitBottomSheet.quitBottomSheet.setOnClickListener { }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        Misc.isActivityCreatingFirstTime = true

        blockView.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            blockView.visibility = View.GONE
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    blockView.visibility = View.VISIBLE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    blockView.visibility = View.GONE
                }
            }
        })

        quitBottomSheet.btnYes.setOnClickListener {
            finishAffinity()
        }

        quitBottomSheet.btnNo.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

//        btnSettings.setOnClickListener {
//            Firebase.analytics.logEvent("Settings", null)
//            startActivity(Intent(this@DashboardActivity, SettingsActivity::class.java))
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//        }

        btnHistory.setOnClickListener {
            Firebase.analytics.logEvent("History", null)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            startActivity(
                Intent(
                    this@DashboardActivity,
                    DisplayHistoryActivity::class.java
                )
            )
        }

        btnTranslation.setOnClickListener {
            Firebase.analytics.logEvent("Translation", null)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            startActivity(Intent(this@DashboardActivity, TranslateActivity::class.java))
        }

//        btnMultipleTranslate.setOnClickListener {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            Firebase.analytics.logEvent("Multi_Translation", null)
//
//            startActivity(
//                Intent(
//                    this@DashboardActivity,
//                    MultiLanguageTranslationActivity::class.java
//                )
//            )
//        }

//        btnChat.setOnClickListener {
//            Firebase.analytics.logEvent("Chat", null)
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            startActivity(Intent(this@DashboardActivity, ConversationActivity::class.java))
//        }

//        btnPhrasebook.setOnClickListener {
//            Firebase.analytics.logEvent("Phrasebook", null)
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            startActivity(Intent(this@DashboardActivity, PhrasesActivity::class.java))
//        }

//        btnGame.setOnClickListener {
//            Firebase.analytics.logEvent("Game", null)
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            startActivity(Intent(this@DashboardActivity, GameActivity::class.java))
//        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            quitBottomSheet.nativeAdFrameLayoutQuit.visibility = View.GONE
            Log.d(Misc.logKey, "Bottom sheet clicked.")
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun showDashboardInterstitial() {
        InterstitialAd.show(this, Misc.dashboardIntAm)
    }

    override fun onResume() {
        super.onResume()
        BannerAds.show(bannerFrame)
        Log.d(Misc.logKey, "onResume")
        if (Misc.isNativeAdClicked) {
            showNativeAd()
        }
    }


    private fun showNativeAd() {
        if (Misc.isDashboardInBetweenNativeEnabled) {
            NativeAds.showSmallNativeAd(
                this,
                Misc.dashboardNativeAm,
                nativeAdFrameLayoutInBetween
            )
        } else {
            NativeAds.manageShowNativeAd(
                this,
                Misc.dashboardNativeAm,
                nativeAdFrameLayout
            )
        }
    }
}