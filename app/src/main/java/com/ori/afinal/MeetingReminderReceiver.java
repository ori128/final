package com.ori.afinal;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class MeetingReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "MEETING_REMINDERS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("EVENT_TITLE");
        String eventId = intent.getStringExtra("EVENT_ID");

        if (title == null) title = "פגישה מתקרבת!";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // יצירת ערוץ התראות (חובה מאנדרואיד 8 ומעלה)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "תזכורות לפגישות",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("התראות עבור פגישות שמתחילות בקרוב");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // כשהמשתמש לוחץ על ההתראה, נפתח את האפליקציה (מסך הפתיחה)
        Intent openIntent = new Intent(context, Splash.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                eventId != null ? eventId.hashCode() : 0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo) // משתמש בלוגו של האפליקציה שלך
                .setContentTitle("תזכורת לפגישה: " + title)
                .setContentText("הפגישה שלך מתחילה בעוד 15 דקות!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis(), builder.build());
        }
    }
}