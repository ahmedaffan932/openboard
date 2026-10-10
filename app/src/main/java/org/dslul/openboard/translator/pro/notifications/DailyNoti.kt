package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.App
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG

/**
 * Daily Noti (Noti.pdf §4, Figma NotiDaily): "Phrase of the day." at noti_daily_slots (default
 * 08:00, 19:00 local). Channel Daily Tips, DEFAULT importance.
 *
 * The frame is text-only - "<phrase>" in <language>. Tap to hear how it sounds. - with one HEAR IT
 * NOW button ([NotiTextCard]). The phrase comes from the bundled phrasebook ([PhraseOfTheDay]);
 * the button opens the Translate screen with it, which translates into the user's target language
 * and reads the result aloud. Written in the app's language, like the phrase it quotes.
 */
object DailyNoti {

    const val NOTI_ID = 2005
    private const val TYPE = "daily"

    fun onSlotFired(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (!NotiConfig.marketingAllowed(context)) return

        // Not in the spec, but pointless while the user is actively in the app.
        if (App.isAppOnScreen()) {
            Log.d(TAG, "DailyNoti: app in foreground, slot skipped")
            return
        }

        val pick = PhraseOfTheDay.pick(context)
        if (pick == null) {
            Log.d(TAG, "DailyNoti: no phrase for this language setup, slot skipped")
            return
        }

        val res = PhraseOfTheDay.appLanguageContext(context)
        val hearPi = NotiRouterActivity.pendingIntent(
            context, NotiTargets.PHRASE, NOTI_ID, text = pick.phrase
        )
        val notification = NotiTextCard.build(
            context,
            NotiChannels.DAILY_TIPS,
            NotificationCompat.PRIORITY_DEFAULT,
            title = res.getString(R.string.noti_daily_title),
            desc = res.getString(
                R.string.noti_daily_desc,
                pick.phrase,
                PhraseOfTheDay.languageName(context, pick.targetLanguage)
            ),
            primary = NotiTextCard.Button(res.getString(R.string.noti_daily_btn), hearPi),
            contentIntent = hearPi,
        )

        if (NotiPoster.postSafely(context, NOTI_ID, NotiChannels.DAILY_TIPS, notification)) {
            NotiAnalytics.notiShow(context, TYPE)
        }
    }
}
