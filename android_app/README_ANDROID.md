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
