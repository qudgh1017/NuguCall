package edu.skku.monet.nugucall;
/*
    2019.01.12
    by 유병호
    컨텐츠 등록, 수정, 삭제, 초기화, 파일첨부, 문서검색 기능
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class ContentsActivity extends AppCompatActivity {

    private Handler handler;

    String userName = "", userText = "", userSource = "", userSize = "";
    // 폰 정보 불러온 값을 저장할 변수
    String userIMEI = "", userPhoneNumber = "";
    String filePath = "";
    // 서버에 보낼 값, 폰 정보 불러온 값을 보여줄 TextView
    EditText textName, textText;
    TextView textPhoneNumber, textIMEI, textSource;
    Button btn_send, btn_reset, btn_delete, btn_fileUpload, btn_PreviewActivity;

    // btn_send가 등록인지 수정인지 알기위해 (등록:0, 수정:1)
    int btn_check = 0;

    private ThreadReceive uploadThreadReceive = new ThreadReceive() {
        @Override
        public void onReceiveRun(String fileName, long fileSize) {
            userSource = fileName;
            userSize = String.valueOf(fileSize);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    textSource.setText(userSource);
                }
            });
            // contents 업로드 성공하면 등록 또는 수정
            if (btn_check == 0) { // 등록 버튼일 경우
                insertContents();
            } else if (btn_check == 1) { // 수정 버튼일 경우
                updateContents();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        handler = new Handler(getMainLooper());

        textName = findViewById(R.id.textName);
        textText = findViewById(R.id.textText);
        textSource = findViewById(R.id.textSource);
        textPhoneNumber = findViewById(R.id.textPhoneNumber);
        textIMEI = findViewById(R.id.textIMEI);
        btn_send = findViewById(R.id.btn_send);
        btn_reset = findViewById(R.id.btn_reset);
        btn_delete = findViewById(R.id.btn_delete);
        btn_fileUpload = findViewById(R.id.btn_fileUpload);
        btn_PreviewActivity = findViewById(R.id.btn_PreviewActivity);

        // 폰 정보 불러오기 (userIMEI, userPhoneNumber)
        getUserPhoneInformation();

        // 등록, 수정 버튼 누른경우 insert
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textName.getText().toString().equals("") || textText.getText().toString().equals("") || textSource.getText().toString().equals("")) {
                    Toast.makeText(ContentsActivity.this, "모든 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    // 고객이 입력한 값 저장
                    userName = textName.getText().toString();
                    userText = textText.getText().toString();
                    userSource = textSource.getText().toString();

                    // contents 업로드할 때 쓰는 contentsFileUpload 클래스 생성
                    // 생성자에 threadReceive 인터페이스를 변수로 보냄
                    ContentsFileUpload contentsFileUpload = new ContentsFileUpload(uploadThreadReceive, filePath);

                    // ContentsDB 등록하기 전 먼저 파일을 서버에 보내기, fileUpload 함수에서 실행하는 fileUploadThread에서 서버와 데이터를 주고받은 후
                    // insertContents()와 updateContents()함수를 실행하는 threadReceive.onReceiveRun(message)를 실행
                    // => fileUploadThread와 메인스레드가 동시에 작업을 하므로 파일이 업로드되기 전 등록 또는 수정이 될 수 있어서
                    // 순차적으로 작업을 하기위해 onReceive 스레드함수에 메인스레드에서 실행할 작업을 정의하고 호출함
                    contentsFileUpload.fileUpload();

                }
            }
        });

        // 초기화 버튼 누른경우 초기화
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textName.setText("");
                textText.setText("");
                textSource.setText("");
            }
        });

        // 삭제 버튼 누른 경우 finish()
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteContents();
            }
        });

        // 파일첨부 버튼 누른 경우 문서 제공자 함수 호출
        btn_fileUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }
        });

        // 미리보기 버튼 누른 경우
        btn_PreviewActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), PreviewActivity.class);
                startActivity(intent);
            }
        });
    }

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
            userPhoneNumber = (tm.getLine1Number()).replace("+82", "0");

            textIMEI.setText(userIMEI);
            textPhoneNumber.setText(userPhoneNumber);

            // 처음 컨텐츠 등록된 상태인지 조회하기 위해
            hasContents();

        } catch (Exception e) { // 권한 오류로 인한 경우 catch
            e.printStackTrace();
        }
    }

    public void hasContents() {
        Log.i(Global.TAG, "hasContents() invoked.");

        try {
            String address = "select_my_contents"; // 통신할 JSP 주소
            // select_my_contents에서 IMEI로 정보 조회
            JSONObject parameter = new JSONObject();
            parameter.put("imei", userIMEI); // 매개변수, 값

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject jsonObject = new JSONObject(out);
                            String result = jsonObject.getString("result"); // 반환 값

                            switch (result) {
                                case "1": // JSP - DB 통신 성공
                                    JSONArray jsonArray = jsonObject.getJSONArray("items");
                                    if (jsonArray.length() > 0) {
                                        // String id = jsonArray.getJSONObject(0).getString("id");
                                        String name = jsonArray.getJSONObject(0).getString("name");
                                        // String phone = jsonArray.getJSONObject(0).getString("phone");
                                        String text = jsonArray.getJSONObject(0).getString("text");
                                        String source = jsonArray.getJSONObject(0).getString("source");
                                        // String size = jsonArray.getJSONObject(0).getString("size");
                                        // String imei = jsonArray.getJSONObject(0).getString("imei");

                                        // 이미 등록된 컨텐츠 정보를 띄우고, 수정 버튼으로 변경
                                        btn_check = 1;
                                        btn_send.setText("수정");
                                        textName.setText(name);
                                        textText.setText(text);
                                        textSource.setText(source);
                                    } else {
                                        // 새로 컨텐츠를 등록할 수 있게 띄우고, 등록 버튼으로 변경
                                        btn_check = 0;
                                        btn_send.setText("등록");
                                        btn_delete.setVisibility(View.GONE);
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

    public void insertContents() {
        Log.i(Global.TAG, "insertContents() invoked.");

        try {
            String address = "insert_my_contents"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("name", userName);
            parameter.put("phone", userPhoneNumber);
            parameter.put("text", userText);
            parameter.put("source", userSource);
            parameter.put("size", userSize);
            parameter.put("imei", userIMEI);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {
                                case "1": // JSP - DB 통신 성공
                                    Toast.makeText(getApplicationContext(), "컨텐츠가 등록되었습니다.", Toast.LENGTH_SHORT).show();

                                    // 등록되면 수정으로 바꿔주고 삭제버튼 보이게
                                    btn_check = 1;
                                    btn_send.setText("수정");
                                    btn_delete.setVisibility(View.VISIBLE);
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

    public void updateContents() {
        Log.i(Global.TAG, "updateContents() invoked.");

        try {
            String address = "update_my_contents"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("name", userName);
            parameter.put("phone", userPhoneNumber);
            parameter.put("text", userText);
            parameter.put("source", userSource);
            parameter.put("size", userSize);
            parameter.put("imei", userIMEI);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {
                                case "1": // JSP - DB 통신 성공
                                    Toast.makeText(getApplicationContext(), "컨텐츠가 수정되었습니다.", Toast.LENGTH_SHORT).show();
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

    public void deleteContents() {
        Log.i(Global.TAG, "deleteContents() invoked.");

        try {
            String address = "delete_my_contents"; // 통신할 JSP 주소

            JSONObject parameter = new JSONObject();
            parameter.put("imei", userIMEI);

            CommunicateDB communicateDB = new CommunicateDB(address, parameter, new CallbackDB() {
                @Override
                public void callback(String out) {
                    try {
                        if (out != null) { // 안드로이드 - JSP 통신 성공
                            JSONObject json = new JSONObject(out);
                            String result = json.getString("result");

                            switch (result) {
                                case "1": // JSP - DB 통신 성공
                                    Toast.makeText(getApplicationContext(), "컨텐츠가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    btn_check = 0;
                                    finish();
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

    // 문서 제공자 검색을 위한 함수1 (안드로이드 Developers의 Core topics - App data & files - Content Providers - Open files using storage access framework)
    public void performFileSearch() {
        // 앱이 ACTION_OPEN_DOCUMENT 인텐트를 실행시키면 이는 일치하는 문서 제공자를 모두 표시하는 선택기를 시작합니다.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // CATEGORY_OPENABLE 카테고리를 인텐트에 추가하면 결과를 필터하여 이미지 파일 등 열 수 있는 문서만 표시합니다.
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // intent.setType("image/*") 문으로 한층 더 필터링을 수행하여 MIME 데이터 유형이 이미지인 문서만 표시하도록 합니다.
        // image, video 파일만 표시하도록
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});

        startActivityForResult(intent, Global.REQ_CODE_FILE_SELECT);
    }

    // 문서 제공자 검색을 위한 함수2 (결과처리)
    // 사용자가 선택기에서 문서를 선택하면 onActivityResult()가 호출됩니다. 선택한 문서를 가리키는 URI는 resultData 매개변수 안에 들어 있습니다.
    // 이 URI를 getData()를 사용하여 추출합니다. 일단 이것을 가지게 되면 이를 사용하여 사용자가 원하는 문서를 검색하면 됩니다
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == Global.REQ_CODE_FILE_SELECT && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                Uri uri = resultData.getData();
                if (uri != null) {
                    Log.i(Global.TAG, "컨텐츠 화면 - Uri: " + uri.getPath());
                    Log.i(Global.TAG, "컨텐츠 화면 - Path: " + ContentsFilePath.getPath(getApplicationContext(), uri));

                    // filePath
                    filePath = ContentsFilePath.getPath(getApplicationContext(), uri);

                    if (filePath != null) {
                        // filePath(/storage/emulated/0/Movies/)를 변경해서 파일명.확장자 고객화면에 보여주기
                        String[] splitFilePath = filePath.split("/");
                        userSource = splitFilePath[splitFilePath.length - 1];
                        textSource.setText(userSource);
                    }
                }
            }
        }
    }
}