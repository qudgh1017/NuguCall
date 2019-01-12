package edu.skku.monet.nugucall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.LinearLayout;

public class BackgroundService extends Service {

    private Handler handler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handler = new Handler();

        setNotification();
        setWindowLayout();

        return super.onStartCommand(intent, flags, startId);
    }

    public void setNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(Global.NOTIFICATION_CHANNEL_ID, Global.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), Global.REQ_CODE_NOTIFICATION_INTENT, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Global.NOTIFICATION_CHANNEL_ID);
        Notification notification = builder
                .setContentTitle("NuguCall")
                .setContentText("NuguCall이 실행 중입니다.")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(Global.NOTIFICATION_ID, notification);
    }

    public void setWindowLayout() {

    }

    private class CallScreenLayout extends LinearLayout {
        public CallScreenLayout(Context context) {
            super(context);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}