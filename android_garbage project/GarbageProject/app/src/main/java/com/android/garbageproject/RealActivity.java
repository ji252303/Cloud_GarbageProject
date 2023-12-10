package com.android.garbageproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.garbageproject.ui.apicall.GetThingShadow;
import com.android.garbageproject.ui.apicall.UpdateShadow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;


public class RealActivity extends AppCompatActivity {
    final static String TAG = "Garbage_can";
    String urlStr1;
    Timer timer;
    private TextView budgetTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real);

        urlStr1 = "https://ezun10aqeh.execute-api.ap-southeast-2.amazonaws.com/pro/devices/Garbage_can";
        budgetTextView = findViewById(R.id.budgetTextView);

        timer = new Timer();
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               new GetThingShadow(RealActivity.this, urlStr1).execute();
                           }
                       },
                0, 2000);

        // 이 부분에 추가 코드를 넣어주세요
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showWeightMessage(0.23); // 여기에 음식물 쓰레기 무게를 전달
                    }
                });
            }
        }).start();

        Button onBtn = findViewById(R.id.onBtn);
        onBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();

                try {
                    JSONArray jsonArray = new JSONArray();
                    String led_input = "ON";
                    if (led_input != null && !led_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "LED");
                        tag2.put("tagValue", led_input);

                        jsonArray.put(tag2);
                    }

                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(RealActivity.this,urlStr1).execute(payload);
                else
                    Toast.makeText(RealActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });

        Button offBtn = findViewById(R.id.offBtn);
        offBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();
                try {
                    JSONArray jsonArray = new JSONArray();
                    String led_input = "OFF";
                    if (led_input != null && !led_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "LED");
                        tag2.put("tagValue", led_input);

                        jsonArray.put(tag2);
                    }

                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(RealActivity.this,urlStr1).execute(payload);
                else
                    Toast.makeText(RealActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showWeightMessage(double weightValue) {
        // 7.5kg 중에 배출된 무게에 따라 메시지 표시
        final double averageWeight = 7.5; // Declare 'averageWeight' as final
        final String message; // Declare 'message' as final

        if (weightValue >= averageWeight) {
            message = "한달 평균 " + averageWeight + "kg 중에 그 무게 이상이 배출되었습니다.";
        } else {
            message = "한달 평균 " + averageWeight + "kg 중에 적은 양의 음식물 쓰레기가 배출되었습니다.";
        }

        final double costPerKg = 130; // 1kg당 예산 가격
        final double Budget = weightValue * costPerKg;
        final int myInt = (int) Budget;

        // UI 업데이트를 위해 runOnUiThread 사용
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TextView 등을 활용하여 메시지를 표시하는 부분
                Toast.makeText(RealActivity.this, message, Toast.LENGTH_SHORT).show();
                budgetTextView.setText("현재 무게의 예산 가격: ");
            }
        });
    }
}
