package edu.skku.monet.nugucall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
import android.util.Log;

public class PreviewActivity extends AppCompatActivity {
    private String name;
    private String phone;
    private String text;
    private String source;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        //ContentsActivity로부터 넘어온 intent
        Intent contents_intent = getIntent();
        name = contents_intent.getStringExtra(Global.INTENT_EXTRA_NAME);
        phone = contents_intent.getStringExtra(Global.INTENT_EXTRA_PHONE_NUMBER);
        text = contents_intent.getStringExtra(Global.INTENT_EXTRA_TEXT);
        source = contents_intent.getStringExtra(Global.INTENT_EXTRA_SOURCE);
        filePath = contents_intent.getStringExtra(Global.INTENT_EXTRA_FILEPATH);

        //브로드캐스트 리시버 등록
        setBroadcastReceiver_preview();

        //미리보기 화면 띄우기
        turnOnpreviewContents();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //미리보기 화면 끄기
        turnOffpreviewContents();

        //액티비티 종료
        finish();
    }

    //미리보기 화면 띄우기, BackgroundService로 브로드캐스트를 보내 CallScreenLayout의 turnOnpreviewContents메소드 호출
    public void turnOnpreviewContents() {
        Intent i = new Intent(Global.INTENT_ACTION_PREVIEW_CONTENTS_ON);
        // 데이터 보내기
        i.putExtra(Global.INTENT_EXTRA_NAME, name);
        i.putExtra(Global.INTENT_EXTRA_PHONE_NUMBER, phone);
        i.putExtra(Global.INTENT_EXTRA_TEXT, text);
        i.putExtra(Global.INTENT_EXTRA_SOURCE, source);
        i.putExtra(Global.INTENT_EXTRA_FILEPATH, filePath);
        // BackgroundService로 인텐트 발신
        sendBroadcast(i);
    }

    //미리보기 화면 끄기, BackgroundService로 브로드캐스트를 보내 CallScreenLayout의 turnOffpreviewContents메소드 호출
    public void turnOffpreviewContents(){
        Intent i = new Intent(Global.INTENT_ACTION_PREVIEW_CONTENTS_OFF);
        sendBroadcast(i);
    }

    public void setBroadcastReceiver_preview(){
        Log.i(Global.TAG, "setBroadcastReceiver_preview() invoked.");
        //인텐트 필터 선언 & 액션 추가
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Global.INTENT_ACTION_PREVIEW_ACTIVITY_OFF);
        //브로드캐스트리시버&인텐트필터 등록
        registerReceiver(broadcastReceiver_preview, intentFilter);
    }

    public BroadcastReceiver broadcastReceiver_preview = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null){
                switch (action){
                    case Global.INTENT_ACTION_PREVIEW_ACTIVITY_OFF:
                        finish();
                        break;
                }
            }
        }
    };
}