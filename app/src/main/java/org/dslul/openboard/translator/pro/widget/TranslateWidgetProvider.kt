package org.dslul.openboard.translator.pro.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.notifications.NotiRouterActivity
import org.dslul.openboard.translator.pro.notifications.NotiTargets

/**
 * Home-screen quick-translate widget: pill search bar + mic + camera.
 *
 * Declaring this provider is also what puts the "Widgets" entry in the app-icon long-press menu.
 * Static content (updatePeriodMillis=0). Every tile routes through [NotiRouterActivity] and from
 * there through the splash, so a tap runs the same splash -> ad -> feature-screen flow a
 * notification tap does.
 */
class TranslateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    private fun buildViews(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_translate).apply {
            setOnClickPendingIntent(
                R.id.widgetRoot,
                NotiRouterActivity.widgetPendingIntent(context, NotiTargets.TRANSLATE, 0)
            )
            setOnClickPendingIntent(
                R.id.widgetHint,
                NotiRouterActivity.widgetPendingIntent(context, NotiTargets.TRANSLATE, 0)
            )
            setOnClickPendingIntent(
                R.id.widgetMic,
                NotiRouterActivity.widgetPendingIntent(context, NotiTargets.CONVERSATION, 1)
            )
            setOnClickPendingIntent(
                R.id.widgetCamera,
                NotiRouterActivity.widgetPendingIntent(context, NotiTargets.CAMERA, 2)
            )
        }
}
