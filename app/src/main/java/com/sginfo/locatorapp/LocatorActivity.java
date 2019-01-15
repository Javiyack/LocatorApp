/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sginfo.locatorapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * LocatorActivity defines the second activity in the app. It is
 * launched from an intent with a message, and sends an intent
 * back with a second message.
 */
public class LocatorActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_TAG = LocatorActivity.class.getSimpleName();

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public static final String EXTRA_MODE = "extra.mode";

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;
    private boolean requestingUpdates = false;
    private boolean buttonEnabled = false;
    private boolean online = false;

    // UI elements.
    private Button uiRequestLocationUpdatesButton;
    private Button uiConfigButton;
    private Button uiKeyButton;
    private TextView uiTextViewUsername;
    private TextView uiTextViewHArdware;
    private TextView uiTextViewMode;
    private TextView uiTextViewAddress;
    private TextView uiTextViewToken;
    private TextView uiTextViewLocation;


    // App location
    private Location appLocation;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myReceiver = new MyReceiver();
        setContentView(R.layout.activity_locator);


    }

    @Override
    protected void onStart() {
        super.onStart();
// Setup view Buttons
        uiRequestLocationUpdatesButton = (Button) findViewById(R.id.powerButton);
        uiKeyButton = (Button) findViewById(R.id.keyButton);
        uiConfigButton = (Button) findViewById(R.id.configButton);


        // Setup view texts
        uiTextViewAddress = findViewById(R.id.textViewAddress);
        uiTextViewUsername = findViewById(R.id.textViewUsername);
        uiTextViewToken = findViewById(R.id.textViewToken);
        uiTextViewHArdware = findViewById(R.id.textViewHarwareID);
        uiTextViewMode = findViewById(R.id.textViewMode);
        uiTextViewLocation = findViewById(R.id.textViewLocation);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        uiRequestLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleService(view);

            }

        });

        String username = getIntent().getStringExtra(MainActivity.EXTRA_USERNAME);
        uiTextViewUsername.setText("User name: " + username);

        String token = getIntent().getStringExtra(MainActivity.EXTRA_TOKEN);
        uiTextViewToken.setText("Token assigned: " + token);

        String hardware = getIntent().getStringExtra(MainActivity.EXTRA_HARDWARE_ID);
        uiTextViewHArdware.setText("Harwarw ID: " + hardware);

        String mode = getIntent().getStringExtra(MainActivity.EXTRA_MODE);
        online = (mode != null && mode.equals("online")) ? true : false;
        uiTextViewMode.setText(mode);


        // Restore the state of the buttons when the activity (re)launches.
        setButtonsState(requestingUpdates);

        buttonEnabled = false;
        uiRequestLocationUpdatesButton.setEnabled(requestingUpdates);
        uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources()
                .getColor((requestingUpdates) ? R.color.red : R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
        startTimer();

        if (requestPermissionsLogic()) {
            Intent serviceIntent = new Intent(this, LocationUpdatesService.class);
            serviceIntent.putExtra(MainActivity.EXTRA_TOKEN, token);
            serviceIntent.putExtra(MainActivity.EXTRA_USERNAME, username);
            serviceIntent.putExtra(MainActivity.EXTRA_HARDWARE_ID, hardware);
            serviceIntent.putExtra(MainActivity.EXTRA_MODE, mode);
            serviceIntent.putExtra(MainActivity.EXTRA_ONLINE, online);
            bindService(serviceIntent, mServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }

        deactivateButtons();

    }

    private Timer timer;
    private TimerTask timerTask;
    private int counter = 0;
    long oldTime = 0;

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 5 second
        timer.schedule(timerTask, 150, 150); //
    }

    public void stopTimer() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        counter = 0;
        final Context context = this;
        timerTask = new TimerTask() {
            public void run() {
                counter++;
                if (requestingUpdates) {
                    buttonEnabled = true;
                    stopTimer();
                } else if (counter > 10) {
                    buttonEnabled = true;
                    stopTimer();
                }
            }
        };
    }

    public void activateButtons() {
        uiRequestLocationUpdatesButton.setEnabled(true);
        uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
        uiConfigButton.setEnabled(true);
        uiConfigButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
        uiKeyButton.setEnabled(true);
        uiKeyButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
        uiTextViewAddress.setTextColor(getResources().getColor(R.color.cool_android_green));
        uiTextViewHArdware.setTextColor(getResources().getColor(R.color.cool_android_green));
        uiTextViewLocation.setTextColor(getResources().getColor(R.color.cool_android_green));
        uiTextViewMode.setTextColor(getResources().getColor(R.color.cool_android_green));
        uiTextViewToken.setTextColor(getResources().getColor(R.color.cool_android_green));
        uiTextViewUsername.setTextColor(getResources().getColor(R.color.cool_android_green));
    }

    private void deactivateButtons() {
        uiRequestLocationUpdatesButton.setEnabled(false);
        uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
        uiConfigButton.setEnabled(false);
        uiConfigButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
        uiKeyButton.setEnabled(true);
        uiKeyButton.getBackground().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.SRC_ATOP);
        uiTextViewAddress.setTextColor(getResources().getColor(R.color.cool_ligh_gray));
        uiTextViewHArdware.setTextColor(getResources().getColor(R.color.cool_ligh_gray));
        uiTextViewLocation.setTextColor(getResources().getColor(R.color.cool_ligh_gray));
        uiTextViewMode.setTextColor(getResources().getColor(R.color.cool_ligh_gray));
        uiTextViewToken.setTextColor(getResources().getColor(R.color.cool_ligh_gray));
        uiTextViewUsername.setTextColor(getResources().getColor(R.color.cool_ligh_gray));

    }

    public void checkStatus(View view) {
        if (buttonEnabled) {
            activateButtons();
            buttonEnabled = false;

        }
    }

    public void toggleService(View view) {
        if (requestingUpdates) {
            stopLocatorService();
            requestingUpdates = false;
            mBound=false;

        } else {
            if (requestPermissionsLogic()) {
                startLocatorService();
                uiRequestLocationUpdatesButton.setEnabled(false);
                uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
                buttonEnabled = true;
                requestingUpdates = true;
                mBound=true;
            }
        }
    }

    private void stopLocatorService() {

        mService.removeLocationUpdates();
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
    }

    private void startLocatorService() {
        if (mService == null) {
            Intent serviceIntent = new Intent(this, LocationUpdatesService.class);
            serviceIntent.putExtra(MainActivity.EXTRA_TOKEN, uiTextViewToken.getText().toString());
            serviceIntent.putExtra(MainActivity.EXTRA_USERNAME, uiTextViewUsername.getText().toString());
            serviceIntent.putExtra(MainActivity.EXTRA_HARDWARE_ID, uiTextViewHArdware.getText().toString());
            serviceIntent.putExtra(MainActivity.EXTRA_MODE, uiTextViewMode.getText().toString());
            serviceIntent.putExtra(MainActivity.EXTRA_ONLINE, online);
            bindService(serviceIntent, mServiceConnection,
                    Context.BIND_AUTO_CREATE);

            mBound = false;
        }
        mService.requestLocationUpdates();
        uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.SRC_ATOP);


    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        uiRequestLocationUpdatesButton.setEnabled(true);
        uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        requestingUpdates = false;
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }


    public boolean requestPermissionsLogic() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(LOG_TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.activity_locator),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(LocatorActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            } else {
                return true;
            }
        }
        return false;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(LOG_TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.activity_locator),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(LocatorActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(LOG_TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(LOG_TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                startLocatorService();
            } else {
                // Permission denied.
                setButtonsState(false);
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


    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            String address = intent.getStringExtra(LocationUpdatesService.EXTRA_ADDRESS);
            String error = intent.getStringExtra(LocationUpdatesService.EXTRA_INFO);
            String info = intent.getStringExtra(LocationUpdatesService.EXTRA_ERROR);
            boolean foregroundService = intent.getBooleanExtra(LocationUpdatesService.EXTRA_FOREGROUND, false);
            requestingUpdates = intent.getBooleanExtra(LocationUpdatesService.EXTRA_REQUESTING_STATUS, false);
            if (buttonEnabled) {
                activateButtons();
                buttonEnabled = false;

            }
            setButtonsState(requestingUpdates);
            uiTextViewLocation = (TextView) findViewById(R.id.textViewLocation);
            uiTextViewLocation.setText(location.getLatitude() + ", " + location.getLongitude());
            uiTextViewAddress.setText(getString(R.string.address_text,
                    address, System.currentTimeMillis()));
            if (!foregroundService) {
                toastMessages(error, info);
            }
        }
    }

    private void toastMessages(String error, String info) {
        Toast toast;
        String displayedText;
        String text;
        if (error != null && !error.isEmpty()) {
            toast = Toast.makeText(this, R.string.msg_login_error,
                    Toast.LENGTH_LONG);
            displayedText = ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).getText().toString();
            text = "Error: " + displayedText;
            toast.setText(text);
            if (error != null && !error.isEmpty()) {
                toast = Toast.makeText(this, R.string.msg_app_info,
                        Toast.LENGTH_LONG);
                displayedText = ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).getText().toString();
                text += "\nInfo: " + displayedText;
                toast.setText(text);
            }
            toast.show();
        } else if (error != null && !error.isEmpty()) {
            toast = Toast.makeText(this, R.string.msg_app_info,
                    Toast.LENGTH_LONG);
            displayedText = ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).getText().toString();
            text = "Info: " + displayedText;
            toast.setText(text);
            toast.show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Update the buttons state depending on whether location updates are being requested.
        if (s.equals(Utils.KEY_REQUESTING_LOCATION_UPDATES)) {
            setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES,
                    false));
        }
    }

    private void setButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
            requestingUpdates = requestingLocationUpdates;
            uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.SRC_ATOP);

        } else {
            uiRequestLocationUpdatesButton.setEnabled(true);
            requestingUpdates = requestingLocationUpdates;
            uiRequestLocationUpdatesButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);


        }
    }

}
