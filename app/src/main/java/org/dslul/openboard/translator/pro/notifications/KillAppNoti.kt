package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG

/**
 * Kill-App notification (Figma NotiKillApp; the "kill apps" half of Noti.pdf §2): posted when
 * the user swipes the app away from Recents, pointing them back at the camera translator.
 * Detected by [KillAppWatcherService.onTaskRemoved]. Channel Updates & News, HIGH.
 *
 * Throttled to one per [NotiConfig.killAppCooldownHours] - people clear Recents far more often
 * than once a day - and skipped while the Clicked-Ads notification is already asking the user to
 * come back, or before first open has finished.
 */
object KillAppNoti {

    const val NOTI_ID = 2007
    private const val TYPE = "killapp"

    fun onAppKilled(context: Context) {
        if (!NotiConfig.killAppEnabled || Misc.isFirstTime(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (!NotiConfig.marketingAllowed(context)) return
        if (NotiPoster.isActive(context, ClickedAdsNoti.NOTI_ID)) {
            Log.d(TAG, "KillAppNoti: clicked-ads noti already showing, skipped")
            return
        }
        val cooldownMs = NotiConfig.killAppCooldownHours.coerceAtLeast(0) * 60L * 60 * 1000
        if (!NotiPrefs.shouldPostKillApp(context, cooldownMs)) return
        // The process can be killed the moment onTaskRemoved returns, which can beat Firebase's
        // own threads to disk - so the show is written down synchronously here and reported by
        // the next process (App.onCreate -> reportPendingShow).
        if (post(context, track = false)) NotiPrefs.markKillAppShowPending(context)
    }

    /** Called from App.onCreate: reports a show a killed process could not. */
    fun reportPendingShow(context: Context) {
        if (NotiPrefs.consumeKillAppShowPending(context)) NotiAnalytics.notiShow(context, TYPE)
    }

    /**
     * Posts unconditionally; [onAppKilled] holds the gates. Also the debug receiver's entry,
     * which runs in a live process and can track right away.
     */
    fun post(context: Context, track: Boolean = true): Boolean {
        val notification = NotiPromoCard.build(
            context,
            NotiChannels.UPDATES_NEWS,
            collapsedTitle = context.getString(R.string.noti_kill_title),
            title = context.getString(R.string.noti_kill_title),
            desc = context.getString(R.string.noti_kill_desc),
            button = context.getString(R.string.noti_ls_card_btn_try),
            creative = NotiPromoCard.Creative(
                hero = R.drawable.noti_hero_killapp,
                heroRounded = R.drawable.noti_hero_killapp_rounded,
                thumb = R.drawable.noti_thumb_killapp,
            ),
            contentIntent = NotiRouterActivity.pendingIntent(context, NotiTargets.CAMERA, NOTI_ID),
        )
        val posted = NotiPoster.postSafely(context, NOTI_ID, NotiChannels.UPDATES_NEWS, notification)
        if (posted && track) NotiAnalytics.notiShow(context, TYPE)
        return posted
    }
}
