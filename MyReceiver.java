package com.samsungphone.knox;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MyReceiver extends BroadcastReceiver {
    //public static final String ACTION_START_ALARM = "ACTION_START_ALARM";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {

        //setNote(context);
        //setPause(10);
        switch (intent.getAction()) {
            /*case ACTION_START_ALARM:
                setAlarm (context, intent);
                context.startForegroundService(new Intent(context, MyIntentService.class));
                break;*/
            case ACTION_BOOT_COMPLETED:
                Log.d("onReceive", "ACTION_BOOT_COMPLETED");
                context.startForegroundService(new Intent(context, MyIntentService.class));
                break;
            default:
                throw new IllegalArgumentException("Unknown action.");
        }
    }

    private void setAlarm (Context context, Intent intent){
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + (60*1000), pendingIntent);
    }
    // не работает
    private void setNote (Context context){
        //Создание канала для вывода уведомлений
        String NOTIFICATION_CHANNEL_ID = "com.samsungphone.appforsendarchive.br";
        String channelName = "My Background Broadcast";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        // Создание уведомления
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_service)
                .setContentTitle("Broadcast Broadcast Broadcast")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        manager.notify(1, notification);
    }
    private void setPause (int sec){
        long endTime = System.currentTimeMillis() + sec*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
    }
}

