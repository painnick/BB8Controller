package com.painnick.bb8controller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "Main";

    static final String BLUETOOTH_NAME = "VR-Trainer";

    // Get permission
    static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    static final int REQUEST_ENABLE_BT = 1;
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    BluetoothSocket btSocket = null;
    String targetAddress = null;
    ConnectedThread connectedThread;
    ConnectThreadHandler connectedThreadHandler;
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                assert device != null;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                Log.d(TAG, String.format("FOUND '%s'(%s)", deviceName, deviceHardwareAddress));
                if (BLUETOOTH_NAME.equals(deviceName)) {
                    connectBt(deviceHardwareAddress);
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null) {
                    String deviceAddress = device.getAddress();
                    Log.d(TAG, String.format("CONNECT %s Target:%s", deviceAddress, targetAddress));
                    if (targetAddress != null && targetAddress.equals(deviceAddress)) {
                        Toast.makeText(context, "BB-8 연결에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null) {
                    String deviceAddress = device.getAddress();
                    Log.d(TAG, String.format("DISCONNECT %s Target:%s", deviceAddress, targetAddress));
                    if (targetAddress != null && targetAddress.equals(deviceAddress)) {
                        Toast.makeText(context, "BB-8과의 연결이 종료되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
    };
    // 포맷 정의
    DateTimeFormatter TimeFormatter = DateTimeFormatter.ofPattern("mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show paired devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btReceiver, filter);

        ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, 1);

        // Enable bluetooth
        BluetoothManager btManager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            fillPaired();
        }

        findViewById(R.id.btnHelp).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("help");
            }
        });

        findViewById(R.id.btnClearLogs).setOnClickListener(v -> {
            TextView textBtLogs = findViewById(R.id.txtBtLogs);
            textBtLogs.setText("");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                fillPaired();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void fillPaired() {
        boolean found = false;
        String foundAddress = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (BLUETOOTH_NAME.equals(deviceName)) {
                    found = true;
                    foundAddress = deviceHardwareAddress;
                }
            }
        }
        if (found) {
            connectBt(foundAddress);
        } else {
            Toast.makeText(this, "블루투스 BB-8을 검색합니다...", Toast.LENGTH_SHORT).show();
            btAdapter.cancelDiscovery();
            btAdapter.startDiscovery();
        }
    }

    private void connectBt(String foundAddress) {
        targetAddress = foundAddress;
        BluetoothDevice device = btAdapter.getRemoteDevice(foundAddress);

        boolean connected = false;
        try {
            btSocket = createBluetoothSocket(device);
            if (btSocket != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                btSocket.connect();
                connected = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (connected) {
            connectedThreadHandler = new ConnectThreadHandler();
            connectedThread = new ConnectedThread(btSocket, connectedThreadHandler);
            connectedThread.start();
        } else {
            Toast.makeText(this, "블루투스 BB-8 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(btReceiver);
    }

    class ConnectThreadHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            // 현재 날짜 구하기
            LocalTime now = LocalTime.now();
            String formattedNow = now.format(TimeFormatter);

            Bundle bundle = msg.getData();
            String cmd = bundle.getString("cmd");
            String value = Objects.requireNonNull(bundle.getString("value")).trim();

            String[] lines = value.split("\n");

            // 핸들러 내에서 변경을 하기에 가능하다.
            TextView textBtLogs = findViewById(R.id.txtBtLogs);
            if (textBtLogs != null) {
                for (String line : lines) {
                    textBtLogs.append(formattedNow);
                    if ("send".equals(cmd)) {
                        textBtLogs.append(" >> ");
                    } else if ("recv".equals(cmd)) {
                        textBtLogs.append(" << ");
                    }
                    textBtLogs.append(line);
                    textBtLogs.append("\n");
                }
            }
        }
    }
}