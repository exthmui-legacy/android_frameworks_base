package com.android.packageinstaller.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;

import java.io.File;

public class Utils {

    public static Notification buildNotification(Context context, PendingIntent pendingIntent, String channelId, String title, String message, int icon, CharSequence ticker) {
        Notification.Builder mBuilder = new Notification.Builder(context, channelId);

        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setTicker(ticker)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(icon)
                .setAutoCancel(true);

        return mBuilder.build();
    }

    public static void createNotificationChannel(Context context, String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static String getFileSize(Context context, String filePath){
        return Formatter.formatFileSize(context, new File(filePath).length());
    }

}

