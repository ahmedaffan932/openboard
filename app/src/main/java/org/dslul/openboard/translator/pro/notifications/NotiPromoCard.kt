package org.dslul.openboard.translator.pro.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import org.dslul.openboard.inputmethod.latin.R

/**
 * The card shared by the Win-Back, Kill-App and Clicked-Ads notifications (Figma NotiWinback /
 * NotiKillApp / NotiClickAds): collapsed row with a thumbnail, expanded card with a photo and the
 * gradient "Try now". All are HIGH on Updates & News, so the collapsed row is also the heads-up.
 *
 * Everything goes in as text or resource ids - no bitmap enters the notification parcel.
 */
object NotiPromoCard {

    class Creative(
        @DrawableRes val hero: Int,
        /** [hero] with its corners already cut, for API 24-30 where RemoteViews cannot clip. */
        @DrawableRes val heroRounded: Int,
        @DrawableRes val thumb: Int,
    )

    fun build(
        context: Context,
        channelId: String,
        collapsedTitle: String,
        title: String,
        desc: String,
        button: String,
        creative: Creative,
        contentIntent: PendingIntent,
    ): Notification {
        val collapsed = RemoteViews(context.packageName, R.layout.noti_promo_collapsed).apply {
            setTextViewText(R.id.tvNotiTitle, collapsedTitle)
            setTextViewText(R.id.tvNotiDesc, desc)
            setImageViewResource(R.id.ivNotiThumb, creative.thumb)
            pinCollapsedTextSizes(R.id.tvNotiTitle, R.id.tvNotiDesc)
        }
        val hero = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) creative.hero else creative.heroRounded
        val expanded = RemoteViews(context.packageName, R.layout.noti_promo_expanded).apply {
            setTextViewText(R.id.tvNotiTitle, title)
            setTextViewText(R.id.tvNotiDesc, desc)
            setImageViewResource(R.id.ivNotiHero, hero)
            setTextViewText(R.id.btnNotiCta, button)
            setOnClickPendingIntent(R.id.btnNotiCta, contentIntent)
        }
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_baseline_translate_24_selected)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            // Standard-template fallback (accessibility, wearables, stripped custom views).
            .setContentTitle(collapsedTitle)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }
}

/**
 * Collapsed custom rows have a fixed ~38dp on API 24-27 that does not grow with the font scale,
 * so a 14sp title over a 13sp line - 36dp at 1.0 - is clipped from about 1.1x. There the two
 * lines are pinned to the same sizes in dp; from API 28 the budget scales and sp is kept.
 */
internal fun RemoteViews.pinCollapsedTextSizes(titleId: Int, descId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return
    setTextViewTextSize(titleId, TypedValue.COMPLEX_UNIT_DIP, 14f)
    setTextViewTextSize(descId, TypedValue.COMPLEX_UNIT_DIP, 13f)
}
