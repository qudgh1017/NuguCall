package edu.skku.monet.nugucall;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class PreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
    }
    
    public void previewContents() {
        Intent i = new Intent(Global.INTENT_ACTION_PREVIEW_CONTENTS);
        // i.putExtra(Global.INTENT_EXTRA_PHONE_NUMBER, phoneNumber); // 데이터 보내기
        sendBroadcast(i); // BackgroundService로 인텐트 발신
    }
}
