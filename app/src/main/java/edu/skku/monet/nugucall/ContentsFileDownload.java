package edu.skku.monet.nugucall;
/*
    2019.01.14
    by 유병호
    소켓 통신으로 이미지 또는 동영상 서버에서 받기
*/

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

class ContentsFileDownload {

    private ThreadReceive threadReceive;
    private String path;
    private String size;

    // 생성자
    ContentsFileDownload(ThreadReceive threadReceive, String path, String size) {
        this.threadReceive = threadReceive;
        this.path = path;
        this.size = size;
    }

    void fileDownload() {
        FileDownloadThread fileDownloadThread = new FileDownloadThread();
        fileDownloadThread.start();
    }

    // 파일 업로드 스레드
    private class FileDownloadThread extends Thread {
        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(Global.SERVER_IP, Global.SERVER_DOWNLOAD_PORT);
                socket.connect(socketAddress, 3000);

                // 서버로부터 문자열(파일 이름) 받기 위해서 수신 기능 씀
                // --- 모든 수신
                InputStream inputStream = socket.getInputStream();
                // 문자열 수신 기능
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                // 문자열 속도 개선
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                // 서버에 문자열(파일 이름) 보내기 위해서 발신 기능 씀
                // --- 모든 발신
                OutputStream outputStream = socket.getOutputStream();
                // 문자열 발신 기능
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                // 문자열 속도 개선
                PrintWriter printWriter = new PrintWriter(outputStreamWriter);

                // Byte(파일) 수신 기능 (+속도개선)
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                // 파일 선언
                File file = new File(path);
                // 파일 쓰는 기능
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                // 파일 쓰는 기능 (+속도 개선)
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                String fileName = file.getName();
                long fileSize = Long.parseLong(size);

                // 1. 보내려는 파일 이름과 파일 크기 JSONObject에 담아서 PrintWriter(문자열)로 서버에 보내기, 보내준 거(printWriter) flush 해주기
                JSONObject putMessage = new JSONObject();
                putMessage.put("fileName", fileName);
                putMessage.put("fileSize", fileSize);
                printWriter.print(putMessage.toString());
                printWriter.flush();

                // 2. BufferedInputStream을 통해 서버에서 파일 데이터를 수신함과 동시에 BufferedOutPutStream을 통해 파일을 작성
                // 파일 작성 (bufferedOutputStream) flush 해주기
                byte[] buffer = new byte[65536];
                long check = 0;
                while (check != fileSize) {
                    int length = bufferedInputStream.read(buffer);
                    bufferedOutputStream.write(buffer, 0, length);
                    check += length;
                }
                bufferedOutputStream.flush();
                Log.i(Global.TAG, "File Download Completed.");

                // close
                try {
                    bufferedOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    bufferedInputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    printWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // BackgroundService onReceive 함수 호출 (컨텐츠 보여주는 기능)
                threadReceive.onReceiveRun(fileName, fileSize);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}