package com.dui.hospitalscanner;

import androidx.appcompat.app.AppCompatActivity;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dui.hospitalscanner.helper.AppConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {
TextView name,id,medicineLevel,pulseRate;
String sessionId;
    public String responseMessage = new String();

    public Activity self = this;
    public Context context = DashboardActivity.this;
    Jedis jedis = new Jedis(AppConstants.DUI_CORE_HOST);
    JedisPubSub jedisPubSub;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        name = findViewById(R.id.tvPatientName);
        id = findViewById(R.id.tvPatientId);
        medicineLevel = findViewById(R.id.tvMedicineLevel);
        pulseRate = findViewById(R.id.tvPulseRate);
        sessionId = getIntent().getExtras().getString(AppConstants.SESSION_ID);

        String url = AppConstants.DUI_CORE_URL + "api/v1/session/" +sessionId;
        RequestQueue queue = Volley.newRequestQueue(DashboardActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject data = (JSONObject) response.get("data");
                            JSONObject session = (JSONObject) data.get("session");
                            JSONObject profile = (JSONObject) session.get("profile");
                            name.setText(profile.getString("tvPatientName"));
                            id.setText(profile.getString("tvPatientId"));
                            medicineLevel.setText(profile.getString("tvMedicineLevel"));
                            pulseRate.setText(profile.getString("tvPulseRate"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error);

                    }
                });

        queue.add(jsonObjectRequest);
        new RedisHelper().execute();

    }
    public class RedisHelper extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        @Override
        protected void onPostExecute(String s) {
            System.out.println("In post execute Home activity: " + s);
            if (responseMessage.length() > 0) {
                final String arr[] = responseMessage.split("::");
                System.out.println("Message string" + arr[1]);
                try {
                    switch (arr[0]) {
                        case "INTEGER":
                            final TextView ed = findViewById(context.getResources().getIdentifier(arr[1], "id", context.getPackageName()));
                            if (ed != null) {
                                DashboardActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ed.setText(arr[2]);
                                    }
                                });
                            }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    new RedisHelper().execute();
                }
            }
        }

        @Override
        protected String doInBackground(Void... strings) {
            if(android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();
            try  {

                jedis.connect();
                jedisPubSub = new JedisPubSub() {

                    @Override
                    public void onMessage(String channel, String message) {
                        // TODO Auto-generated method stub
                        responseMessage = message;
                        System.out.println("Message : " + responseMessage);
                        onPostExecute(responseMessage);
                    }

                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        // TODO Auto-generated method stub
                        System.out.println("Client is Subscribed to channel Home activity: "+ channel);
                    }

                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        // TODO Auto-generated method stub
                        super.onUnsubscribe(channel, subscribedChannels);
                        System.out.println("Channel unsubscribed at Home activity: " + channel);
                    }

                };
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(DashboardActivity.this);
//                String sessionId = sharedPreferences.getString(AppConstants.SESSION_ID, null);
                if (sessionId != null) {
                    jedis.subscribe(jedisPubSub,sessionId);
                    System.out.println("Channel Subscribed at Home activity: " + sessionId);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}