package com.example.datn;

import static java.lang.String.format;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity2 extends AppCompatActivity {
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private Button up, down, left, right;

    private SeekBar speedSeekBar, kpSeekBar, kdSeekBar, kiSeekBar;
    private TextView speedText, kpText, kdText, kiText;
    private TextView tvAx, tvAy, tvAz, tvGx, tvGy, tvGz, tvSpeed, tvKp, tvKd, tvKi;

    private float kpValue = 0;
    private float kdValue = 0;
    private float kiValue = 0;
    private int spdV = 0;

    private boolean isUpPressed = false;
    private boolean isDownPressed = false;
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;

    private String lastCommand = "";
    private int lastSpeedValue = -1;
    private float lastKpValue = -1;
    private float lastKdValue = -1;
    private float lastKiValue = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main2);

        // Lấy socket Bluetooth từ Intent
        bluetoothSocket = BluetoothSocketSingleton.getInstance().getBluetoothSocket();

        if (bluetoothSocket != null) {
            try {
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
            } catch (IOException e) {
                Log.e("MainActivity2", "Không thể lấy luồng xuất", e);
            }
        }



        up = findViewById(R.id.up1);
        down = findViewById(R.id.down1);
        left = findViewById(R.id.left1);
        right = findViewById(R.id.right1);

        speedSeekBar = findViewById(R.id.seekBar);
        kpSeekBar = findViewById(R.id.seekBar2);
        kdSeekBar = findViewById(R.id.seekBar3);
        kiSeekBar = findViewById(R.id.seekBar4);

        speedText = findViewById(R.id.textView1);
        kpText = findViewById(R.id.textView3);
        kdText = findViewById(R.id.textView4);
        kiText = findViewById(R.id.textView5);
        tvAx = findViewById(R.id.tv_ax);
        tvAy = findViewById(R.id.tv_ay);
        tvAz = findViewById(R.id.tv_az);
        tvGx = findViewById(R.id.tv_gx);
        tvGy = findViewById(R.id.tv_gy);
        tvGz = findViewById(R.id.tv_gz);
        tvSpeed = findViewById(R.id.tv_sp);
        tvKp = findViewById(R.id.tv_kp);
        tvKd = findViewById(R.id.tv_kd);
        tvKi = findViewById(R.id.tv_ki);


        setupButtonListeners();
        setupSeekBarListeners();
        listenForData();
    }
    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);
                    // Cập nhật giá trị từ luồng lên gui chính
                    if (receivedMessage.length() > 1){
                        runOnUiThread(() -> updateTextViews_Recv(receivedMessage));
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    private void updateTextViews_Recv(String receivedMessage) {
        // Tách các cặp "key:value"
        String[] pairs = receivedMessage.split(" ");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // Cập nhật TextView tương ứng
                switch (key) {
                    case "ax":
                        tvAx.setText("Ax: " + value);
                        break;
                    case "ay":
                        tvAy.setText("Ay: " + value);
                        break;
                    case "az":
                        tvAz.setText("Az: " + value);
                        break;
                    case "gx":
                        tvGx.setText("Gx: " + value);
                        break;
                    case "gy":
                        tvGy.setText("Gy: " + value);
                        break;
                    case "gz":
                        tvGz.setText("Gz: " + value);
                        break;
                    case "SP":
                        tvSpeed.setText("Speed: " + value);
                        break;
                    case "Kp":
                        tvKp.setText("Kp: " + value);
                        break;
                    case "Kd":
                        tvKd.setText("Kd: " + value);
                        break;
                    case "Ki":
                        tvKi.setText("Ki: " + value);
                        break;
                }
            }
        }
    }


    public static class BluetoothSocketSingleton {
        private static BluetoothSocketSingleton instance;
        private BluetoothSocket bluetoothSocket;

        private BluetoothSocketSingleton() {

        }

        public static synchronized BluetoothSocketSingleton getInstance() {
            if (instance == null) {
                instance = new BluetoothSocketSingleton();
            }
            return instance;
        }

        public BluetoothSocket getBluetoothSocket() {
            return bluetoothSocket;
        }

        public void setBluetoothSocket(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtonListeners() {
        up.setOnTouchListener((v, event) -> handleButtonTouch(event, "UP"));
        down.setOnTouchListener((v, event) -> handleButtonTouch(event, "DOWN"));
        left.setOnTouchListener((v, event) -> handleButtonTouch(event, "LEFT"));
        right.setOnTouchListener((v, event) -> handleButtonTouch(event, "RIGHT"));
    }

    private boolean handleButtonTouch(MotionEvent event, String command) {
        Button button;
        switch (command) {
            case "UP":
                button = up;
                break;
            case "DOWN":
                button = down;
                break;
            case "LEFT":
                button = left;
                break;
            case "RIGHT":
                button = right;
                break;
            default:
                return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                button.setScaleX(0.9f);
                button.setScaleY(0.9f);
                button.setAlpha(0.7f);
                updateButtonState(command, true);
                break;
            case MotionEvent.ACTION_UP:
                button.setScaleX(1.0f);
                button.setScaleY(1.0f);
                button.setAlpha(1.0f);
                updateButtonState(command, false);
                break;
        }
        sendCombinedCommand();
        return true;
    }

    private void updateButtonState(String command, boolean isPressed) {
        switch (command) {
            case "UP":
                isUpPressed = isPressed;
                break;
            case "DOWN":
                isDownPressed = isPressed;
                break;
            case "LEFT":
                isLeftPressed = isPressed;
                break;
            case "RIGHT":
                isRightPressed = isPressed;
                break;
        }
    }

    private void sendCombinedCommand() {
        StringBuilder lstcm = new StringBuilder();

        if (isUpPressed) lstcm.append("UP-");
        if (isDownPressed) lstcm.append("DOWN-");
        if (isLeftPressed) lstcm.append("LEFT-");
        if (isRightPressed) lstcm.append("RIGHT-");

        if (lstcm.length() > 0) {
            lstcm.setLength(lstcm.length() - 1); // Xóa dấu -
        } else {
            lstcm.append("STOP");
        }

        String lstgui = lstcm.toString();
        if (!lstgui.equals(lastCommand))
        {
            sendDataToESP32(lstgui);
            lastCommand = lstgui;
        }
    }

    private void setupSeekBarListeners() {
        speedSeekBar.setMax(99);
        speedSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener(progress -> {
            spdV = progress;
            speedText.setText(format("Speed: %d", spdV));
        }, () -> {
            if (spdV != lastSpeedValue) {
                sendDataToESP32(format("Speed%d", spdV));
                lastSpeedValue = spdV;
            }
        }));

        kpSeekBar.setMax(990);
        kpSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener(progress -> {
            kpValue = (float) progress / 10;
            kpText.setText(format("KP: %.1f", kpValue));
        }, () -> {
            if (kpValue != lastKpValue) {
                sendDataToESP32(format("Kp%.1f", kpValue));
                lastKpValue = kpValue;
            }
        }));

        kdSeekBar.setMax(990);
        kdSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener(progress -> {
            kdValue = (float) progress / 10;
            kdText.setText(format("KD: %.1f", kdValue));
        }, () -> {
            if (kdValue != lastKdValue) {
                sendDataToESP32(format("Kd%.1f", kdValue));
                lastKdValue = kdValue;
            }
        }));

        kiSeekBar.setMax(990);
        kiSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener(progress -> {
            kiValue = (float) progress / 10;
            kiText.setText(format("KI: %.1f", kiValue));
        }, () -> {
            if (kiValue != lastKiValue) {
                sendDataToESP32(format("Ki%.1f", kiValue));
                lastKiValue = kiValue;
            }
        }));
    }


    private void sendDataToESP32(String data) {

        if (outputStream != null) {
            try {
                outputStream.write((data + "\n").getBytes());
                Log.d("Bluetooth", "Sent: " + data);
            } catch (IOException e) {
                Log.e("Bluetooth", "Error sending data", e);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "Error closing Bluetooth socket", e);
            }
        }
    }

    private interface SeekBarChangeListener {
        void onProgressChanged(int progress);
    }

    private static class SimpleSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final SeekBarChangeListener onProgressChangedListener;
        private final Runnable onStopTrackingTouchListener;

        SimpleSeekBarChangeListener(SeekBarChangeListener progressChangedListener, Runnable stopTrackingTouchListener) {
            this.onProgressChangedListener = progressChangedListener;
            this.onStopTrackingTouchListener = stopTrackingTouchListener;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                onProgressChangedListener.onProgressChanged(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            onStopTrackingTouchListener.run();
        }
    }
}
