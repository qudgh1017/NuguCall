package edu.skku.monet.nugucall;
/*
    2019.01.09
    by 유병호
    소켓 통신으로 이미지 또는 동영상 서버에 올리기
*/

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

class ContentsFileUpload {

    private ThreadReceive threadReceive;
    private String filePath;

    // 생성자
    ContentsFileUpload(ThreadReceive threadReceive, String filePath) {
        this.threadReceive = threadReceive;
        this.filePath = filePath;
    }

    void fileUpload() {
        FileUploadThread fileUploadThread = new FileUploadThread();
        fileUploadThread.start();
    }

    // 파일 업로드 스레드
    private class FileUploadThread extends Thread {
        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(Global.SERVER_IP, Global.SERVER_UPLOAD_PORT);
                socket.connect(socketAddress, 3000);

                // 서버로부터 문자열(파일 이름) 받기 위해서 수신 기능 씀
                // --- 모든 수신
                InputStream inputStream = socket.getInputStream();
                // 문자열 수신 기능
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                // 문자열 속도 개선
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                // 서버에 문자열(파일 이름, 파일 크기) 보내기 위해서 발신 기능 씀
                // --- 모든 발신
                OutputStream outputStream = socket.getOutputStream();
                // 문자열 발신 기능
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                // 문자열 속도 개선
                PrintWriter printWriter = new PrintWriter(outputStreamWriter);

                // Byte(파일) 발신 기능 (+속도개선)
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                // 파일 선언
                File file = new File(filePath);
                // 파일 읽는 기능
                FileInputStream fileInputStream = new FileInputStream(file);
                // 파일 읽는 기능 (+속도 개선)
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

                String fileName = file.getName();
                long fileSize = file.length();

                // 1. 보내려는 파일 이름과 파일 크기 JSONObject에 담아서 PrintWriter(문자열)로 서버에 보내기, 보내준 거(printWriter) flush 해주기
                JSONObject parameter = new JSONObject();
                parameter.put("fileName", fileName);
                parameter.put("fileSize", fileSize);
                printWriter.print(parameter.toString());
                printWriter.flush();
                Log.i(Global.TAG, "파일 업로드 - fileName: " + fileName);
                Log.i(Global.TAG, "파일 업로드 - fileSize: " + fileSize);

                // 2. BufferedReader를 통해 서버에서 올 문자열 답변에 대기(readLine()) => 답변으로 "(년월일시분초).확장자" => ContentsDB의 source에 입력될 문자열
                String message = bufferedReader.readLine();
                Log.i(Global.TAG, "파일 업로드 - JSON message: " + message);
                JSONObject object = new JSONObject(message);
                message = object.getString("fileName");

                // 3. BufferedInputStream을 통해 파일을 읽음과 동시에 BufferedOutputStream을 통해 파일을 서버로 전송
                // 보내준 거(bufferedOutputStream) flush 해주기
                byte[] buffer = new byte[65536];
                long check = 0;
                while (check != fileSize) {
                    int length = bufferedInputStream.read(buffer);
                    bufferedOutputStream.write(buffer, 0, length);
                    check += length;
                }
                bufferedOutputStream.flush();

                Log.i(Global.TAG, "파일 업로드 완료");

                // close
                bufferedOutputStream.close();
                bufferedInputStream.close();
                bufferedReader.close();
                printWriter.close();
                socket.close();

                // ContentsActivity onReceive 함수 호출 (컨텐츠 등록 또는 수정하는 기능)
                threadReceive.onReceiveRun(message);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
