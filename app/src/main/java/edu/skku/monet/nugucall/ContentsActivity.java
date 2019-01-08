package edu.skku.monet.nugucall;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ContentsActivity extends AppCompatActivity {

    String userName="", userText="";
    // 폰 정보 불러온 값을 저장할 변수
    String userIMEI = "", userPhoneNumber = "";
    // 서버에 보낼 값, 폰 정보 불러온 값을 보여줄 TextView
    EditText textName, textText;
    TextView textPhoneNumber, textIMEI;
    Button btn_send, btn_reset;

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

        // 폰정보 불러오기(userIMEI, userPhoneNumber)
        getPhoneState();

        // 고객 화면에 보여주기위한 값
        textPhoneNumber.setText(userPhoneNumber);
        textIMEI.setText(userIMEI);

        // 고객이 입력한 값 저장
        userName = textName.getText().toString();
        userText = textText.getText().toString();




        // hasContents();
    }

    public void getPhoneState() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                userIMEI = tm.getImei();
            } else {
                userIMEI = tm.getDeviceId();
            }
            //userPhoneNumber = tm.getLine1Number();
            userPhoneNumber = "01067373845"; //임의로 설정(USIM 없어서)

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
                                        btn_send.setText("수정");



                                    } else { // 조회된 컨텐츠가 없는 경우
                                        Log.i(Global.TAG, "contents not exist.");

                                        // TODO: 새로 컨텐츠를 등록할 수 있게 띄우고, 등록 버튼으로 변경
                                        btn_send.setText("등록");


                                    }

                                    //reset 버튼 누른경우????
                                    
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
}

//추가할 것
//파일 탐색해서 사진 또는 동영상 올리는 기능 추가해야함.
//사진 또는 동영상 보내는 기능 추가해야함