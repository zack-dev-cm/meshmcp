# BitChat Android

This directory contains a minimal Android implementation of BitChat using Jetpack Compose. It mirrors the iOS project and is based on the [BitChat project](https://github.com/permissionlesstech/bitchat). The app continues to be distributed under the same [Unlicense](../LICENSE) terms.

## Privacy & Security

The project follows BitChat's privacy-first design:

- **No personal data collection** – no names, emails or phone numbers are gathered
- **No servers** – all messaging happens peer-to-peer on your device
- **No tracking** – the code contains no telemetry or analytics

> Private message and channel features have not received external security review and may contain vulnerabilities. Do not use for sensitive use cases, and do not rely on its security until it has been reviewed.

## Building

```
cd android_app
./gradlew assembleDebug
```

Target SDK 33, minSdk 26+. Required permissions are declared in the manifest for Bluetooth and location.

## Runtime Permissions

The app requires several permissions at runtime:

- `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` and `BLUETOOTH_CONNECT` – allow peering with nearby devices over Bluetooth LE for discovery and data transfer.
- `ACCESS_FINE_LOCATION` – Android ties Bluetooth scanning to location access; without it the system blocks discovery of peers.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_CONNECTED_DEVICE` – keep the mesh service alive in the background and permit sending data while running as a foreground service.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` – prevents aggressive power management from killing the service so messages can relay even when the phone is idle.

These permissions are requested on launch to start the mesh service automatically.

### Advertising Size Limits

Android restricts BLE advertisement packets to 31 bytes. The service truncates
your nickname to seven characters before advertising to avoid exceeding this
limit. If the system still reports `ADVERTISE_FAILED_DATA_TOO_LARGE`, the
service retries once with a shorter name and logs the adjustment.

## Testing & Linting

Run unit tests and Kotlin formatting checks:

```
./gradlew test ktlintCheck
```

## Basic Usage

Launching the app automatically starts the background mesh service once the above permissions are granted. Messages are exchanged directly between nearby peers—no servers are involved. You can interact using a few built‑in chat commands:

- `/msg <peerId> <message>` – send a private message to a peer.
- `/who` – list currently connected peers.
- `/wipe` – erase all local data and restart the service.

All conversations occur only on your device and the devices you connect with. No data is sent to any third‑party servers.

## High-Level Test Strategy

This project uses Gradle with Kotlin and Jetpack Compose. Automated testing ensures robustness across core components and the user interface.

### Types of Tests

- **Unit Tests** – Validate individual classes such as view models, repositories, and utility functions in isolation. Use JUnit and MockK for mocking dependencies.
- **Integration Tests** – Exercise multiple modules together, including persistence and networking layers. Run these as instrumented tests on an emulator with `./gradlew connectedCheck`.
- **Functional/UI Tests** – Verify complete user flows using Compose UI testing or Espresso. Examples include starting the mesh service, exchanging messages, and wiping local data.

### Compile & Run Verification

To ensure all Kotlin sources build and run properly:

1. **Assemble builds**: `./gradlew assembleDebug assembleRelease` compiles every module and produces APKs. This catches syntax errors or missing dependencies.
2. **Run unit tests**: `./gradlew test` executes all JUnit tests, confirming that business logic executes as expected with the latest code.
3. **Run instrumented tests**: `./gradlew connectedAndroidTest` launches the app on a device or emulator to confirm runtime behavior.
4. **Static analysis**: `./gradlew lint ktlintCheck` verifies code style and common Android issues.

Successful completion of these steps confirms that every Kotlin file compiles and that core features behave as intended.

