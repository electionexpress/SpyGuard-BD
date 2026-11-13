# SpyGuard BD

**Version:** 0.5.0  
**Platform:** Android (Kotlin + Jetpack Compose + CameraX)  
**Purpose:** Personal privacy inspection helper. This app is not a certified counter-surveillance instrument and cannot guarantee detection of every hidden camera or recorder.

## Current capabilities

- **Camera-assisted inspection (0.4.0):** live rear-camera preview, optional device torch control, and user-triggered evidence photos.
- **Local evidence storage:** inspection photos are saved under the app-private `files/camera-evidence` directory. This feature does not upload photos to a server.
- **Evidence status:** saved photos are labelled `Visual Candidate — manual review required`; no camera is automatically declared verified.
- **Conservative evidence checklist (0.4.0):** combines manual visual observations with manually confirmed camera-specific protocol evidence and direct stream/recording evidence. Wi-Fi/BLE presence, IR glow, or reflection alone cannot produce a verified/active status.
- **Evidence policy unit tests:** tests ensure a camera is not marked verified or active without the required independent evidence.
- **Local ONVIF discovery (0.5.0):** user-initiated, bounded WS-Discovery multicast probe on the current Wi-Fi LAN; displays responding source IP and advertised HTTP(S) service addresses. No IP-range sweep, login, RTSP stream opening, or cloud upload.
- **Wi-Fi scan:** lists nearby networks visible to Android, with SSID, frequency and RSSI when the OS permits access.
- **Bluetooth LE scan:** listens for nearby BLE advertisements, shows device label/address when available and signal strength (RSSI). Scan automatically stops after 15 seconds.
- **IR inspection guide:** explains a manual camera-based check. It does not automatically prove that a light source is an infrared camera.
- **Permission status:** requests runtime permissions only when needed.

## Camera verification roadmap

The current release is the first camera-inspection foundation, not the complete six-engine system.

1. CameraX visual inspection and local evidence capture — implemented.
2. Manual evidence assessment checklist with conservative levels and unit tests — implemented; this is not automatic recognition.
3. Optical lens/reflection analysis with a labelled test dataset — not implemented.
4. Camera-specific AI recognition model and false-positive/false-negative evaluation — not implemented.
5. IR compatibility checks — guidance only; automatic IR detection is not implemented.
6. Local ONVIF WS-Discovery probe — implemented as an opt-in local-network discovery feature; physical-router/camera compatibility testing remains required.
7. RTSP stream validation, authenticated camera inventory, optical/AI recognition and automatic evidence correlation — not implemented.

## Important limits — read before relying on results

- A Wi-Fi or Bluetooth device is not necessarily a camera. Most scan results cannot identify device purpose.
- ONVIF discovery only finds devices that respond to WS-Discovery on the same LAN. Multicast filtering, client isolation, VLANs, disabled ONVIF, unsupported devices, or wired/offline devices can prevent discovery.
- A camera or recorder that is offline, powered off, wired, recording to local storage, shielded, or not advertising over Wi-Fi/Bluetooth may not appear.
- The phone has no general-purpose RF spectrum analyzer. Wi-Fi/BLE scans do not detect all radio frequencies or transmitters.
- Phone cameras have IR filters and vary by model. An IR LED may be invisible; reflections can also come from ordinary objects.
- Camera preview and a captured image do not, by themselves, prove that an object is a camera or that it is recording.
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
- [ ] Camera permission is requested only when opening camera inspection.
- [ ] Denying camera permission leaves the rest of the dashboard usable.
- [ ] Camera preview opens on a physical device and closes when leaving the screen.
- [ ] Torch control is enabled only on devices with a flash unit.
- [ ] Evidence capture creates a JPEG in app-private storage and reports failures clearly.
- [ ] Captured evidence is not uploaded to a cloud service.
- [ ] Wi-Fi scan with permission and Location services enabled shows available networks; empty results are not treated as proof that no devices exist.
- [ ] BLE scan runs, accumulates multiple unique devices, and automatically stops after about 15 seconds.
- [ ] BLE scan can be manually stopped and restarted.
- [ ] Rotate the screen / background and return to confirm no crash or stuck scan.
- [ ] Test with Wi-Fi/Bluetooth disabled and with airplane mode to confirm offline UI behavior.
- [ ] Check battery use during repeated scans.

A green CI run confirms the evidence-policy unit tests, compilation and packaging, not real-world detection accuracy. Camera recognition, IR detection, network camera discovery, and false-positive/false-negative rates require separate implementation and physical-device validation.
