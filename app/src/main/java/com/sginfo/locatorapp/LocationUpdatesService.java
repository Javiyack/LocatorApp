/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sginfo.locatorapp;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification assocaited with that service is removed.
 */
public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "channel_01";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    static final String EXTRA_ADDRESS = PACKAGE_NAME + ".address";
    static final String EXTRA_FOREGROUND = PACKAGE_NAME + ".foreground.service";
    static final String EXTRA_REQUESTING_STATUS = PACKAGE_NAME + ".requesting.status";
    static final String EXTRA_ERROR = PACKAGE_NAME + ".error";
    static final String EXTRA_INFO = PACKAGE_NAME + ".info";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;


    private NotificationManager mNotificationManager;

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;
    private HandlerThread handlerThread;
    /**
     * The current location.
     */
    private Location mLocation;

    private String mAddress;
    BackGroundSaver backGroundSaver = new BackGroundSaver();
    // Intent usefull values
    private String token;
    private String username;
    private String mode;
    private String hardwareID;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;
    private boolean requestingActive = false;
    private boolean tracking = false;
    private boolean online;
    private boolean foregroundService;

    public LocationUpdatesService() {
        Log.i(TAG, "User interaction was cancelled.");
    }

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                requestingActive = false;
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            mChannel.setSound(null,null); // ignore sound
            mChannel.enableVibration(false);
            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);
        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        // Empieza a moitorizar la ubicacion
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    private boolean isOnline(){
        return this.online;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");

        stopForeground(true);
        mChangingConfiguration = false;
        username = intent.getStringExtra(MainActivity.EXTRA_USERNAME);
        token = intent.getStringExtra(MainActivity.EXTRA_TOKEN);
        hardwareID = intent.getStringExtra(MainActivity.EXTRA_HARDWARE_ID);
        mode = intent.getStringExtra(MainActivity.EXTRA_MODE);
        online = intent.getBooleanExtra(MainActivity.EXTRA_ONLINE,false);
        backGroundSaver.setOnline(online);
        return mBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            /*
            // TODO(developer). If targeting O, use the following code.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                mNotificationManager.startServiceInForeground(new Intent(this,
                        LocationUpdatesService.class), NOTIFICATION_ID, getNotification());
            } else {
                startForeground(NOTIFICATION_ID, getNotification());
            }
             */
            startForeground(NOTIFICATION_ID, getNotification());
        }else{
            stopTimer();
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        stopTimer();
        Toast.makeText(this, "Locator Service Stopped.", Toast.LENGTH_SHORT).show();
        mServiceHandler.removeCallbacksAndMessages(null);

    }


    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        startTimer();
    }

    private Timer timer;
    private TimerTask timerTask;
    private int couter = 0;
    long oldTime = 0;

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 5 second
        timer.schedule(timerTask, 200, 1200); //
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
        final Context context = this;
        timerTask = new TimerTask() {
            public void run() {
                startRequestingLocationUpdates();
            }
        };
    }

    private void startRequestingLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if (Utils.requestingLocationUpdates(this)) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, handlerThread.getLooper());
            requestingActive = true;
            foregroundService = serviceIsRunningInForeground(this);
            // Notify anyone listening for broadcasts about the new location.
            Intent intent = new Intent(ACTION_BROADCAST);
            intent.putExtra(EXTRA_LOCATION, mLocation);
            intent.putExtra(EXTRA_ADDRESS, mAddress);
            intent.putExtra(EXTRA_INFO, "");
            intent.putExtra(EXTRA_ERROR, "");
            intent.putExtra(EXTRA_FOREGROUND, foregroundService);
            intent.putExtra(EXTRA_REQUESTING_STATUS, requestingActive);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } else {
            requestingActive = false;
        }
        try {
            //Thread.sleep(1000);
            mFusedLocationClient.flushLocations();
        } catch (Throwable oops) {

        }

    }


    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        stopTimer();
        mFusedLocationClient.flushLocations();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        requestingActive = false;
        Log.i(TAG, "Removing location updates");
        try {
            Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }

    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Intent locatorIntent = new Intent(this, LocatorActivity.class);
        locatorIntent.putExtra(MainActivity.EXTRA_TOKEN, token);
        locatorIntent.putExtra(MainActivity.EXTRA_USERNAME, username);
        locatorIntent.putExtra(MainActivity.EXTRA_HARDWARE_ID, hardwareID);
        locatorIntent.putExtra(MainActivity.EXTRA_MODE, mode);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                locatorIntent, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .addAction(R.drawable.ic_launcher_background, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_window_close, getString(R.string.remove_location_updates),
                        servicePendingIntent)
                .setContentTitle((mAddress!=null)?mAddress:text)
                .setContentText(DateFormat.getDateTimeInstance().format(new Date()))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void sendResumeStatus() {
        if (Utils.requestingLocationUpdates(this)) {
            getLastLocation();
            Intent intent = new Intent(ACTION_BROADCAST);
            intent.putExtra(EXTRA_LOCATION, mLocation);
            intent.putExtra(EXTRA_ADDRESS, mAddress);
            intent.putExtra(EXTRA_INFO, "");
            intent.putExtra(EXTRA_ERROR, "");
            intent.putExtra(EXTRA_FOREGROUND, foregroundService);
            intent.putExtra(EXTRA_REQUESTING_STATUS, requestingActive);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);
        boolean foregroundServive = serviceIsRunningInForeground(this);
        mLocation = location;
        saveResult(mLocation);
        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        intent.putExtra(EXTRA_ADDRESS, mAddress);
        intent.putExtra(EXTRA_INFO, "");
        intent.putExtra(EXTRA_ERROR, "");
        intent.putExtra(EXTRA_FOREGROUND, foregroundServive);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (foregroundServive) {

            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
            // Guardar en servidor la localizacion.

        }

        Toast.makeText(this, "New location sended.", Toast.LENGTH_SHORT).show();

    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveResult(Location location) {
        backGroundSaver.setOnline(online);
        backGroundSaver.setLocation(location);
        backGroundSaver.setURL("http://sginfo.ddns.net:8050/Acme-CRM/webservice/location/save.do");
        backGroundSaver.setContext(getApplicationContext());
        backGroundSaver.save(location);
        backGroundSaver.setForegroundService(serviceIsRunningInForeground(getApplicationContext()));
        mAddress = backGroundSaver.getAddress();
    }
}
