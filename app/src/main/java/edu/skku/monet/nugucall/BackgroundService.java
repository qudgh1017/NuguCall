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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Time;
import java.util.concurrent.ExecutionException;

public class BackgroundService extends Service {

    private SharedPreferences sharedPreferences;

    private TelephonyManager telephonyManager;

    private boolean isIncomingCall = false;

    private WindowManager windowManager;
    private CallScreenLayout callScreenLayout;

    // getPhonestate() 함수 쓰려고
    ContentsActivity contentsActivity = new ContentsActivity();

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
        Log.i(Global.TAG, "insertRecords() invoked.");

        try {
            String address = "insert_my_records"; // 통신할 JSP 주소

            contentsActivity.getPhoneState();
            long time = System.currentTimeMillis();

            JSONObject parameter = new JSONObject();
            parameter.put("sender", contentsActivity.getUserPhoneNumber());
            parameter.put("receiver", phoneNumber);
            parameter.put("imei", contentsActivity.getUserIMEI());
            parameter.put("time", time);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try{
                        if(out!=null){ // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch(result){
                                case "1": // JSP - DB 통신 성공
                                    Log.i(Global.TAG, "insert_my_records() : 발신기록을 DB에 삽입하였습니다.");
                                    break;
                                case "0": // JSP - DB 통신 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }else { // 안드로이드 - JSP 통신 오류 발생
                            Toast.makeText(getApplicationContext(), "JSP Error Occured.", Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            communicateDB.execute();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void selectRecords(String phoneNumber) {
        // TODO: 수신했을 경우 발신 기록을 DB에서 조회
        Log.i(Global.TAG, "selectRecords() invoked.");

        try {
            String address = "select_your_records"; // 통신할 JSP 주소

            contentsActivity.getPhoneState();

            JSONObject parameter = new JSONObject();
            parameter.put("sender", contentsActivity.getUserPhoneNumber());
            parameter.put("receiver", phoneNumber);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try{
                        if(out!=null){ // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch(result){
                                case "-1": // 조작된 번호
                                    Toast.makeText(getApplicationContext(), "조작된 번호입니다.", Toast.LENGTH_SHORT).show();
                                    // 디자인해서 핸드폰에 띄어주기
                                    break;
                                case "0": // 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;

                                case "1": // 오류 없음 (컨텐츠 다운받아서 보여주기)
                                    Toast.makeText(getApplicationContext(), "오류 없음", Toast.LENGTH_SHORT).show();
                                    // 핸드폰에 컨텐츠 보여주기
                                    break;

                            }
                        }else { // 안드로이드 - JSP 통신 오류
                            Toast.makeText(getApplicationContext(), "JSP Error Occured.", Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            communicateDB.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}