package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import org.dslul.openboard.translator.pro.notifications.receivers.NotiDismissReceiver

/**
 * Lock-screen notification engine (260817 - MO - Lockscreen Noti Logic Spec, v1.0).
 *
 * Per slot:  permission gate → premium gate → active-check → skip (Default) / replace
 * (Variant A) → pick content (never same twice in a row) → post → wake screen 20s → arm one
 * re-wake. The notification persists until the user taps "X" or opens the app; the skip
 * decision keys off activeNotifications, so it does NOT reset at midnight.
 *
 * Presentation follows Figma "Lockscreen 1-3", fitted into the notification's own custom views.
 * There is deliberately no full-screen intent: USE_FULL_SCREEN_INTENT would require the Play
 * Console full-screen-intent declaration, which this app does not qualify for (not a calling or
 * alarm app). The notification is public on the lock screen and the screen is woken for it.
 */
object LockscreenNotiEngine {

    const val NOTI_ID = 2001
    private const val TYPE = "lockscreen"

    fun onSlotFired(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Lockscreen noti: notifications disabled, slot ignored")
            return
        }
        if (!NotiConfig.marketingAllowed(context)) {
            Log.d(TAG, "Lockscreen noti: premium user, slot ignored")
            return
        }

        val stillShowing = NotiPoster.isActive(context, NOTI_ID)
        if (stillShowing && !NotiConfig.lockscreenReplace) {
            // Default variant: previous notification not dismissed → skip this slot.
            // No midnight reset — skipping continues into following days.
            Log.d(TAG, "Lockscreen noti: previous still showing, slot skipped")
            NotiAnalytics.notiSkip(context, TYPE, NotiAnalytics.REASON_STILL_SHOWING)
            return
        }

        val contentList = NotiContentStore.lockscreenContent(context)
        if (contentList.isEmpty()) return
        val content = NotiContentStore.pickRandomExcluding(
            contentList,
            NotiPrefs.getLastContentId(context, TYPE)
        )

        // Variant A replaces a live notification in place (same id).
        if (!post(context, content)) return

        NotiPrefs.setLastContentId(context, TYPE, content.id)
        val postStamp = System.currentTimeMillis()
        NotiPrefs.setLockscreenPostStamp(context, postStamp)
        NotiAnalytics.notiShow(context, TYPE, replaced = stillShowing)

        // Wake only when the notification is really showing: a channel block or OEM drop must
        // never cause a 20s phantom screen wake.
        if (NotiPoster.isActive(context, NOTI_ID)) {
            ScreenWakeController.wake(context, NotiConfig.lockscreenWakeSeconds)
            ScreenWakeController.armOneRewake(context, NOTI_ID, postStamp)
        }
    }

    private fun post(context: Context, content: LockscreenContent): Boolean {
        val dismissPi = NotiDismissReceiver.pendingIntent(context, NOTI_ID)
        val contentPi = NotiRouterActivity.pendingIntent(context, content.target, NOTI_ID)
        val descVisibility = if (content.desc.isBlank()) View.GONE else View.VISIBLE

        // Everything here is a resource id or text: no bitmap enters the notification parcel.
        // The expanded logo is fixed in the layout; thumb and hero are picked per target.
        val collapsed = RemoteViews(context.packageName, R.layout.noti_lockscreen_collapsed).apply {
            setTextViewText(R.id.tvNotiTitle, content.title)
            setTextViewText(R.id.tvNotiDesc, content.desc)
            setViewVisibility(R.id.tvNotiDesc, descVisibility)
            setImageViewResource(R.id.ivNotiThumb, LockscreenArt.notificationThumb(content.target))
            setOnClickPendingIntent(R.id.btnNotiClose, dismissPi)
            pinCollapsedTextSizes(R.id.tvNotiTitle, R.id.tvNotiDesc)
        }
        val expanded = RemoteViews(context.packageName, R.layout.noti_lockscreen_expanded).apply {
            setTextViewText(R.id.tvNotiTitle, content.title)
            setTextViewText(R.id.tvNotiDesc, content.desc)
            setViewVisibility(R.id.tvNotiDesc, descVisibility)
            setTextViewText(R.id.btnNotiCta, content.button)
            setOnClickPendingIntent(R.id.btnNotiClose, dismissPi)
            setOnClickPendingIntent(R.id.btnNotiCta, contentPi)
            setImageViewResource(R.id.ivNotiHero, LockscreenArt.notificationHero(content.target))
        }

        val notification = NotificationCompat.Builder(context, NotiChannels.LOCKSCREEN)
            .setSmallIcon(R.drawable.ic_baseline_translate_24_selected)
            // Deliberately NO setLargeIcon here: DecoratedCustomViewStyle keeps the
            // standard chrome, which renders its own large-icon slot next to the card's logo.
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setContentTitle(content.title) // accessibility / wearable fallback
            .setContentText(content.desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPi)
            // Swipe dismissal keeps system default behavior (spec edge case); the
            // delete intent just cancels state + records the dismissal.
            .setDeleteIntent(dismissPi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()

        return NotiPoster.postSafely(context, NOTI_ID, NotiChannels.LOCKSCREEN, notification)
    }
}
