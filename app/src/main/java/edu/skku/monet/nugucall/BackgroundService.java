package edu.skku.monet.nugucall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class BackgroundService extends Service {

    private SharedPreferences sharedPreferences;

    private TelephonyManager telephonyManager;

    private boolean isIncomingCall = false;

    private WindowManager windowManager;
    private CallScreenLayout callScreenLayout;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sharedPreferences = getSharedPreferences(Global.SHARED_PREFERENCES, MODE_PRIVATE);

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        setNotification();
        setWindowLayout();
        setBroadcastReceiver();

        return super.onStartCommand(intent, flags, startId);
    }

    public void setNotification() {
        if (Build.VERSION.SDK_INT >= 26) { // 안드로이드 8.0 이상에서는 알림 채널 생성이 필수
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
        startForeground(Global.NOTIFICATION_ID, notification); // 포그라운드 서비스로 실행
    }

    public void setWindowLayout() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        callScreenLayout = new CallScreenLayout(getApplicationContext());
        WindowManager.LayoutParams callScreenLayoutParams;
        if (Build.VERSION.SDK_INT >= 26) {
            callScreenLayoutParams = new WindowManager.LayoutParams(
                    // 가로 세로 크기는 SplashActivity에서 구해서 저장해둔 것을 불러옴
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_WIDTH, 0),
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_HEIGHT, 0),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 안드로이드 8.0 이상
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            callScreenLayoutParams = new WindowManager.LayoutParams(
                    // 가로 세로 크기는 SplashActivity에서 구해서 저장해둔 것을 불러옴
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_WIDTH, 0),
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_HEIGHT, 0),
                    WindowManager.LayoutParams.TYPE_PHONE, // 안드로이드 8.0 미만
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
        }
        callScreenLayoutParams.gravity = Gravity.CENTER;
        callScreenLayoutParams.windowAnimations = android.R.style.Animation_Toast;
    }

    private class CallScreenLayout extends LinearLayout {

        private TextView tv_name; // 이름
        private TextView tv_phone; // 전화번호
        private ImageView iv_source; // 이미지 컨텐츠
        private VideoView vv_source; // 동영상 컨텐츠
        private TextView tv_text; // 문구

        public CallScreenLayout(Context context) {
            super(context);
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
                layoutInflater.inflate(R.layout.service_background, this, true);
            }
            tv_name = findViewById(R.id.tv_name);
            tv_phone = findViewById(R.id.tv_phone);
            iv_source = findViewById(R.id.iv_source);
            vv_source = findViewById(R.id.vv_source);
            tv_text = findViewById(R.id.tv_text);
        }
    }

    public void setBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String phoneNumber;

            // 전화 발신 (안드로이드 버전 8.0 미만) 확인
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                isIncomingCall = false;
                phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
                Log.i(Global.TAG, "phoneNumber : " + phoneNumber);
                insertRecords(phoneNumber);
            }

            // 전화 발신 (안드로이드 버전 8.0 이상) & 수신 확인
            switch (telephonyManager.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING: // 전화 수신
                    isIncomingCall = true;
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
                    Log.i(Global.TAG, "phoneNumber : " + phoneNumber);
                    selectRecords(phoneNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isIncomingCall) { // 전화 수신 및 통화 시작

                    } else { // 전화 발신
                        phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
                        Log.i(Global.TAG, "phoneNumber : " + phoneNumber);
                        insertRecords(phoneNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (isIncomingCall) { // 전화 수신 및 통화 종료

                    } else { // 전화 발신 및 통화 종료

                    }
                    break;
            }
        }
    };

    public void insertRecords(String phoneNumber) {
        // TODO: 발신했을 경우 발신 기록을 DB에 삽입
    }

    public void selectRecords(String phoneNumber) {
        // TODO: 수신했을 경우 발신 기록을 DB에서 조회
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}