package com.example.multispeaker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_BT = 10;
    private static final int REQ_FILE = 11;

    private AudioManager audioManager;
    private final List<AudioDeviceInfo> devices = new ArrayList<>();
    private Spinner speaker1, speaker2;
    private TextView status, fileName;
    private Uri audioUri;
    private MediaPlayer player1, player2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        speaker1 = findViewById(R.id.speaker1);
        speaker2 = findViewById(R.id.speaker2);
        status = findViewById(R.id.status);
        fileName = findViewById(R.id.fileName);

        findViewById(R.id.refreshButton).setOnClickListener(v -> {
            requestBluetoothPermissionThenRefresh();
        });

        findViewById(R.id.fileButton).setOnClickListener(v -> chooseAudio());

        findViewById(R.id.playButton).setOnClickListener(v -> playBoth());
        findViewById(R.id.pauseButton).setOnClickListener(v -> pauseBoth());
        findViewById(R.id.stopButton).setOnClickListener(v -> stopBoth());

        requestBluetoothPermissionThenRefresh();
    }

    private void requestBluetoothPermissionThenRefresh() {
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, REQ_BT);
        } else {
            refreshDevices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_BT) refreshDevices();
    }

    private void refreshDevices() {
        devices.clear();
        List<String> names = new ArrayList<>();
        AudioDeviceInfo[] outs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        for (AudioDeviceInfo d : outs) {
            int t = d.getType();
            if (t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                t == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                t == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                t == AudioDeviceInfo.TYPE_BLE_SPEAKER) {
                devices.add(d);
                String name;
                try { name = d.getProductName().toString(); }
                catch (Exception e) { name = "Bluetooth device"; }
                names.add(name + " (" + typeName(t) + ")");
            }
        }

        if (names.isEmpty()) {
            names.add("No Bluetooth media output detected");
            status.setText("No Bluetooth media output detected. Connect both speakers in Samsung Bluetooth settings, then Refresh.");
        } else {
            status.setText("Found " + names.size() + " Bluetooth media output(s).");
        }

        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        speaker1.setAdapter(a);
        speaker2.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        if (names.size() > 1) speaker2.setSelection(1);
    }

    private String typeName(int t) {
        if (t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return "A2DP";
        if (t == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) return "SCO";
        if (t == AudioDeviceInfo.TYPE_BLE_HEADSET) return "BLE headset";
        if (t == AudioDeviceInfo.TYPE_BLE_SPEAKER) return "BLE speaker";
        return "Bluetooth";
    }

    private void chooseAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("audio/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, REQ_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == Activity.RESULT_OK && data != null) {
            audioUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                    audioUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            fileName.setText("Selected: " + audioUri);
        }
    }

    private void playBoth() {
        if (audioUri == null) {
            status.setText("Choose an MP3 first.");
            return;
        }
        if (devices.size() < 2) {
            status.setText("Need two Bluetooth media outputs. Connect both speakers and Refresh.");
            return;
        }

        stopBoth();

        try {
            AudioDeviceInfo d1 = devices.get(speaker1.getSelectedItemPosition());
            AudioDeviceInfo d2 = devices.get(speaker2.getSelectedItemPosition());

            player1 = new MediaPlayer();
            player2 = new MediaPlayer();

            player1.setDataSource(this, audioUri);
            player2.setDataSource(this, audioUri);

            if (Build.VERSION.SDK_INT >= 23) {
                player1.setPreferredDevice(d1);
                player2.setPreferredDevice(d2);
            }

            player1.setOnPreparedListener(mp -> {
                mp.start();
                status.setText("Playing player 1 + player 2. Listen to BOTH speakers.");
            });
            player2.setOnPreparedListener(MediaPlayer::start);

            player1.setOnErrorListener((mp, what, extra) -> {
                status.setText("Player 1 error: " + what + "/" + extra);
                return true;
            });
            player2.setOnErrorListener((mp, what, extra) -> {
                status.setText("Player 2 error: " + what + "/" + extra);
                return true;
            });

            player1.prepareAsync();
            player2.prepareAsync();

        } catch (Exception e) {
            status.setText("Could not start: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void pauseBoth() {
        try { if (player1 != null && player1.isPlaying()) player1.pause(); } catch (Exception ignored) {}
        try { if (player2 != null && player2.isPlaying()) player2.pause(); } catch (Exception ignored) {}
    }

    private void stopBoth() {
        try { if (player1 != null) { player1.stop(); player1.release(); } } catch (Exception ignored) {}
        try { if (player2 != null) { player2.stop(); player2.release(); } } catch (Exception ignored) {}
        player1 = null;
        player2 = null;
    }

    @Override
    protected void onDestroy() {
        stopBoth();
        super.onDestroy();
    }
}
