package edu.skku.monet.nugucall;

import android.annotation.SuppressLint;
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
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
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

    // 안드로이드는 메인 스레드에서만 UI 관련 작업을 할 수 있음
    // 메인 스레드가 아닌 다른 스레드에서 UI 관련 작업을 하고 싶을 때 사용
    // 또는 딜레이를 주기 위해 사용
    private Handler handler;

    // 간단한 데이터 저장 및 불러오기에 사용
    private SharedPreferences sharedPreferences;

    // 팝업 윈도우 관리자
    private WindowManager windowManager;
    // 개발자가 정의한 레이아웃 디자인 클래스
    private CallScreenLayout callScreenLayout;
    // 팝업 윈도우의 크기와 어디에 띄울지 정하는 파라미터
    private WindowManager.LayoutParams callScreenLayoutParams;

    private String userIMEI;
    private String userPhoneNumber;

    private String contentsName;
    private String contentsPhone;
    private String contentsText;
    private String contentsSource;
    private String contentsSize;

    //프리뷰 콜백을 위한 String
    private String contentsNamePreview;
    private String contentsPhonePreview;
    private String contentsTextPreview;
    private String contentsSourcePreview;
    private String contentsSizePreview;

    // 서비스가 처음 시작될 때 실행
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handler = new Handler(getMainLooper());

        sharedPreferences = getSharedPreferences(Global.SHARED_PREFERENCES, MODE_PRIVATE);

        setNotification();
        setWindowLayout();
        setBroadcastReceiver();
        getUserPhoneInformation();

        return super.onStartCommand(intent, flags, startId);
    }

    //파일 다운로드 콜백
    private ThreadReceive downloadThreadReceive = new ThreadReceive() {
        @Override
        public void onReceiveRun(String fileName, long fileSize) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callScreenLayout.turnOnContents(contentsName, contentsPhone, contentsText, contentsSource);
                }
            });
        }
    };

    //프리뷰 파일 다운로드 콜백
    private ThreadReceive downloadThreadReceivePreview = new ThreadReceive() {
        @Override
        public void onReceiveRun(String fileName, long fileSize) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callScreenLayout.turnOnpreviewContents(contentsNamePreview, contentsPhonePreview, contentsTextPreview, contentsSourcePreview, contentsSizePreview);
                }
            });
        }
    };

    // 알림바 선언
    public void setNotification() {
        Log.i(Global.TAG, "setNotification() invoked.");
        // 안드로이드 8.0 이상에서는 알림 채널 생성이 필수
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(Global.NOTIFICATION_CHANNEL_ID, Global.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        // PendingIntent & Intent : 알림바를 눌렀을 때 켜질 화면
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), Global.REQ_CODE_NOTIFICATION_INTENT, intent, 0);

        // 알림바 만들기
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Global.NOTIFICATION_CHANNEL_ID);
        Notification notification = builder
                .setContentTitle("NuguCall") // 알림바 제목
                .setContentText("NuguCall이 실행 중입니다.") // 알림바 내용
                .setSmallIcon(R.drawable.icon) // 알림바 아이콘
                .setContentIntent(pendingIntent) // 알림바 눌렀을 때 켜질 화면
                .build();
        // (음악 같이) 계속 켜져있어야 하는 서비스에서 사용
        startForeground(Global.NOTIFICATION_ID, notification);

        // 일반적인 방법인데 작업이 종료되면 같이 꺼짐
        // NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // notificationManager.notify(Global.NOTIFICATION_ID, notification);
    }

    public void setWindowLayout() {
        Log.i(Global.TAG, "setWindowLayout() invoked.");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        callScreenLayout = new CallScreenLayout(getApplicationContext());
        if (Build.VERSION.SDK_INT >= 26) {
            callScreenLayoutParams = new WindowManager.LayoutParams(
                    // 가로 세로 크기는 SplashActivity에서 구해서 저장해둔 것을 SharedPreferences를 통해 불러옴
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_WIDTH, 0), // 가로
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_HEIGHT, 0), // 세로
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 안드로이드 8.0 이상 팝업 윈도우 정의
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // 터치 가능하게
                    PixelFormat.TRANSLUCENT // 투명 지원
            );
        } else {
            callScreenLayoutParams = new WindowManager.LayoutParams(
                    // 가로 세로 크기는 SplashActivity에서 구해서 저장해둔 것을 SharedPreferences를 통해 불러옴
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_WIDTH, 0), // 가로
                    sharedPreferences.getInt(Global.SHARED_PREFERENCES_HEIGHT, 0), // 세로
                    WindowManager.LayoutParams.TYPE_PHONE, // 안드로이드 8.0 미만 팝업 윈도우 정의
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // 터치 가능하게
                    PixelFormat.TRANSLUCENT // 투명 지원
            );
        }
        // 중앙 정렬
        callScreenLayoutParams.gravity = Gravity.CENTER;
        // verticalMargin을 써서 아래로 내릴 수 있음
        // callScreenLayoutParams.verticalMargin = 0;
        // 투명한 상태로 나왔다가 투명하게 사라지는 효과
        callScreenLayoutParams.windowAnimations = android.R.style.Animation_Toast;
    }

    private class CallScreenLayout extends LinearLayout {
        private TextView tv_name; // 이름
        private TextView tv_phone; // 전화번호
        private ImageView iv_source; // 이미지 컨텐츠
        private VideoView vv_source; // 동영상 컨텐츠
        private TextView tv_text; // 문구

        private boolean isShowing = false; // 컨텐츠가 보여지고 있는지
        private boolean isShowingPreview = false; // 미리보기가 보여지고 있는지

        public CallScreenLayout(Context context) {
            super(context);
            // 레이아웃을 CallScreenLayout 클래스에 입히기
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
                layoutInflater.inflate(R.layout.service_background, this, true);
            }
            tv_name = findViewById(R.id.tv_name);
            tv_phone = findViewById(R.id.tv_phone);
            iv_source = findViewById(R.id.iv_source);
            vv_source = findViewById(R.id.vv_source);
            tv_text = findViewById(R.id.tv_text);

            // 지나가는 애니메이션을 위해 필요
            tv_text.setSelected(true);
        }

        // 컨텐츠 보여주기
        public void turnOnContents(String name, String phone, String text, String source) {
            Log.i(Global.TAG, "turnOnContents() invoked.");
            if (isShowing) {
                return;
            }
            isShowing = true;

            tv_name.setText(name);
            tv_phone.setText(PhoneNumberUtils.formatNumber(phone));
            tv_text.setText(text);

            // 팝업 창 보이기
            windowManager.addView(callScreenLayout, callScreenLayoutParams);

            // source : 파일이름.확장자
            // 안드로이드 기본 경로는 /storage/emulated/0/NuguCall
            // 종합 경로 : /storage/emulated/0/NuguCall/"파일이름.확장자"
            String filePath = Global.DEFAULT_PATH + File.separator + source;
            // 파일 성질 알아내기 (image/png) (video/mp4)
            String mimeType = URLConnection.guessContentTypeFromName(filePath);
            // 슬래시 앞에 것 따오기
            mimeType = mimeType.substring(0, mimeType.indexOf("/"));
            File file = new File(filePath);

            switch (mimeType) {

                case "image":
                    iv_source.setVisibility(View.VISIBLE);
                    // 이미지 띄워주는 라이브러리
                    RequestOptions requestOptions = new RequestOptions().centerCrop().placeholder(R.drawable.icon);
                    Glide.with(getApplicationContext()).load(file).apply(requestOptions).into(iv_source);
                    break;

                case "video":
                    vv_source.setVisibility(View.VISIBLE);
                    // 비디오 파일 위치
                    vv_source.setVideoPath(file.getPath());
                    // 재생이 완료됐을 경우
                    vv_source.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // 재생
                            vv_source.start();
                        }
                    });
                    // 재생
                    vv_source.start();
                    break;

                default:
                    Toast.makeText(getApplicationContext(), "지원하지 않는 파일입니다.", Toast.LENGTH_SHORT).show();
                    break;

            }
        }

        // 경고창 보여주기
        public void turnOnContents(String phoneNumber) {
            Log.i(Global.TAG, "turnOnContents() invoked.");
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
            Log.i(Global.TAG, "turnOffContents() invoked.");
            if (!isShowing) {
                return;
            }
            isShowing = false;

            // 다시 다 안 보이게
            iv_source.setVisibility(View.GONE);
            vv_source.setVisibility(View.GONE);
            // 비디오가 재생 중이면 정지
            if (vv_source.isPlaying()) {
                vv_source.stopPlayback();
            }
            // 팝업 창 끄기
            windowManager.removeView(callScreenLayout);
        }

        //미리보기 화면 띄우기
        public void turnOnpreviewContents(String name, String phone, String text, String source, String size) {
            Log.i(Global.TAG, "turnOnpreviewContents() invoked.");

            // source : 파일이름.확장자
            // 안드로이드 기본 경로는 /storage/emulated/0/NuguCall
            // 종합 경로 : /storage/emulated/0/NuguCall/"파일이름.확장자"
            String filePath = Global.DEFAULT_PATH + File.separator + source;
            File file = new File(filePath);

            // 해당 파일이 존재하지 않는 경우 다운로드 실행
            if (!file.exists()) {
                //콜백의 arguments로 사용됨
                contentsNamePreview = name;
                contentsPhonePreview = phone;
                contentsTextPreview = text;
                contentsSourcePreview = source;
                contentsSizePreview = size;

                //파일 다운로드 후 콜백을 통해서 turnOnpreviewContents를 다시 한번 부르게됨
                ContentsFileDownload contentsFileDownloadPreview = new ContentsFileDownload(downloadThreadReceivePreview, filePath, size);
                contentsFileDownloadPreview.fileDownload();
                return;
            }

            // 파일 성질 알아내기 (image/png) (video/mp4)
            String mimeType = URLConnection.guessContentTypeFromName(filePath);
            // 슬래시 앞에 것 따오기
            mimeType = mimeType.substring(0, mimeType.indexOf("/"));

            if (isShowingPreview) {
                return;
            }
            isShowingPreview = true;

            tv_name.setText(name);
            tv_phone.setText(PhoneNumberUtils.formatNumber(phone));
            tv_text.setText(text);

            // 팝업 창 보이기
            windowManager.addView(callScreenLayout, callScreenLayoutParams);

            switch (mimeType) {

                case "image":
                    iv_source.setVisibility(View.VISIBLE);
                    // 이미지 띄워주는 라이브러리
                    RequestOptions requestOptions = new RequestOptions().centerCrop().placeholder(R.drawable.icon).error(R.mipmap.ic_launcher);
                    Glide.with(getApplicationContext()).load(file).apply(requestOptions).into(iv_source);
                    break;

                case "video":
                    vv_source.setVisibility(View.VISIBLE);
                    // 비디오 파일 위치
                    vv_source.setVideoPath(file.getPath());
                    // 재생이 완료됐을 경우
                    vv_source.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // 재생
                            vv_source.start();
                        }
                    });
                    // 재생
                    vv_source.start();
                    break;

                default:
                    Toast.makeText(getApplicationContext(), "지원하지 않는 파일입니다.", Toast.LENGTH_SHORT).show();
                    break;

            }
        }

        //미리보기 화면 끄기
        public void turnOffpreviewContents() {
            Log.i(Global.TAG, "turnOffpreviewContents() invoked.");
            if (!isShowingPreview) {
                return;
            }
            isShowingPreview = false;

            // 다시 다 안 보이게
            iv_source.setVisibility(View.GONE);
            vv_source.setVisibility(View.GONE);
            // 비디오가 재생 중이면 정지
            if (vv_source.isPlaying()) {
                vv_source.stopPlayback();
            }
            // 팝업 창 끄기
            windowManager.removeView(callScreenLayout);
        }
    }

    public void insertRecords(final String phoneNumber) {
        Log.i(Global.TAG, "insertRecords() invoked.");

        try {
            String address = "insert_my_records"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("sender", userPhoneNumber);
            parameter.put("receiver", phoneNumber);
            parameter.put("imei", userIMEI);
            parameter.put("time", System.currentTimeMillis());

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {

                                case "1": // JSP - DB 통신 성공
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
        Log.i(Global.TAG, "selectRecords() invoked.");

        try {
            String address = "select_your_records"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("sender", phoneNumber);
            parameter.put("receiver", userPhoneNumber);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {

                                case "-1": // 조작된 번호
                                    callScreenLayout.turnOnContents(phoneNumber); // 스마트폰에 경고 화면 보여주기
                                    break;

                                case "0": // 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;

                                case "1": // 오류 없음
                                    selectYourContents(phoneNumber); // DB에서 상대 컨텐츠 조회
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
        Log.i(Global.TAG, "selectYourContents() invoked.");

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

                                case "1": // 오류 없음
                                    JSONArray jsonArray = jsonObject.getJSONArray("items");

                                    if (jsonArray.length() > 0) {

                                        // String id = jsonArray.getJSONObject(0).getString("id");
                                        contentsName = jsonArray.getJSONObject(0).getString("name");
                                        contentsPhone = jsonArray.getJSONObject(0).getString("phone");
                                        contentsText = jsonArray.getJSONObject(0).getString("text");
                                        contentsSource = jsonArray.getJSONObject(0).getString("source");
                                        contentsSize = jsonArray.getJSONObject(0).getString("size");
                                        // String imei = jsonArray.getJSONObject(0).getString("imei");


                                        String filePath = Global.DEFAULT_PATH + File.separator + contentsSource;
                                        File file = new File(filePath);
                                        if (!file.exists()) {
                                            // 해당 파일이 존재하지 않는 경우 다운로드 실행
                                            ContentsFileDownload contentsFileDownload = new ContentsFileDownload(downloadThreadReceive, filePath, contentsSize);
                                            contentsFileDownload.fileDownload();
                                        } else {
                                            // 이미 해당 파일이 존재하는 경우 다운로드 안 하고 바로 컨텐츠 출력
                                            callScreenLayout.turnOnContents(contentsName, contentsPhone, contentsText, contentsSource);
                                        }

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

    public void setBroadcastReceiver() {
        Log.i(Global.TAG, "setBroadcastReceiver() invoked.");
        // 인텐트 액션 추가
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Global.INTENT_ACTION_INSERT_RECORDS);
        intentFilter.addAction(Global.INTENT_ACTION_SELECT_RECORDS);
        intentFilter.addAction(Global.INTENT_ACTION_TURN_OFF_CONTENTS);
        intentFilter.addAction(Global.INTENT_ACTION_PREVIEW_CONTENTS_ON);
        intentFilter.addAction(Global.INTENT_ACTION_PREVIEW_CONTENTS_OFF);
        // 브로드캐스트 리시버 & 인텐트 필터 등록
        // 브로드캐스트 리시버 & 인텐트 필터 등록을 안 하면 broadcastReceiver의 onReceive가 호출이 안 됨
        registerReceiver(broadcastReceiver, intentFilter);
    }

    // 백그라운드 서비스가 다른 액티비티, 리시버 등과 통신하는 방법 (매우 중요)
    // CallReceiver에게서 인텐트 수신
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Global.INTENT_ACTION_INSERT_RECORDS:
                        insertRecords(intent.getStringExtra(Global.INTENT_EXTRA_PHONE_NUMBER));
                        break;
                    case Global.INTENT_ACTION_SELECT_RECORDS:
                        selectRecords(intent.getStringExtra(Global.INTENT_EXTRA_PHONE_NUMBER));
                        break;
                    case Global.INTENT_ACTION_TURN_OFF_CONTENTS:
                        callScreenLayout.turnOffContents();
                        break;
                    case Global.INTENT_ACTION_PREVIEW_CONTENTS_ON:
                        //PreviewActivity로부터 넘어온 String들을 받아서 callScreenLayout.previeContents호출
                        String i_name = intent.getStringExtra(Global.INTENT_EXTRA_NAME);
                        String i_phone = intent.getStringExtra(Global.INTENT_EXTRA_PHONE_NUMBER);
                        String i_text = intent.getStringExtra(Global.INTENT_EXTRA_TEXT);
                        String i_source = intent.getStringExtra(Global.INTENT_EXTRA_SOURCE);
                        String i_size = intent.getStringExtra(Global.INTENT_EXTRA_SOURCE);
                        callScreenLayout.turnOnpreviewContents(i_name, i_phone, i_text, i_source, i_size);
                        break;
                    case Global.INTENT_ACTION_PREVIEW_CONTENTS_OFF:
                        callScreenLayout.turnOffpreviewContents();
                        break;
                }
            }
        }
    };

    @SuppressLint({"HardwareIds", "MissingPermission"})
    public void getUserPhoneInformation() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                userIMEI = tm.getImei();
            } else {
                userIMEI = tm.getDeviceId();
            }

            // 번호를 받아와 +82를 0으로 바꿔주기
            //TextUtils.isEmpty(string) : Returns true if the string is null or 0-length.
            if(TextUtils.isEmpty(tm.getLine1Number())){
                userPhoneNumber = "번호를 불러오지 못함";
            }else{
                userPhoneNumber = (tm.getLine1Number()).replace("+82", "0");
            }

        } catch (Exception e) { // 권한 오류로 인한 경우 catch
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}