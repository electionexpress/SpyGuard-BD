# SpyGuard BD

**Version:** 0.2.1  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Purpose:** Personal privacy inspection helper. This app is not a certified counter-surveillance instrument and cannot guarantee detection of every hidden camera or recorder.

## Current capabilities

- **Wi-Fi scan:** lists nearby networks visible to Android, with SSID, frequency and RSSI when the OS permits access.
- **Bluetooth LE scan:** listens for nearby BLE advertisements, shows device label/address when available and signal strength (RSSI). Scan automatically stops after 15 seconds.
- **IR inspection guide:** explains a manual camera-based check. It does not automatically prove that a light source is an infrared camera.
- **Permission status:** requests runtime permissions only when needed.

## Important limits — read before relying on results

- A Wi-Fi or Bluetooth device is not necessarily a camera. Most scan results cannot identify device purpose.
- A camera or recorder that is offline, powered off, wired, recording to local storage, shielded, or not advertising over Wi-Fi/Bluetooth may not appear.
- The phone has no general-purpose RF spectrum analyzer. Wi-Fi/BLE scans do not detect all radio frequencies or transmitters.
- Phone cameras have IR filters and vary by model. An IR LED may be invisible; reflections can also come from ordinary objects.
- Android does not allow an ordinary app to silently inspect another app's microphone/camera feed. Privacy indicators and Android Privacy Dashboard are the supported ways to review recent sensor access.
- No mobile-only method can promise 100% detection or 100% safety. For a high-risk concern, use a professional inspection and physical checks.

## Build

GitHub Actions runs `gradle assembleDebug` on pushes and pull requests to `main`, then uploads the debug APK as the `SpyGuard-BD-debug` artifact.

To install:
1. Open the latest successful **Android CI** run.
2. Download `SpyGuard-BD-debug` and extract the ZIP.
3. Install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that browser/file manager.
4. Review requested permissions; deny any permission that is not needed for the feature you are testing.

## Manual device QA checklist

- [ ] App launches and dashboard shows correct device / Android version.
- [ ] Denying permissions produces a clear explanation and does not crash the app.
- [ ] Wi-Fi scan with permission and Location services enabled shows available networks; empty results are not treated as proof that no devices exist.
- [ ] BLE scan runs, accumulates multiple unique devices, and automatically stops after about 15 seconds.
- [ ] BLE scan can be manually stopped and restarted.
- [ ] Turning Bluetooth off produces a clear message.
- [ ] Rotate the screen / background and return to confirm no crash or stuck scan.
- [ ] Test with Wi-Fi/Bluetooth disabled and with airplane mode to confirm offline UI behavior.
- [ ] Check battery use during repeated scans.

## Roadmap (not yet implemented)

- Camera preview-assisted visual inspection and optional torch control with explicit user action.
- Android privacy-dashboard guidance and a local sensor-use checklist.
- Better scan history, deduplication, and exportable reports.
- Automated unit/instrumentation tests and a documented device test matrix.

A green CI build confirms compilation and packaging, not the real-world accuracy of detection features. Each capability must be verified on physical devices.
