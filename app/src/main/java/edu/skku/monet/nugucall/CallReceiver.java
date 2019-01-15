package edu.skku.monet.nugucall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {

    private boolean isIncomingCall;

    CallReceiver(){
        this.isIncomingCall = false;
    }

    public void setIncomingCall(boolean isIncomingCall){
        this.isIncomingCall = isIncomingCall;
    }

    public boolean getisIncomingCall(){
        return isIncomingCall;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Global.TAG, "onReceive() 실행!");

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber;

        // 전화 발신 (안드로이드 버전 8.0 미만) 확인
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Log.i(Global.TAG, "onReceive() if (action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) 실행!");
            setIncomingCall(false);
            phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.i(Global.TAG, "전화를 발신했습니다. 수신 번호 : " + phoneNumber + " (below Android Oreo)");
            // insertRecords(phoneNumber);
        }

        // 전화 발신 (안드로이드 버전 8.0 이상) & 수신 확인
        switch (telephonyManager.getCallState()) {

            case TelephonyManager.CALL_STATE_RINGING: // 전화 수신
                Log.i(Global.TAG, "onReceive()  case TelephonyManager.CALL_STATE_RINGING: 실행!");
                setIncomingCall(true);
                phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.i(Global.TAG, "전화를 수신했습니다. 발신 번호 : " + phoneNumber);
                // selectRecords(phoneNumber);
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.i(Global.TAG, "onReceive()  case TelephonyManager.CALL_STATE_OFFHOOK: 실행!");
                if (getisIncomingCall()) { // 전화 수신 및 통화 시작
                    Log.i(Global.TAG, "전화를 수신 및 통화가 시작됐습니다.");
                } else { // 전화 발신
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Log.i(Global.TAG, "전화를 발신했습니다. 수신 번호 : " + phoneNumber + " (above Android Oreo)");
                    // insertRecords(phoneNumber);
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                Log.i(Global.TAG, "onReceive()  case TelephonyManager.CALL_STATE_IDLE: 실행!");
                if (getisIncomingCall()) { // 전화 수신 및 통화 종료
                    Log.i(Global.TAG, "전화를 수신 및 통화가 종료됐습니다.");
                    // callScreenLayout.turnOffContents();
                } else { // 전화 발신 및 통화 종료
                    Log.i(Global.TAG, "전화를 발신 및 통화가 종료됐습니다.");
                    // callScreenLayout.turnOffContents();
                }
                break;

        }
    }
}
