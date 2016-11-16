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
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class DataCollector extends Service {

    private LocationListener locationListener;
    private LocationManager locationManager;

    private SensorEventListener sensorEventListener;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String deviceAddress  = intent.getStringExtra("btAddress");

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket socket = null;
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Intent i = new Intent("location_update");
                i.putExtra("location", "Lon:"+ location.getLongitude() + ", Lat:" + location.getLatitude());
                sendBroadcast(i);
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

                    Intent i = new Intent("sensor_update");
                    i.putExtra("accel","X:" + xAxs +", Y:" + yAxs + ", Z:" + zAxs);
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
        }
        catch( SecurityException ex){
            Log.d("X", "SecurityException while registering listeners : " + ex.getMessage());
        }
        catch( Exception ex) {
            Log.d("X", "Exception while registering listeners : " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if( locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }

            if( sensorManager != null) {
                sensorManager.unregisterListener(sensorEventListener, sensorAccelerometer);
            }
        }
        catch( SecurityException ex){
            Log.d("X", "SecurityException while onDestroy");
        }
        catch( Exception ex) {
            Log.d("X", "Exception while onDestroy");
        }
    }
}
