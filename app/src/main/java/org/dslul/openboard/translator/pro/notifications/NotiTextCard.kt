package org.dslul.openboard.translator.pro.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import org.dslul.openboard.inputmethod.latin.R

/**
 * The text-only card of the Shortcut (Figma NotiShortcut) and Daily (Figma NotiDaily)
 * notifications: the standard collapsed row, and an expanded view with a ruled row of blue caps
 * buttons (noti_text_card_expanded.xml). With no [secondary] button the primary one spans the row.
 */
object NotiTextCard {

    class Button(val label: String, val intent: PendingIntent)

    fun build(
        context: Context,
        channelId: String,
        priority: Int,
        title: String,
        desc: String,
        primary: Button,
        secondary: Button? = null,
        contentIntent: PendingIntent,
    ): Notification {
        val expanded = RemoteViews(context.packageName, R.layout.noti_text_card_expanded).apply {
            setTextViewText(R.id.tvNotiTitle, title)
            setTextViewText(R.id.tvNotiDesc, desc)
            setTextViewText(R.id.btnNotiPrimary, primary.label)
            setOnClickPendingIntent(R.id.btnNotiPrimary, primary.intent)
            if (secondary != null) {
                setTextViewText(R.id.btnNotiSecondary, secondary.label)
                setOnClickPendingIntent(R.id.btnNotiSecondary, secondary.intent)
            } else {
                setViewVisibility(R.id.btnNotiSecondary, View.GONE)
                setViewVisibility(R.id.vNotiActionDivider, View.GONE)
            }
        }
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_baseline_translate_24_selected)
            // No custom collapsed view: DecoratedCustomViewStyle then keeps the standard one.
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(expanded)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(priority)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }
}
