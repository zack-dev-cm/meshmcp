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

- `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` and `BLUETOOTH_CONNECT` – allow peering with nearby devices over Bluetooth LE.
- `ACCESS_FINE_LOCATION` – required by Android to perform Bluetooth device discovery.
- `FOREGROUND_SERVICE` – keeps the mesh service running while the app is in the background.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` – ensures the service stays alive for reliable message relaying.

These permissions are requested on launch to start the mesh service automatically.

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
