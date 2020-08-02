package com.chatapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import im.vector.Matrix;
import im.vector.R;

public class NoitficationUtils {
    public static final String CHANNEL_ID = "channel123";
    public static final String CHANNEL_NAME = "my notification";
    public static final String CHANNEL_DESCRIPTION = "Test";


    public static void showNotification(Context context) {
        if (Matrix.getInstance(context).getDefaultSession() == null) {
            return;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setSound(null, null);
                channel.setDescription(CHANNEL_DESCRIPTION);
                NotificationManager manager = context.getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    1012, new Intent(context, SplashActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.icon_notif_important)
                            .setContentTitle(context.getString(R.string.app_name))
                            .setContentIntent(pendingIntent)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                            .setContentText("")
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(false)
                            .setSound(null)
                            .setProgress(0, 1000000000, false)
                            .setOngoing(true).setPriority(2);

            NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
            mNotificationManager.notify(1, mBuilder.build());
        }
    }

    public static void cancelNotification(Context context) {
        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();
    }
}
