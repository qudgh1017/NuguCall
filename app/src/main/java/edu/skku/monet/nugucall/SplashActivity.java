package edu.skku.monet.nugucall;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, Global.REQ_CODE_PERMISSION_PHONE);
        }
    }

    public void checkStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            checkOverlayPermission();
        } else {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Global.REQ_CODE_PERMISSION_STORAGE);
        }
    }

    public void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(getApplicationContext())) {
                goToContentsActivity();
            } else {
                Toast.makeText(getApplicationContext(), "다른 앱 위에 그리기 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, Global.REQ_CODE_PERMISSION_OVERLAY);
            }
        }
    }

    public void goToContentsActivity() {
        // 스마트폰 해상도를 이용해 서비스 화면 출력 사이즈 정하기
        int status_bar_height = 0;
        int resource_identifier = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource_identifier > 0) {
            status_bar_height = getResources().getDimensionPixelSize(resource_identifier);
        }
        Point point = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(point);
        int full_width = point.x;
        int full_height = point.y - status_bar_height;
        sharedPreferences.edit().putInt(Global.SHARED_PREFERENCES_WIDTH, full_width / 2).apply();
        sharedPreferences.edit().putInt(Global.SHARED_PREFERENCES_HEIGHT, full_height / 2).apply();

        // 누구콜 전용 컨텐츠 폴더를 생성하기
        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getString(R.string.app_name));
        if (file.exists()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), ContentsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }, 1000);
        } else {
            if (file.mkdir()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), ContentsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                }, 1000);
            } else {
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Global.REQ_CODE_PERMISSION_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkStoragePermission();
            } else {
                finish();
            }
        } else if (requestCode == Global.REQ_CODE_PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkOverlayPermission();
            } else {
                finish();
            }
        } else if (requestCode == Global.REQ_CODE_PERMISSION_OVERLAY) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.canDrawOverlays(getApplicationContext())) {
                    goToContentsActivity();
                } else {
                    finish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}