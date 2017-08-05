package com.leible.pcl.leible;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,BluetoothDataCallback{
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private Button btn_show_data;
    private Button btn_send;
    private EditText edit_body;
    public static final String TEST_MAC="你需要连接的ble设备mac地址";
    private Handler handler=new Handler(){
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what==1){
                BluetoothDevice bluetoothDevice= (BluetoothDevice) msg.obj;
                if(bluetoothDevice!=null){
                    Log.i("BluetoothLeServiceBLE","蓝牙设备-!"+bluetoothDevice.getAddress()+"devices地址"+TEST_MAC);
                    if (bluetoothDevice.getAddress().equals(TEST_MAC)) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        ToastUtil.toastInfo(MainActivity.this,"发现指定蓝牙设备蓝牙设备!");
                        //开启蓝牙服务
                        Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                        MainActivity.this.bindService(gattServiceIntent, mServiceConnection, MainActivity.BIND_AUTO_CREATE);
                        MainActivity.this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    }
                }
            }else{
                BluetoothGattCharacteristic characteristic= (BluetoothGattCharacteristic) msg.obj;
                String returnData=new String(characteristic.getValue());
                btn_show_data.setText(returnData);
            }
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Message message=new Message();
                            message.obj=device;
                            message.what=1;
                            handler.sendMessage(message);
                        }
                    });
                }
            };

    /**
     * 与service交互
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 当与service的连接建立后被调用
            //系统调用这个来传送在service的onBind()中返回的IBinder．
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.setBluetoothDataCallback(MainActivity.this);
            if (!mBluetoothLeService.initialize()) {
                //无法初始化蓝牙
                Log.e("BluetoothLeServiceBLE", "无法初始化蓝牙");
                finish();
            }
            //在成功启动时自动连接到设备
            mBluetoothLeService.connect(TEST_MAC);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Android系统在同service的连接意外丢失时调用这个．比如当service崩溃了或被强杀了．当客户端解除绑定时，这个方法不会被调用．
        }
    };

    //广播监听
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(final Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                ToastUtil.toastInfo(context, "连接ing");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                ToastUtil.toastInfo(context, "断开的");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //广播到设备
                ToastUtil.toastInfo(context, "搜索到服务列表");
                mBluetoothLeService.setNotification();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                ToastUtil.toastInfo(context, "数据特征被读事件");
                Log.i("BluetoothLeServiceBLE",TEST_MAC+"------"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
        return intentFilter;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
// 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        btn_send=(Button)findViewById(R.id.btn_send);
        edit_body=(EditText)findViewById(R.id.edit_body);
        btn_show_data=(Button)findViewById(R.id.btn_show_data);
        btn_send.setOnClickListener(this);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.btn_send){
            String data=edit_body.getText().toString();
            if (mBluetoothLeService!=null){
                mBluetoothLeService.setWrite(hexStringToBytes(data));
            }

        }
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }
    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    @Override
    public void callbackData(BluetoothGattCharacteristic characteristic) {
        Message message=new Message();
        message.what=2;
        message.obj=characteristic;
        handler.sendMessage(message);

    }
}
