package com.sginfo.locatorapp;

import android.content.Intent;
import android.provider.Settings;
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

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    // Unique tag required for the intent extra
    public static final String TOKEN_MESSAGE
            = "com.sginfo.android.locator.token.MESSAGE";
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
    }

    public void checkCredentials(View view) {

        iUsername = (TextView) findViewById(R.id.username);
        iPasword = (TextView) findViewById(R.id.password);
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
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
                    Log.d(LOG_TAG, "Button clicked!");
                    Intent intent = new Intent(this, TrackingActivity.class);
                    String message = token;
                    intent.putExtra(TOKEN_MESSAGE, message);
                    startActivityForResult(intent, TEXT_REQUEST);
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

    public void saveTracking(View view) {
        iUsername = (TextView) findViewById(R.id.username);
        iPasword = (TextView) findViewById(R.id.password);
        if (iUsername != null) {
            tUsername.setText(iUsername.getText());
            iUsername.setText("");
        }

        Toast toast = Toast.makeText(this, R.string.label_username,
                Toast.LENGTH_LONG);
        String displayedText = ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).getText().toString();
        toast.setText(displayedText + ": " + tUsername.getText());
        toast.show();

    }
}
