package com.rocketsoft.cardatacollector;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button startBtn;
    private Button stopBtn;
    private TextView gpsLog;
    private TextView accelLog;
    private BroadcastReceiver locationBcReceiver = null;
    private BroadcastReceiver sensorBcReceiver = null;

    @Override
    protected void onResume() {
        super.onResume();

        if( locationBcReceiver == null) {
            locationBcReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    gpsLog.append( "\n GPS: " + intent.getExtras().get("location"));
                }
            };
        }
        registerReceiver( locationBcReceiver, new IntentFilter("location_update"));

        if( sensorBcReceiver == null) {
            sensorBcReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    accelLog.append( "\n ACCEL: " + intent.getExtras().get("accel"));
                }
            };
        }
        registerReceiver( sensorBcReceiver, new IntentFilter("sensor_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( locationBcReceiver != null) {
            unregisterReceiver(locationBcReceiver);
        }

        if( sensorBcReceiver != null) {
            unregisterReceiver(sensorBcReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setEnabled(false);

        stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setEnabled(false);

        gpsLog = (TextView) findViewById(R.id.gpsLog);
        gpsLog.setMovementMethod(new ScrollingMovementMethod());

        accelLog = (TextView) findViewById(R.id.accelLog);
        accelLog.setMovementMethod(new ScrollingMovementMethod());

        if(!getPermissions()) {
            enableButtons();
        }
    }

    private void enableButtons() {

        final Intent intent = new Intent(this, DataCollector.class);
        startBtn.setEnabled(true);

        startBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                startService(intent);
            }
        });

        stopBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                stopService(intent);
            }
        });
    }

    private boolean getPermissions() {
        if(Build.VERSION.SDK_INT >=  23 &&
                ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,  permissions, grantResults);

        if( requestCode == 100)  {
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED ) {
                enableButtons();
            } else {
                getPermissions();
            }
        }
    }
}
