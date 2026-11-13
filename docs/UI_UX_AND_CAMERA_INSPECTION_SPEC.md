# SpyGuard BD — UI/UX and Camera Inspection Engineering Specification

**Status:** Design and engineering specification (not a claim that these features are implemented)
**Target:** Native Android app, Kotlin + Jetpack Compose
**Current baseline:** v0.2.1

## Product priorities

1. Make camera inspection the primary workflow.
2. Present evidence and confidence honestly; never label an unverified signal as a confirmed hidden camera.
3. Keep all scanning user-initiated, visible, permission-based, and stoppable.
4. Work offline for local visual inspection and keep inspection records on-device by default.
5. Make the interface accessible, responsive, bilingual (Bangla/English), and usable one-handed.

## Visual design system

- Offer Dark and Light themes; default to a restrained dark security theme.
- Palette: deep navy/graphite surfaces, high-contrast white text, cyan/teal interactive accents, amber for uncertain findings, red only for verified critical warnings, green for passed checks.
- Use Material 3 design tokens, consistent 8dp spacing, 12–20dp corner radii, clear typography hierarchy, and minimum 48dp touch targets.
- Use restrained motion: radar sweep, scan progress, signal history, and state transitions. Respect Android reduced-motion/accessibility settings and avoid flashing effects.
- Use vector icons and charts; do not use decorative animation as a substitute for sensor evidence.
- Provide Bangla and English strings through Android resources; do not hard-code interface text in composables.

## Information architecture

Bottom navigation:
- Home
- Camera Check (primary)
- Devices
- Reports
- Settings

Home dashboard:
- Large Camera Check action
- Inspection readiness and permission status
- Last inspection summary
- Separate cards for visible cameras, optical checks, network discoveries, and hardware sensor status
- Clear “not scanned / scan unavailable / no finding / possible finding / verified manually” states

Camera Check workflow:
1. Explain the selected test and its limitations.
2. Ask for only the required permission at the moment it is needed.
3. Provide a live CameraX preview with user-controlled torch; never activate camera or torch covertly.
4. Offer guided room sweep with on-screen prompts and coverage checklist.
5. Run optional optical reflection checks with a calibration step and capture evidence only after explicit user action.
6. Present findings with evidence type, timestamp, confidence band, and recommended manual verification.
7. Allow the user to save/export a report after preview and consent.

Devices:
- Separate Wi-Fi access points, BLE advertisements, and other supported sources.
- Deduplicate observations and show source, RSSI, first/last seen, and whether manufacturer/model is actually known.
- Never infer exact phone model or owner from a randomized address or generic advertisement.

Reports:
- Timeline, filters, evidence detail, export/delete controls, and clear retention settings.
- Distinguish observations from conclusions and user-confirmed findings.

## Camera inspection engine — modular design

### A. Guided visible-camera inspection
- Use CameraX preview and a room-by-room checklist.
- Provide practical prompts for likely placements (smoke detectors, clocks, chargers, vents, decor, mirrors) without asserting that an object is a camera.
- Let users mark an object for closer inspection and attach an optional photo.
- Use on-device image analysis only as an assistive classifier; return “possible camera-like object” with confidence and a request for human verification.

### B. Lens-reflection assistance
- Use a controlled, user-directed light/torch and camera preview.
- Detect candidate bright glints/reflections and show their coordinates/region for manual inspection.
- Calibrate for distance, angle, exposure, lens cover, and device camera characteristics.
- Treat ordinary glass, screws, LEDs, and reflective surfaces as common false positives.
- Do not call a reflection a confirmed lens or hidden camera.

### C. Infrared (IR) assistance
- Provide a user-guided test for IR emitters using the phone camera only where the device camera can actually see the relevant wavelength.
- Explain that many phone cameras include IR-cut filters; results vary by model and cannot rule cameras in or out.
- Do not claim that a bright point is a camera; it may be a remote control or another IR source.
- Keep this module labeled “experimental / device-dependent” until validated on a documented phone test matrix.

### D. Network camera clues
- Inspect only networks and devices visible through Android's permitted Wi-Fi/BLE APIs or a network the user is authorized to inspect.
- Identify known camera services only when there is reliable, protocol-level evidence.
- A device on the network is not automatically a camera; an unknown device is not automatically malicious.
- Do not attempt credential guessing, exploitation, packet interception, or access to devices without authorization.

### E. External RF sensor integration (future hardware)
- Define a documented interface for an external spectrum analyzer/SDR or purpose-built RF detector.
- Display frequency band, measured signal strength, time history, and sensor calibration status.
- Explain that RF energy does not identify a camera by itself; ordinary routers, phones, wearables, and appliances produce signals.
- Keep hardware findings separate from optical/network findings and correlate only with transparent evidence.

## Result model

Each observation should include:
- observationId, sourceType, observedAt, locationWithinRoom (optional user annotation)
- raw measurement and units (where appropriate)
- device/candidate category, known/unknown status
- confidence band and explanation
- false-positive notes and verification steps
- user-confirmed status (separate from algorithm output)

Recommended result labels:
- No candidate detected in this test (not “room is safe”)
- Possible optical reflection
- Possible camera-like object
- Possible IR emitter (device-dependent)
- Network device — purpose unknown
- Camera service identified (only with reliable evidence)
- Manually verified by user

## Privacy, permissions, and safety

- Request CAMERA only for camera inspection; request location only where Android Wi-Fi scan rules require it and explain why.
- Do not request microphone, contacts, SMS, call logs, or background location for the core inspection workflow.
- No covert camera/microphone access, hidden recording, or scanning of other people's private content.
- Keep raw images local by default; explicit consent before sharing/exporting.
- Provide a clear stop control and foreground indication for every active scan.
- Use Android Keystore for locally stored sensitive report data where encryption is needed.

## Technical implementation plan

1. Audit current Compose screens, navigation, theme, permission flows, and scan lifecycle before modifying production UI.
2. Extract design tokens and reusable components: StatusCard, ScanActionCard, EvidenceRow, SignalChart, PermissionBanner, EmptyState, and ResultConfidence.
3. Rework navigation and Camera Check as the primary screen.
4. Add CameraX preview and lifecycle-safe torch controls.
5. Add guided inspection checklist and local report model using Room.
6. Add optical/IR assistance as explicitly experimental modules with device-specific validation.
7. Add authorized network camera clues and optional external RF hardware adapter.
8. Add unit tests, Compose UI tests, permission-denial tests, lifecycle tests, and physical-device validation.

## Acceptance criteria

- No scan starts without an explicit user action.
- Camera preview/torch stop when the user stops the scan or the lifecycle requires it.
- Permission denial produces a helpful explanation and no crash.
- UI clearly differentiates no finding, unsupported test, inconclusive result, and verified finding.
- Every possible finding shows evidence and verification guidance.
- No screen promises 100% hidden-camera detection or identifies every nearby phone.
- Tests cover small screens, large font sizes, Bangla strings, dark/light themes, rotation, background/foreground, and low-light camera behavior.
- CI compilation and tests pass; physical-device test results are recorded separately from build success.

## Explicit limitations

A phone-only app cannot detect every powered-off, wired, shielded, non-transmitting, or locally recording camera. A phone camera is not a calibrated IR detector or RF spectrum analyzer. External RF hardware can improve measurement coverage but cannot prove that a signal belongs to a camera without additional evidence. No “all clear” result should be presented as a guarantee of privacy or safety.
