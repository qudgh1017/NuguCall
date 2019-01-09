package edu.skku.monet.nugucall;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

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
        Notification.Builder builder = new Notification.Builder(getApplicationContext());

    }

    public void setWindowLayout() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}