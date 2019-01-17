package edu.skku.monet.nugucall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Global.TAG, "onReceive() invoked.");

        SharedPreferences sharedPreferences = context.getSharedPreferences(Global.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber;
        Intent i;

        // 전화 발신 (안드로이드 버전 8.0 미만) 확인
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.i(Global.TAG, "전화를 발신했습니다. 수신 번호 : " + phoneNumber + " (below Android Oreo)");
            i = new Intent(Global.INTENT_ACTION_INSERT_RECORDS);
            i.putExtra(Global.INTENT_EXTRA_PHONE_NUMBER, phoneNumber);
            context.sendBroadcast(i);
        }

        // 전화 발신 (안드로이드 버전 8.0 이상) & 수신 확인
        switch (telephonyManager.getCallState()) {

            case TelephonyManager.CALL_STATE_RINGING: // 전화 수신
                sharedPreferences.edit().putBoolean(Global.SHARED_PREFERENCES_INCOMING, true).apply();
                phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.i(Global.TAG, "전화를 수신했습니다. 발신 번호 : " + phoneNumber);
                i = new Intent(Global.INTENT_ACTION_SELECT_RECORDS);
                i.putExtra(Global.INTENT_EXTRA_PHONE_NUMBER, phoneNumber);
                context.sendBroadcast(i);
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (sharedPreferences.getBoolean(Global.SHARED_PREFERENCES_INCOMING, false)) { // 전화 수신 및 통화 시작
                    Log.i(Global.TAG, "전화를 수신 및 통화가 시작됐습니다.");
                } else { // 전화 발신
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Log.i(Global.TAG, "전화를 발신했습니다. 수신 번호 : " + phoneNumber + " (above Android Oreo)");
                    i = new Intent(Global.INTENT_ACTION_INSERT_RECORDS);
                    i.putExtra(Global.INTENT_EXTRA_PHONE_NUMBER, phoneNumber);
                    context.sendBroadcast(i);
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                if (sharedPreferences.getBoolean(Global.SHARED_PREFERENCES_INCOMING, false)) { // 전화 수신 및 통화 종료
                    sharedPreferences.edit().putBoolean(Global.SHARED_PREFERENCES_INCOMING, false).apply();
                    Log.i(Global.TAG, "전화를 수신 및 통화가 종료됐습니다.");
                    i = new Intent(Global.INTENT_ACTION_TURN_OFF_CONTENTS);
                    context.sendBroadcast(i);
                } else { // 전화 발신 및 통화 종료
                    Log.i(Global.TAG, "전화를 발신 및 통화가 종료됐습니다.");
                    i = new Intent(Global.INTENT_ACTION_TURN_OFF_CONTENTS);
                    context.sendBroadcast(i);
                }
                break;

        }
    }
}