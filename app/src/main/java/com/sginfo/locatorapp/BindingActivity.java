
package com.sginfo.locatorapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Example of binding and unbinding to the local service.
 * bind to, receiving an object through which it can communicate with the service.
 * <p>
 * Note that this is implemented as an inner class only keep the sample
 * all together; typically this code would appear in some separate class.
 */
public class BindingActivity extends Activity {
    // Don't attempt to unbind from the service unless the client has received some
    // information about the service's state.
    private boolean mShouldUnbind;

    // To invoke the bound service, first make sure that this value
    // is not null.
    private LocalService mBoundService;

    // Las constantes para utilizar en mensajes y otros metodos. Asi, emisor y receptor usan la misma clave.
    private static final String PACKAGE_NAME =
            "LocalService";
    private static final String MODE_UNBINDED =
            "unbinded";
    private static final String MODE_BINDED =
            "binded";
    private static final String MODE_STARTED =
            "satarted";
    private static final String MODE_STOPED =
            "stoped";
    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;

    // UI elements.
    private Button uiPowerButton;
    private Button uiConfigButton;
    private Button uiKeyButton;
    private TextView uiTextViewUsername;
    private TextView uiTextViewHArdware;
    private TextView uiTextViewMode;
    private TextView uiTextViewAddress;
    private TextView uiTextViewToken;
    private TextView uiTextViewLocation;
    private TextView uiCountView;

    private boolean counting;
    private boolean serverStarted;


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((LocalService.LocalBinder) service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(BindingActivity.this, R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(BindingActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        if (bindService(new Intent(BindingActivity.this, LocalService.class),
                mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.

            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    public void clickOnKeyButonAction(View view) {
        Log.i(TAG, "Botón llave clickeado");
        if (!mShouldUnbind) {
            doBindService();
            setButtons(BindingActivity.MODE_BINDED);
            uiKeyButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
        }else{
            doUnbindService();
            setButtons(BindingActivity.MODE_UNBINDED);
            uiKeyButton.getBackground().clearColorFilter();
        }

    }

    public void clickOnPowerButonAction(View view) {
        Log.i(TAG, "Botón power clickeado");
        if (!counting) {
            mBoundService.startTimer();
            setButtons(BindingActivity.MODE_STARTED);
        } else {
            mBoundService.stopTimer();
            setButtons(BindingActivity.MODE_STOPED);
        }
        counting = !counting;

    }

    private void setButtons() {
        if (mShouldUnbind) {
            setButtons(BindingActivity.MODE_BINDED);
            if (counting)
                setButtons(BindingActivity.MODE_STARTED);
            else
                setButtons(BindingActivity.MODE_STOPED);
        }else
        setButtons(BindingActivity.MODE_UNBINDED);

    }

    private void setButtons(boolean active) {
        if (counting)
            if (active)
                activateButtons();
            else
                deactivateButtons();
    }

    public void activateButtons() {
        uiPowerButton.setEnabled(true);
        uiPowerButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
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
        uiCountView.setTextColor(getResources().getColor(R.color.cool_android_green));
    }

    private void deactivateButtons() {
        uiPowerButton.setEnabled(false);
        uiPowerButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
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
        uiCountView.setTextColor(getResources().getColor(R.color.cool_ligh_gray));

    }

    private void setButtons(String mode) {
        switch (mode) {
            case BindingActivity.MODE_BINDED:
                activateButtons();
                break;
            case BindingActivity.MODE_UNBINDED:
                deactivateButtons();
                break;
            case BindingActivity.MODE_STARTED:
                uiPowerButton.setEnabled(true);
                uiPowerButton.getBackground().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.SRC_ATOP);
                uiKeyButton.setEnabled(false);
                uiKeyButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
                break;
            case BindingActivity.MODE_STOPED:
                uiPowerButton.setEnabled(true);
                uiPowerButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
                uiKeyButton.setEnabled(true);
                uiKeyButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_android_green), PorterDuff.Mode.SRC_ATOP);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(LocalService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        uiPowerButton.setEnabled(true);
        uiPowerButton.getBackground().setColorFilter(getResources().getColor(R.color.cool_ligh_gray), PorterDuff.Mode.SRC_ATOP);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myReceiver = new MyReceiver();
        setContentView(R.layout.activity_binding);
    }

    @Override
    protected void onStart() {
        super.onStart();
        myReceiver = new MyReceiver();
        uiPowerButton = (Button) findViewById(R.id.powerButton);
        uiKeyButton = (Button) findViewById(R.id.keyButton);
        uiConfigButton = (Button) findViewById(R.id.configButton);


        // Setup view texts
        uiTextViewAddress = findViewById(R.id.textViewAddress);
        uiTextViewUsername = findViewById(R.id.textViewUsername);
        uiTextViewToken = findViewById(R.id.textViewToken);
        uiTextViewHArdware = findViewById(R.id.textViewHarwareID);
        uiTextViewMode = findViewById(R.id.textViewMode);
        uiTextViewLocation = findViewById(R.id.textViewLocation);
        uiCountView = findViewById(R.id.uiCountView);
        uiTextViewAddress.setText("Address: Aqui aparecera la direccion");
        uiCountView.setText("...");
        setButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        // stopService(new Intent(BindingActivity.this, LocalService.class));
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String info = intent.getStringExtra(LocalService.EXTRA_MSG);
            uiCountView.setText(info);

        }
    }
}