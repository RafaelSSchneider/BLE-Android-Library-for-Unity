package com.rafael.playblowplugin.ble;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;

import com.rafael.playblowplugin.UnityObject;
import com.unity3d.player.UnityPlayer;

public class BLEManager {

    //MonoBehaviour GameObject to catch the BLE Messages
    private static final String mUnityBLEReceiver = "AndroidMessageManager";

    //Command to send Bluetooth Low Energy data
    private static final String mUnityBLECommand = "OnBleMessage";
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private final Handler handler;

    private boolean isScanning = false;

    private Queue<BluetoothGattCharacteristic> characteristicsQueue = new LinkedList<>();

    private boolean isWritingDescriptor = false;

    public BLEManager(Context context) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.handler = new Handler(Looper.getMainLooper());
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            UnityObject obj = new UnityObject("ScanResult");
            obj.device = result.getDevice().getAddress();
            obj.deviceName = result.getDevice().getName();
            sendToUnity(obj);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            // send error to unity
        }
    };
    @SuppressLint("MissingPermission")
    public void startScan() {
        isScanning = true;
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
            Log.d("SCAN STATUS", "Started");
        }
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            Log.d("SCAN STATUS", "Stopped");

        }
        isScanning = false;
    }
    @SuppressLint("MissingPermission")
    public void connectToDevice(String deviceAddress) {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        if(isScanning) stopScan();

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        bluetoothGatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if( status == BluetoothGatt.GATT_SUCCESS){
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("BLE", "Connected to GATT server.");
                        refreshDeviceCache(gatt);
                        bluetoothGatt.discoverServices();
                        UnityObject obj = new UnityObject("BLEConnectionState");
                        obj.data = "Connected";
                        obj.dataType = "Connection State";
                        sendToUnity(obj);
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d("BLE", "Disconnected from GATT server.");
                        UnityObject obj = new UnityObject("BLEConnectionState");
                        obj.data = "Disconnected";
                        obj.dataType = "Connection State";
                        sendToUnity(obj);
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    for (BluetoothGattService service : bluetoothGatt.getServices()) {
                        Log.d("BLE", "Service UUID: " + service.getUuid().toString());
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            if(characteristic.getUuid().toString().equals("4ca567a9-d997-4660-ae13-51b0d30cf376") || characteristic.getUuid().toString().equals("8e04b536-3d33-4c90-ae48-f19c4aab1203")){
                                characteristicsQueue.add(characteristic);
                            }
                        }
                    }
                   enableNextNotification(gatt);
                }
            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] value = characteristic.getValue();
                UnityObject obj = new UnityObject("BLEData");
                if(characteristic.getUuid().toString().equals("4ca567a9-d997-4660-ae13-51b0d30cf376")){
                    obj.device = gatt.getDevice().getAddress();
                    obj.deviceName = gatt.getDevice().getName();
                    obj.characteristic = characteristic.getUuid().toString();
                    obj.service = characteristic.getService().getUuid().toString();
                    obj.data = new String(value);
                    obj.dataType = "Espirometro";
                }else if(characteristic.getUuid().toString().equals("8e04b536-3d33-4c90-ae48-f19c4aab1203")){
                    Log.d("BATERIA DATA", new String(value));
                    obj.device = gatt.getDevice().getAddress();
                    obj.deviceName = gatt.getDevice().getName();
                    obj.characteristic = characteristic.getUuid().toString();
                    obj.service = characteristic.getService().getUuid().toString();
                    obj.data = new String(value);
                    obj.dataType = "Bateria";
                }
                sendToUnity(obj);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                isWritingDescriptor = false;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    enableNextNotification(gatt);
                }
            }

        }, BluetoothDevice.TRANSPORT_LE);
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(){
        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

    }
    @SuppressLint("MissingPermission")
    private void enableNextNotification(BluetoothGatt gatt){
        if (isWritingDescriptor) return;
        if(characteristicsQueue.isEmpty()) return;

        BluetoothGattCharacteristic characteristic = characteristicsQueue.poll();
        gatt.setCharacteristicNotification(characteristic, true);
        assert characteristic != null;
        BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    private void refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh");
            localMethod.invoke(gatt);
        } catch (Exception localException) {
            Log.e("BLE", "An exception occurred while refreshing device", localException);
        }
    }
    public static void sendToUnity(UnityObject obj){
        UnityPlayer.UnitySendMessage(mUnityBLEReceiver, mUnityBLECommand, obj.toJson());
    }
}
