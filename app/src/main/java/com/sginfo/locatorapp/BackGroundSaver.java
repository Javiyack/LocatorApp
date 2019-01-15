package com.sginfo.locatorapp;

import android.content.Context;
import android.location.Location;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class BackGroundSaver implements FetchAddressTask.OnTaskCompleted {

    private boolean online = false;
    private Context context;
    private Location location;
    private String address;
    private boolean foregroundService=false;

    public void setForegroundService(boolean foregroundService) {
        this.foregroundService = foregroundService;
    }

    private String URL;

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public BackGroundSaver() {
    }

    public BackGroundSaver(boolean online) {
        this.online = online;
    }

    @Override
    public void onTaskCompleted(String result) {
        setAddress(result);
        if (online){
            try {
                HttpCall httpCallPost = new HttpCall();
                httpCallPost.setMethodtype(HttpCall.POST);
                httpCallPost.setUrl(URL);
                HashMap<String, String> paramsPost = new HashMap<>();
                paramsPost.put("address", result);
                paramsPost.put("velocidadX", "" + location.getBearing());
                paramsPost.put("velocidadY", "" + location.getAltitude());
                paramsPost.put("velocidadZ", "" + location.getAccuracy());
                paramsPost.put("modulo", "" + location.getSpeed());
                paramsPost.put("latitude", "" + location.getLatitude());
                paramsPost.put("longitude", "" + location.getLongitude());
                paramsPost.put("token", MainActivity.token);
                httpCallPost.setParams(paramsPost);
                String response = new HttpRequest() {

                }.execute(httpCallPost).get();
                System.out.println("Response: " + response);
                Toast toast;
                if (response != null) {

                    //toast msg
                    switch (response) {
                        case "msg.save.ok":
                            toast = Toast.makeText(context, R.string.msg_save_ok,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        case "msg.save.failed":
                            toast = Toast.makeText(context, R.string.msg_save_failed,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        default:
                            toast = Toast.makeText(context, R.string.msg_unknown_server_response,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                    }
                } else {
                    toast = Toast.makeText(context, R.string.msg_login_error,
                            Toast.LENGTH_LONG);
                    toast.show();
                }
                if(foregroundService)
                toast.show();
            } catch (Throwable oops) {
                Toast toast = Toast.makeText(context, R.string.msg_http_error,
                        Toast.LENGTH_LONG);
                String displayedText = ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).getText().toString();
                toast.setText(displayedText + ": " + oops.getLocalizedMessage());
                if(foregroundService)
                    toast.show();
            }
        }

    }

    public void save(Location location) {
        new FetchAddressTask(context, BackGroundSaver.this)
                .execute(location);
    }
}
