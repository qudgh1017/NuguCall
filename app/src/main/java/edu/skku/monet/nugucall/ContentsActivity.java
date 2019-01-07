package edu.skku.monet.nugucall;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

public class ContentsActivity extends AppCompatActivity {

    String test="1";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        hasContents();
    }

    public void hasContents() {
        Log.i(Global.TAG, "hasContents() invoked.");
        try {
            String address = "contents/select_my_contents.jsp"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("imei", "1234567890"); // 매개변수, 값

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
                                    } else { // 조회된 컨텐츠가 없는 경우
                                        Log.i(Global.TAG, "contents not exist.");

                                        // TODO: 새로 컨텐츠를 등록할 수 있게 띄우고, 등록 버튼으로 변경
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
}