package com.android.garbageproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.garbageproject.ui.apicall.GetLog;
import com.android.garbageproject.ui.apicall.UpdateShadow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class AllActivity extends AppCompatActivity {
    final static String TAG = "Garbage_can";

    String urlStr1;
    String urlStr2;

    private TextView textView_Date1;
    private TextView textView_Date2;
    private DatePickerDialog.OnDateSetListener callbackMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);

        urlStr1 = "https://ezun10aqeh.execute-api.ap-southeast-2.amazonaws.com/pro/devices/Garbage_can";
        urlStr2 = "https://ezun10aqeh.execute-api.ap-southeast-2.amazonaws.com/pro/devices/Garbage_can/log";

        Button startDateBtn = findViewById(R.id.start_date_button);
        startDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callbackMethod = new DatePickerDialog.OnDateSetListener()
                {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                    {
                        textView_Date1 = (TextView)findViewById(R.id.textView_date1);
                        textView_Date1.setText(String.format("%d-%d-%d ", year ,monthOfYear+1,dayOfMonth));
                    }
                };

                DatePickerDialog dialog = new DatePickerDialog(AllActivity.this, callbackMethod, 2023, 12, 0);

                dialog.show();


            }
        });

        Button startTimeBtn = findViewById(R.id.start_time_button);
        startTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        TextView textView_Time1 = (TextView)findViewById(R.id.textView_time1);
                        textView_Time1.setText(String.format("%d:%d", hourOfDay, minute));
                    }
                };

                TimePickerDialog dialog = new TimePickerDialog(AllActivity.this, listener, 0, 0, false);
                dialog.show();

            }
        });


        Button endDateBtn = findViewById(R.id.end_date_button);
        endDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callbackMethod = new DatePickerDialog.OnDateSetListener()
                {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                    {
                        textView_Date2 = (TextView)findViewById(R.id.textView_date2);
                        textView_Date2.setText(String.format("%d-%d-%d ", year ,monthOfYear+1,dayOfMonth));
                    }
                };

                DatePickerDialog dialog = new DatePickerDialog(AllActivity.this, callbackMethod, 2023, 12, 0);

                dialog.show();


            }
        });

        Button endTimeBtn = findViewById(R.id.end_time_button);
        endTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        TextView textView_Time2 = (TextView)findViewById(R.id.textView_time2);
                        textView_Time2.setText(String.format("%d:%d", hourOfDay, minute));
                    }
                };

                TimePickerDialog dialog = new TimePickerDialog(AllActivity.this, listener, 0, 0, false);
                dialog.show();

            }
        });

        Button start = findViewById(R.id.log_start_button);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetLog(AllActivity.this,urlStr2).execute();
            }
        });

        Button onBtn = findViewById(R.id.onBtn);
        onBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();

                try {
                    JSONArray jsonArray = new JSONArray();

                    String ledbuzzer_input = "ON";
                    if (ledbuzzer_input != null && !ledbuzzer_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "LED");
                        tag2.put("tagValue", ledbuzzer_input);

                        jsonArray.put(tag2);
                    }

                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(AllActivity.this,urlStr1).execute(payload);
                else
                    Toast.makeText(AllActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });

        Button offBtn = findViewById(R.id.offBtn);
        offBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();

                try {
                    JSONArray jsonArray = new JSONArray();

                    String ledbuzzer_input = "OFF";
                    if (ledbuzzer_input != null && !ledbuzzer_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "LED");
                        tag2.put("tagValue", ledbuzzer_input);

                        jsonArray.put(tag2);
                    }

                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(AllActivity.this,urlStr1).execute(payload);
                else
                    Toast.makeText(AllActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });


    }
}