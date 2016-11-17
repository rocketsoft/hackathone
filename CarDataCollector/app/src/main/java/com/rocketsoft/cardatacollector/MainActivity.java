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
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button startBtn = null;
    private Button stopBtn = null;
    private TextView gpsLog = null;
    private TextView accelLog = null;
    private BroadcastReceiver dataReciever = null;
    private String deviceAddress = null;
    private boolean isEmptyLog = true;

    @Override
    protected void onResume() {
        super.onResume();

        if (dataReciever == null) {
            dataReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(!isEmptyLog) {
                        gpsLog.append(",");
                    }
                    gpsLog.append("\n" + intent.getExtras().get("json"));
                    isEmptyLog = false;
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

        if (!getGpsPermissions() ) {
            enableButtons();
        }

        getSensorPermissions();
        getDiskPermissions();

        connectObdReader();
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
                gpsLog.setText("");
                startService(intent);
                isEmptyLog = true;
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                stopService(intent);

                StringBuilder strBldr  = new StringBuilder();
                strBldr.append("{\"data\":[");
                strBldr.append(gpsLog.getText());
                strBldr.append("]}");


                if(canWriteOnExternalStorage()) {
                    writeToFile(strBldr.toString(), getApplicationContext());
                }
            }
        });
    }

    private boolean getGpsPermissions() {

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)  {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return true;
        }

        return false;
    }

    private boolean getSensorPermissions() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED)  {

            requestPermissions(new String[]{Manifest.permission.VIBRATE}, 101);
            return true;
        }

        return false;
    }

    private boolean getDiskPermissions() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)  {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
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
                getGpsPermissions();
            }
        }

        if (requestCode == 101) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getSensorPermissions();
            }
        }

        if (requestCode == 102) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                getDiskPermissions();
            }
        }
    }

    public static boolean canWriteOnExternalStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Log.v("X", "Yes, can write to external storage.");
            return true;
        }
        return false;
    }

    private void writeToFile(String data,Context context) {
        try {
            String tripFileName = new SimpleDateFormat("'TRIP_'yyyyMMddhhmm'.json'").format(new Date());

            File saveDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);

            File saveDir = new File(saveDirectory.getAbsolutePath() + "/TRIP_FILES/");
            if(!saveDir.exists()) {
                saveDir.mkdir();
            }

            File file = new File(saveDir, tripFileName);
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(data.getBytes());
            outStream.close();

            Toast.makeText(context, "Saved : " + tripFileName, Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Log.e("X", "File write failed: " + e.toString());
        }
    }
}
