/*


 */


package com.rafael.playblowplugin;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.rafael.playblowplugin.ble.BLEManager;
import android.content.pm.PackageManager;

import androidx.annotation.RequiresApi;

public class UnityBLEBridge extends Application {

    private static UnityBLEBridge instance = null;
    private static BLEManager bleManager = null;
    private static final String TAG = "UnityBLEBridge";
    private static Context currentActivity = null;

    public static void initialize() {
        Log.d(TAG, "Initializing UnityBLEBridge without context");
        try {
            Class<?> unityPlayer = Class.forName("com.unity3d.player.UnityPlayer");
            currentActivity = (Context) unityPlayer.getDeclaredField("currentActivity").get(null);
            if (instance == null) {
                Log.d(TAG, "Creating new instance");
                instance = new UnityBLEBridge();
                instance.onCreate();
            }
            bleManager = new BLEManager(currentActivity);
            Log.d(TAG, "BLEManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UnityBLEBridge", e);
        }
    }

    public Context getApplicationContext() {
        Log.d(TAG, "Getting application context");
        return currentActivity;
    }

    public static UnityBLEBridge getInstance() {
        Log.d(TAG, "Getting instance of UnityBLEBridge");
        return instance;
    }

    public static void startScan() {
        Log.d(TAG, "Starting BLE scan");
        if (bleManager != null && hasPermissions(currentActivity)) {
            bleManager.startScan();
        }
    }

    public static void stopScan() {
        Log.d(TAG, "Stopping BLE scan");
        if (bleManager != null && hasPermissions(currentActivity)) {
            bleManager.stopScan();
        }
    }

    public static void connectToDevice(final String deviceAddress){
        Log.d(TAG, "Connecting to device!" + deviceAddress);
        if (bleManager != null && hasPermissions(currentActivity)) {
            bleManager.connectToDevice(deviceAddress);
        }
    }

    public static void disconnectDevice(){
        Log.d(TAG, "Disconnecting device!");
        if (bleManager != null && hasPermissions(currentActivity)) {
            bleManager.disconnectDevice();
        }
    }
    private static boolean hasPermissions(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called, setting instance");
        instance = this;
    }
}
