package com.painnick.bb8controller;


import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {

    static final String TAG = "BT";

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    protected Handler handler;
    protected boolean isRun = true;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        this.handler = handler;

        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (isRun) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if (bytes != 0) {
                    buffer = new byte[1024];
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                    Log.d(TAG, new String(buffer));

                    sendToHandler("recv", buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
    }

    private void sendToHandler(String cmd, byte[] buffer) {
        Bundle bundle = new Bundle();
        bundle.putString("cmd", cmd);
        bundle.putString("value", new String(buffer));

        Message message = handler.obtainMessage();
        message.setData(bundle);

        handler.sendMessage(message);
    }


    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes(); //converts entered String into bytes
        try {
            mmOutStream.write(bytes);

            sendToHandler("send", bytes);
        } catch (IOException e) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        isRun = false;
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}
