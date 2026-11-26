package org.dslul.openboard.translator.pro.learn

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.PremiumScreenActivity
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * Learn and Phrase Practice are Pro features, locked like the dashboard's Camera and
 * Conversation tabs: Pro opens them for good; a free user can watch the unlock-premium rewarded
 * ad to open them until the app is closed. When Remote Config turns the Pro screen off
 * (`Misc.isProScreenEnabled`), they are open to everyone, as the tabs are.
 */
object LearnAccess {
    const val FEATURE_LEARN = "learn"
    const val FEATURE_PHRASES = "phrase_practice"

    /** Unlocked by a rewarded ad in this app process (cleared when the app is closed). */
    @Volatile
    var unlockedByReward = false
        private set

    fun canOpen(activity: Activity): Boolean =
        !Misc.isProScreenEnabled || Misc.getPurchasedStatus(activity) || unlockedByReward

    /**
     * Runs [open] straight away when the user may use Learn; otherwise shows the Pro lock.
     * [open] gets `afterAd = true` when a rewarded ad just unlocked it (so callers skip their own
     * interstitial). [onCancel] runs when the user closes the lock without unlocking.
     */
    fun guard(activity: Activity, feature: String, onCancel: () -> Unit = {}, open: (afterAd: Boolean) -> Unit) {
        if (canOpen(activity)) {
            open(false)
            return
        }
        showLock(activity, feature, onCancel, open)
    }

    /** The dashboard's lock card (camera_translate_pro_dialog): Upgrade, or Watch ad. */
    fun showLock(activity: Activity, feature: String, onCancel: () -> Unit, open: (afterAd: Boolean) -> Unit): Dialog {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.camera_translate_pro_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener { onCancel() }
        dialog.findViewById<TextView>(R.id.tvDescription)?.text =
            activity.getString(R.string.unlock_premium_feature_description)
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.cancel() }
        dialog.findViewById<TextView>(R.id.btnUpgrade)?.setOnClickListener {
            dialog.dismiss()
            AppAnalytics.featureLockAction(feature, "upgrade")
            activity.startActivity(
                Intent(activity, PremiumScreenActivity::class.java)
                    .putExtra(AppAnalytics.EXTRA_PREMIUM_SOURCE, AppAnalytics.PREMIUM_SOURCE_FEATURE_LOCK)
            )
        }
        dialog.findViewById<TextView>(R.id.btnWatchAd)?.setOnClickListener {
            dialog.dismiss()
            AppAnalytics.featureLockAction(feature, "watch_ad")
            LearnAds.rewardedUnlock(activity) { method ->
                unlockedByReward = true
                AppAnalytics.featureUnlock(feature, method)
                if (!activity.isFinishing && !activity.isDestroyed) open(true)
            }
        }
        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
            AppAnalytics.featureLockView(feature)
        }
        return dialog
    }
}
