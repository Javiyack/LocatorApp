package com.sginfo.locatorapp;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/**
 * Created by filipp on 6/16/2016.
 */
public class GPS_Service extends Service {

    // Constants
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TRACKING_LOCATION_KEY = "tracking_location";


    // Location classes
    private boolean mTrackingLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d("GPS_SERVICE", "onBing called");
        return null;
    }

    @Override
    public void onCreate() {

        // Initialize the FusedLocationClient.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(
                this);

        // Initialize the location callbacks.
        mLocationCallback = new LocationCallback() {
            /**
             * This is the callback that is triggered when the
             * FusedLocationClient updates your location.
             * @param locationResult The result containing the device location.
             */
            @Override
            public void onLocationResult(LocationResult locationResult) {
                System.out.println("Location Result: " + locationResult);
            }
        };

        startTrackingLocation();


    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("This notification is from a foreground service")
                .setContentText("Touch to open activity handling this service")
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setTicker("Starting up!!!")
                .setContentIntent(contentIntent)
                .setOngoing(false) //Always true in startForeground
                .build();
        startForeground(12345687, notification);

        //startService(intent);

        // startForeground(12345678, );
        //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }


    /**
     * Starts tracking the device. Checks for
     * permissions, and requests them if they aren't present. If they are,
     * requests periodic location updates, sets a loading text and starts the
     * animation.
     */
    private void startTrackingLocation() {
        mTrackingLocation = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ;
        }
        mFusedLocationClient.requestLocationUpdates
                (getLocationRequest(),
                        mLocationCallback,
                        null /* Looper */);

    }


    /**
     * Sets up the location request.
     *
     * @return The LocationRequest object containing the desired parameters.
     */
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }


}
