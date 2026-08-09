# MultiSpeaker MP3 — first test

Target: Samsung Galaxy M21 / Android 12 / One UI 4.1.

Purpose:
- Connect two Bluetooth media speakers through Samsung Bluetooth.
- Select an MP3 stored on the phone.
- Create two MediaPlayer instances and request a different Android output device for each.

This is an EXPERIMENTAL test. Android/Samsung may still route both players to one Bluetooth A2DP device. It does not guarantee simultaneous two-speaker playback.

Build:
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install app-debug.apk.

Test:
1. Connect Aavante Bar 590 and Stone 1208.
2. Open app and grant Bluetooth permission.
3. Refresh Bluetooth outputs.
4. Select the two speakers.
5. Choose a local MP3.
6. Press PLAY ON BOTH.
