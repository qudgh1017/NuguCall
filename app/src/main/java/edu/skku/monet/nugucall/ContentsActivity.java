package edu.skku.monet.nugucall;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ContentsActivity extends AppCompatActivity {

    // 유병호 학생
    String userName = "", userText = "";
    // 폰 정보 불러온 값을 저장할 변수
    String userIMEI = "", userPhoneNumber = "";
    // 서버에 보낼 값, 폰 정보 불러온 값을 보여줄 TextView
    EditText textName, textText;
    TextView textPhoneNumber, textIMEI;
    Button btn_send, btn_reset, btn_delete;

    //btn_send가 등록인지 수정인지 알기위해 (등록:0, 수정:1)
    int btn_check = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        textName = (EditText) findViewById(R.id.textName);
        textText = (EditText) findViewById(R.id.textText);
        textPhoneNumber = (TextView) findViewById(R.id.textPhoneNumber);
        textIMEI = (TextView) findViewById(R.id.textIMEI);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_delete = (Button) findViewById(R.id.btn_delete);

        // 폰정보 불러오기(userIMEI, userPhoneNumber)
        getPhoneState();

        // 고객 화면에 보여주기위한 값
        textPhoneNumber.setText(userPhoneNumber);
        textIMEI.setText(userIMEI);

        // 처음 컨텐츠 등록된 상태인지 조회하기 위해
        hasContents();

        // 등록, 수정 버튼 누른경우 insert
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (textName.getText().toString().equals("") || textText.getText().toString().equals("")) {
                    Toast.makeText(ContentsActivity.this, "이름과 텍스트를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    // 고객이 입력한 값 저장
                    userName = textName.getText().toString();
                    userText = textText.getText().toString();

                    if(btn_check==0){ // 등록 버튼일 경우
                        insertContents();
                    }else if(btn_check==1){// 수정 버튼일 경우
                        updateContents();
                    }

                }
            }
        });

        // 초기화 버튼 누른경우 초기화
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textName.setText("");
                textText.setText("");
            }
        });

        // 삭제 버튼 누른 경우 finish()
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteContents();
            }
        });
    }

    public void getPhoneState() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                userIMEI = tm.getImei();
            } else {
                userIMEI = tm.getDeviceId();
            }
            userPhoneNumber = tm.getLine1Number();
            //userPhoneNumber = "+821067373845"; //임의로 설정(USIM 없어서)
            //+82를 0으로 바꿔주기
            userPhoneNumber = userPhoneNumber.replace("+82", "0");

            Log.i("userIMEI", userIMEI + "");
            Log.i("userPhoneNumber", userPhoneNumber + "");
        } catch (SecurityException e) { //권한 오류로 인한 경우 catch
            e.printStackTrace();
        }
    }

    public void hasContents() {
        Log.i(Global.TAG, "hasContents() invoked.");

        try {
            String address = "contents/select_my_contents.jsp"; // 통신할 JSP 주소
            //select_my_contents에서 IMEI로 정보 조회
            JSONObject parameter = new JSONObject();
            parameter.put("imei", userIMEI + ""); // 매개변수, 값

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result"); // 반환 값

                            switch (result) {
                                case "1": // JSP - DB 통신 성공
                                    json = json.getJSONObject("item");
                                    if (json.length() > 0) { // 조회된 컨텐츠가 있는 경우
                                        Log.i(Global.TAG, "contents exist.");

                                        String id = json.getString("id");
                                        String name = json.getString("name");
                                        String phone = json.getString("phone");
                                        String source = json.getString("source");
                                        String imei = json.getString("imei");

                                        // TODO: 이미 등록된 컨텐츠 정보를 띄우고, 수정 버튼으로 변경
                                        btn_check = 1;
                                        btn_send.setText("수정");
                                        textName.setText(name);
                                        textText.setText(source);

                                    } else { // 조회된 컨텐츠가 없는 경우
                                        Log.i(Global.TAG, "contents not exist.");

                                        // TODO: 새로 컨텐츠를 등록할 수 있게 띄우고, 등록 버튼으로 변경
                                        btn_check = 0;
                                        btn_send.setText("등록");
                                        btn_delete.setVisibility(View.GONE); //
                                        //textName.setText("");
                                        //textText.setText("");
                                    }
                                    break;

                                case "0": // JSP - DB 통신 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        } else { // 안드로이드 - JSP 통신 오류 발생
                            Toast.makeText(getApplicationContext(), "JSP Error Occurred.", Toast.LENGTH_SHORT).show();
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

    public void insertContents(){
        Log.i(Global.TAG, "insertContents() invoked.");

        try {
            String address = "contents/insert_my_contents.jsp"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("name", userName);
            parameter.put("phone", userPhoneNumber);
            parameter.put("source", userText);
            parameter.put("imei", userIMEI);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try{
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result){
                                case "1" :// JSP - DB 통신 성공
                                    Toast.makeText(getApplicationContext(), "컨텐츠가 등록되었습니다.", Toast.LENGTH_SHORT).show();

                                    //등록되면 수정으로 바꿔주고 삭제버튼 보이게
                                    btn_check = 1;
                                    btn_send.setText("수정");
                                    btn_delete.setVisibility(View.VISIBLE);
                                    break;

                                case "0": // JSP - DB 통신 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }else { // 안드로이드 - JSP 통신 오류 발생
                            Toast.makeText(getApplicationContext(), "JSP Error Occurred.", Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            communicateDB.execute();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void updateContents(){
            Log.i(Global.TAG, "updateContents() invoked.");

            try {
                String address = "contents/update_my_contents.jsp"; // 통신할 JSP 주소

                JSONObject parameter = new JSONObject();
                parameter.put("name", userName);
                parameter.put("phone", userPhoneNumber);
                parameter.put("source", userText);
                parameter.put("imei", userIMEI);

                CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                    @Override
                    public void callback(String out) {
                        try{
                            if (out != null) { // 안드로이드 - JSP 통신 성공
                                JSONObject json = new JSONObject(out);
                                String result = json.getString("result");

                                switch (result){
                                    case "1" :// JSP - DB 통신 성공
                                        Toast.makeText(getApplicationContext(), "컨텐츠가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                                        break;

                                    case "0": // JSP - DB 통신 오류 발생
                                        Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }else { // 안드로이드 - JSP 통신 오류 발생
                                Toast.makeText(getApplicationContext(), "JSP Error Occurred.", Toast.LENGTH_SHORT).show();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
                communicateDB.execute();
            }catch(Exception e){
                e.printStackTrace();
            }
    }

    public void deleteContents(){
        Log.i(Global.TAG, "deleteContents() invoked.");

        try {
            String address = "contents/delete_my_contents.jsp"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("imei", userIMEI);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if(out!=null){// 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result){
                                case "1":// JSP - DB 통신 성공
                                    Toast.makeText(getApplicationContext(), "컨텐츠가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    btn_check = 0;
                                    finish();
                                    break;
                                case "0":// JSP - DB 통신 오류 발생
                                    Toast.makeText(getApplicationContext(), "DB Error Occurred.", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }else{// 안드로이드 - JSP 통신 오류 발생

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
}




//추가할 것
//파일 탐색해서 사진 또는 동영상 올리는 기능 추가해야함.
//사진 또는 동영상 보내는 기능 추가해야함