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
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
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
    HashMap<String, String> foundDevices;
    BluetoothSocket btSocket = null;
    String targetAddress = null;
    ConnectedThread connectedThread;
    ConnectThreadHandler connectedThreadHandler;
    CustomLogsLayout logsLayout;

    private Toast toast;

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                assert device != null;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    logsLayout.warn(LocalTime.now(), "Permission is not granted : BLUETOOTH_CONNECT");
                    return;
                }
                String deviceAddress = device.getAddress(); // MAC address
                String deviceName = device.getName();

                String logString = String.format("Found device %s(%s)", deviceName, deviceAddress);
                Log.d(TAG, logString);

                foundDevices.put(deviceAddress, deviceName);

                if (BLUETOOTH_NAME.equals(deviceName)) {
                    logsLayout.info(LocalTime.now(), logString);
                    connectBt(deviceAddress);
                } else {
                    logsLayout.debug(LocalTime.now(), logString);
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null) {
                    String deviceAddress = device.getAddress();
                    String deviceName = device.getName();

                    String logString = String.format("Connected device %s(%s)", deviceName, deviceAddress);
                    Log.d(TAG, logString);

                    if (targetAddress != null && targetAddress.equals(deviceAddress)) {
                        logsLayout.info(LocalTime.now(), logString);
                        showToast(context, "BB-8 연결에 성공하였습니다.", Toast.LENGTH_SHORT);
                    } else {
                        logsLayout.debug(LocalTime.now(), logString);
                    }
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null) {
                    String deviceAddress = device.getAddress();
                    String deviceName = device.getName();

                    String logString = String.format("Disconnected device %s(%s)", deviceName, deviceAddress);
                    Log.d(TAG, logString);

                    if (targetAddress != null && targetAddress.equals(deviceAddress)) {
                        logsLayout.info(LocalTime.now(), logString);
                        showToast(context, "BB-8과의 연결이 종료되었습니다.", Toast.LENGTH_SHORT);
                    } else {
                        logsLayout.debug(LocalTime.now(), logString);
                    }
                }
            } else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null) {
                    String deviceAddress = device.getAddress();
                    String deviceName = device.getName();

                    String logString = String.format("Changed device name %s Target:%s", deviceAddress, deviceName);
                    Log.d(TAG, logString);

                    foundDevices.put(deviceAddress, deviceName);

                    if (BLUETOOTH_NAME.equals(deviceName) && (!deviceAddress.equals(targetAddress))) {
                        logsLayout.info(LocalTime.now(), logString);
                        connectBt(deviceAddress);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        foundDevices = new HashMap<>();

        connectedThreadHandler = new ConnectThreadHandler();

        logsLayout = new CustomLogsLayout(this);
        ScrollView vLogs = findViewById(R.id.vLogs);
        vLogs.addView(logsLayout);

        // Show paired devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        registerReceiver(btReceiver, filter);

        ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, 1);

        // Enable bluetooth
        BluetoothManager btManager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logsLayout.warn(LocalTime.now(), "Permission is not granted : BLUETOOTH_CONNECT");
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            checkPaired();
        }

        findViewById(R.id.btnConnect).setOnClickListener(v -> {
            checkPaired();
        });

        findViewById(R.id.btnHelp).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("help");
            }
        });

        findViewById(R.id.btnHello).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("hello");
            }
        });

        findViewById(R.id.btnBye).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("bye");
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("stop");
            }
        });

        findViewById(R.id.btnFool).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("fool");
            }
        });

        findViewById(R.id.btnMusic).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("music");
            }
        });

        findViewById(R.id.btnLeft).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("left");
            }
        });

        findViewById(R.id.btnRight).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("right");
            }
        });

        findViewById(R.id.btnLightOn).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("light on");
            }
        });

        findViewById(R.id.btnLightOff).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("light off");
            }
        });

        findViewById(R.id.btnLeftRandom).setOnClickListener(v -> {
            if (connectedThread != null) {
                connectedThread.write("led random");
            }
        });

        findViewById(R.id.btnClearLogs).setOnClickListener(v -> {
            logsLayout.removeViews(0, logsLayout.getChildCount());
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                checkPaired();
            } else if (resultCode == RESULT_CANCELED) {
                logsLayout.warn(LocalTime.now(), "사용자 권한 거절 : ENABLE_BT");
                showToast(this, "블루투스를 켜주세요.", Toast.LENGTH_SHORT);
            }
        }
    }

    protected void checkPaired() {
        boolean found = false;
        String foundAddress = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            logsLayout.warn(LocalTime.now(), "Permission is not granted : BLUETOOTH_CONNECT");
            return;
        }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (BLUETOOTH_NAME.equals(deviceName)) {
                    found = true;
                    foundAddress = deviceHardwareAddress;
                    logsLayout.debug(LocalTime.now(), "Found BB-8 from the paired devices");
                }
            }
        }
        if (found) {
            connectBt(foundAddress);
        } else {
            logsLayout.debug(LocalTime.now(), "Cannot find BB-8 from the paired devices");
            findDevice();
        }
    }

    protected void findDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            logsLayout.warn(LocalTime.now(), "Permission is not granted : BLUETOOTH_SCAN");
            return;
        }

        boolean found = false;
        String selectedAddress = null;
        String selectedName = null;
        for (Map.Entry<String, String> entry : foundDevices.entrySet()) {
            String deviceAddress = entry.getKey();
            String deviceName = entry.getValue();

            if (BLUETOOTH_NAME.equals(deviceName)) {
                found = true;
                selectedAddress = deviceAddress;
                selectedName = deviceName;
            }
        }

        if (found) {
            String logString = String.format("Found %s(%s) from the found-list", selectedName, selectedAddress);
            logsLayout.info(LocalTime.now(), logString);
            connectBt(selectedAddress);
        } else {
            showToast(this, "BB-8을 검색합니다...", Toast.LENGTH_SHORT);
            btAdapter.cancelDiscovery();
            logsLayout.debug(LocalTime.now(), "Stop to finding BB-8");
            btAdapter.startDiscovery();
            logsLayout.debug(LocalTime.now(), "Start to finding BB-8...");
        }
    }

    private void connectBt(String foundAddress) {
        targetAddress = foundAddress;
        BluetoothDevice device = btAdapter.getRemoteDevice(foundAddress);

        if (connectedThread != null) {
            logsLayout.info(LocalTime.now(), "Stopping connectedThread");
            connectedThread.cancel();
            try {
                connectedThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            connectedThread = null;
        }

        boolean connected = false;
        try {
            btSocket = createBluetoothSocket(device);
            if (btSocket != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    logsLayout.warn(LocalTime.now(), "Permission is not granted : BLUETOOTH_CONNECT");
                    return;
                }
                btSocket.connect();
                connected = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (connected) {
            connectedThread = new ConnectedThread(btSocket, connectedThreadHandler);
            connectedThread.start();
            logsLayout.debug(LocalTime.now(), "Start to connect new BluetoothSocket");
            showToast(this, "BB-8을 호출할 수 있습니다", Toast.LENGTH_SHORT);
        } else {
            logsLayout.error(LocalTime.now(), "Cannot create new BluetoothSocket");
            showToast(this, "BB-8 소켓 연결에 실패하였습니다.", Toast.LENGTH_SHORT);
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        logsLayout.debug(LocalTime.now(), "Create new BluetoothSocket...");
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            logsLayout.warn(LocalTime.now(), "Permission is not granted : BLUETOOTH_CONNECT");
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

    private void showToast(Context context, String text, int len) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, text, len);
        toast.show();
    }

    class ConnectThreadHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            // 현재 날짜 구하기
            LocalTime now = LocalTime.now();

            Bundle bundle = msg.getData();
            String cmd = bundle.getString("cmd");
            String value = Objects.requireNonNull(bundle.getString("value")).trim();

            String[] lines = value.split("\n");

            for (String line : lines) {
                if ("send".equals(cmd)) {
                    logsLayout.send(now, line);
                } else if ("recv".equals(cmd)) {
                    logsLayout.recv(now, line);
                }
            }
        }
    }
}