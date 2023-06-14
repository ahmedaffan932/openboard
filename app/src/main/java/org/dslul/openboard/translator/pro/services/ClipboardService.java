package org.dslul.openboard.translator.pro.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.translator.pro.SplashScreenActivity;
import org.dslul.openboard.translator.pro.classes.Misc;

public class ClipboardService extends Service implements ClipboardManager.OnPrimaryClipChangedListener  {
    @SuppressLint("StaticFieldLeak")
    public static Service doorservice;
    String CHANNEL_ID = "ClipBoardListenerService";
    public PendingIntent pendingIntent;
    ClipboardManager clipboardManager;

    @SuppressWarnings("deprecation")
    public void onCreate() {

        super.onCreate();
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SplashScreenActivity.class);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("We are here to help you to translate anything quickly.")
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setColorized(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(111, notification);
        }

        doorservice = this;

        Log.e("Service", "started");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrimaryClipChanged() {
        Log.d(Misc.logKey, "changed to: " + clipboardManager.getText());

        ClipData clipData = clipboardManager.getPrimaryClip();

        // Do whatever you want with the new contents of the clipboard.
        // For example, you could log the contents to the logcat.
        Log.d(Misc.logKey, "Clipboard contents changed: " + clipData.getItemAt(0).getText());
            
    }


}