package com.android.garbageproject;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.android.garbageproject.ui.apicall.GetThings;


public class ListThingsActivity extends AppCompatActivity {
    final static String TAG = "Garbage_can";

    String urlStr1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_things);
        urlStr1 = "https://ezun10aqeh.execute-api.ap-southeast-2.amazonaws.com/pro/devices";

        Intent intent = getIntent();
        String url = intent.getStringExtra("urlStr1");
        if (url == null) {
            url = "https://ezun10aqeh.execute-api.ap-southeast-2.amazonaws.com/pro/devices";
        }

        new GetThings(ListThingsActivity.this, url).execute();


    }
}
