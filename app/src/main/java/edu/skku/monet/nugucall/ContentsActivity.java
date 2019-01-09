package edu.skku.monet.nugucall;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
    // 문서 제공자 검색하기 위한 CODE
    public static final int READ_REQUEST_CODE = 42;
    String userName = "", userText = "", userSource = "";
    // 폰 정보 불러온 값을 저장할 변수
    String userIMEI = "", userPhoneNumber = "";
    // 서버에 보낼 값, 폰 정보 불러온 값을 보여줄 TextView
    EditText textName, textText;
    TextView textPhoneNumber, textIMEI, textSource;
    Button btn_send, btn_reset, btn_delete, btn_fileUpload;

    //btn_send가 등록인지 수정인지 알기위해 (등록:0, 수정:1)
    int btn_check = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        textName = (EditText) findViewById(R.id.textName);
        textText = (EditText) findViewById(R.id.textText);
        textSource = (TextView) findViewById(R.id.textSource);
        textPhoneNumber = (TextView) findViewById(R.id.textPhoneNumber);
        textIMEI = (TextView) findViewById(R.id.textIMEI);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_delete = (Button) findViewById(R.id.btn_delete);
        btn_fileUpload = (Button) findViewById(R.id.btn_fileUpload);

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

                if (textName.getText().toString().equals("") || textText.getText().toString().equals("") || textSource.getText().toString().equals("")) {
                    Toast.makeText(ContentsActivity.this, "모든 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    // 고객이 입력한 값 저장
                    userName = textName.getText().toString();
                    userText = textText.getText().toString();
                    userSource = textSource.getText().toString();

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
            userPhoneNumber = "+821067373845"; //임의로 설정(USIM 없어서)
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
                                        String text = json.getString("text");
                                        String source = json.getString("source");
                                        String imei = json.getString("imei");

                                        // TODO: 이미 등록된 컨텐츠 정보를 띄우고, 수정 버튼으로 변경
                                        btn_check = 1;
                                        btn_send.setText("수정");
                                        textName.setText(name);
                                        textText.setText(text);
                                        textSource.setText(source);

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
            parameter.put("text", userText);
            parameter.put("source", userSource);
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
                parameter.put("text", userText);
                parameter.put("source", userSource);
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

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    // 문서 제공자 검색을 위한 함수2 (결과처리)
    // 사용자가 선택기에서 문서를 선택하면 onActivityResult()가 호출됩니다. 선택한 문서를 가리키는 URI는 resultData 매개변수 안에 들어 있습니다.
    // 이 URI를 getData()를 사용하여 추출합니다. 일단 이것을 가지게 되면 이를 사용하여 사용자가 원하는 문서를 검색하면 됩니다
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(Global.TAG,"Uri: " + uri.getPath());
                Log.i(Global.TAG,"Path: " + getPath(getApplicationContext(), uri));

                // filePath 얻어서 파일명.확장자 고객화면에 보여주기
                String filePath = getPath(getApplicationContext(), uri);
                String[] splitFilePath = filePath.split("/");
                userSource = splitFilePath[splitFilePath.length-1];
                textSource.setText(userSource);
            }
        }
    }

    // URI에서 file path 가져오기 관련코드 1
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    // URI에서 file path 가져오기 관련코드 2
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    // URI에서 file path 가져오기 관련코드 3
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    // URI에서 file path 가져오기 관련코드 4
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    // URI에서 file path 가져오기 관련코드 5
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


}




//추가할 것
//파일 탐색해서 사진 또는 동영상 올리는 기능 추가해야함.
//사진 또는 동영상 보내는 기능 추가해야함