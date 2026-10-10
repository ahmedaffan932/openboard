package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Session flag for the Kill App / Clicked Ads template (Noti.pdf §2): set from every
 * ad-click callback, consumed when the app loses the foreground — "fire the notification
 * whenever the user clicks on any ad inside the app and exits the app as a result".
 */
object AdClickTracker {

    /**
     * How long the app has to stay out of the foreground before the departure counts. App calls
     * [onAppPaused] on the process ON_PAUSE, which also fires for an overlay the user can dismiss
     * in a second (Play's in-app sheet), and this notification is a HIGH-importance heads-up.
     */
    private const val LEFT_APP_CONFIRM_MS = 3_000L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingPost: Runnable? = null

    @Volatile
    var adClickedThisSession = false

    /** Called from every ad's onAdClicked (banner, collapsible, native, interstitial, rewarded, app-open). */
    fun notifyClick() {
        adClickedThisSession = true
    }

    /**
     * The app lost the foreground: raise [ClickedAdsNoti] if the user is still away
     * [LEFT_APP_CONFIRM_MS] later. Returning first only withdraws the post — the flag stays armed,
     * so the departure that does stick still gets its notification.
     */
    fun onAppPaused(context: Context) {
        if (!NotiConfig.clickedAdsEnabled || !adClickedThisSession) return
        cancelPendingPost()
        val appContext = context.applicationContext
        val post = Runnable {
            pendingPost = null
            // [onAppResumed] is what normally withdraws this; the state read is the backstop for
            // a resume that beats its own ON_RESUME dispatch to the handler.
            val state = ProcessLifecycleOwner.get().lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.RESUMED)) return@Runnable
            adClickedThisSession = false
            ClickedAdsNoti.show(appContext)
        }
        pendingPost = post
        handler.postDelayed(post, LEFT_APP_CONFIRM_MS)
    }

    /** The user came back before the departure was confirmed. */
    fun onAppResumed() = cancelPendingPost()

    private fun cancelPendingPost() {
        pendingPost?.let { handler.removeCallbacks(it) }
        pendingPost = null
    }
}
