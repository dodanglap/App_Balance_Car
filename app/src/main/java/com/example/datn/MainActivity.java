package com.example.datn;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final UUID UUID_BT_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    LoadingDialog loadingDialog=new LoadingDialog(MainActivity.this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button enableBluetoothButton = findViewById(R.id.enableBluetoothButton);
        Button discoverDevicesButton = findViewById(R.id.discoverDevicesButton);
        ListView devicesListView = findViewById(R.id.devicesListView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        devicesListView.setAdapter(deviceListAdapter);

        enableBluetoothButton.setOnClickListener(v -> enableBluetooth());
        discoverDevicesButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                discoverDevices();
            }
        });

        devicesListView.setOnItemClickListener((parent, view, position, id) -> {
            // Hiển thị dialog trước khi bắt đầu kết nối
            loadingDialog.startLoadingDialog();

            String selectedDevice = deviceListAdapter.getItem(position);
            if (selectedDevice != null) {
                String deviceAddress = selectedDevice.split("\n")[1];

                // Bắt đầu một luồng riêng để kết nối và xử lý
                new Thread(() -> {
                    try {
                        connectToDevice(deviceAddress);
                    } finally {
                        // Đóng dialog khi kết nối xong hoặc gặp lỗi
                        runOnUiThread(loadingDialog::dismissDialog);
                    }
                }).start();
            }
        });

    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
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
            startActivityForResult(enableBluetoothIntent, ENABLE_BLUETOOTH_REQUEST);
        } else {
            Toast.makeText(this, "Bluetooth đã được bật", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private void discoverDevices() {
        deviceListAdapter.clear();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
        }

        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "Không có thiết bị nào đã ghép nối", Toast.LENGTH_SHORT).show();
        }
    }



    private void connectToDevice(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
            return;
        }

        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID_BT_DEVICE);
            socket.connect();

            // Lưu BluetoothSocket vào Singleton
            MainActivity2.BluetoothSocketSingleton.getInstance().setBluetoothSocket(socket);



            // Chuyển sang MainActivity2
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã kết nối đến " + device.getName(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);
            });

        } catch (IOException e) {
            e.printStackTrace();
            // Đóng socket nếu xảy ra lỗi
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Không thể kết nối", Toast.LENGTH_SHORT).show();
            });
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền", Toast.LENGTH_SHORT).show();
                discoverDevices();
            } else {
                Toast.makeText(this, "Quyền bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
