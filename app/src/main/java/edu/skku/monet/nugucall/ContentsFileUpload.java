package edu.skku.monet.nugucall;
/*
    2019.01.09
    by 유병호
    소켓 통신으로 이미지 또는 동영상 서버에 올리기
*/
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

public class ContentsFileUpload {

    private Socket socket; // 연결
    private BufferedReader bufferedReader; // 문자열 수신
    private PrintWriter printWriter; // 문자열 발신
    private BufferedOutputStream bufferedOutputStream; // 바이트 발신
    private BufferedInputStream bufferedInputStream; //바이트 수신
    private String filePath;

    ContentsFileUpload(String filePath){
        this.filePath = filePath;
    }

    public void connect() {
        ConnectThread connectThread = new ConnectThread();
        connectThread.start();
    }

    //connect 이름 바꾸기 fileUploadThread~~~~~~~~~~~~~~~~~~~~~~~
    private class ConnectThread extends Thread {
        @Override
        public void run() {
            try {
                socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(Global.SERVER_IP, Global.SERVER_UPLOAD_PORT);
                socket.connect(socketAddress, 3000);

                // 서버로부터 문자열(파일 이름) 받기위해서 수신기능 씀
                // --- 모든 수신
                InputStream inputStream = socket.getInputStream();
                // 문자열 수신 기능
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                // 문자열 속도 개선
                bufferedReader = new BufferedReader(inputStreamReader);

                // --- 모든 발신
                OutputStream outputStream = socket.getOutputStream();
                // 문자열 발신기능
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                // 문자열 속도 개선
                printWriter = new PrintWriter(outputStreamWriter);

                // Byte(파일) 발신기능(+속도개선)
                bufferedOutputStream = new BufferedOutputStream(outputStream);

                // 파일 선언
                File file = new File(filePath);
                // 파일 읽는 기능
                FileInputStream fileInputStream = new FileInputStream(file);
                // 파일 읽는 기능을 속도 개선
                bufferedInputStream = new BufferedInputStream(fileInputStream);
                // Byte(파일) 수신기능(+속도개선)
                // bufferedInputStream = new BufferedInputStream(inputStream);

                file.getName();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
