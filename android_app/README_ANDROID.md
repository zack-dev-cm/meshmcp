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

## Build Commands

Run the following from the `android_app` directory:

```bash
./gradlew assembleDebug   # build debug APK
./gradlew ktlintCheck     # run Kotlin style checks
./gradlew test            # execute unit tests
```

## Runtime Permissions

The app requests these permissions when first launched:

- `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` and `BLUETOOTH_CONNECT` – discover and connect to nearby peers over Bluetooth LE.
- `ACCESS_FINE_LOCATION` – required by Android to scan for Bluetooth devices.
- `FOREGROUND_SERVICE` – keeps the mesh service running while the app is in the background.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` – ensures the mesh service isn't stopped by aggressive power management.

## Usage

After granting permissions, the mesh service starts automatically in the foreground. Type messages or commands in the text box.

Supported chat commands:

- `/msg <peerId> <message>` – send a private message
- `/w` – list online peers
- `/hug <nickname>` – send a hug
- `/slap <nickname>` – send a slap
- `/clear` – clear displayed messages
- `/block <nickname>` – block a peer
- `/unblock <nickname>` – unblock a peer

See the [Privacy Policy](../PRIVACY_POLICY.md) for details on how the app keeps your data local.
