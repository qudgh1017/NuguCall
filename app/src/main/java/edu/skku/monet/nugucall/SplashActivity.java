package edu.skku.monet.nugucall;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.widget.Toast;

import java.io.File;

public class SplashActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(Global.SHARED_PREFERENCES, MODE_PRIVATE);

        handler = new Handler(getMainLooper());

        if (Build.VERSION.SDK_INT >= 23) {
            checkPhonePermission();
        } else {
            goToContentsActivity();
        }
    }

    public void checkPhonePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_PHONE_STATE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            checkStoragePermission();
        } else {
            // 권한 요청 화면 띄우는 것
            // 권한 요청 허용 또는 거부 선택시 무조건 onRequestPermissionsResult() 실행됨
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, Global.REQ_CODE_PERMISSION_PHONE);
        }
    }

    public void checkStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            checkCameraPermission();
        } else {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Global.REQ_CODE_PERMISSION_STORAGE);
        }
    }

    public void checkCameraPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            checkOverlayPermission();
        } else {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.CAMERA}, Global.REQ_CODE_PERMISSION_CAMERA);
        }
    }

    public void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(getApplicationContext())) {
                goToContentsActivity();
            } else {
                Toast.makeText(getApplicationContext(), "다른 앱 위에 그리기 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // getPacakageName() 패키지 이름(edu.skk.monet.nugucall)
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                // startActivity 하는거와 동시에 결과값을 돌려받기위해 하는 함수
                // 종료될 때 무조건 onActivityResult() 실행됨
                startActivityForResult(intent, Global.REQ_CODE_PERMISSION_OVERLAY);
            }
        }
    }

    public void goToContentsActivity() {
        // 스마트폰 해상도를 이용해 서비스 화면 출력 사이즈 정하기
        int status_bar_height = 0;
        int resource_identifier = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource_identifier > 0) {
            status_bar_height = getResources().getDimensionPixelSize(resource_identifier); // 알림바 높이
        }
        Point point = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(point);
        int full_width = point.x; // 가로 해상도
        int full_height = point.y - status_bar_height; // 세로 해상도 - 알림바 높이
        sharedPreferences.edit().putInt(Global.SHARED_PREFERENCES_WIDTH, full_width * 7 / 10).apply(); // 가로 해상도의 7/10
        sharedPreferences.edit().putInt(Global.SHARED_PREFERENCES_HEIGHT, full_height * 4 / 10).apply(); // 세로 해상도의 4/10

        // 발신/수신시 다운로드 받은 컨텐츠를 저장할 누구콜 전용 폴더를 생성하기
        File file = new File(Global.DEFAULT_PATH);
        if (file.exists()) { // 누구콜 폴더 존재하면
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 컨텐츠 액티비티 실행
                    Intent intent = new Intent(getApplicationContext(), ContentsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }, 1500);
        } else { // 누구콜 폴더 존재x
            if (file.mkdir()) { // 누구콜 폴더 생성
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 컨텐츠 액티비티 실행
                        Intent intent = new Intent(getApplicationContext(), ContentsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                }, 1500);
            } else {
                finish();
            }
        }

        // 누구콜 백그라운드 서비스 실행하기
        if (!isServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
            startService(intent);
        }
    }

    // 권한 요청창에서 허용이나 거부 선택시 무조건 실행되는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // phone정보, 저장소, 카메라, 앱 위에 그리기 순으로
        if (requestCode == Global.REQ_CODE_PERMISSION_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // 허용 선택시
                checkStoragePermission();
            } else { // 거부 선택시
                finish();
            }
        } else if (requestCode == Global.REQ_CODE_PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkCameraPermission();
            } else {
                finish();
            }
        }else if (requestCode == Global.REQ_CODE_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkOverlayPermission();
            } else {
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // startActivityForResult()로 실행한 화면이 종료될 때 실행
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == Global.REQ_CODE_PERMISSION_OVERLAY) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.canDrawOverlays(getApplicationContext())) {
                    goToContentsActivity();
                } else {
                    finish();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 서비스가 이미 실행되고 있는 상태인지 확인
    public boolean isServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // 핸드폰에서 실행중인 모든 서비스를 찾는다.
        for (ActivityManager.RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (runningServiceInfo.service.getClassName().equals("edu.skku.monet.nugucall.BackgroundService")) {
                return true;
            }
        }
        return false;
    }

    // 뒤로가기 눌렀을 경우
    @Override
    public void onBackPressed() {
        // splash 화면일 때 뒤로가기 버튼이 안되어야 하므로 주석처리함.
        // super.onBackPressed();
    }
}