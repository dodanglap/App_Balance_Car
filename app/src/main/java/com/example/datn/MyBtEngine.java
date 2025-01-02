package com.example.datn;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MyBtEngine {

    public static final int BT_STATE_NONE = 0;
    public static final int BT_STATE_CONNECTING = 1;
    public static final int BT_STATE_CONNECTED = 2;

    private static final UUID UUID_BT_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String BT_DEVICE_MAC = "94:54:C5:B6:E2:16";

    private final Context context;
    private final BluetoothAdapter mAdapter;
    private int mState;

    private BtConnectThread mConnectThread;
    private BtWorkThread mWorkThread;

    public MyBtEngine(Context context) {
        this.context = context;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mState = BT_STATE_NONE;
    }

    public synchronized void start() {
        if (mAdapter == null) {
            Toast.makeText(context, "Bluetooth không được hỗ trợ trên thiết bị này", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device;
        try {
            device = mAdapter.getRemoteDevice(BT_DEVICE_MAC);
        } catch (IllegalArgumentException e) {
            Toast.makeText(context, "Địa chỉ MAC không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        stop(); // Hủy các kết nối hiện tại nếu có.

        mConnectThread = new BtConnectThread(device);
        mConnectThread.start();
        setState(BT_STATE_CONNECTING);
    }

    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mWorkThread != null) {
            mWorkThread.cancel();
            mWorkThread = null;
        }
        setState(BT_STATE_NONE);
    }

    private synchronized void setState(int state) {
        Log.d("MyBtEngine", "setState() " + mState + " -> " + state);
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    public void write(byte[] out) {
        BtWorkThread r;
        synchronized (this) {
            if (mState != BT_STATE_CONNECTED || mWorkThread == null) {
                return;
            }
            r = mWorkThread;
        }
        r.write(out);
    }

    private synchronized void startWorker(BluetoothSocket socket, BluetoothDevice device) {
        stop(); // Hủy các kết nối cũ.

        mWorkThread = new BtWorkThread(socket);

        mWorkThread.start();
        setState(BT_STATE_CONNECTED);
    }

    private class BtConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;

        @SuppressLint("MissingPermission")
        public BtConnectThread(BluetoothDevice device) {
            mmDevice = device;
            try {
                mmSocket = device.createRfcommSocketToServiceRecord(UUID_BT_DEVICE);

            } catch (IOException e) {
                Log.e("BtConnectThread", "Không thể tạo socket", e);
            }
        }

        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BtConnectThread", "Quyền BLUETOOTH_CONNECT chưa được cấp");
                Toast.makeText(context, "Quyền Bluetooth chưa được cấp", Toast.LENGTH_SHORT).show();
                return;
            }

            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                synchronized (MyBtEngine.this) {
                    mConnectThread = null;
                }
                startWorker(mmSocket, mmDevice);
            } catch (IOException e) {
                Log.e("BtConnectThread", "Không thể kết nối", e);
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("BtConnectThread", "Không thể đóng socket", closeException);
                }
                setState(BT_STATE_NONE);
                Toast.makeText(context, "Không thể kết nối tới thiết bị", Toast.LENGTH_SHORT).show();
            }
        }

        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e("BtConnectThread", "Không thể đóng socket", e);
            }
        }
    }

    private class BtWorkThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BtWorkThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("BtWorkThread", "Không thể lấy luồng vào/ra", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    // Xử lý dữ liệu nhận được
                    Log.d("BtWorkThread", "Dữ liệu nhận được: " + new String(buffer, 0, bytes));
                } catch (IOException e) {
                    Log.e("BtWorkThread", "Kết nối bị ngắt", e);
                    MyBtEngine.this.start(); // Thử kết nối lại
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e("BtWorkThread", "Không thể gửi dữ liệu", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("BtWorkThread", "Không thể đóng socket", e);
            }
        }
    }
}