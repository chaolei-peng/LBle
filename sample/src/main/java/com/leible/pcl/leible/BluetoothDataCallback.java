package com.leible.pcl.leible;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by pcl on 2017/8/5.
 */

public interface BluetoothDataCallback {
    void callbackData(BluetoothGattCharacteristic characteristic);
}
