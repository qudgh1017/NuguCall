package edu.skku.monet.nugucall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URLConnection;

public class BackgroundService extends Service {

    private SharedPreferences sharedPreferences;

    private WindowManager windowManager;
    private CallScreenLayout callScreenLayout;
    private WindowManager.LayoutParams callScreenLayoutParams;

    // getPhonestate() 함수 쓰려고
    ContentsActivity contentsActivity = new ContentsActivity();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sharedPreferences = getSharedPreferences(Global.SHARED_PREFERENCES, MODE_PRIVATE);

        setNotification();
        setWindowLayout();

        return super.onStartCommand(intent, flags, startId);
    }

    public void setNotification() {
        Log.i(Global.TAG, "setNotification() 실행!");
        if (Build.VERSION.SDK_INT >= 26) { // 안드로이드 8.0 이상에서는 알림 채널 생성이 필수
            Log.i(Global.TAG, "setNotification() if (Build.VERSION.SDK_INT >= 26) 실행!");
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
        Log.i(Global.TAG, "setWindowLayout() 실행!");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        callScreenLayout = new CallScreenLayout(getApplicationContext());
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

        private boolean isShowing = false;

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

            tv_text.setSelected(true);
        }

        // 컨텐츠 보여주기
        public void turnOnContents(String name, String phone, String text, String source) {
            Log.i(Global.TAG, "turnOnContents() 실행!");
            if (isShowing) {
                return;
            }
            isShowing = true;

            tv_name.setText(name);
            tv_phone.setText(PhoneNumberUtils.formatNumber(phone));
            tv_text.setText(text);

            windowManager.addView(callScreenLayout, callScreenLayoutParams);

            String filePath = Global.DEFAULT_PATH + File.separator + source;
            String mimeType = URLConnection.guessContentTypeFromName(filePath);
            mimeType = mimeType.substring(0, mimeType.indexOf("/"));
            File file = new File(filePath);

            switch (mimeType) {

                case "image":
                    Log.i(Global.TAG, "turnOnContents() switch(image)실행!");
                    iv_source.setVisibility(View.VISIBLE);
                    RequestOptions requestOptions = new RequestOptions().centerCrop().placeholder(R.drawable.icon);
                    Glide.with(getApplicationContext()).load(file).apply(requestOptions).into(iv_source);
                    break;

                case "video":
                    Log.i(Global.TAG, "turnOnContents() switch(video)실행!");
                    vv_source.setVisibility(View.VISIBLE);
                    vv_source.setVideoPath(file.getPath());
                    vv_source.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            vv_source.start();
                        }
                    });
                    vv_source.start();
                    break;

                default:
                    Toast.makeText(getApplicationContext(), "지원하지 않는 파일입니다.", Toast.LENGTH_SHORT).show();
                    break;

            }
        }

        // 경고창 보여주기
        public void turnOnContents(String phoneNumber) {
            Log.i(Global.TAG, "turnOnContents(경고창) 실행!");
            if (isShowing) {
                return;
            }
            isShowing = true;

            tv_name.setText("경고");
            tv_phone.setText(PhoneNumberUtils.formatNumber(phoneNumber));
            tv_text.setText("지금 걸려온 전화는 보이스피싱일 수 있습니다. 주의하시기 바랍니다.");

            windowManager.addView(callScreenLayout, callScreenLayoutParams);

            iv_source.setVisibility(View.VISIBLE);
            RequestOptions requestOptions = new RequestOptions().centerCrop().placeholder(R.drawable.icon);
            Glide.with(getApplicationContext()).load(R.drawable.icon).apply(requestOptions).into(iv_source);
        }

        // 창 끄기
        public void turnOffContents() {
            Log.i(Global.TAG, "turnOffContents() 실행!");
            if (!isShowing) {
                return;
            }
            isShowing = false;

            iv_source.setVisibility(View.GONE);
            vv_source.setVisibility(View.GONE);
            if (vv_source.isPlaying()) {
                vv_source.stopPlayback();
            }
            windowManager.removeView(callScreenLayout);
        }
    }

    public void insertRecords(final String phoneNumber) {
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
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {

                                case "1": // JSP - DB 통신 성공
                                    Log.i(Global.TAG, "insert_my_records() : 발신기록을 DB에 삽입하였습니다.");
                                    selectYourContents(phoneNumber);
                                    break;

                                case "0": // JSP - DB 통신 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;

                            }
                        } else { // 안드로이드 - JSP 통신 오류 발생
                            Toast.makeText(getApplicationContext(), "JSP Error Occured.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            communicateDB.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void selectRecords(final String phoneNumber) {
        // TODO: 수신했을 경우 발신 기록을 DB에서 조회
        Log.i(Global.TAG, "selectRecords() invoked.");

        try {
            String address = "select_your_records"; // 통신할 JSP 주소

            contentsActivity.getPhoneState();

            JSONObject parameter = new JSONObject();
            parameter.put("sender", phoneNumber);
            parameter.put("receiver", contentsActivity.getUserPhoneNumber());

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {

                                case "-1": // 조작된 번호
                                    Toast.makeText(getApplicationContext(), "조작된 번호입니다.", Toast.LENGTH_SHORT).show();
                                    // 디자인해서 핸드폰에 띄어주기
                                    callScreenLayout.turnOnContents(phoneNumber);
                                    break;

                                case "0": // 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;

                                case "1": // 오류 없음 (컨텐츠 다운받아서 보여주기)
                                    Toast.makeText(getApplicationContext(), "오류 없음", Toast.LENGTH_SHORT).show();
                                    // 핸드폰에 컨텐츠 보여주기.
                                    selectYourContents(phoneNumber);
                                    break;

                            }
                        } else { // 안드로이드 - JSP 통신 오류
                            Toast.makeText(getApplicationContext(), "JSP Error Occured.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            communicateDB.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectYourContents(String phoneNumber) {
        try {
            String address = "select_your_contents";
            JSONObject parameter = new JSONObject();
            parameter.put("phone", phoneNumber);
            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) {
                            JSONObject jsonObject = new JSONObject(out);
                            String result = jsonObject.getString("result");

                            switch (result) {

                                case "0": // 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;

                                case "1": // 오류 없음 (컨텐츠 다운받아서 보여주기)
                                    JSONArray jsonArray = jsonObject.getJSONArray("items");

                                    if (jsonArray.length() > 0) {
                                        Log.i(Global.TAG, "contents exist.");

                                        // String id = jsonArray.getJSONObject(0).getString("id");
                                        String name = jsonArray.getJSONObject(0).getString("name");
                                        String phone = jsonArray.getJSONObject(0).getString("phone");
                                        String text = jsonArray.getJSONObject(0).getString("text");
                                        String source = jsonArray.getJSONObject(0).getString("source");
                                        // String imei = jsonArray.getJSONObject(0).getString("imei");

                                        callScreenLayout.turnOnContents(name, phone, text, source);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "서버에 등록되지 않은 번호입니다.", Toast.LENGTH_SHORT).show();
                                    }
                                    break;

                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "JSP Error Occured.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            communicateDB.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}