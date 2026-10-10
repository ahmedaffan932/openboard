package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R

/**
 * Clicked Ads notification (Noti.pdf §2, Figma NotiClickAds): reminds the user to come back
 * after they clicked an ad and left the app. Collapsed title intentionally differs from the
 * expanded one (spec: "Before you go" vs the promo headline). Channel Updates & News, HIGH.
 * The card is [NotiPromoCard]; the copy and photo are the frame's own, opening the dashboard
 * the copy describes ("every tool sits on one screen").
 */
object ClickedAdsNoti {

    const val NOTI_ID = 2003
    private const val TYPE = "clickedads"

    fun show(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (!NotiConfig.marketingAllowed(context)) return

        val notification = NotiPromoCard.build(
            context,
            NotiChannels.UPDATES_NEWS,
            collapsedTitle = context.getString(R.string.noti_before_you_go),
            title = context.getString(R.string.noti_click_title),
            desc = context.getString(R.string.noti_click_desc),
            button = context.getString(R.string.noti_ls_card_btn_try),
            creative = NotiPromoCard.Creative(
                hero = R.drawable.noti_hero_clickads,
                heroRounded = R.drawable.noti_hero_clickads_rounded,
                thumb = R.drawable.noti_thumb_clickads,
            ),
            contentIntent = NotiRouterActivity.pendingIntent(context, NotiTargets.HOME, NOTI_ID),
        )
        if (NotiPoster.postSafely(context, NOTI_ID, NotiChannels.UPDATES_NEWS, notification)) {
            NotiAnalytics.notiShow(context, TYPE)
        }
    }
}
