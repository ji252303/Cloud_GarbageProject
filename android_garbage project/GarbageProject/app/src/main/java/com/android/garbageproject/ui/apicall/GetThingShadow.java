package com.android.garbageproject.ui.apicall;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.garbageproject.httpconnection.GetRequest;
import com.android.garbageproject.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GetThingShadow extends GetRequest {
    final static String TAG = "Garbage_can";
    String urlStr;

    public GetThingShadow(Activity activity, String urlStr) {
        super(activity);
        this.urlStr = urlStr;
    }

    @Override
    protected void onPreExecute() {
        try {
            Log.e(TAG, urlStr);
            url = new URL(urlStr);

        } catch (MalformedURLException e) {
            Toast.makeText(activity,"URL is invalid:"+urlStr, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            activity.finish();
        }
    }

    @Override
    public void onPostExecute(String jsonString) {
        if (jsonString == null)
            return ;
        Map<String, String> state = getStateFromJSONString(jsonString);

        TextView reported_ledTV = activity.findViewById(R.id.reported_led);
        TextView reported_weightTV = activity.findViewById(R.id.reported_weight);

        reported_weightTV.setText(state.get("reported_weight"));
        reported_ledTV.setText(state.get("reported_LED"));

    }

    protected Map<String, String> getStateFromJSONString(String jsonString) {
        Map<String, String> output = new HashMap<>();
        try {
            // 처음 double-quote와 마지막 double-quote 제거
            jsonString = jsonString.substring(1,jsonString.length()-1);
            // \\\" 를 \"로 치환
            jsonString = jsonString.replace("\\\"","\"");
            Log.i(TAG, "jsonString="+jsonString);
            JSONObject root = new JSONObject(jsonString);
            JSONObject state = root.getJSONObject("state");
            JSONObject reported = state.getJSONObject("reported");
            String weightValue = reported.getString("weight");

            String ledValue = reported.getString("LED");
            output.put("reported_weight", weightValue);

            output.put("reported_LED",ledValue);
        } catch (JSONException e) {
            Log.e(TAG, "Exception in processing JSONString.", e);
            e.printStackTrace();
        }
        return output;
    }
}
