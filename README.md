# SpyGuard BD

**Version:** 0.7.0  
**Platform:** Android (Kotlin + Jetpack Compose + CameraX)  
**Purpose:** Personal privacy inspection helper. This app is not a certified counter-surveillance instrument and cannot guarantee detection of every hidden camera or recorder.

## Current capabilities

- **Camera-assisted inspection (0.4.0):** live rear-camera preview, optional device torch control, and user-triggered evidence photos.
- **Local evidence storage:** inspection photos are saved under the app-private `files/camera-evidence` directory. This feature does not upload photos to a server.
- **Evidence status:** saved photos are labelled `Visual Candidate — manual review required`; no camera is automatically declared verified.
- **Conservative evidence checklist (0.4.0):** combines manual visual observations with manually confirmed camera-specific protocol evidence and direct stream/recording evidence. Wi-Fi/BLE presence, IR glow, or reflection alone cannot produce a verified/active status.
- **Evidence policy unit tests:** tests ensure a camera is not marked verified or active without the required independent evidence.
- **Local ONVIF discovery (0.7.0):** user-initiated, bounded WS-Discovery multicast probe on the current Wi-Fi LAN; displays responding source IP and advertised HTTP(S) service addresses. No IP-range sweep, login, RTSP stream opening, or cloud upload.
- **Wi-Fi scan:** lists nearby networks visible to Android, with SSID, frequency and RSSI when the OS permits access.
- **Bluetooth LE scan:** listens for nearby BLE advertisements, shows device label/address when available and signal strength (RSSI). Scan automatically stops after 15 seconds.
- **Nearby-device category hints:** BLE advertised names are conservatively grouped into possible phone/tablet, computer, network equipment, camera/surveillance, audio, wearable, or unknown categories. These are name-based hints only, not verified make/model or proof of device purpose; unnamed/randomized advertisements remain unknown.
- **Local LAN service discovery (0.7.0):** opt-in mDNS/DNS-SD discovery for advertised Chromecast, AirPlay, printer, SSH, SMB, HTTP(S), HomeKit and RTSP service types. Displays discovered service names and resolved endpoint information when Android provides it. This is service discovery, not a complete device inventory; multiple services may belong to one device. No IP-range sweep, login or endpoint connection is performed.
- **IR inspection guide:** explains a manual camera-based check. It does not automatically prove that a light source is an infrared camera.
- **Permission status:** requests runtime permissions only when needed.



## Nearby electronic-device discovery (0.7.0)

The primary workflow is now nearby electronic-device inspection. The app combines Android-visible Wi-Fi scan results, BLE advertisements, and same-LAN ONVIF WS-Discovery responses as separate evidence sources. BLE names receive conservative category hints (possible phone/tablet, computer, network equipment, camera/surveillance, audio, wearable, or unknown); the app does not claim a verified model from a broadcast name. Wi-Fi SSIDs are access points/networks, not a list of connected client phones. Android does not expose a universal nearby-device inventory or arbitrary phone camera state to ordinary apps. A device with radios disabled, no discoverable advertisement, or no supported LAN service may remain invisible. For reliable inventory of a device, it must cooperate or be explicitly paired; Companion mode is not yet implemented.

## Unified nearby device inventory (0.7.0)

The dashboard combines current BLE observations and Android-visible Wi-Fi scan results into a single local inventory view. Each row preserves its observation source, available RSSI, conservative category hint, and an explanation. Wi-Fi entries are access points/networks, not connected client devices. BLE device names are self-advertised and can be absent, randomized, misleading, or spoofed; name-based categories are not verified model identification. Duplicate rows are removed by source key within the current scan snapshot.

Users can share a JSON report for the current in-memory scan session using Android's share sheet. The report is generated locally and is not automatically uploaded. It records the scan timestamp and explicit coverage limitations. It does not persist a surveillance history or claim that absent devices are safe.

## Nearby Android phone camera-activity detection

A standard Android app cannot remotely query whether an arbitrary nearby Android phone has opened or is using its camera. Wi-Fi SSIDs, BLE advertisements, device names, RSSI, and ordinary network presence are not evidence that a camera is active. The current app therefore does **not** generate a “nearby phone camera active” alert from those signals.

A future opt-in **SpyGuard Companion Mode** can provide a limited cooperative signal:

- The monitored Android phone must have SpyGuard Companion installed and explicitly paired/consented to.
- The companion may observe its own device camera availability using Android camera APIs and publish a short-lived, authenticated “camera busy” event while permitted to run.
- The receiving SpyGuard app can alert only that a paired companion reported camera-busy status. It must not claim that video is being recorded, identify the app using the camera, or infer malicious intent.
- A camera-busy event is not recording proof; Android may make a camera unavailable for reasons other than recording.
- Bluetooth/Wi-Fi disabled, background restrictions, permission revocation, battery optimisation, radio range, or companion termination can prevent delivery. No signal means **unknown**, not safe.
- Pairing must use explicit consent, rotating identifiers, authenticated messages, minimal retention, and a visible stop-sharing control. No covert installation or monitoring is supported.

This companion protocol is a roadmap item, not an implemented feature in v0.7.0. The dashboard explains this limitation so that normal nearby-device detections are not misrepresented as camera activity.

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
- [ ] ONVIF discovery returns a response from a known ONVIF test camera on the same Wi-Fi LAN.
- [ ] ONVIF discovery handles no-response, multicast-disabled and Wi-Fi-disconnected cases without crashing.
- [ ] Confirm the app never logs into discovered endpoints or opens RTSP streams automatically.
- [ ] Rotate the screen / background and return to confirm no crash or stuck scan.
- [ ] Test with Wi-Fi/Bluetooth disabled and with airplane mode to confirm offline UI behavior.
- [ ] Check battery use during repeated scans.

A green CI run confirms the evidence-policy unit tests, compilation and packaging, not real-world detection accuracy. Camera recognition, IR detection, network camera discovery, and false-positive/false-negative rates require separate implementation and physical-device validation.
