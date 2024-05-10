package org.dslul.openboard.translator.pro.fcm.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.picasso.Picasso
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.fcm.utils.DB
import java.util.concurrent.atomic.AtomicInteger


@SuppressLint("MissingFirebaseInstanceTokenRefresh")
open class CustomMessagingService : FirebaseMessagingService() {

    companion object {

        const val ICON_KEY = "icon"
        const val APP_TITLE_KEY = "title"
        const val SHORT_DESC_KEY = "short_desc"
        const val LONG_DESC_KEY = "long_desc"
        const val APP_FEATURE_KEY = "feature"
        const val APP_URL_KEY = "app_url"
        const val IS_PREMIUM = "is_premium"
        private val seed = AtomicInteger()

        fun getNextInt(): Int {
            return seed.incrementAndGet()
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("CUSTOM", "NOTIFICATION")
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val iconURL = data[ICON_KEY]
            val title = data[APP_TITLE_KEY]
            val shortDesc = data[SHORT_DESC_KEY]
            val longDesc = data[LONG_DESC_KEY]
            val feature = data[APP_FEATURE_KEY]
            val appURL = data[APP_URL_KEY]
            val notificationID = getNextInt()

            if (iconURL != null && title != null && shortDesc != null && feature != null && appURL != null) {
                val standard = "https://play.google.com/store/apps/details?id="

                try {
                    val id = appURL.substring(standard.length)
                    if (BuildConfig.DEBUG) Log.e("package sent ", id)

                    if (!isAppInstalled(id, this) && !DB.getInstance(this).getBoolean(IS_PREMIUM))
                        Handler(this.mainLooper).post {
                            customNotification(
                                iconURL,
                                title, shortDesc, longDesc,
                                feature, appURL, notificationID
                            )
                        }

                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e("FcmFireBase", "package not valid")
                }
            }
        }

        if (BuildConfig.DEBUG) Log.e("From: ", remoteMessage.from.toString())
        if (remoteMessage.notification != null) {
            if (BuildConfig.DEBUG) Log.e("Message  Body:", remoteMessage.notification!!.body.toString())
        }

    }

    //custom view
    private fun customNotification(
        iconURL: String, title: String, shortDesc: String, longDesc: String?,
        feature: String?, appURL: String, notificationID: Int
    ) {

        Log.e("CUSTOM", "NOTIFICATION CALLED")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appURL))
        val pendingIntent =
            PendingIntent.getActivity(this, 0 /* request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val remoteViews = RemoteViews(packageName, R.layout.custom_notification_layout)

        remoteViews.setTextViewText(R.id.tv_title, title)
        remoteViews.setTextViewText(R.id.tv_short_desc, shortDesc)
        remoteViews.setTextViewText(R.id.tv_long_desc, longDesc)
        remoteViews.setViewVisibility(
            R.id.tv_long_desc,
            if (longDesc != null && longDesc.isNotEmpty()) View.VISIBLE else View.GONE
        )

        val builder = NotificationCompat.Builder(this, title)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                title,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mNotificationManager.createNotificationChannel(channel)
        }

        mNotificationManager.notify(notificationID, builder.build())

        Picasso.get()
            .load(iconURL)
            .into(remoteViews, R.id.iv_icon, notificationID, builder.build())
        Picasso.get()
            .load(feature)
            .into(remoteViews, R.id.iv_feature, notificationID, builder.build())


    }


    private fun isAppInstalled(uri: String, context: Context): Boolean {
        val pm = context.packageManager
        return try {
            val applicationInfo = pm.getApplicationInfo(uri, 0)
            applicationInfo.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    }
}