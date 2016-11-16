package com.rocketsoft.cardatacollector;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.JsonWriter;
import android.util.Log;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.UUID;

import static android.R.id.message;
import static android.content.ContentValues.TAG;

public class DataCollector extends Service {

    private LocationListener locationListener;
    private LocationManager locationManager;

    private SensorEventListener sensorEventListener;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    BluetoothSocket socket = null;
    private String rpm = "0";
    private String speed = "0";
    private double lat = 0.0;
    private double lon = 0.0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String deviceAddress = intent.getStringExtra("btAddress");


        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    socket.connect();

                    if (socket.isConnected()) {
                        new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());

                        new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());

                        new TimeoutCommand(5000).run(socket.getInputStream(), socket.getOutputStream());

                        new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

                        RPMCommand engineRpmCommand = new RPMCommand();
                        SpeedCommand speedCommand = new SpeedCommand();
                        while (!Thread.currentThread().isInterrupted()) {
                            engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                            speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                            rpm = engineRpmCommand.getFormattedResult();
                            speed  = speedCommand.getFormattedResult();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lat = location.getLongitude();
                lon = location.getLatitude();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float xAxs = sensorEvent.values[0];
                    float yAxs = sensorEvent.values[1];
                    float zAxs = sensorEvent.values[2];

                    JSONObject json = new JSONObject();
                    try {
                        json.put("lat",lat);
                        json.put("lon",lon);
                        json.put("x",xAxs);
                        json.put("y",yAxs);
                        json.put("z",zAxs);
                        json.put("speed",speed);
                        json.put("rpm",rpm);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent i = new Intent("data_update");
                    i.putExtra("json", json.toString());
                    sendBroadcast(i);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);
            sensorManager.registerListener(sensorEventListener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (SecurityException ex) {
            Log.d("X", "SecurityException while registering listeners : " + ex.getMessage());
        } catch (Exception ex) {
            Log.d("X", "Exception while registering listeners : " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {

            if (null != socket) {
                socket.close();
                socket = null;
            }

            if (locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }

            if (sensorManager != null) {
                sensorManager.unregisterListener(sensorEventListener, sensorAccelerometer);
            }
        } catch (SecurityException ex) {
            Log.d("X", "SecurityException while onDestroy");
        } catch (Exception ex) {
            Log.d("X", "Exception while onDestroy");
        }
    }
}
