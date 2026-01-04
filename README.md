# NosferatuLauncher

A minimalist, invisible bridge launcher designed for **Nosferatu OS**.

## Purpose

NosferatuLauncher is not a traditional Android launcher. Its sole purpose is to act as a "traffic controller" for the system's `HOME` intent. Upon boot or when the Home button is pressed, this launcher instantly redirects the user to **KOReader**, ensuring a seamless and distraction-free e-reading environment.

## Features

* **Instant Redirection**: Zero-latency jump to the KOReader activity.
* **Auto-Boot**: Automatically handles the first-run experience in Nosferatu OS.
* **Minimal Footprint**: Lightweight Java code with no UI overhead.

## Requirements

* **Android SDK**: 25 (Nougat 7.1.2) or higher.
* **Target Environment**: Nosferatu OS (or any AOSP-based system with KOReader installed).
* **Package Target**: `org.koreader.launcher`

## Build Instructions

This project uses the standard Gradle build system. You can build the APK using Android Studio or via the command line:

```bash
# Build the debug APK
./gradlew assembleDebug

# Build the release APK
./gradlew assembleRelease
