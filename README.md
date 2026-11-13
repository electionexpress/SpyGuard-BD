# SpyGuard BD

**Version:** 0.2.1  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Purpose:** Personal privacy inspection helper. This app is not a certified counter-surveillance instrument and cannot guarantee detection of every hidden camera or recorder.

## Current capabilities

- **Wi-Fi scan:** lists nearby networks visible to Android, with SSID, frequency and RSSI when the OS permits access.
- **Bluetooth LE scan:** listens for nearby BLE advertisements, shows an advertised label, masked address and signal strength (RSSI), plus an observation-freshness label. Names and signal strength do not verify a device model, physical identity, or camera function. Scan automatically stops after 15 seconds.
- **CameraX live preview:** user-authorized back-camera preview with explicit torch control where flash hardware is available, plus device-supported manual zoom controls.
- **Optical observation checklist:** lets the user record lens-like elements, repeatable glints from multiple angles, visible power/data connections, and independent rechecks. These are manual observations, not automatic detections or a camera-risk score.
- **Inspection report history:** creates user-authored plain-text reports with local timestamp, room label, checklist values, notes, and the currently visible Wi-Fi/BLE scan results. Users can share through Android's share sheet, export a `.txt` file through the document picker, or explicitly save a copy in app-private local history and later share/delete it. History is not uploaded or synced. Wireless entries are observations only, not camera identification.
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

GitHub Actions runs `gradle testDebugUnitTest assembleDebug` on pushes and pull requests to `main`, then uploads the debug APK as the `SpyGuard-BD-debug` artifact.

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
- [ ] BLE observation freshness changes from live to recent to stale at the documented time thresholds; advertised names are not presented as verified device models.
- [ ] Turning Bluetooth off produces a clear message.
- [ ] Grant CAMERA permission and verify the CameraX back-camera preview starts; deny permission and verify the explanation is clear.
- [ ] Verify torch control only enables when the device has flash hardware; leave the screen and confirm the torch turns off.
- [ ] Verify preview zoom controls respect the device-reported minimum and maximum zoom ratios; confirm unsupported zoom ranges remain safely disabled.
- [ ] Test each optical observation checkbox and reset; confirm the app never labels observations as a confirmed camera.
- [ ] Create a report with a room label and notes; verify timestamp and checklist values, then share only to a destination selected by the user.
- [ ] Create a report with blank optional fields; verify the report clearly labels them as unspecified and does not claim a camera was detected.
- [ ] Save a report as `.txt` through Android's document picker; verify the selected destination contains the UTF-8 report and cancelling does not create a file.
- [ ] Explicitly save a report to local history; verify it remains after leaving and reopening the screen, can be shared, and can be deleted.
- [ ] Run Wi-Fi/BLE scans, create a report, and verify only the currently visible scan results are attached with the non-camera-identification warning.
- [ ] Press Home while the torch is on; confirm the torch turns off, then return and verify preview behavior.
- [ ] Revoke CAMERA permission in Android Settings while the app is backgrounded; return and confirm the preview is released and permission is requested again.
- [ ] Rotate the screen / background and return to confirm no crash or stuck scan.
- [ ] Test with Wi-Fi/Bluetooth disabled and with airplane mode to confirm offline UI behavior.
- [ ] Check battery use during repeated scans.

## Roadmap (not yet implemented)

- Automated image-based lens/glint candidate analysis with device-specific validation and false-positive evaluation.
- Android privacy-dashboard guidance and a local sensor-use checklist.
- Scan-result deduplication and linking user-selected Wi-Fi/BLE observations to a report.
- Instrumentation tests and a documented physical-device test matrix.

A green CI build confirms compilation and packaging, not the real-world accuracy of detection features. Each capability must be verified on physical devices.
