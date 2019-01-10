package edu.skku.monet.nugucall;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class CommunicateDB extends AsyncTask<Void, Void, String> {

    private String address; // 주소
    private JSONObject parameter; // JSON 매개변수
    private CallbackDB callbackDB; // 콜백함수

    CommunicateDB(String address, JSONObject parameter, CallbackDB callbackDB) {
        this.address = address;
        this.parameter = parameter;
        this.callbackDB = callbackDB;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(Global.SERVER_ADDRESS + address);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDefaultUseCaches(false);

            if (parameter != null) {
                // JSON을 매개변수로 전송
                OutputStream outputStream = httpURLConnection.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                bufferedWriter.write(parameter.toString(1));
                bufferedWriter.flush();
                bufferedWriter.close();
            }

            if (httpURLConnection.getResponseCode() == 200) {
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder output = new StringBuilder();
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    output.append(line);
                }
                return output.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String out) {
        super.onPostExecute(out);
        callbackDB.callback(out);
    }
}

// JSON을 URL 매개변수로 변경
// StringBuilder stringBuilder = new StringBuilder();
// Iterator<String> iterator = parameter.keys();
// while (iterator.hasNext()) {
// String key = iterator.next();
// String str = "&" + key + "=" + parameter.getString(key);
// stringBuilder.append(str);
// }