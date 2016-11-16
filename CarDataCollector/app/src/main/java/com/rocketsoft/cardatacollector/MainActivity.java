package com.rocketsoft.cardatacollector;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button startBtn = null;
    private Button stopBtn = null;
    private TextView gpsLog = null;
    private TextView accelLog = null;
    private BroadcastReceiver dataReciever = null;
    private String deviceAddress = null;

    @Override
    protected void onResume() {
        super.onResume();

        if (dataReciever == null) {
            dataReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    gpsLog.append("\n: " + intent.getExtras().get("json"));
                }
            };
        }
        registerReceiver(dataReciever, new IntentFilter("data_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataReciever != null) {
            unregisterReceiver(dataReciever);
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

        connectObdReader();

        if (!getPermissions()) {
            enableButtons();
        }
    }

    private void connectObdReader() {
        ArrayList deviceStrs = new ArrayList();
        final ArrayList devices = new ArrayList();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }

        // show list
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = String.valueOf(devices.get(position));
            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    private void enableButtons() {

        final Intent intent = new Intent(this, DataCollector.class);
        startBtn.setEnabled(true);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("btAddress", deviceAddress);
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                startService(intent);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                stopService(intent);
            }
        });
    }

    private boolean getPermissions() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enableButtons();
            } else {
                getPermissions();
            }
        }
    }
}
