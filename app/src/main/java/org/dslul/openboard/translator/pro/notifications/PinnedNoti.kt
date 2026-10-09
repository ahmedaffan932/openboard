package org.dslul.openboard.translator.pro.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * The always-on "pinned" notification (id 1234, Figma NotiPinNoti): "Translate in one tap!" over
 * four tiles - Translate, Voice, Camera, File - each opening its tool at once through
 * [QuickAccessActivity]. A plain ongoing notification, not a service's: nothing runs behind it.
 *
 * Collapsed, the row is the four tiles themselves (noti_pinned_collapsed.xml), so the tools are one
 * tap away without expanding; expanded, it is the frame (noti_pinned_expanded.xml). Texts are set
 * in the app's own language, which a @string in the layout would not follow.
 *
 * Posted by the splash on every launch after first open, by the dashboard (end of first open,
 * after the notification permission prompt) and by the Settings switch; the user's off switch
 * ([Misc.isNotificationTurnedOff]) always wins.
 */
object PinnedNoti {

    const val NOTI_ID = 1234

    /** LOW: a utility that sits in the tray, never alerts. */
    private const val CHANNEL_ID = "quick_access"

    /** Bumped when the card changes, so a card already in the tray is replaced - once. */
    private const val LAYOUT_VERSION = 1
    private const val EXTRA_LAYOUT_VERSION = "pinnedLayoutVersion"
    private const val EXTRA_LANGUAGE = "pinnedLanguage"

    private class TileViews(val tile: QuickAccessActivity.Tile, val tileId: Int, val labelId: Int, val label: Int)

    private val tiles = listOf(
        TileViews(QuickAccessActivity.Tile.TRANSLATE, R.id.pinTileTranslate, R.id.pinLabelTranslate, R.string.pin_noti_translate),
        TileViews(QuickAccessActivity.Tile.VOICE, R.id.pinTileVoice, R.id.pinLabelVoice, R.string.pin_noti_voice),
        TileViews(QuickAccessActivity.Tile.CAMERA, R.id.pinTileCamera, R.id.pinLabelCamera, R.string.pin_noti_camera),
        TileViews(QuickAccessActivity.Tile.FILE, R.id.pinTileFile, R.id.pinLabelFile, R.string.pin_noti_file),
    )

    /**
     * Posts the card unless the user switched it off or an up-to-date one is already showing.
     * A card from an older build - or in another app language - is replaced in place (same id, and
     * [NotificationCompat.Builder.setOnlyAlertOnce]), so the update never alerts. An absent card
     * (swiped away on Android 14+) comes back here, i.e. on the next launch.
     */
    fun post(context: Context) {
        if (Misc.isNotificationTurnedOff(context)) return
        val language = Misc.getAppLanguage(context)
        val showing = showingExtras(context)
        if (showing != null && isCurrent(
                showing.getInt(EXTRA_LAYOUT_VERSION, 0), showing.getString(EXTRA_LANGUAGE), language
            )
        ) return

        ensureChannel(context)
        NotiPoster.postSafely(context, NOTI_ID, CHANNEL_ID, build(context, language))
    }

    fun cancel(context: Context) = NotiPoster.cancel(context, NOTI_ID)

    fun isShowing(context: Context): Boolean = NotiPoster.isActive(context, NOTI_ID)

    /** The card in the tray is this build's, in the app's current language. */
    internal fun isCurrent(version: Int, language: String?, appLanguage: String): Boolean =
        version == LAYOUT_VERSION && language == appLanguage

    private fun showingExtras(context: Context): Bundle? = try {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.activeNotifications.firstOrNull { it.id == NOTI_ID }?.notification?.extras
    } catch (e: Exception) {
        null
    }

    /** Re-creating an existing channel only refreshes its name (e.g. after a language change). */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = PhraseOfTheDay.appLanguageContext(context).getString(R.string.pin_noti_subtext)
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            .apply {
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun build(context: Context, language: String): Notification {
        val res = PhraseOfTheDay.appLanguageContext(context)
        val title = res.getString(R.string.pin_noti_title)
        val desc = res.getString(R.string.pin_noti_desc)

        val collapsed = RemoteViews(context.packageName, R.layout.noti_pinned_collapsed)
        val expanded = RemoteViews(context.packageName, R.layout.noti_pinned_expanded)
        expanded.setTextViewText(R.id.tvNotiTitle, title)
        expanded.setTextViewText(R.id.tvNotiDesc, desc)
        for (t in tiles) {
            val label = res.getString(t.label)
            val tap = QuickAccessActivity.pendingIntent(context, t.tile)
            for (views in listOf(collapsed, expanded)) {
                views.setTextViewText(t.labelId, label)
                views.setOnClickPendingIntent(t.tileId, tap)
            }
            // The collapsed row's height is fixed below API 28, whatever the font scale.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                collapsed.setTextViewTextSize(t.labelId, TypedValue.COMPLEX_UNIT_DIP, 10f)
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_translate_24_selected)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            // Standard fallback where custom views are not shown, and what accessibility reads.
            .setContentTitle(title)
            .setContentText(desc)
            .setSubText(res.getString(R.string.pin_noti_subtext))
            // A tap outside the tiles opens the quick translator.
            .setContentIntent(QuickAccessActivity.pendingIntent(context, QuickAccessActivity.Tile.TRANSLATE))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addExtras(Bundle().apply {
                putInt(EXTRA_LAYOUT_VERSION, LAYOUT_VERSION)
                putString(EXTRA_LANGUAGE, language)
            })
            .build()
    }
}
