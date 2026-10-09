package org.dslul.openboard.translator.pro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityUninstallSurveyBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemUninstallFeatureBinding
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.Ads

/**
 * Landing screen for the "Uninstall" long-press shortcut.
 *
 * Flow: tap the "Uninstall" shortcut -> PreSplashScreenActivity (its app-open ad is the spec's
 * "splash ad") -> this screen: a retention survey, a reminder of the features the user would lose,
 * and a shared-bottom native ad. The shortcut is labelled "Uninstall" but opens the app (a
 * client-spec pattern flagged for Play-policy risk in NOTIFICATIONS.md). To keep a genuine exit,
 * [confirmUninstall] fires the real system uninstaller via ACTION_DELETE rather than trapping the
 * user here.
 *
 * Excluded from the resume app-open ad (App.onMoveToForeground), so returning from the system
 * uninstaller does not pop an ad over this screen.
 */
class UninstallSurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUninstallSurveyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityUninstallSurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            // Never leave the user on a dead-end shortcut screen.
            goToDashboard()
        }

        logEvent("uninstall_survey_view")

        bindFeature(binding.featureCamera, R.drawable.ic_baseline_camera_alt_24, R.string.uninstall_feature_camera)
        bindFeature(binding.featureVoice, R.drawable.ic_conversation, R.string.uninstall_feature_voice)
        // openboard has no AI chatbot; the phrasebook stands in for xtech's chatbot feature.
        bindFeature(binding.featureChatbot, R.drawable.ic_baseline_menu_book_24, R.string.phrasebook)
        bindFeature(binding.featureOffline, R.drawable.ic_enable_offline, R.string.uninstall_feature_offline)

        Ads.loadAndShowSharedBottomNativeAd(this, binding.nativeAdFrameLayout)

        binding.btnKeep.setOnClickListener {
            logResult("kept")
            goToDashboard()
        }

        binding.btnUninstall.setOnClickListener {
            logResult("uninstall")
            confirmUninstall()
        }
    }

    private fun bindFeature(item: ItemUninstallFeatureBinding, iconRes: Int, labelRes: Int) {
        item.imgFeature.setImageResource(iconRes)
        item.tvFeature.setText(labelRes)
    }

    private fun selectedReason(): String = when (binding.rgReasons.checkedRadioButtonId) {
        R.id.reasonAds -> "ads"
        R.id.reasonBetter -> "better_app"
        R.id.reasonConfusing -> "confusing"
        R.id.reasonQuality -> "quality"
        R.id.reasonOther -> "other"
        else -> "none"
    }

    /** Hands off to the system uninstaller so a user who really wants to leave still can. */
    private fun confirmUninstall() {
        try {
            startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            e.printStackTrace()
            goToDashboard()
        }
    }

    private fun goToDashboard() {
        startActivity(
            Intent(this, FragmentsDashboardActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun logResult(status: String) {
        val params = Bundle().apply {
            putString("status", status)
            putString("reason", selectedReason())
        }
        Firebase.analytics.logEvent("uninstall_survey_result", params)
    }

    private fun logEvent(event: String) {
        Firebase.analytics.logEvent(event, Bundle())
    }
}
