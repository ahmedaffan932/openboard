package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.App
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG

/**
 * Win-Back notification (Noti.pdf §1, Figma NotiWinback): recurring notification sent while the
 * user is inactive, at slots noti_winback_slots (default 09:00, 11:00, 18:00 local), re-sent once
 * 30 minutes after each post when the app was not opened in between. Channel
 * Updates & News, HIGH.
 *
 * The card is [NotiPromoCard] with the frame's own copy and travel photo ("Travelling soon?"),
 * opening the dashboard its copy lists.
 */
object WinbackNoti {

    const val NOTI_ID = 2002
    private const val TYPE = "winback"

    fun onSlotFired(context: Context) = send(context, followUp = false)

    fun onFollowUpFired(context: Context) {
        val opensAtPost = NotiPrefs.getWinbackOpensAtPost(context)
        NotiPrefs.setWinbackOpensAtPost(context, -1L)
        if (opensAtPost < 0 || NotiPrefs.appOpenCount(context) != opensAtPost) {
            Log.d(TAG, "WinbackNoti: app opened since the post, follow-up skipped")
            return
        }
        send(context, followUp = true)
    }

    /** Must run on every process foreground: it is what stops the pending follow-up. */
    fun onAppOpened(context: Context) {
        NotiPrefs.recordAppOpen(context)
        NotiScheduler.cancelWinbackFollowUp(context)
    }

    private fun send(context: Context, followUp: Boolean) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (!NotiConfig.marketingAllowed(context)) return

        // "Sent while the user is inactive" — never fire on top of an active session.
        if (App.isAppOnScreen()) {
            Log.d(TAG, "WinbackNoti: app in foreground, slot skipped")
            return
        }

        val opensAtSend = NotiPrefs.appOpenCount(context)
        val title = context.getString(R.string.noti_winback_title)
        val notification = NotiPromoCard.build(
            context,
            NotiChannels.UPDATES_NEWS,
            collapsedTitle = title,
            title = title,
            desc = context.getString(R.string.noti_winback_desc),
            button = context.getString(R.string.noti_ls_card_btn_try),
            creative = NotiPromoCard.Creative(
                hero = R.drawable.noti_hero_winback,
                heroRounded = R.drawable.noti_hero_winback_rounded,
                thumb = R.drawable.noti_thumb_winback,
            ),
            contentIntent = NotiRouterActivity.pendingIntent(context, NotiTargets.HOME, NOTI_ID),
        )

        if (NotiPoster.postSafely(context, NOTI_ID, NotiChannels.UPDATES_NEWS, notification)) {
            NotiAnalytics.notiShow(context, TYPE)
            if (!followUp) {
                NotiPrefs.setWinbackOpensAtPost(context, opensAtSend)
                NotiScheduler.scheduleWinbackFollowUp(context)
            }
        }
    }
}
