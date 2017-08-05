package com.leible.pcl.leible;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2017/6/26.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends Service {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private String TAG="BluetoothLeServiceBLE";
    private int mConnectionState = STATE_DISCONNECTED;//链接状态
    private static final int STATE_DISCONNECTED = 0;//断开
    private static final int STATE_CONNECTING = 1;//链接
    private static final int STATE_CONNECTED = 2;//保持链接
    public final static String ACTION_GATT_CONNECTED = "com.funbike.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.funbike.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.funbike.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.funbike.ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.funbike.ble.EXTRA_DATA";
    private String Service_UUID="0000fee7-0000-1000-8000-00805f9b34fb";
    private String Read_UUID="000036f6-0000-1000-8000-00805f9b34fb";
    private String Write_UUID="000036f5-0000-1000-8000-00805f9b34fb";
    private BluetoothGattCharacteristic write_Characteristic;//写入的特征
    private BluetoothGattCharacteristic read_Characteristic;
    private BluetoothGattService bluetoothGattService;
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    /**
     * 初始化对本地蓝牙适配器的引用。
     * @return 如果初始化成功，则返回true。
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "没有初始化或未指定地址的blue牙签适配器.");
            return false;
        }
        // 以前连接设备。尝试重新连接
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.i(TAG, "尝试使用现有的mblue牙签来连接");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.i(TAG, "没有设备");
            return false;
        }
        // 我们想直接连接到设备上，所以我们设置了
        // 自动更正
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.i(TAG, "尝试创建一个新的连接。");

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * 表明GATT客户端与远程GATT服务器连接/断开连接的情况。
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            //已连接
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;//链接状态

                broadcastUpdate(intentAction);
                Log.i(TAG, "服务器连接到GATT协定。");
                // 尝试在成功连接后发现服务。
                Log.i(TAG, "尝试启动服务发现" + mBluetoothGatt.discoverServices());
            //已断开
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }

        }
        /**
         * 当远程设备的远程服务、特征和描述符的列表被更新时，调用了新的服务。
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i("BluetoothLeServiceBLE","onCharacteristicChanged:特征改变 "+bytesToHexString(characteristic.getValue())+"---"+new String(characteristic.getValue()));
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        /**
         * 报告一个特征读操作的结果。
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.i("BluetoothLeServiceBLE","onCharacteristicRead: "+bytesToHexString(characteristic.getValue())+"---"+new String(characteristic.getValue()));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE,bytesToHexString(characteristic.getValue()));
        //        Log.i(TAG,"onCharacteristicRead: 读取Value:  "+characteristic.getValue().toString());
            }
            if (bluetoothDataCallback!=null){
                bluetoothDataCallback.callbackData(characteristic);
            }
        }
        /**
         *表示特征写操作的结果。
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
               Log.i("BluetoothLeServiceBLE","onCharacteristicWrite 写入成功： "+characteristic.getUuid());
                Log.i(TAG,"onCharWrite "+gatt.getDevice().getName()
                        +" write "
                        +characteristic.getUuid().toString()
                        +" -> "
                        +new String(characteristic.getValue()));
            }
        }
    };

    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    /**
     * 设置通知
     */
    public void setNotification(){
        bluetoothGattService=mBluetoothGatt.getService(UUID.fromString(Service_UUID));
        if(bluetoothGattService!=null){
            write_Characteristic=bluetoothGattService.getCharacteristic(UUID.fromString(Write_UUID));
            read_Characteristic=bluetoothGattService.getCharacteristic(UUID.fromString(Read_UUID));
            if(read_Characteristic!=null){
                setCharacteristicNotification(read_Characteristic,true);
            }
        }

    }
    public void setWrite(byte[] info){
        if(write_Characteristic!=null){

            write_Characteristic.setValue(info);
            mBluetoothGatt.writeCharacteristic(write_Characteristic);
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enable){
        Log.i("demo", "setCharacteristicNotification");
        if(mBluetoothAdapter == null || mBluetoothGatt == null||characteristic==null){
            Log.i("demo", "null");
            return;
        }

        boolean isEnableNotification= mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
        Log.i("demo", isEnableNotification+"");

        if(isEnableNotification){
            List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
            if(descriptorList != null && descriptorList.size() > 0) {
                for(BluetoothGattDescriptor descriptor : descriptorList) {
                    Log.i("demo", isEnableNotification+"----ENABLE_NOTIFICATION_VALUE");
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
        Log.i("demo", "ok");
    }



    private void broadcastUpdate(final String action,String r) {
        final Intent intent = new Intent(action);
        if (r != null&&!r.equals("") ) {
            intent.putExtra(EXTRA_DATA, r);
        }
        sendBroadcast(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {

        disconnect();
        close();
        return super.onUnbind(intent);
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.i("BluetoothLeServiceBLE","BluetoothLeServiceBLE:close");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("BluetoothLeServiceBLE", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        Log.i("BluetoothLeServiceBLE","BluetoothLeServiceBLE:   disconnect");
    }

    private BluetoothDataCallback bluetoothDataCallback;
    public void setBluetoothDataCallback(BluetoothDataCallback bluetoothDataCallback){
        this.bluetoothDataCallback=bluetoothDataCallback;
    }
}
