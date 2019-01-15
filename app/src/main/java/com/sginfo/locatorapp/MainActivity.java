package com.sginfo.locatorapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    // Unique tag required for the intent extra
    public static final String TOKEN_MESSAGE
            = "com.sginfo.android.locator.token.MESSAGE";
    public static final String EXTRA_TOKEN               = "EXTRA_TOKEN";
    public static final String EXTRA_USERNAME            = "EXTRA_USERNAME";
    public static final String EXTRA_HARDWARE_ID         = "EXTRA_HARDWARE_ID";
    public static final String EXTRA_MODE        = "EXTRA_MODE";
    public static final String EXTRA_ONLINE        = "EXTRA_ONLINE";
    // Unique tag for the intent reply
    public static final int TEXT_REQUEST = 1;
    private int mCount = 0;

    private String android_id;
    public static String token;
    private TextView tUsername;

    private TextView iUsername;
    private TextView iPasword;

    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public void startTrackingActivity(View view) {
        Log.d(LOG_TAG, "Backdoor used!");
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        }else{
            startLocatorActivity();
        }
    }
        public void checkCredentials(View view) {

        iUsername = (TextView) findViewById(R.id.username);
        iPasword = (TextView) findViewById(R.id.password);
        Log.d("Android", "Android ID : " + android_id);
        try {
            HttpCall httpCallPost = new HttpCall();
            httpCallPost.setMethodtype(HttpCall.POST);
            httpCallPost.setUrl("http://sginfo.ddns.net:8050/Acme-CRM/webservice/checkCredentials.do");
            HashMap<String, String> paramsPost = new HashMap<>();
            paramsPost.put("username", iUsername.getText().toString());
            paramsPost.put("password", iPasword.getText().toString());
            paramsPost.put("hardwareId", android_id);
            httpCallPost.setParams(paramsPost);
            token = new HttpRequest() {

            }.execute(httpCallPost).get();
            System.out.println("token: " + token);
            if (token != null) {
                if (token.startsWith("TOKEN-")) {
                    // go to nex activity
                    if(requestPermissions()){
                        username = iUsername.getText().toString();
                        startLocatorActivity();
                    }
                    Log.d(LOG_TAG, "Login successfull!");
                } else if (token.startsWith("msg.")) {
                    //toast msg
                    Toast toast;
                    switch (token) {
                        case "msg.wrong.username":
                            toast = Toast.makeText(this, R.string.msg_wrong_username,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        case "msg.wrong.password":
                            toast = Toast.makeText(this, R.string.msg_wrong_password,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        case "msg.wrong.hardwareId":
                            toast = Toast.makeText(this, R.string.msg_wrong_hardwareId,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        default:
                            toast = Toast.makeText(this, R.string.msg_login_error,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                    }
                } else {
                    Toast toast = Toast.makeText(this, R.string.msg_server_conection_failed,
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            } else {
                Toast toast = Toast.makeText(this, R.string.msg_login_error,
                        Toast.LENGTH_LONG);
                toast.show();
            }
        } catch (Throwable oops) {
            Toast toast = Toast.makeText(this, R.string.msg_login_error,
                    Toast.LENGTH_LONG);
            String displayedText = ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).getText().toString();
            toast.setText(displayedText + ": " + oops.getLocalizedMessage());
            toast.show();
        }


    }

    public boolean requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            return true;
        }
        return  false;
    }

    public String getUsename() {
        return iUsername.getText().toString();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(LOG_TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                startLocatorActivity();

            } else {
                // Permission denied.
                Snackbar.make(
                        findViewById(R.id.activity_locator),
                        R.string.no_location,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    private void startLocatorActivity(){
        Intent intent = new Intent(this, LocatorActivity.class);
        intent.putExtra(EXTRA_TOKEN, (token!=null)?token:"none");
        intent.putExtra(EXTRA_USERNAME, (username!=null)?username:"annonymous");
        intent.putExtra(EXTRA_HARDWARE_ID, android_id);
        intent.putExtra(EXTRA_MODE, (token!=null)?"online":"offline");
        startActivity(intent);
    }

}
